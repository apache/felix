/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scrplugin.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.Project;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.description.AbstractDescription;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.ReferenceCardinality;
import org.apache.felix.scrplugin.description.ReferenceDescription;
import org.apache.felix.scrplugin.description.ReferencePolicy;
import org.apache.felix.scrplugin.description.ReferencePolicyOption;
import org.apache.felix.scrplugin.description.ReferenceStrategy;
import org.apache.felix.scrplugin.description.ServiceDescription;

public class Validator {

    private final ComponentContainer container;

    private final Options options;

    private final Project project;

    private final IssueLog iLog;

    public Validator(final ComponentContainer container,
                    final Project project,
                    final Options options,
                    final IssueLog iLog) {
        this.container = container;
        this.project = project;
        this.options = options;
        this.iLog = iLog;
    }

    private void logWarn(final AbstractDescription desc, final String message) {
        // check if location of description is the same as the class
        final String classLocation = this.container.getComponentDescription().getSource();
        if ( classLocation.equals(desc.getSource()) ) {
            iLog.addWarning(desc.getIdentifier() + " : " + message, desc.getSource());
        } else {
            iLog.addWarning(desc.getIdentifier() + " (" + desc.getSource() + ") : " + message, classLocation);
        }
    }

    private void logError(final AbstractDescription desc, final String message) {
        // check if location of description is the same as the class
        final String classLocation = this.container.getComponentDescription().getSource();
        if ( classLocation.equals(desc.getSource()) ) {
            iLog.addError(desc.getIdentifier() + " : " + message, desc.getSource());
        } else {
            iLog.addError(desc.getIdentifier() + " (" + desc.getSource() + ") : " + message, classLocation);
        }
    }

    /**
     * Validate the component description. If errors occur a message is added to
     * the issues list, warnings can be added to the warnings list.
     */
    public void validate()
    throws SCRDescriptorException {
        final ComponentDescription component = this.container.getComponentDescription();

        // nothing to check if this is ignored
        if (!component.isCreateDs()) {
            return;
        }

        final int currentIssueCount = iLog.getNumberOfErrors();

        // if the component is abstract, we do not validate everything
        if (!component.isAbstract()) {
            // if configuration pid is set and different from name, we need 1.2
            if ( component.getConfigurationPid() != null && !component.getConfigurationPid().equals(component.getName())
                 && options.getSpecVersion().ordinal() < SpecVersion.VERSION_1_2.ordinal() ) {
                this.logError(component, "Different configuration pid requires "
                                + SpecVersion.VERSION_1_2.getName() + " or higher.");
            }

            // ensure non-abstract, public class
            if (!Modifier.isPublic(this.container.getClassDescription().getDescribedClass().getModifiers())) {
                this.logError(component, "Class must be public: "
                                + this.container.getClassDescription().getDescribedClass().getName());
            }
            if (Modifier.isAbstract(this.container.getClassDescription().getDescribedClass().getModifiers())
                            || this.container.getClassDescription().getDescribedClass().isInterface()) {
                this.logError(component, "Class must be concrete class (not abstract or interface) : "
                                + this.container.getClassDescription().getDescribedClass().getName());
            }

            // no errors so far, let's continue
            if (iLog.getNumberOfErrors() == currentIssueCount) {

                final String activateName = component.getActivate() == null ? "activate" : component.getActivate();
                final String deactivateName = component.getDeactivate() == null ? "deactivate" : component.getDeactivate();

                // check activate and deactivate methods
                this.checkLifecycleMethod(activateName, true, component.getActivate() != null);
                this.checkLifecycleMethod(deactivateName, false, component.getDeactivate() != null);

                if (component.getModified() != null) {
                    if ( this.options.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                        this.checkLifecycleMethod(component.getModified(), true, true);
                    } else {
                        this.logError(component, "If modified version is specified, spec version must be " +
                            SpecVersion.VERSION_1_1.name() + " or higher : " + component.getModified());
                    }
                }

                // ensure public default constructor
                boolean constructorFound = true;
                Constructor<?>[] constructors = this.container.getClassDescription().getDescribedClass().getDeclaredConstructors();
                for (int i = 0; constructors != null && i < constructors.length; i++) {
                    // if public default, succeed
                    if (Modifier.isPublic(constructors[i].getModifiers())
                        && (constructors[i].getParameterTypes() == null || constructors[i].getParameterTypes().length == 0)) {
                        constructorFound = true;
                        break;
                    }

                    // non-public/non-default constructor found, must have
                    // explicit
                    constructorFound = false;
                }

                if (!constructorFound) {
                    this.logError(component, "Class must have public default constructor: " + this.container.getClassDescription().getDescribedClass().getName());
                }

                // verify properties
                for (final PropertyDescription prop : this.container.getProperties().values()) {
                    this.validateProperty(prop);
                }

                // verify service
                boolean isServiceFactory = false;
                if (this.container.getServiceDescription() != null) {
                    if (this.container.getServiceDescription().getInterfaces().size() == 0) {
                        this.logError(component, "Service interface information is missing!");
                    }
                    this.validateService(this.container.getServiceDescription());
                    isServiceFactory = this.container.getServiceDescription().isServiceFactory();
                }

                // serviceFactory must not be true for immediate of component factory
                if (isServiceFactory && component.getImmediate() != null && component.getImmediate().booleanValue()
                    && component.getFactory() != null) {
                    this.logError(component,
                        "Component must not be a ServiceFactory, if immediate and/or component factory: "
                        + this.container.getClassDescription().getDescribedClass().getName());
                }

                // immediate must not be true for component factory
                if (component.getImmediate() != null && component.getImmediate().booleanValue() && component.getFactory() != null) {
                    this.logError(component,
                        "Component must not be immediate if component factory: " + this.container.getClassDescription().getDescribedClass().getName());
                }
            }

            // additional check for metatype (FELIX-4035)
            if ( this.container.getMetatypeContainer() != null ) {
                if ( this.container.getMetatypeContainer().getProperties().size() == 0 ) {
                    this.logError(component, "Component is defined to generate metatype information, however no properties have been " +
                        "defined; in case no properties are wanted, consider to use 'metatype=false'");
                }
            }
            if (iLog.getNumberOfErrors() == currentIssueCount) {
                // verify references
                for (final ReferenceDescription ref : this.container.getReferences().values()) {
                    this.validateReference(ref, component.isAbstract());
                }
            }
        }
    }

    private static final String TYPE_COMPONENT_CONTEXT = "org.osgi.service.component.ComponentContext";
    private static final String TYPE_BUNDLE_CONTEXT = "org.osgi.framework.BundleContext";
    private static final String TYPE_MAP = "java.util.Map";
    private static final String TYPE_INT = "int";
    private static final String TYPE_INTEGER = "java.lang.Integer";

    private static Method getMethod(final Project project,
            final ComponentContainer container,
            final String name, final String[] sig)
    throws SCRDescriptorException {
        Class<?>[] classSig = (sig == null ? null : new Class<?>[sig.length]);
        if ( sig != null ) {
            for(int i = 0; i<sig.length; i++) {
                try {
                    if ( sig[i].equals("int") ) {
                        classSig[i] = int.class;
                    } else {
                        classSig[i] = project.getClassLoader().loadClass(sig[i]);
                    }
                } catch (final ClassNotFoundException e) {
                    throw new SCRDescriptorException("Unable to load class.", e);
                }
            }
        }
        return getMethod(container.getClassDescription(), name, classSig);
    }

    /**
     * Find a lifecycle methods.
     *
     * @param methodName
     *            The method name.
     * @param isActivate Whether this is the activate or deactivate method.
     */
    public static MethodResult findLifecycleMethod(
                                      final Project project,
                                      final ComponentContainer container,
                                      final String methodName,
                                      final boolean isActivate)
    throws SCRDescriptorException {
        final MethodResult result = new MethodResult();
        result.requiredSpecVersion = SpecVersion.VERSION_1_0;

        // first candidate is (de)activate(ComponentContext)
        result.method = getMethod(project, container, methodName, new String[] { TYPE_COMPONENT_CONTEXT });
        if (result.method == null) {
            // Spec 1.1 or higher required
            result.requiredSpecVersion = SpecVersion.VERSION_1_1;
            // second candidate is (de)activate(BundleContext)
            result.method = getMethod(project, container, methodName, new String[] { TYPE_BUNDLE_CONTEXT });
            if (result.method == null) {
                // third candidate is (de)activate(Map)
                result.method = getMethod(project, container, methodName, new String[] { TYPE_MAP });

                if (result.method == null) {
                    // if this is a deactivate method, we have two
                    // additional possibilities
                    // a method with parameter of type int and one of type
                    // Integer
                    if (!isActivate) {
                        result.method = getMethod(project, container, methodName, new String[] { TYPE_INT });
                        if (result.method == null) {
                            result.method = getMethod(project, container, methodName, new String[] { TYPE_INTEGER });
                        }
                    }
                }

                if (result.method == null) {
                    // fourth candidate is (de)activate with two or three
                    // arguments (type must be BundleContext, ComponentCtx
                    // and Map)
                    // as we have to iterate now and the fifth candidate is
                    // zero arguments
                    // we already store this option
                    Method zeroArgMethod = null;
                    Method found = result.method;
                    final Method[] methods = container.getClassDescription().getDescribedClass().getDeclaredMethods();
                    int i = 0;
                    while (i < methods.length) {
                        if (methodName.equals(methods[i].getName())) {

                            if (methods[i].getParameterTypes() == null || methods[i].getParameterTypes().length == 0) {
                                zeroArgMethod = methods[i];
                            } else if (methods[i].getParameterTypes().length >= 2) {
                                boolean valid = true;
                                for (int m = 0; m < methods[i].getParameterTypes().length; m++) {
                                    final String type = methods[i].getParameterTypes()[m].getName();
                                    if (!type.equals(TYPE_BUNDLE_CONTEXT) && !type.equals(TYPE_COMPONENT_CONTEXT)
                                        && !type.equals(TYPE_MAP)) {
                                        // if this is deactivate, int and
                                        // integer are possible as well
                                        if (isActivate || (!type.equals(TYPE_INT) && !type.equals(TYPE_INTEGER))) {
                                            valid = false;
                                        }
                                    }
                                }
                                if (valid) {
                                    if (found == null) {
                                        found = methods[i];
                                    } else {
                                        // print warning
                                        result.additionalWarning = "Lifecycle method " + methods[i].getName()
                                                  + " occurs several times with different matching signature.";
                                    }
                                }
                            }
                        }
                        i++;
                    }
                    if (found != null) {
                        result.method = found;
                    } else {
                        result.method = zeroArgMethod;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Check for existence of lifecycle methods.
     *
     * @param methodName
     *            The method name.
     * @param isActivate Whether this is the activate or deactivate method.
     * @param isSpecified Whether this method has explicitely been specified or is just
     *                    the default
     */
    private void checkLifecycleMethod(final String methodName,
                                      final boolean isActivate,
                                      final boolean isSpecified)
    throws SCRDescriptorException {
        final MethodResult result = findLifecycleMethod(this.project, this.container, methodName, isActivate);
        if ( result.additionalWarning != null ) {
            this.logWarn(this.container.getComponentDescription(), result.additionalWarning);
        }

        // if no method is found, we check for any method with that name to print some warnings or errors!
        if (result.method == null) {
           final Method[] methods = this.container.getClassDescription().getDescribedClass().getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methodName.equals(methods[i].getName())) {
                    if (methods[i].getParameterTypes() == null || methods[i].getParameterTypes().length != 1) {
                        final String msg = "Lifecycle method " + methods[i].getName() + " has wrong number of arguments";
                        if ( isSpecified ) {
                            this.logError(container.getComponentDescription(), msg);
                        } else {
                            this.logWarn(container.getComponentDescription(), msg);
                        }
                    } else {
                        final String msg = "Lifecycle method " + methods[i].getName() + " has wrong argument "
                                + methods[i].getParameterTypes()[0].getName();
                        if ( isSpecified ) {
                            this.logError(container.getComponentDescription(), msg);
                        } else {
                            this.logWarn(container.getComponentDescription(), msg);
                        }
                    }
                }
            }
        }

        // method must be protected for version 1.0
        if (result.method != null && options.getSpecVersion() == SpecVersion.VERSION_1_0) {
            // check protected
            if (Modifier.isPublic(result.method.getModifiers())) {
                this.logWarn(container.getComponentDescription(), "Lifecycle method " + result.method.getName() + " should be declared protected");
            } else if (!Modifier.isProtected(result.method.getModifiers())) {
                this.logWarn(container.getComponentDescription(), "Lifecycle method " + result.method.getName() +
                            " has wrong qualifier, public or protected required");
            }
        }
    }

    /**
     * Validate the service and its interfaces
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    private void validateService(final ServiceDescription service) throws SCRDescriptorException {
        for (final String interfaceName : service.getInterfaces()) {
            if (this.container.getClassDescription().getDescribedClass().isInterface()) {
                this.logError(service, "Must be declared in a Java class - not an interface");
            } else {
                try {
                    final Class<?> interfaceClass = project.getClassLoader().loadClass(interfaceName);
                    if (!interfaceClass.isAssignableFrom(this.container.getClassDescription().getDescribedClass())) {
                        // interface not implemented
                        this.logError(service, "Class must implement provided interface " + interfaceName);
                    }
                } catch (final ClassNotFoundException cnfe) {
                    throw new SCRDescriptorException("Unable to load interface class.", cnfe);
                }
            }
        }
    }

    /**
     * Validate the property.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    private void validateProperty(final PropertyDescription property) {
        if (property.getName() == null || property.getName().trim().length() == 0) {
            this.logError(property, "Property name can not be empty.");
        }
        if (property.getType() != null) {
            // now check for old and new char
            if (this.options.getSpecVersion() == SpecVersion.VERSION_1_0 && property.getType() == PropertyType.Character) {
                property.setType(PropertyType.Char);
            }
            if (this.options.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal()
                            && property.getType() == PropertyType.Char) {
                property.setType(PropertyType.Character);
            }
            // check character property value
            if ( property.getType() == PropertyType.Char || property.getType() == PropertyType.Character ) {
                if ( property.getValue() != null ) {
                    if ( property.getValue().length() != 1 ) {
                        this.logError(property, "Value is not a character: " + property.getValue());
                    }
                }
                if ( property.getMultiValue() != null ) {
                    for(final String value : property.getMultiValue() ) {
                        if ( value.length() != 1 ) {
                            this.logError(property, "Value is not a character: " + value);
                        }
                    }
                }
            }
        }
        // TODO might want to check value
    }

    /**
     * Validate the reference.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    private void validateReference(final ReferenceDescription ref, final boolean componentIsAbstract)
    throws SCRDescriptorException {
        final int currentIssueCount = iLog.getNumberOfErrors();

        // validate name
        if (StringUtils.isEmpty(ref.getName())) {
            if (this.options.getSpecVersion().ordinal() < SpecVersion.VERSION_1_1.ordinal() ) {
                this.logError(ref, "Reference has no name");
            }
        }

        // validate interface
        if (StringUtils.isEmpty(ref.getInterfaceName())) {
            this.logError(ref, "Missing interface name");
        } else {
            try {
                this.project.getClassLoader().loadClass(ref.getInterfaceName());
            } catch (final ClassNotFoundException e) {
                this.logError(ref, "Interface class can't be loaded: " + ref.getInterfaceName());
            }
        }

        // validate cardinality
        if (ref.getCardinality() == null) {
            ref.setCardinality(ReferenceCardinality.MANDATORY_UNARY);
        }

        // validate policy
        if (ref.getPolicy() == null) {
            ref.setPolicy(ReferencePolicy.STATIC);
        }

        // validate policy option
        if ( ref.getPolicyOption() == null ) {
            ref.setPolicyOption(ReferencePolicyOption.RELUCTANT);
        }
        if ( ref.getPolicyOption() != ReferencePolicyOption.RELUCTANT ) {
            if ( this.options.getSpecVersion().ordinal() < SpecVersion.VERSION_1_2.ordinal() ) {
                this.logError(ref, "ReferencePolicyOption " + ref.getPolicyOption().name() +
                                " requires spec version " + SpecVersion.VERSION_1_2.getName() + " or higher.");
            }
        }
        // validate strategy
        if (ref.getStrategy() == null) {
            ref.setStrategy(ReferenceStrategy.EVENT);
        }

        // validate methods only if interface name is set
        if (!StringUtils.isEmpty(ref.getInterfaceName())) {
            // validate bind and unbind methods
            if (ref.getStrategy() != ReferenceStrategy.LOOKUP) {
                String bindName = ref.getBind();
                String unbindName = ref.getUnbind();

                final boolean canGenerate = this.options.isGenerateAccessors() &&
                                ref.getField() != null
                                && (ref.getCardinality() == ReferenceCardinality.OPTIONAL_UNARY || ref.getCardinality() == ReferenceCardinality.MANDATORY_UNARY);
                if (bindName == null && !canGenerate ) {
                    bindName = "bind";
                }
                if (unbindName == null && !canGenerate ) {
                    unbindName = "unbind";
                }

                if ( bindName != null ) {
                    bindName = this.validateMethod(ref, bindName, componentIsAbstract);
                    if ( bindName == null && ref.getField() != null ) {
                        this.logError(ref, "Something went wrong: " + canGenerate + " - " + this.options.isGenerateAccessors() + " - " + ref.getCardinality());
                    }
                } else {
                    bindName = "bind" + Character.toUpperCase(ref.getName().charAt(0)) + ref.getName().substring(1);
                }
                if ( unbindName != null ) {
                    if ( "-".equals(unbindName) )
                    {
                        unbindName = null;
                    } else {
                        unbindName = this.validateMethod(ref, unbindName, componentIsAbstract);
                    }
                } else {
                    unbindName = "unbind" + Character.toUpperCase(ref.getName().charAt(0)) + ref.getName().substring(1);
                }

                // check for volatile on dynamic field reference with cardinality unary
                if ( !this.options.isSkipVolatileCheck() ) {
                    if ( ref.getField() != null
                         && (ref.getCardinality() == ReferenceCardinality.OPTIONAL_UNARY || ref.getCardinality() == ReferenceCardinality.MANDATORY_UNARY)
                         && ref.getPolicy() == ReferencePolicy.DYNAMIC ) {
                        final boolean fieldIsVolatile = Modifier.isVolatile(ref.getField().getModifiers());

                        if ( ref.isBindMethodCreated() || ref.isUnbindMethodCreated() ) {
                            // field must be volatile
                            if (!fieldIsVolatile) {
                                this.logError(ref, "Dynamic field must be declared volatile for unary references");
                            }
                        }
                    }
                }

                if (iLog.getNumberOfErrors() == currentIssueCount) {
                    ref.setBind(bindName);
                    ref.setUnbind(unbindName);
                }

            } else {
                ref.setBind(null);
                ref.setUnbind(null);
            }

            // validate updated method
            if (ref.getUpdated() != null) {
                if (this.options.getSpecVersion().ordinal() < SpecVersion.VERSION_1_1_FELIX.ordinal()) {
                    this.logError(ref, "Updated method declaration requires version "
                                    + SpecVersion.VERSION_1_1_FELIX.getName() + ", " + SpecVersion.VERSION_1_2.getName() + " or newer");
                }
                this.validateMethod(ref, ref.getUpdated(), componentIsAbstract);
            }
        }
    }

    private String validateMethod(final ReferenceDescription ref, final String methodName, final boolean componentIsAbstract)
    throws SCRDescriptorException {
        final MethodResult result = findMethod(this.project, this.options, this.container.getClassDescription(), ref, methodName);
        if (result == null) {
            if (!componentIsAbstract) {
                this.logError(ref,
                                "Missing method " + methodName + " for reference "
                                                + (ref.getName() == null ? "" : ref.getName()));
            }
            return null;
        }

        // method needs to be protected for 1.0
        if (this.options.getSpecVersion() == SpecVersion.VERSION_1_0) {
            if (Modifier.isPublic(result.method.getModifiers())) {
                this.logWarn(ref, "Method " + result.method.getName() + " should be declared protected");
            } else if (!Modifier.isProtected(result.method.getModifiers())) {
                this.logError(ref, "Method " + result.method.getName() + " has wrong qualifier, public or protected required");
                return null;
            }
        }

        if (this.options.getSpecVersion().ordinal() < result.requiredSpecVersion.ordinal() ) {
            this.logError(ref, "Method declaration for '" + result.method.getName() + "' requires version "
                    + result.requiredSpecVersion + " or newer");
        }
        return result.method.getName();
    }

    private static final String TYPE_SERVICE_REFERENCE = "org.osgi.framework.ServiceReference";

    private static Method getMethod(final ClassDescription cd, final String name, final Class<?>[] sig) {
        Class<?> checkClass = cd.getDescribedClass();
        while ( checkClass != null ) {
            try {
                return checkClass.getDeclaredMethod(name, sig);
            } catch (final SecurityException e) {
                // ignore
            } catch (final NoSuchMethodException e) {
                // ignore
            }
            checkClass = checkClass.getSuperclass();
        }
        return null;
    }

    public static final class MethodResult {
        public Method method;
        public SpecVersion requiredSpecVersion;
        public String additionalWarning;
    }

    /**
     * Find the method and the required spec version
     * @throws SCRDescriptorException If the class can't be found
     */
    public static MethodResult findMethod(final Project project,
                    final Options options,
                    final ClassDescription cd,
                    final ReferenceDescription ref,
                    final String methodName)
    throws SCRDescriptorException {
        if ( "-".equals(methodName) ) {
            return null;
        }

        SpecVersion requiredVersion = SpecVersion.VERSION_1_0;
        try {
            final Class<?>[] sig = new Class<?>[] { project.getClassLoader().loadClass(TYPE_SERVICE_REFERENCE) };
            final Class<?>[] sig2 = new Class<?>[] { project.getClassLoader().loadClass(ref.getInterfaceName()) };
            final Class<?>[] sig3 = new Class<?>[] { project.getClassLoader().loadClass(ref.getInterfaceName()), Map.class };

            // service interface or ServiceReference first
            String realMethodName = methodName;
            Method method = getMethod(cd, realMethodName, sig);
            if (method == null) {
                method = getMethod(cd, realMethodName, sig2);
                if (method == null && (options.getSpecVersion() == null || options.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal()) ) {
                    method = getMethod(cd, realMethodName, sig3);
                    requiredVersion = SpecVersion.VERSION_1_1;
                }
            }

            // append reference name with service interface and ServiceReference
            if (method == null) {
                final String info;
                if (StringUtils.isEmpty(ref.getName())) {
                    final String interfaceName = ref.getInterfaceName();
                    final int pos = interfaceName.lastIndexOf('.');
                    info = interfaceName.substring(pos + 1);
                } else {
                    info = ref.getName();
                }
                realMethodName = methodName + Character.toUpperCase(info.charAt(0)) + info.substring(1);

                method = getMethod(cd, realMethodName, sig);
            }
            if (method == null) {
                method = getMethod(cd, realMethodName, sig2);
                if (method == null && (options.getSpecVersion() == null || options.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal()) ) {
                    method = getMethod(cd, realMethodName, sig3);
                    requiredVersion = SpecVersion.VERSION_1_1;
                }
            }

            // append type name with service interface and ServiceReference
            if (method == null) {
                int lastDot = ref.getInterfaceName().lastIndexOf('.');
                realMethodName = methodName + ref.getInterfaceName().substring(lastDot + 1);
                method = getMethod(cd, realMethodName, sig);
            }
            if (method == null) {
                method = getMethod(cd, realMethodName, sig2);
                if (method == null && (options.getSpecVersion() == null || options.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal()) ) {
                    method = getMethod(cd, realMethodName, sig3);
                    requiredVersion = SpecVersion.VERSION_1_1;
                }
            }

            if ( method == null ) {
                return null;
            }
            final MethodResult result = new MethodResult();
            result.method = method;
            result.requiredSpecVersion = requiredVersion;

            return result;
        } catch (final ClassNotFoundException cnfe) {
            throw new SCRDescriptorException("Unable to load class!", cnfe);
        }
    }
}
