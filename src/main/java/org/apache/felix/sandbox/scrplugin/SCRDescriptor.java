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
package org.apache.felix.sandbox.scrplugin;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.sandbox.scrplugin.tags.JavaClassDescription;
import org.apache.felix.sandbox.scrplugin.tags.JavaMethod;
import org.apache.felix.sandbox.scrplugin.tags.JavaParameter;
import org.apache.felix.sandbox.scrplugin.tags.JavaTag;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Model: scr base descriptor
 */
public class SCRDescriptor extends AbstractDescriptorElement {

    public static final String COMPONENT = "scr.component";

    public static final String COMPONENT_NAME = "name";

    public static final String COMPONENT_LABEL = "label";

    public static final String COMPONENT_DESCRIPTION = "description";

    public static final String COMPONENT_ENABLED = "enabled";

    public static final String COMPONENT_FACTORY = "factory";

    public static final String COMPONENT_IMMEDIATE = "immediate";

    public static final String COMPONENT_INHERIT = "inherit";

    public static final String COMPONENT_METATYPE = "metatype";

    public static final String COMPONENT_ABSTRACT = "abstract";

    public static final String PROPERTY = "scr.property";

    public static final String PROPERTY_NAME = "name";

    public static final String PROPERTY_LABEL = "label";

    public static final String PROPERTY_DESCRIPTION = "description";

    public static final String PROPERTY_VALUE = "value";

    public static final String PROPERTY_TYPE = "type";

    public static final String PROPERTY_CARDINALITY = "cardinality";

    public static final String PROPERTY_PRIVATE = "private";

    public static final String PROPERTY_OPTIONS = "options";

    public static final String SERVICE = "scr.service";

    public static final String SERVICE_INTERFACE = "interface";

    public static final String SERVICE_FACTORY = "servicefactory";

    public static final String REFERENCE = "scr.reference";

    public static final String REFERENCE_NAME = "name";

    public static final String REFERENCE_INTERFACE = "interface";

    public static final String REFERENCE_CARDINALITY = "cardinality";

    public static final String REFERENCE_POLICY = "policy";

    public static final String REFERENCE_TARGET = "target";

    public static final String REFERENCE_BIND = "bind";

    public static final String REFERENCE_UNDBIND = "unbind";

    private String name;

    private String label;

    private String description;

    private boolean enabled;

    private String factory;

    private boolean immediate;

    private String implClass;

    private boolean serviceFactory;

    private boolean hasMetaType;

    private boolean isAbstract;

    // map of properties
    private Set properties = new TreeSet();

    // list of service interfaces
    private Set services = new TreeSet();

    // list of service references
    private Set references = new TreeSet();

    SCRDescriptor(Log log, JavaTag tag) {
        super(log, tag);

        // whether metatype information is to generated for the component
        String metaType = tag.getNamedParameter(COMPONENT_METATYPE);
        this.hasMetaType = metaType == null || "yes".equalsIgnoreCase(metaType)
            || "true".equalsIgnoreCase(metaType);
        // check if this is an abstract definition
        String abstractType = tag.getNamedParameter(COMPONENT_ABSTRACT);
        this.isAbstract = (abstractType == null ? false : "yes".equalsIgnoreCase(abstractType) || "true".equalsIgnoreCase(abstractType));
    }

    boolean isAbstract() {
        return this.isAbstract;
    }

    // validates the descriptor and returns an array of issues or null
    boolean validate() throws MojoExecutionException {

        JavaClassDescription javaClass = this.tag.getJavaClassDescription();
        if (javaClass == null) {
            this.log("Tag not declared in a Java Class");
            return false;
        }

        boolean valid = true;

        // if the service is abstract, we do not validate everything
        if ( !this.isAbstract ) {
            // ensure non-abstract, public class
            if (!javaClass.isPublic()) {
                this.log("Class must be public");
                valid = false;
            }
            if (javaClass.isAbstract() || javaClass.isInterface()) {
                this.log("Class must be concrete class (not abstract or interface)");
                valid = false;
            }

            // check activate and deactivate methods
            this.checkActivationMethod(javaClass, "activate");
            this.checkActivationMethod(javaClass, "deactivate");

            // ensure public default constructor
            boolean constructorFound = true;
            JavaMethod[] methods = javaClass.getMethods();
            for (int i = 0; methods != null && i < methods.length; i++) {
                if (methods[i].isConstructor()) {
                    // if public default, succeed
                    if (methods[i].isPublic()
                        && (methods[i].getParameters() == null || methods[i].getParameters().length == 0)) {
                        constructorFound = true;
                        break;
                    }

                    // non-public/non-default constructor found, must have explicit
                    constructorFound = false;
                }
            }
            if (!constructorFound) {
                this.log("Class must have public default constructor");
                valid = false;
            }

            // verify properties
            for (Iterator pi = this.getProperties(); pi.hasNext();) {
                Property prop = (Property) pi.next();
                valid &= prop.validate();
            }

            // verify services
            for (Iterator si = this.getServices(); si.hasNext();) {
                Service service = (Service) si.next();
                valid &= service.validate();
            }

            // serviceFactory must not be true for immediate of component factory
            if (this.isServiceFactory() && this.isImmediate() && this.getFactory() != null) {
                this.log("Component must not be a ServiceFactory, if immediate and/or component factory");
                valid = false;
            }

            // verify references
            for (Iterator ri = this.getReferences(); ri.hasNext();) {
                Reference ref = (Reference) ri.next();
                valid &= ref.validate();
            }
        }
        return valid;
    }

    private void checkActivationMethod(JavaClassDescription javaClass, String methodName) {
        JavaMethod[] methods = javaClass.getMethods();
        JavaMethod activation = null;
        for (int i=0; i < methods.length; i++) {
            // ignore method not matching the name
            if (!methodName.equals(methods[i].getName())) {
                continue;
            }

            // if the method has the correct parameter type, check protected
            JavaParameter[] params = methods[i].getParameters();
            if (params == null || params.length != 1) {
                continue;
            }

            // this might be considered, if it is an overload, drop out of check
            if (activation != null) {
                return;
            }

            // consider this method for further checks
            activation = methods[i];
        }

        // no activation method found
        if (activation == null) {
            return;
        }

        // check protected
        if (activation.isPublic()) {
            this.warn("Activation method " + activation.getName() + " should be declared protected");
        } else if (!activation.isProtected()) {
            this.warn("Activation method " + activation.getName() + " has wrong qualifier, public or protected required");
        }

        // check paramter (we know there is exactly one)
        JavaParameter param = activation.getParameters()[0];
        if (!"org.osgi.service.component.ComponentContext".equals(param.getType())) {
            this.warn("Activation method " + methodName + " has wrong argument type " + param.getType());
        }
    }

    void generate(XMLWriter xw) {
        xw.printElementStart("component", true);
        xw.printAttribute("name", this.getName());
        if (!this.isEnabled())
            xw.printAttribute("enabled", String.valueOf(this.isEnabled()));
        if (this.getFactory() != null) xw.printAttribute("factory", this.getFactory());
        xw.printAttribute("immediate", String.valueOf(this.isImmediate()));
        xw.printElementStartClose(false);

        xw.indent();

        xw.printElementStart("implementation", true);
        xw.printAttribute("class", this.getImplClass());
        xw.printElementStartClose(true);

        // properties
        for (Iterator pi = this.getProperties(); pi.hasNext();) {
            ((Property) pi.next()).generate(xw);
        }

        // services
        Iterator si = this.getServices();
        if (si.hasNext()) {
            xw.printElementStart("service", this.isServiceFactory());
            if (this.isServiceFactory()) {
                xw.printAttribute("servicefactory", "true");
                xw.printElementStartClose(false);
            }
            xw.indent();
            while (si.hasNext()) {
                ((Service) si.next()).generate(xw);
            }
            xw.outdent();
            xw.printElementEnd("service");
        }

        // references
        for (Iterator ri = this.getReferences(); ri.hasNext();) {
            ((Reference) ri.next()).generate(xw);
        }

        xw.outdent();
        xw.printElementEnd("component");
    }

    void generateMetaTypeInfo(XMLWriter xw) {

        // if there is no metatype, we return early
        if (!this.hasMetaType) {
            return;
        }

        xw.printElementStart("OCD", true);
        xw.printAttribute("id", this.getName());

        if (this.getLabel() != null) {
            xw.printAttribute("name", this.getLabel());
        } else {
            // use the name as a localizable key by default
            xw.printAttribute("name", "%" + this.getName() + ".name");
        }

        if (this.getDescription() != null) {
            xw.printAttribute("description", this.getDescription());
        } else {
            // use the name as a localizable key by default
            xw.printAttribute("description", "%" + this.getName() + ".description");
        }

        xw.printElementStartClose(false);

        // properties
        for (Iterator pi = this.getProperties(); pi.hasNext();) {
            ((Property) pi.next()).generateMetaTypeInfo(xw);
        }

        xw.printElementEnd("OCD");

        xw.printElementStart("Designate", true);
        xw.printAttribute("pid", this.getName());
        xw.printElementStartClose(false);

        xw.printElementStart("Object", true);
        xw.printAttribute("ocdref", this.getName());
        xw.printElementStartClose(true);

        xw.printElementEnd("Designate");
    }

    // ---------- Setters and Getters for the properties -----------------------

    public boolean isEnabled() {
        return this.enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFactory() {
        return this.factory;
    }

    void setFactory(String factory) {
        this.factory = factory;
    }

    public boolean isImmediate() {
        return this.immediate;
    }

    void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    public String getImplClass() {
        return this.implClass;
    }

    void setImplClass(String implClass) {
        this.implClass = implClass;
    }

    public String getName() {
        return this.name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getLabel() {
        return this.label;
    }

    void setLabel(String label) {
        this.label = label;
    }

    String getDescription() {
        return this.description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    public Iterator getProperties() {
        return this.properties.iterator();
    }

    void addProperty(Property property) {
        this.properties.add(property);
    }

    public Iterator getReferences() {
        return this.references.iterator();
    }

    void addReference(Reference reference) {
        this.references.add(reference);
    }

    public boolean isServiceFactory() {
        return this.serviceFactory;
    }

    void setServiceFactory(boolean serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public Iterator getServices() {
        return this.services.iterator();
    }

    void addService(Service service) {
        this.services.add(service);
    }
}
