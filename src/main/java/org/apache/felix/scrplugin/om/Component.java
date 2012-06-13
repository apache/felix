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
package org.apache.felix.scrplugin.om;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.annotations.ScannedAnnotation;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;
import org.apache.felix.scrplugin.description.SpecVersion;

/**
 * <code>Component</code> is a described component.
 *
 */
public class Component extends AbstractObject {

    /** The name of the component. */
    protected String name;

    /** Is this component enabled? */
    protected Boolean enabled;

    /** Is this component immediately started. */
    protected Boolean immediate;

    /** The factory. */
    protected String factory;

    /** All properties. */
    protected List<Property> properties = new ArrayList<Property>();

    /** The corresponding service. */
    protected Service service;

    /** The references. */
    protected List<Reference> references = new ArrayList<Reference>();

    /** Is this an abstract description? */
    protected boolean isAbstract;

    /** Is this a descriptor to be ignored ? */
    protected boolean isDs;

    /** Configuration policy. (V1.1) */
    protected ComponentConfigurationPolicy configurationPolicy;

    /** Activation method. (V1.1) */
    protected String activate;

    /** Deactivation method. (V1.1) */
    protected String deactivate;

    /** Modified method. (V1.1) */
    protected String modified;

    /** The spec version. */
    protected SpecVersion specVersion;

    /** The class description. */
    private final ClassDescription classDescription;

    /**
     * Constructor from java source.
     */
    public Component(final ClassDescription cDesc, final ScannedAnnotation annotation, final String sourceLocation) {
        super(annotation, sourceLocation);
        this.classDescription = cDesc;
    }

    public ClassDescription getClassDescription() {
        return this.classDescription;
    }

    /**
     * Get the spec version.
     */
    public SpecVersion getSpecVersion() {
        return this.specVersion;
    }

    /**
     * Set the spec version.
     */
    public void setSpecVersion(final SpecVersion value) {
        // only set a higher version, never "downgrade"
        if (this.specVersion == null || this.specVersion.ordinal() < value.ordinal()) {
            this.specVersion = value;
        }
    }

    /**
     * @return All properties of this component.
     */
    public List<Property> getProperties() {
        return this.properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public void addProperty(Property property) {
        this.properties.add(property);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFactory() {
        return this.factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isImmediate() {
        return this.immediate;
    }

    public void setImmediate(Boolean immediate) {
        this.immediate = immediate;
    }

    public Service getService() {
        return this.service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public List<Reference> getReferences() {
        return this.references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    public void addReference(Reference ref) {
        this.references.add(ref);
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isDs() {
        return isDs;
    }

    public void setDs(boolean isDs) {
        this.isDs = isDs;
    }

    /**
     * Get the name of the activate method (or null for default)
     */
    public String getActivate() {
        return this.activate;
    }

    /**
     * Set the name of the deactivate method (or null for default)
     */
    public void setDeactivate(final String value) {
        this.deactivate = value;
    }

    /**
     * Get the name of the deactivate method (or null for default)
     */
    public String getDeactivate() {
        return this.deactivate;
    }

    /**
     * Set the name of the activate method (or null for default)
     */
    public void setActivate(final String value) {
        this.activate = value;
    }

    /**
     * Set the name of the modified method (or null for default)
     */
    public void setModified(final String value) {
        this.modified = value;
    }

    /**
     * Get the name of the modified method (or null for default)
     */
    public String getModified() {
        return this.modified;
    }

    /**
     * Validate the component description. If errors occur a message is added to
     * the issues list, warnings can be added to the warnings list.
     */
    public void validate(final Context context) throws SCRDescriptorException {
        // nothing to check if this is ignored
        if (!isDs()) {
            return;
        }

        final int currentIssueCount = context.getIssueLog().getNumberOfErrors();

        // if the service is abstract, we do not validate everything
        if (!this.isAbstract) {
            // ensure non-abstract, public class
            if (!Modifier.isPublic(context.getClassDescription().getDescribedClass().getModifiers())) {
                this.logError(context.getIssueLog(), "Class must be public: "
                                + context.getClassDescription().getDescribedClass().getName());
            }
            if (Modifier.isAbstract(context.getClassDescription().getDescribedClass().getModifiers())
                            || context.getClassDescription().getDescribedClass().isInterface()) {
                this.logError(context.getIssueLog(), "Class must be concrete class (not abstract or interface) : "
                                + context.getClassDescription().getDescribedClass().getName());
            }

            // no errors so far, let's continue
            if (context.getIssueLog().getNumberOfErrors() == currentIssueCount) {

                final String activateName = this.activate == null ? "activate" : this.activate;
                final String deactivateName = this.deactivate == null ? "deactivate" : this.deactivate;

                // check activate and deactivate methods
                this.checkLifecycleMethod(context, activateName, true);
                this.checkLifecycleMethod(context, deactivateName, false);

                if (this.modified != null) {
                    if ( context.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                        this.checkLifecycleMethod(context, this.modified, true);
                    } else {
                        this.logError(context.getIssueLog(), "If modified version is specified, spec version must be " +
                            SpecVersion.VERSION_1_1.name() + " or higher : " + this.modified);
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
                    this.logError(context.getIssueLog(), "Class must have public default constructor: " + this.classDescription.getDescribedClass().getName());
                }

                // verify properties
                for (final Property prop : this.getProperties()) {
                    prop.validate(context);
                }

                // verify service
                boolean isServiceFactory = false;
                if (this.getService() != null) {
                    if (this.getService().getInterfaces().size() == 0) {
                        this.logError(context.getIssueLog(), "Service interface information is missing!");
                    }
                    this.getService().validate(context);
                    isServiceFactory = this.getService().isServiceFactory();
                }

                // serviceFactory must not be true for immediate of component factory
                if (isServiceFactory && this.isImmediate() != null && this.isImmediate().booleanValue()
                    && this.getFactory() != null) {
                    this.logError(context.getIssueLog(),
                        "Component must not be a ServiceFactory, if immediate and/or component factory: "
                        + this.getClassDescription().getDescribedClass().getName());
                }

                // immediate must not be true for component factory
                if (this.isImmediate() != null && this.isImmediate().booleanValue() && this.getFactory() != null) {
                    this.logError(context.getIssueLog(),
                        "Component must not be immediate if component factory: " + this.getClassDescription().getDescribedClass().getName());
                }
            }
        }
        if (context.getIssueLog().getNumberOfErrors() == currentIssueCount) {
            // verify references
            for (final Reference ref : this.getReferences()) {
                ref.validate(context, this.isAbstract);
            }
        }
    }

    private static final String TYPE_COMPONENT_CONTEXT = "org.osgi.service.component.ComponentContext";
    private static final String TYPE_BUNDLE_CONTEXT = "org.osgi.framework.BundleContext";
    private static final String TYPE_MAP = "java.util.Map";
    private static final String TYPE_INT = "int";
    private static final String TYPE_INTEGER = "java.lang.Integer";

    private Method getMethod(final Context ctx, final String name, final String[] sig)
    throws SCRDescriptorException {
        Class<?>[] classSig = (sig == null ? null : new Class<?>[sig.length]);
        if ( sig != null ) {
            for(int i = 0; i<sig.length; i++) {
                try {
                    if ( sig[i].equals("int") ) {
                        classSig[i] = int.class;
                    } else {
                        classSig[i] = ctx.getProject().getClassLoader().loadClass(sig[i]);
                    }
                } catch (final ClassNotFoundException e) {
                    throw new SCRDescriptorException("Unable to load class.", e);
                }
            }
        }
        try {
            return ctx.getClassDescription().getDescribedClass().getDeclaredMethod(name, classSig);
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
    private void checkLifecycleMethod(final Context ctx,
                                      final String methodName,
                                      final boolean isActivate)
    throws SCRDescriptorException {
        // first candidate is (de)activate(ComponentContext)
        Method method = this.getMethod(ctx, methodName, new String[] { TYPE_COMPONENT_CONTEXT });
        if (method == null) {
            if (ctx.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                // second candidate is (de)activate(BundleContext)
                method = this.getMethod(ctx, methodName, new String[] { TYPE_BUNDLE_CONTEXT });
                if (method == null) {
                    // third candidate is (de)activate(Map)
                    method = this.getMethod(ctx, methodName, new String[] { TYPE_MAP });

                    if (method == null) {
                        // if this is a deactivate method, we have two
                        // additional possibilities
                        // a method with parameter of type int and one of type
                        // Integer
                        if (!isActivate) {
                            method = this.getMethod(ctx, methodName, new String[] { TYPE_INT });
                            if (method == null) {
                                method = this.getMethod(ctx, methodName, new String[] { TYPE_INTEGER });
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
                        final Method[] methods = ctx.getClassDescription().getDescribedClass().getDeclaredMethods();
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
                                            this.logWarn(ctx.getIssueLog(), "Lifecycle method " + methods[i].getName()
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
           final Method[] methods = ctx.getClassDescription().getDescribedClass().getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methodName.equals(methods[i].getName())) {
                    if (methods[i].getParameterTypes() == null || methods[i].getParameterTypes().length != 1) {
                        this.logWarn(ctx.getIssueLog(), "Lifecycle method " + methods[i].getName() + " has wrong number of arguments");
                    } else {
                        this.logWarn(ctx.getIssueLog(),
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
                this.logWarn(ctx.getIssueLog(), "Lifecycle method " + method.getName() + " should be declared protected");
            } else if (!Modifier.isProtected(method.getModifiers())) {
                this.logWarn(ctx.getIssueLog(), "Lifecycle method " + method.getName() +
                            " has wrong qualifier, public or protected required");
            }
        }
    }

    /**
     * Return the configuration policy.
     */
    public ComponentConfigurationPolicy getConfigurationPolicy() {
        return this.configurationPolicy;
    }

    /**
     * Set the configuration policy.
     */
    public void setConfigurationPolicy(final ComponentConfigurationPolicy value) {
        this.configurationPolicy = value;
    }

    @Override
    public String toString() {
        return "Component " + this.name + " (" + "enabled=" + (enabled == null ? "<notset>" : enabled) + ", immediate="
                        + (immediate == null ? "<notset>" : immediate) + ", abstract=" + isAbstract + ", isDS=" + isDs
                        + (factory != null ? ", factory=" + factory : "")
                        + (configurationPolicy != null ? ", configurationPolicy=" + configurationPolicy : "")
                        + (activate != null ? ", activate=" + activate : "")
                        + (deactivate != null ? ", deactivate=" + deactivate : "")
                        + (modified != null ? ", modified=" + modified : "") + ", specVersion=" + specVersion
                        + ", service=" + service + ", properties=" + properties
                        + ", references=" + references + ")";
    }
}