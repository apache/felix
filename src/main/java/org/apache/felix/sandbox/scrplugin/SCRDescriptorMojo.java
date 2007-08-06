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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.sandbox.scrplugin.om.Component;
import org.apache.felix.sandbox.scrplugin.om.Components;
import org.apache.felix.sandbox.scrplugin.om.Implementation;
import org.apache.felix.sandbox.scrplugin.om.Interface;
import org.apache.felix.sandbox.scrplugin.tags.JavaClassDescription;
import org.apache.felix.sandbox.scrplugin.tags.JavaClassDescriptorManager;
import org.apache.felix.sandbox.scrplugin.tags.JavaField;
import org.apache.felix.sandbox.scrplugin.tags.JavaTag;
import org.apache.felix.sandbox.scrplugin.xml.ComponentDescriptorIO;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * The <code>SCRDescriptorMojo</code>
 * generates a service descriptor file based on annotations found in the sources.
 *
 * @goal scr
 * @phase generate-resources
 * @description Build Service Descriptors from Java Source
 * @requiresDependencyResolution compile
 */
public class SCRDescriptorMojo extends AbstractMojo {

    /**
     * @parameter expression="${project.build.directory}/scr-plugin-generated"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Name of the generated descriptor.
     *
     * @parameter expression="${scr.descriptor.name}" default-value="serviceComponents.xml"
     */
    private String finalName;

    public void execute() throws MojoExecutionException, MojoFailureException {
        this.getLog().debug("Starting SCRDescriptorMojo....");

        boolean hasFailures = false;

        JavaClassDescriptorManager jManager = new JavaClassDescriptorManager(this.getLog(),
                                                                             this.project);
        // iterate through all source classes and check for component tag
        final List descriptors = new ArrayList();
        final List abstractDescriptors = new ArrayList();
        final JavaClassDescription[] javaSources = jManager.getSourceDescriptions();

        // test the new om?
        boolean testNewOM = false;
        final Components components = new Components();
        final Components abstractComponents = new Components();

        for (int i = 0; i < javaSources.length; i++) {
            this.getLog().debug("Testing source " + javaSources[i].getName());
            final JavaTag tag = javaSources[i].getTagByName(SCRDescriptor.COMPONENT);
            if (tag != null) {
                this.getLog().debug("Processing service class " + javaSources[i].getName());
                final SCRDescriptor descriptor = this.createSCRDescriptor(javaSources[i]);
                if (descriptor != null) {
                    if ( descriptor.isAbstract() ) {
                        this.getLog().debug("Adding abstract descriptor " + descriptor);
                        abstractDescriptors.add(descriptor);
                    } else {
                        this.getLog().debug("Adding descriptor " + descriptor);
                        descriptors.add(descriptor);
                    }
                } else {
                    hasFailures = true;
                }
                if ( testNewOM ) {
                    final Component comp = this.createComponent(javaSources[i]);
                    if (comp != null) {
                        if ( comp.isAbstract() ) {
                            this.getLog().debug("Adding abstract descriptor " + descriptor);
                            abstractComponents.addComponent(comp);
                        } else {
                            this.getLog().debug("Adding descriptor " + descriptor);
                            components.addComponent(comp);
                        }
                    }
                }
            }
        }

        // after checking all classes, throw if there were any failures
        if (hasFailures) {
            throw new MojoFailureException("SCR Descriptor parsing had failures (see log)");
        }

        jManager.writeAbstractDescriptorsFile(abstractDescriptors, this.outputDirectory);

        // terminate if there is nothing to write
        if (descriptors.isEmpty()) {
            this.getLog().info("No SCR Descriptors found in project");
            return;
        }

        // finally the descriptors have to be written ....
        if (StringUtils.isEmpty(this.finalName)) {
            this.getLog().error("Descriptor file name must not be empty");
            return;
        }

        File descriptorFile = new File(new File(this.outputDirectory, "OSGI-INF"), this.finalName);
        descriptorFile.getParentFile().mkdirs(); // ensure parent dir

        this.getLog().info("Generating " + descriptors.size()
                + " Service Component Descriptors to " + descriptorFile);

        FileOutputStream descriptorStream = null;
        XMLWriter xw = null;
        try {
            if ( testNewOM ) {
                final ComponentDescriptorIO io = new ComponentDescriptorIO();
                io.write(descriptorFile, components);
            } else {
                descriptorStream = new FileOutputStream(descriptorFile);
                xw = new XMLWriter(descriptorStream);

                for (Iterator di=descriptors.iterator(); di.hasNext(); ) {
                    SCRDescriptor sd = (SCRDescriptor) di.next();
                    sd.generate(xw);
                }
            }

        } catch (IOException ioe) {
            hasFailures = true;
            this.getLog().error("Cannot write descriptor to " + descriptorFile, ioe);
            throw new MojoFailureException("Failed to write descriptor to " + descriptorFile);
        } finally {
            IOUtil.close(xw);
            IOUtil.close(descriptorStream);

            // remove the descriptor file in case of write failure
            if (hasFailures) {
                descriptorFile.delete();
            }
        }

        // create metatype information
        File mtFile = new File(this.outputDirectory, "OSGI-INF" + File.separator + "metatype" + File.separator + "metatype.xml");
        mtFile.getParentFile().mkdirs();

        xw = null;
        descriptorStream = null;
        try {
            descriptorStream = new FileOutputStream(mtFile);
            xw = new XMLWriter(descriptorStream);

            xw.printElementStart("MetaData", true);
            xw.printAttribute("localization", "metatype");
            xw.printElementStartClose(false);

            for (Iterator di=descriptors.iterator(); di.hasNext(); ) {
                SCRDescriptor sd = (SCRDescriptor) di.next();
                sd.generateMetaTypeInfo(xw);
            }

            xw.printElementEnd("MetaData");

        } catch (IOException ioe) {
            this.getLog().error("Cannot write meta type descriptor", ioe);
            throw new MojoFailureException("Failed to write meta type descriptor");
        } finally {
            IOUtil.close(xw);
            IOUtil.close(descriptorStream);
        }

        // now add the descriptor file to the maven resources
        final String ourRsrcPath = this.outputDirectory.getAbsolutePath();
        boolean found = false;
        final Iterator rsrcIterator = this.project.getResources().iterator();
        while ( !found && rsrcIterator.hasNext() ) {
            final Resource rsrc = (Resource)rsrcIterator.next();
            found = rsrc.getDirectory().equals(ourRsrcPath);
        }
        if ( !found ) {
            final Resource resource = new Resource();
            resource.setDirectory(this.outputDirectory.getAbsolutePath());
            this.project.addResource(resource);
        }
        // and set include accordingly
        this.project.getProperties().setProperty("Service-Component", "OSGI-INF/" + this.finalName);
    }

    private SCRDescriptor createSCRDescriptor(JavaClassDescription description)
    throws MojoExecutionException {

        final JavaTag component = description.getTagByName(SCRDescriptor.COMPONENT);
        final SCRDescriptor sd = new SCRDescriptor(this.getLog(), component);
        sd.setImplClass(description.getName());

        boolean inherited = this.getBoolean(component, SCRDescriptor.COMPONENT_INHERIT, false);

        this.doComponent(component, sd);

        boolean serviceFactory = this.doServices(description.getTagsByName(SCRDescriptor.SERVICE, inherited), sd, description);
        sd.setServiceFactory(serviceFactory);

        this.doProperties(description.getTagsByName(SCRDescriptor.PROPERTY, inherited), sd);

        this.doReferences(description.getTagsByName(SCRDescriptor.REFERENCE, inherited), sd);

        do {
            JavaField[] fields = description.getFields();
            for (int i=0; fields != null && i < fields.length; i++) {
                JavaTag tag = fields[i].getTagByName(SCRDescriptor.REFERENCE);
                if (tag != null) {
                    this.doReference(tag, fields[i].getName(), sd);
                }

                tag = fields[i].getTagByName(SCRDescriptor.PROPERTY);
                if (tag != null) {
                    this.doProperty(tag, fields[i].getInitializationExpression(), sd);
                }
            }

            description = description.getSuperClass();
        } while (inherited && description != null);

        // return nothing if validation fails
        return sd.validate() ? sd : null;
    }

    /**
     * Create a component for the java class description.
     * @param description
     * @return The generated component descriptor or null if any error occurs.
     * @throws MojoExecutionException
     */
    protected Component createComponent(JavaClassDescription description)
    throws MojoExecutionException {

        final JavaTag componentTag = description.getTagByName(SCRDescriptor.COMPONENT);
        final Component component = new Component(componentTag);

        // set implementation
        component.setImplementation(new Implementation(description.getName()));

        this.doComponent(componentTag, component);

        boolean inherited = this.getBoolean(componentTag, SCRDescriptor.COMPONENT_INHERIT, false);
        boolean serviceFactory = this.doServices(description.getTagsByName(SCRDescriptor.SERVICE, inherited), component, description);
        component.setServiceFactory(serviceFactory);

        // properties
        final JavaTag[] properties = description.getTagsByName(SCRDescriptor.PROPERTY, inherited);
        if (properties != null && properties.length > 0) {
            for (int i=0; i < properties.length; i++) {
                this.doProperty(properties[i], null, component);
            }
        }

        // references
        final JavaTag[] references = description.getTagsByName(SCRDescriptor.REFERENCE, inherited);
        if (references != null || references.length > 0) {
            for (int i=0; i < references.length; i++) {
                this.doReference(references[i], null, component);
            }
        }

        // fields
        do {
            JavaField[] fields = description.getFields();
            for (int i=0; fields != null && i < fields.length; i++) {
                JavaTag tag = fields[i].getTagByName(SCRDescriptor.REFERENCE);
                if (tag != null) {
                    this.doReference(tag, fields[i].getName(), component);
                }

                tag = fields[i].getTagByName(SCRDescriptor.PROPERTY);
                if (tag != null) {
                    this.doProperty(tag, fields[i].getInitializationExpression(), component);
                }
            }

            description = description.getSuperClass();
        } while (inherited && description != null);

        final List issues = new ArrayList();
        final List warnings = new ArrayList();
        component.validate(issues, warnings);

        // now log warnings and errors (warnings first)
        Iterator i = warnings.iterator();
        while ( i.hasNext() ) {
            this.getLog().warn((String)i.next());
        }
        i = issues.iterator();
        while ( i.hasNext() ) {
            this.getLog().error((String)i.next());
        }

        // return nothing if validation fails
        return issues.size() == 0 ? component : null;
    }

    /**
     * Fill the component object with the information from the tag.
     * @param tag
     * @param component
     */
    protected void doComponent(JavaTag tag, Component component) {

        // check if this is an abstract definition
        final String abstractType = tag.getNamedParameter(SCRDescriptor.COMPONENT_ABSTRACT);
        component.setAbstract((abstractType == null ? false : "yes".equalsIgnoreCase(abstractType) || "true".equalsIgnoreCase(abstractType)));

        String name = tag.getNamedParameter(SCRDescriptor.COMPONENT_NAME);
        component.setName(StringUtils.isEmpty(name) ? component.getImplementation().getClassame() : name);

        component.setEnabled(Boolean.valueOf(this.getBoolean(tag, SCRDescriptor.COMPONENT_ENABLED, true)));
        component.setFactory(tag.getNamedParameter(SCRDescriptor.COMPONENT_FACTORY));
        component.setImmediate(Boolean.valueOf(this.getBoolean(tag, SCRDescriptor.COMPONENT_IMMEDIATE, true)));

        // whether metatype information is to generated for the component
        final String metaType = tag.getNamedParameter(SCRDescriptor.COMPONENT_METATYPE);
        final boolean hasMetaType = metaType == null || "yes".equalsIgnoreCase(metaType)
            || "true".equalsIgnoreCase(metaType);
        component.setHasMetaType(hasMetaType);
        component.setLabel(tag.getNamedParameter(SCRDescriptor.COMPONENT_LABEL));
        component.setDescription(tag.getNamedParameter(SCRDescriptor.COMPONENT_DESCRIPTION));
    }

    /**
     * Process the service annotations
     * @param services
     * @param component
     * @param description
     * @return
     * @throws MojoExecutionException
     */
    protected boolean doServices(JavaTag[] services, Component component, JavaClassDescription description)
    throws MojoExecutionException {
        // no services, hence certainly no service factory
        if (services == null || services.length == 0) {
            return false;
        }

        org.apache.felix.sandbox.scrplugin.om.Service service = new org.apache.felix.sandbox.scrplugin.om.Service();
        component.setService(service);
        boolean serviceFactory = false;
        for (int i=0; i < services.length; i++) {
            String name = services[i].getNamedParameter(SCRDescriptor.SERVICE_INTERFACE);
            if (StringUtils.isEmpty(name)) {

                while (description != null) {
                    JavaClassDescription[] interfaces = description.getImplementedInterfaces();
                    for (int j=0; interfaces != null && j < interfaces.length; j++) {
                        final Interface interf = new Interface(services[i]);
                        interf.setInterfacename(interfaces[j].getName());
                        service.addInterface(interf);
                    }

                    // try super class
                    description = description.getSuperClass();
                }
            } else {
                final Interface interf = new Interface(services[i]);
                interf.setInterfacename(name);
                service.addInterface(interf);
            }

            serviceFactory |= this.getBoolean(services[i], SCRDescriptor.SERVICE_FACTORY, false);
        }

        return serviceFactory;
    }

    private void doComponent(JavaTag comp, SCRDescriptor sd) {
        String name = comp.getNamedParameter(SCRDescriptor.COMPONENT_NAME);
        sd.setName(StringUtils.isEmpty(name) ? sd.getImplClass() : name);

        sd.setEnabled(this.getBoolean(comp, SCRDescriptor.COMPONENT_ENABLED, true));
        sd.setFactory(comp.getNamedParameter(SCRDescriptor.COMPONENT_FACTORY));
        sd.setImmediate(this.getBoolean(comp, SCRDescriptor.COMPONENT_IMMEDIATE,
            true));

        sd.setLabel(comp.getNamedParameter(SCRDescriptor.COMPONENT_LABEL));
        sd.setDescription(comp.getNamedParameter(SCRDescriptor.COMPONENT_DESCRIPTION));
    }

    private boolean doServices(JavaTag[] services, SCRDescriptor sd, JavaClassDescription description)
    throws MojoExecutionException {
        // no services, hence certainly no service factory
        if (services == null || services.length == 0) {
            return false;
        }

        boolean serviceFactory = false;
        for (int i=0; i < services.length; i++) {
            String name = services[i].getNamedParameter(SCRDescriptor.SERVICE_INTERFACE);
            if (StringUtils.isEmpty(name)) {

                while (description != null) {
                    JavaClassDescription[] interfaces = description.getImplementedInterfaces();
                    for (int j=0; interfaces != null && j < interfaces.length; j++) {
                        Service service = new Service(this.getLog(), services[i]);
                        service.setInterfaceName(interfaces[j].getName());
                        sd.addService(service);
                    }

                    // try super class
                    description = description.getSuperClass();
                }
            } else {
                Service service = new Service(this.getLog(), services[i]);
                service.setInterfaceName(name);
                sd.addService(service);
            }

            serviceFactory |= this.getBoolean(services[i], SCRDescriptor.SERVICE_FACTORY, false);
        }

        return serviceFactory;
    }

    private void doProperties(JavaTag[] properties, SCRDescriptor sd) {
        if (properties == null || properties.length == 0) {
            return;
        }

        for (int i=0; i < properties.length; i++) {
            this.doProperty(properties[i], null, sd);
        }
    }

    private void doProperty(JavaTag property, String defaultName, SCRDescriptor sd) {
        String name = property.getNamedParameter(SCRDescriptor.PROPERTY_NAME);
        if (StringUtils.isEmpty(name) && defaultName!= null) {
            name = defaultName.trim();
            if (name.startsWith("\"")) name = name.substring(1);
            if (name.endsWith("\"")) name = name.substring(0, name.length()-1);
        }

        if (!StringUtils.isEmpty(name)) {
            Property prop = new Property(this.getLog(), property);
            prop.setName(name);
            prop.setLabel(property.getNamedParameter(SCRDescriptor.PROPERTY_LABEL));
            prop.setDescription(property.getNamedParameter(SCRDescriptor.PROPERTY_DESCRIPTION));
            prop.setValue(property.getNamedParameter(SCRDescriptor.PROPERTY_VALUE));
            prop.setType(property.getNamedParameter(SCRDescriptor.PROPERTY_TYPE));
            prop.setPrivateProperty(this.getBoolean(property,
                SCRDescriptor.PROPERTY_PRIVATE, prop.isPrivateProperty()));

            // set optional multivalues, cardinailty might be overwritten by setValues !!
            prop.setCardinality(property.getNamedParameter(SCRDescriptor.PROPERTY_CARDINALITY));
            prop.setValues(property.getNamedParameterMap());

            // check options
            String[] parameters = property.getParameters();
            Map options = null;
            for (int j=0; j < parameters.length; j++) {
                if (SCRDescriptor.PROPERTY_OPTIONS.equals(parameters[j])) {
                    options = new LinkedHashMap();
                } else if (options != null) {
                    String optionLabel = parameters[j];
                    String optionValue = (j < parameters.length-2) ? parameters[j+2] : null;
                    if (optionValue != null) {
                        options.put(optionLabel, optionValue);
                    }
                    j += 2;
                }
            }
            prop.setOptions(options);

            sd.addProperty(prop);
        }
    }

    /**
     * @param property
     * @param defaultName
     * @param component
     */
    protected void doProperty(JavaTag property, String defaultName, Component component) {
        String name = property.getNamedParameter(SCRDescriptor.PROPERTY_NAME);
        if (StringUtils.isEmpty(name) && defaultName!= null) {
            name = defaultName.trim();
            if (name.startsWith("\"")) name = name.substring(1);
            if (name.endsWith("\"")) name = name.substring(0, name.length()-1);
        }

        if (!StringUtils.isEmpty(name)) {
            org.apache.felix.sandbox.scrplugin.om.Property prop = new org.apache.felix.sandbox.scrplugin.om.Property(property);
            prop.setName(name);
            prop.setLabel(property.getNamedParameter(SCRDescriptor.PROPERTY_LABEL));
            prop.setDescription(property.getNamedParameter(SCRDescriptor.PROPERTY_DESCRIPTION));
            prop.setValue(property.getNamedParameter(SCRDescriptor.PROPERTY_VALUE));
            prop.setType(property.getNamedParameter(SCRDescriptor.PROPERTY_TYPE));
            prop.setPrivateProperty(this.getBoolean(property,
                SCRDescriptor.PROPERTY_PRIVATE, prop.isPrivateProperty()));

            // set optional multivalues, cardinality might be overwritten by setValues !!
            final String value = property.getNamedParameter(SCRDescriptor.PROPERTY_CARDINALITY);
            if (value != null) {
                if ("-".equals(value)) {
                    // unlimited vector
                    prop.setCardinality(new Integer(Integer.MIN_VALUE));
                } else if ("+".equals(value)) {
                   // unlimited array
                    prop.setCardinality(new Integer(Integer.MAX_VALUE));
                } else {
                    try {
                        prop.setCardinality(Integer.valueOf(value));
                    } catch (NumberFormatException nfe) {
                        // default to scalar in case of conversion problem
                    }
                }
            }
            prop.setValues(property.getNamedParameterMap());

            // check options
            String[] parameters = property.getParameters();
            Map options = null;
            for (int j=0; j < parameters.length; j++) {
                if (SCRDescriptor.PROPERTY_OPTIONS.equals(parameters[j])) {
                    options = new LinkedHashMap();
                } else if (options != null) {
                    String optionLabel = parameters[j];
                    String optionValue = (j < parameters.length-2) ? parameters[j+2] : null;
                    if (optionValue != null) {
                        options.put(optionLabel, optionValue);
                    }
                    j += 2;
                }
            }
            prop.setOptions(options);

            component.addProperty(prop);
        }
    }

    /**
     * @param reference
     * @param defaultName
     * @param component
     */
    protected void doReference(JavaTag reference, String defaultName, Component component) {
        String name = reference.getNamedParameter(SCRDescriptor.REFERENCE_NAME);
        if (StringUtils.isEmpty(name)) {
            name = defaultName;
        }

        // ensure interface
        String type = reference.getNamedParameter(SCRDescriptor.REFERENCE_INTERFACE);
        if (StringUtils.isEmpty(type)) {
            if ( reference.getField() != null ) {
                type = reference.getField().getType();
            }
        }

        if (!StringUtils.isEmpty(name)) {
            org.apache.felix.sandbox.scrplugin.om.Reference ref = new org.apache.felix.sandbox.scrplugin.om.Reference(reference);
            ref.setName(name);
            ref.setInterfacename(type);
            ref.setCardinality(reference.getNamedParameter(SCRDescriptor.REFERENCE_CARDINALITY));
            ref.setPolicy(reference.getNamedParameter(SCRDescriptor.REFERENCE_POLICY));
            ref.setTarget(reference.getNamedParameter(SCRDescriptor.REFERENCE_TARGET));
            String value;
            value = reference.getNamedParameter(SCRDescriptor.REFERENCE_BIND);
            if ( value != null ) {
                ref.setBind(value);
            }
            value = reference.getNamedParameter(SCRDescriptor.REFERENCE_UNDBIND);
            if ( value != null ) {
                ref.setUnbind(value);
            }
            component.addReference(ref);
        }
    }

    private void doReferences(JavaTag[] references, SCRDescriptor sd) {
        if (references == null || references.length == 0) {
            return;
        }

        for (int i=0; i < references.length; i++) {
            this.doReference(references[i], null, sd);
        }
    }

    private void doReference(JavaTag reference, String defaultName, SCRDescriptor sd) {
        String name = reference.getNamedParameter(SCRDescriptor.REFERENCE_NAME);
        if (StringUtils.isEmpty(name)) {
            name = defaultName;
        }

        // ensure interface
        String type = reference.getNamedParameter(SCRDescriptor.REFERENCE_INTERFACE);
        if (StringUtils.isEmpty(type)) {
            if ( reference.getField() != null ) {
                type = reference.getField().getType();
            }
        }

        if (!StringUtils.isEmpty(name)) {
            Reference ref = new Reference(this.getLog(), reference);
            ref.setName(name);
            ref.setInterface(type);
            ref.setCardinality(reference.getNamedParameter(SCRDescriptor.REFERENCE_CARDINALITY));
            ref.setPolicy(reference.getNamedParameter(SCRDescriptor.REFERENCE_POLICY));
            ref.setTarget(reference.getNamedParameter(SCRDescriptor.REFERENCE_TARGET));
            ref.setBind(reference.getNamedParameter(SCRDescriptor.REFERENCE_BIND));
            ref.setUnbind(reference.getNamedParameter(SCRDescriptor.REFERENCE_UNDBIND));
            sd.addReference(ref);
        }
    }

    protected boolean getBoolean(JavaTag tag, String name, boolean defaultValue) {
        String value = tag.getNamedParameter(name);
        return (value == null) ? defaultValue : Boolean.valueOf(value).booleanValue();
    }
}
