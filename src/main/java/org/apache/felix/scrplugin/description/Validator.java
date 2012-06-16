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
package org.apache.felix.scrplugin.description;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.Project;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.helper.IssueLog;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Interface;
import org.apache.felix.scrplugin.om.Property;
import org.apache.felix.scrplugin.om.Reference;
import org.apache.felix.scrplugin.om.Service;

public class Validator {

    private final ClassDescription classDescription;

    private final Component component;

    private final SpecVersion specVersion;

    private final Options options;

    private final Project project;

    public Validator(final ClassDescription cd,
                    final Component component,
                    final SpecVersion specVersion,
                    final Project project,
                    final Options options) {
        this.classDescription = cd;
        this.project = project;
        this.specVersion = specVersion;
        this.component = component;
        this.options = options;
    }

    /**
     * Validate the component description. If errors occur a message is added to
     * the issues list, warnings can be added to the warnings list.
     */
    public void validate(final IssueLog iLog)
    throws SCRDescriptorException {
        // nothing to check if this is ignored
        if (!component.isDs()) {
            return;
        }

        final int currentIssueCount = iLog.getNumberOfErrors();

        // if the component is abstract, we do not validate everything
        if (!this.component.isAbstract()) {
            // if configuration pid is set and different from name, we need 1.2
            if ( this.component.getConfigurationPid() != null && !this.component.getConfigurationPid().equals(this.component.getName())
                 && this.specVersion.ordinal() < SpecVersion.VERSION_1_2.ordinal() ) {
                this.component.logError(iLog, "Different configuration pid requires "
                                + SpecVersion.VERSION_1_2.getName() + " or higher.");
            }

            // ensure non-abstract, public class
            if (!Modifier.isPublic(this.classDescription.getDescribedClass().getModifiers())) {
                this.component.logError(iLog, "Class must be public: "
                                + this.classDescription.getDescribedClass().getName());
            }
            if (Modifier.isAbstract(this.classDescription.getDescribedClass().getModifiers())
                            || this.classDescription.getDescribedClass().isInterface()) {
                this.component.logError(iLog, "Class must be concrete class (not abstract or interface) : "
                                + this.classDescription.getDescribedClass().getName());
            }

            // no errors so far, let's continue
            if (iLog.getNumberOfErrors() == currentIssueCount) {

                final String activateName = this.component.getActivate() == null ? "activate" : this.component.getActivate();
                final String deactivateName = this.component.getDeactivate() == null ? "deactivate" : this.component.getDeactivate();

                // check activate and deactivate methods
                this.checkLifecycleMethod(iLog, activateName, true);
                this.checkLifecycleMethod(iLog, deactivateName, false);

                if (this.component.getModified() != null) {
                    if ( this.specVersion.ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                        this.checkLifecycleMethod(iLog, this.component.getModified(), true);
                    } else {
                        this.component.logError(iLog, "If modified version is specified, spec version must be " +
                            SpecVersion.VERSION_1_1.name() + " or higher : " + this.component.getModified());
                    }
                }

                // ensure public default constructor
                boolean constructorFound = true;
                Constructor<?>[] constructors = this.classDescription.getDescribedClass().getDeclaredConstructors();
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
                    this.component.logError(iLog, "Class must have public default constructor: " + this.classDescription.getDescribedClass().getName());
                }

                // verify properties
                for (final Property prop : this.component.getProperties()) {
                    this.validateProperty(iLog, prop);
                }

                // verify service
                boolean isServiceFactory = false;
                if (this.component.getService() != null) {
                    if (this.component.getService().getInterfaces().size() == 0) {
                        this.component.logError(iLog, "Service interface information is missing!");
                    }
                    this.validateService(iLog, component.getService());
                    isServiceFactory = this.component.getService().isServiceFactory();
                }

                // serviceFactory must not be true for immediate of component factory
                if (isServiceFactory && this.component.isImmediate() != null && this.component.isImmediate().booleanValue()
                    && this.component.getFactory() != null) {
                    this.component.logError(iLog,
                        "Component must not be a ServiceFactory, if immediate and/or component factory: "
                        + this.classDescription.getDescribedClass().getName());
                }

                // immediate must not be true for component factory
                if (this.component.isImmediate() != null && this.component.isImmediate().booleanValue() && this.component.getFactory() != null) {
                    this.component.logError(iLog,
                        "Component must not be immediate if component factory: " + this.classDescription.getDescribedClass().getName());
                }
            }
        }
        if (iLog.getNumberOfErrors() == currentIssueCount) {
            // verify references
            for (final Reference ref : this.component.getReferences()) {
                this.validateReference(iLog, ref, this.component.isAbstract());
            }
        }
    }

    private static final String TYPE_COMPONENT_CONTEXT = "org.osgi.service.component.ComponentContext";
    private static final String TYPE_BUNDLE_CONTEXT = "org.osgi.framework.BundleContext";
    private static final String TYPE_MAP = "java.util.Map";
    private static final String TYPE_INT = "int";
    private static final String TYPE_INTEGER = "java.lang.Integer";

    private Method getMethod(final String name, final String[] sig)
    throws SCRDescriptorException {
        Class<?>[] classSig = (sig == null ? null : new Class<?>[sig.length]);
        if ( sig != null ) {
            for(int i = 0; i<sig.length; i++) {
                try {
                    if ( sig[i].equals("int") ) {
                        classSig[i] = int.class;
                    } else {
                        classSig[i] = this.project.getClassLoader().loadClass(sig[i]);
                    }
                } catch (final ClassNotFoundException e) {
                    throw new SCRDescriptorException("Unable to load class.", e);
                }
            }
        }
        try {
            return this.classDescription.getDescribedClass().getDeclaredMethod(name, classSig);
        } catch (final SecurityException e) {
            // ignore
        } catch (final NoSuchMethodException e) {
            // ignore
        }
        return null;
    }

    /**
     * Check for existence of lifecycle methods.
     *
     * @param specVersion
     *            The spec version
     * @param javaClass
     *            The java class to inspect.
     * @param methodName
     *            The method name.
     * @param warnings
     *            The list of warnings used to add new warnings.
     */
    private void checkLifecycleMethod(final IssueLog iLog,
                                      final String methodName,
                                      final boolean isActivate)
    throws SCRDescriptorException {
        // first candidate is (de)activate(ComponentContext)
        Method method = this.getMethod(methodName, new String[] { TYPE_COMPONENT_CONTEXT });
        if (method == null) {
            if (this.specVersion.ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                // second candidate is (de)activate(BundleContext)
                method = this.getMethod(methodName, new String[] { TYPE_BUNDLE_CONTEXT });
                if (method == null) {
                    // third candidate is (de)activate(Map)
                    method = this.getMethod(methodName, new String[] { TYPE_MAP });

                    if (method == null) {
                        // if this is a deactivate method, we have two
                        // additional possibilities
                        // a method with parameter of type int and one of type
                        // Integer
                        if (!isActivate) {
                            method = this.getMethod(methodName, new String[] { TYPE_INT });
                            if (method == null) {
                                method = this.getMethod(methodName, new String[] { TYPE_INTEGER });
                            }
                        }
                    }

                    if (method == null) {
                        // fourth candidate is (de)activate with two or three
                        // arguments (type must be BundleContext, ComponentCtx
                        // and Map)
                        // as we have to iterate now and the fifth candidate is
                        // zero arguments
                        // we already store this option
                        Method zeroArgMethod = null;
                        Method found = method;
                        final Method[] methods = this.classDescription.getDescribedClass().getDeclaredMethods();
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
                                            this.component.logWarn(iLog, "Lifecycle method " + methods[i].getName()
                                                      + " occurs several times with different matching signature.");
                                        }
                                    }
                                }
                            }
                            i++;
                        }
                        if (found != null) {
                            method = found;
                        } else {
                            method = zeroArgMethod;
                        }
                    }
                }
            }
        }
        // if no method is found, we check for any method with that name to print some warnings!
        if (method == null) {
           final Method[] methods = this.classDescription.getDescribedClass().getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methodName.equals(methods[i].getName())) {
                    if (methods[i].getParameterTypes() == null || methods[i].getParameterTypes().length != 1) {
                        this.component.logWarn(iLog, "Lifecycle method " + methods[i].getName() + " has wrong number of arguments");
                    } else {
                        this.component.logWarn(iLog,
                            "Lifecycle method " + methods[i].getName() + " has wrong argument "
                            + methods[i].getParameterTypes()[0].getName());
                    }
                }
            }
        }

        // method must be protected for version 1.0
        if (method != null && specVersion == SpecVersion.VERSION_1_0) {
            // check protected
            if (Modifier.isPublic(method.getModifiers())) {
                this.component.logWarn(iLog, "Lifecycle method " + method.getName() + " should be declared protected");
            } else if (!Modifier.isProtected(method.getModifiers())) {
                this.component.logWarn(iLog, "Lifecycle method " + method.getName() +
                            " has wrong qualifier, public or protected required");
            }
        }
    }

    /**
     * Validate the service.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    private void validateService(final IssueLog iLog, final Service service) throws SCRDescriptorException {
        for (final Interface interf : service.getInterfaces()) {
            this.validateInterface(iLog, interf);
        }
    }

    /**
     * Validate the interface.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    private void validateInterface(final IssueLog iLog, final Interface iFace)
    throws SCRDescriptorException {
        if (this.classDescription.getDescribedClass().isInterface()) {
            iFace.logError(iLog, "Must be declared in a Java class - not an interface");
        } else {
            try {
                final Class<?> interfaceClass = project.getClassLoader().loadClass(iFace.getInterfaceName());
                if (!interfaceClass.isAssignableFrom(this.classDescription.getDescribedClass())) {
                    // interface not implemented
                    iFace.logError(iLog, "Class must implement provided interface " + iFace.getInterfaceName());
                }
            } catch (final ClassNotFoundException cnfe) {
                throw new SCRDescriptorException("Unable to load interface class.", cnfe);
            }
        }
    }

    /**
     * Validate the property.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    private void validateProperty(final IssueLog iLog, final Property property) {
        if (property.getName() == null || property.getName().trim().length() == 0) {
            property.logError(iLog, "Property name can not be empty.");
        }
        if (property.getType() != null) {
            // now check for old and new char
            if (this.specVersion == SpecVersion.VERSION_1_0 && property.getType() == PropertyType.Character) {
                property.setType(PropertyType.Char);
            }
            if (this.specVersion.ordinal() >= SpecVersion.VERSION_1_1.ordinal()
                            && property.getType() == PropertyType.Char) {
                property.setType(PropertyType.Character);
            }
        }
        // TODO might want to check value
    }

    /**
     * Validate the reference.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    private void validateReference(final IssueLog iLog, final Reference ref, final boolean componentIsAbstract)
    throws SCRDescriptorException {
        final int currentIssueCount = iLog.getNumberOfErrors();

        // validate name
        if (StringUtils.isEmpty(ref.getName())) {
            if (this.specVersion.ordinal() < SpecVersion.VERSION_1_1.ordinal() ) {
                ref.logError(iLog, "Reference has no name");
            }
        }

        // validate interface
        if (StringUtils.isEmpty(ref.getInterfacename())) {
            ref.logError(iLog, "Missing interface name");
        }
        try {
            this.project.getClassLoader().loadClass(ref.getInterfacename());
        } catch (final ClassNotFoundException e) {
            ref.logError(iLog, "Interface class can't be loaded: " + ref.getInterfacename());
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
            if ( this.specVersion.ordinal() < SpecVersion.VERSION_1_2.ordinal() ) {
                ref.logError(iLog, "ReferencePolicyOption " + ref.getPolicyOption().name() +
                                " requires spec version " + SpecVersion.VERSION_1_2.getName() + " or higher.");
            }
        }
        // validate strategy
        if (ref.getStrategy() == null) {
            ref.setStrategy(ReferenceStrategy.EVENT);
        }

        // validate bind and unbind methods
        if (!ref.isLookupStrategy()) {
            String bindName = ref.getBind();
            String unbindName = ref.getUnbind();

            final boolean canGenerate = this.options.isGenerateAccessors() &&
                            !ref.isLookupStrategy() && ref.getField() != null
                            && (ref.getCardinality() == ReferenceCardinality.OPTIONAL_UNARY || ref.getCardinality() == ReferenceCardinality.MANDATORY_UNARY);
            if (bindName == null && !canGenerate ) {
                bindName = "bind";
            }
            if (unbindName == null && !canGenerate ) {
                unbindName = "unbind";
            }

            if ( bindName != null ) {
                bindName = this.validateMethod(iLog, ref, bindName, componentIsAbstract);
            } else {
                bindName = "bind" + Character.toUpperCase(ref.getName().charAt(0)) + ref.getName().substring(1);
            }
            if ( unbindName != null ) {
                unbindName = this.validateMethod(iLog, ref, unbindName, componentIsAbstract);
            } else {
                unbindName = "unbind" + Character.toUpperCase(ref.getName().charAt(0)) + ref.getName().substring(1);
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
            if (this.specVersion.ordinal() < SpecVersion.VERSION_1_1_FELIX.ordinal()) {
                ref.logError(iLog, "Updated method declaration requires version "
                                + SpecVersion.VERSION_1_1_FELIX.getName() + ", " + SpecVersion.VERSION_1_2.getName() + " or newer");
            }
        }

    }

    private String validateMethod(final IssueLog iLog, final Reference ref, final String methodName, final boolean componentIsAbstract)
    throws SCRDescriptorException {
        final Method method = this.findMethod(iLog, ref, methodName);
        if (method == null) {
            if (!componentIsAbstract) {
                ref.logError(iLog,
                                "Missing method " + methodName + " for reference "
                                                + (ref.getName() == null ? "" : ref.getName()));
            }
            return null;
        }

        // method needs to be protected for 1.0
        if (this.specVersion == SpecVersion.VERSION_1_0) {
            if (Modifier.isPublic(method.getModifiers())) {
                ref.logWarn(iLog, "Method " + method.getName() + " should be declared protected");
            } else if (!Modifier.isProtected(method.getModifiers())) {
                ref.logError(iLog, "Method " + method.getName() + " has wrong qualifier, public or protected required");
                return null;
            }
        }
        return method.getName();
    }

    private static final String TYPE_SERVICE_REFERENCE = "org.osgi.framework.ServiceReference";

    private Method getMethod(final String name, final Class<?>[] sig) {
        try {
            return this.classDescription.getDescribedClass().getDeclaredMethod(name, sig);
        } catch (final SecurityException e) {
            // ignore
        } catch (final NoSuchMethodException e) {
            // ignore
        }
        return null;
    }

    public Method findMethod(final IssueLog iLog, final Reference ref, final String methodName)
    throws SCRDescriptorException {
        try {
            final Class<?>[] sig = new Class<?>[] { this.project.getClassLoader().loadClass(TYPE_SERVICE_REFERENCE) };
            final Class<?>[] sig2 = new Class<?>[] { this.project.getClassLoader().loadClass(ref.getInterfacename()) };
            final Class<?>[] sig3 = new Class<?>[] { this.project.getClassLoader().loadClass(ref.getInterfacename()), Map.class };

            // service interface or ServiceReference first
            String realMethodName = methodName;
            Method method = getMethod(realMethodName, sig);
            if (method == null) {
                method = getMethod(realMethodName, sig2);
                if (method == null && this.specVersion.ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                    method = getMethod(realMethodName, sig3);
                }
            }

            // append reference name with service interface and ServiceReference
            if (method == null) {
                final String info;
                if (StringUtils.isEmpty(ref.getName())) {
                    final String interfaceName = ref.getInterfacename();
                    final int pos = interfaceName.lastIndexOf('.');
                    info = interfaceName.substring(pos + 1);
                } else {
                    info = ref.getName();
                }
                realMethodName = methodName + Character.toUpperCase(info.charAt(0)) + info.substring(1);

                method = getMethod(realMethodName, sig);
            }
            if (method == null) {
                method = getMethod(realMethodName, sig2);
                if (method == null && this.specVersion.ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                    method = getMethod(realMethodName, sig3);
                }
            }

            // append type name with service interface and ServiceReference
            if (method == null) {
                int lastDot = ref.getInterfacename().lastIndexOf('.');
                realMethodName = methodName + ref.getInterfacename().substring(lastDot + 1);
                method = getMethod(realMethodName, sig);
            }
            if (method == null) {
                method = getMethod(realMethodName, sig2);
                if (method == null && this.specVersion.ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                    method = getMethod(realMethodName, sig3);
                }
            }

            return method;
        } catch (final ClassNotFoundException cnfe) {
            throw new SCRDescriptorException("Unable to load class!", cnfe);
        }
    }
}
