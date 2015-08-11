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
package org.apache.felix.scrplugin.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerException;

import org.apache.felix.scrplugin.Log;
import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.PropertyUnbounded;
import org.apache.felix.scrplugin.description.ReferenceCardinality;
import org.apache.felix.scrplugin.description.ReferenceDescription;
import org.apache.felix.scrplugin.description.ReferencePolicy;
import org.apache.felix.scrplugin.description.ReferencePolicyOption;
import org.apache.felix.scrplugin.description.ReferenceStrategy;
import org.apache.felix.scrplugin.description.ServiceDescription;
import org.apache.felix.scrplugin.helper.ComponentContainer;
import org.apache.felix.scrplugin.helper.ComponentContainerUtil;
import org.apache.felix.scrplugin.helper.ComponentContainerUtil.ComponentContainerContainer;
import org.apache.felix.scrplugin.helper.DescriptionContainer;
import org.apache.felix.scrplugin.helper.IssueLog;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <code>ComponentDescriptorIO</code>
 *
 * is a helper class to read and write component descriptor files.
 *
 */
public class ComponentDescriptorIO {

    private static final String PROPERTY_ATTR_TYPE = "type";

    /** General attribute for the name (component, reference, property) */
    private static final String ATTR_NAME = "name";

    private static final String ATTR_CARDINALITY = "cardinality";

    private static final String ATTR_DESCRIPTION = "description";

    private static final String ATTR_LABEL = "label";

    /** The inner namespace - used for all inner elements. */
    public static final String INNER_NAMESPACE_URI = "";

    /** The prefix used for the namespace. */
    private static final String PREFIX = "scr";

    /** The root element. */
    private static final String COMPONENTS = "components";

    /** The component element. */
    private static final String COMPONENT = "component";

    /** The qualified component element. */
    private static final String COMPONENT_QNAME = PREFIX + ':' + COMPONENT;

    /** The enabled attribute. */
    private static final String COMPONENT_ATTR_ENABLED = "enabled";

    /** Component: The policy attribute. */
    private static final String COMPONENT_ATTR_POLICY = "configuration-policy";

    /** Component: The factory attribute. */
    private static final String COMPONENT_ATTR_FACTORY = "factory";

    /** Component: The immediate attribute. */
    private static final String COMPONENT_ATTR_IMMEDIATE = "immediate";

    /** Component: The activate attribute. */
    private static final String COMPONENT_ATTR_ACTIVATE = "activate";

    /** Component: The deactivate attribute. */
    private static final String COMPONENT_ATTR_DEACTIVATE = "deactivate";

    /** Component: The modified attribute. */
    private static final String COMPONENT_ATTR_MODIFIED = "modified";

    /** Component: The configuration pid attribute. */
    private static final String COMPONENT_ATTR_CONFIGURATION_PID = "configuration-pid";

    private static final String IMPLEMENTATION = "implementation";

    private static final String IMPLEMENTATION_QNAME = IMPLEMENTATION;

    private static final String IMPLEMENTATION_ATTR_CLASS = "class";

    private static final String SERVICE = "service";

    private static final String SERVICE_QNAME = SERVICE;

    private static final String SERVICE_ATTR_FACTORY = "servicefactory";

    private static final String PROPERTY = "property";

    private static final String PROPERTY_QNAME = PROPERTY;

    private static final String PROPERTY_ATTR_VALUE = "value";

    private static final String PROPERTY_ATTR_PRIVATE = "private";

    private static final String REFERENCE = "reference";

    private static final String REFERENCE_QNAME = REFERENCE;

    private static final String REFERENCE_ATTR_POLICY = "policy";

    private static final String REFERENCE_ATTR_POLICY_OPTION = "policy-option";

    private static final String REFERENCE_ATTR_UPDATED = "updated";

    private static final String REFERENCE_ATTR_UNBIND = "unbind";

    private static final String REFERENCE_ATTR_BIND = "bind";

    private static final String REFERENCE_ATTR_TARGET = "target";

    private static final String REFERENCE_ATTR_STRATEGY = "strategy";

    private static final String INTERFACE = "provide";

    private static final String INTERFACE_QNAME = INTERFACE;

    private static final String INTERFACE_ATTR_NAME = "interface";

    private static final String PROPERTIES = "properties";

    public static List<ClassDescription> read(final InputStream file,
            final ClassLoader classLoader,
            final IssueLog iLog, final String location) throws SCRDescriptorException {
        try {
            final XmlHandler xmlHandler = new XmlHandler(classLoader, iLog, location);
            IOUtils.parse(file, xmlHandler);
            return xmlHandler.components;
        } catch (final TransformerException e) {
            throw new SCRDescriptorException("Unable to read xml", location, e);
        }
    }

    /**
     * Generate the xml top level element and start streaming
     * the components.
     *
     * @param components
     * @param contentHandler
     * @throws SAXException
     */
    private static void generateXML(final DescriptionContainer module,
            final List<ComponentContainer> components,
            final File descriptorFile,
            final Log logger) throws SAXException, IOException, TransformerException {
        logger.info("Writing " + components.size() + " Service Component Descriptors to "
                + descriptorFile);
        final ContentHandler contentHandler = IOUtils.getSerializer(descriptorFile);
        // detect namespace to use
        final String namespace = module.getOptions().getSpecVersion().getNamespaceUrl();

        contentHandler.startDocument();
        contentHandler.startPrefixMapping(PREFIX, namespace);

        IOUtils.newline(contentHandler);
        // wrapper element to generate well formed xml if 0 or more than 1 component
        int startIndent = 0;
        if ( components.size() != 1 ) {
            contentHandler.startElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS, new AttributesImpl());
            IOUtils.newline(contentHandler);
            startIndent = 1;
        }
        for (final ComponentContainer component : components) {
            generateXML(namespace, module, component, contentHandler, startIndent);
        }

        // end wrapper element
        if ( components.size() != 1 ) {
            contentHandler.endElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS);
            IOUtils.newline(contentHandler);
        }
        contentHandler.endPrefixMapping(PREFIX);
        contentHandler.endDocument();
    }

    /**
     * Write the xml for a Component
     *
     * @param component
     * @param contentHandler
     * @throws SAXException
     */
    private static void generateXML(final String namespace,
            final DescriptionContainer module,
            final ComponentContainer container,
            final ContentHandler contentHandler,
            final int indent)
    throws SAXException {
        final ComponentDescription component = container.getComponentDescription();

        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, COMPONENT_ATTR_ENABLED, component.getEnabled());
        IOUtils.addAttribute(ai, COMPONENT_ATTR_IMMEDIATE, component.getImmediate());
        IOUtils.addAttribute(ai, ATTR_NAME, component.getName());
        IOUtils.addAttribute(ai, COMPONENT_ATTR_FACTORY, component.getFactory());

        // attributes new in 1.1
        if (module.getOptions().getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
            if ( component.getConfigurationPolicy() != null
                    && component.getConfigurationPolicy() != ComponentConfigurationPolicy.OPTIONAL ) {
                IOUtils.addAttribute(ai, COMPONENT_ATTR_POLICY, component.getConfigurationPolicy().name().toLowerCase());
            }
            IOUtils.addAttribute(ai, COMPONENT_ATTR_ACTIVATE, component.getActivate());
            IOUtils.addAttribute(ai, COMPONENT_ATTR_DEACTIVATE, component.getDeactivate());
            IOUtils.addAttribute(ai, COMPONENT_ATTR_MODIFIED, component.getModified());
        }
        // attributes new in 1.2
        if ( module.getOptions().getSpecVersion().ordinal() >= SpecVersion.VERSION_1_2.ordinal() ) {
            if ( component.getConfigurationPid() != null && !component.getConfigurationPid().equals(component.getName())) {
                IOUtils.addAttribute(ai, COMPONENT_ATTR_CONFIGURATION_PID, component.getConfigurationPid());
            }
        }
        IOUtils.indent(contentHandler, indent);
        contentHandler.startElement(namespace, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME, ai);
        IOUtils.newline(contentHandler);
        generateImplementationXML(container, contentHandler, indent+1);
        if (container.getServiceDescription() != null) {
            generateServiceXML(container.getServiceDescription(), contentHandler, indent+1);
        }
        for (final PropertyDescription property : container.getProperties().values()) {
            generatePropertyXML(property, contentHandler, indent+1);
        }

        for (final ReferenceDescription reference : container.getReferences().values()) {
            generateReferenceXML(component, module, reference, contentHandler, indent+1);
        }

        IOUtils.indent(contentHandler, indent);
        contentHandler.endElement(namespace, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for an Implementation.
     *
     * @param implementation
     * @param contentHandler
     * @throws SAXException
     */
    private static void generateImplementationXML(final ComponentContainer component,
            final ContentHandler contentHandler,
            final int indent)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, IMPLEMENTATION_ATTR_CLASS, component.getClassDescription().getDescribedClass().getName());
        IOUtils.indent(contentHandler, indent);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION,
                ComponentDescriptorIO.IMPLEMENTATION_QNAME, ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION,
                ComponentDescriptorIO.IMPLEMENTATION_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a service.
     *
     * @param service
     * @param contentHandler
     * @throws SAXException
     */
    private static void generateServiceXML(
            final ServiceDescription service,
            final ContentHandler contentHandler,
            final int indent)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, SERVICE_ATTR_FACTORY, String.valueOf(service.isServiceFactory()));
        IOUtils.indent(contentHandler, indent);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME, ai);
        if (service.getInterfaces() != null && service.getInterfaces().size() > 0) {
            IOUtils.newline(contentHandler);
            for (final String interf : service.getInterfaces()) {
                generateServiceXML(interf, contentHandler, indent+1);
            }
            IOUtils.indent(contentHandler, indent);
        }
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a interface
     *
     * @param interf
     * @param contentHandler
     * @throws SAXException
     */
    private static void generateServiceXML(final String interfaceName,
            final ContentHandler contentHandler,
            final int indent)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, INTERFACE_ATTR_NAME, interfaceName);
        IOUtils.indent(contentHandler, indent);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME,
                ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a property.
     *
     * @param property
     * @param contentHandler
     * @throws SAXException
     */
    private static void generatePropertyXML(final PropertyDescription property,
            final ContentHandler contentHandler,
            final int indent)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, ATTR_NAME, property.getName());
        if ( property.getType() != PropertyType.String && property.getType() != PropertyType.Password) {
            IOUtils.addAttribute(ai, PROPERTY_ATTR_TYPE, property.getType());
        }
        String value = property.getValue();
        if ( value != null ) {
            if ( property.getType() == PropertyType.Character || property.getType() == PropertyType.Char ) {
                value = String.valueOf((int)value.charAt(0));
            }
            IOUtils.addAttribute(ai, PROPERTY_ATTR_VALUE, value);
        }

        IOUtils.indent(contentHandler, indent);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME, ai);
        if (property.getMultiValue() != null && property.getMultiValue().length > 0) {
            // generate a new line first
            IOUtils.text(contentHandler, "\n");
            for (int i = 0; i < property.getMultiValue().length; i++) {
                IOUtils.indent(contentHandler, indent + 1);
                value = property.getMultiValue()[i];
                if ( property.getType() == PropertyType.Character || property.getType() == PropertyType.Char ) {
                    value = String.valueOf((int)value.charAt(0));
                }
                IOUtils.text(contentHandler, value);
                IOUtils.newline(contentHandler);
            }
            IOUtils.indent(contentHandler, indent);
        }
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a Reference.
     *
     * @param reference
     * @param contentHandler
     * @throws SAXException
     */
    private static void generateReferenceXML(final ComponentDescription component,
            final DescriptionContainer module,
            final ReferenceDescription reference,
            final ContentHandler contentHandler,
            final int indent)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, ATTR_NAME, reference.getName());
        IOUtils.addAttribute(ai, INTERFACE_ATTR_NAME, reference.getInterfaceName());
        IOUtils.addAttribute(ai, ATTR_CARDINALITY, reference.getCardinality().getCardinalityString());
        IOUtils.addAttribute(ai, REFERENCE_ATTR_POLICY, reference.getPolicy().name().toLowerCase());
        IOUtils.addAttribute(ai, REFERENCE_ATTR_TARGET, reference.getTarget());
        IOUtils.addAttribute(ai, REFERENCE_ATTR_BIND, reference.getBind());
        IOUtils.addAttribute(ai, REFERENCE_ATTR_UNBIND, reference.getUnbind());

        // attributes new in 1.1-felix (FELIX-1893)
        if (module.getOptions().getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1_FELIX.ordinal() ) {
            IOUtils.addAttribute(ai, REFERENCE_ATTR_UPDATED, reference.getUpdated());
        }

        // attributes new in 1.2
        if (module.getOptions().getSpecVersion().ordinal() >= SpecVersion.VERSION_1_2.ordinal() ) {
            if ( reference.getPolicyOption() != ReferencePolicyOption.RELUCTANT ) {
                IOUtils.addAttribute(ai, REFERENCE_ATTR_POLICY_OPTION, reference.getPolicyOption().name().toLowerCase());
            }
        }

        IOUtils.indent(contentHandler, indent);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.REFERENCE, ComponentDescriptorIO.REFERENCE_QNAME,
                ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.REFERENCE, ComponentDescriptorIO.REFERENCE_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * A content handler for parsing the component descriptions.
     *
     */
    protected static final class XmlHandler extends DefaultHandler {

        /** The components container. */
        private final List<ClassDescription> components = new ArrayList<ClassDescription>();

        /** Spec version. */
        private SpecVersion specVersion;

        /** A reference to the current class. */
        private ClassDescription currentClass;

        /** A reference to the current component. */
        private ComponentDescription currentComponent;

        /** The current service. */
        private ServiceDescription currentService;

        /** Pending property. */
        private PropertyDescription pendingProperty;

        /** Flag for detecting the first element. */
        private boolean firstElement = true;

        /** Flag for elements inside a component element */
        private boolean isComponent = false;

        /** Override namespace. */
        private String overrideNamespace;

        /** The issue log. */
        private final IssueLog iLog;

        /** XML file location. */
        private final String location;

        /** Classloader. */
        private final ClassLoader classLoader;

        public XmlHandler(final ClassLoader classLoader, final IssueLog iLog, final String loc) {
            this.iLog = iLog;
            this.location = loc;
            this.classLoader = classLoader;
        }

        /**
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(String uri, final String localName, final String name, final Attributes attributes)
                throws SAXException {
            // according to the spec, the elements should have the namespace,
            // except when the root element is the "component" element
            // So we check this for the first element, we receive.
            if (this.firstElement) {
                this.firstElement = false;
                if (localName.equals(COMPONENT) && "".equals(uri)) {
                    this.overrideNamespace = SpecVersion.VERSION_1_0.getNamespaceUrl();
                }
            }

            if (this.overrideNamespace != null && "".equals(uri)) {
                uri = this.overrideNamespace;
            }

            // however the spec also states that the inner elements
            // of a component are unqualified, so they don't have
            // the namespace - we allow both: with or without namespace!
            if (this.isComponent && "".equals(uri)) {
                uri = SpecVersion.VERSION_1_0.getNamespaceUrl();
            }

            // from here on, uri has the namespace regardless of the used xml format
            specVersion = SpecVersion.fromNamespaceUrl(uri);
            if (specVersion != null) {

                if (localName.equals(COMPONENT)) {
                    this.isComponent = true;

                    final ComponentDescription desc = new ComponentDescription(null);
                    desc.setName(attributes.getValue(ATTR_NAME));

                    // enabled attribute is optional
                    if (attributes.getValue(COMPONENT_ATTR_ENABLED) != null) {
                        desc.setEnabled(Boolean.valueOf(attributes.getValue(COMPONENT_ATTR_ENABLED)));
                    }

                    // immediate attribute is optional
                    if (attributes.getValue(COMPONENT_ATTR_IMMEDIATE) != null) {
                        desc.setImmediate(Boolean.valueOf(attributes.getValue(COMPONENT_ATTR_IMMEDIATE)));
                    }

                    desc.setFactory(attributes.getValue(COMPONENT_ATTR_FACTORY));

                    desc.setConfigurationPolicy(ComponentConfigurationPolicy.OPTIONAL);
                    // check for version 1.1 attributes
                    if (specVersion.ordinal() >= SpecVersion.VERSION_1_1.ordinal()) {
                        final String policy = attributes.getValue(COMPONENT_ATTR_POLICY);
                        if ( policy != null ) {
                            try {
                                desc.setConfigurationPolicy(ComponentConfigurationPolicy.valueOf(policy.toUpperCase()));
                            } catch (final IllegalArgumentException iae) {
                                iLog.addWarning("Invalid value for attribute " + COMPONENT_ATTR_POLICY + " : " + policy, this.location);
                            }
                        }
                        if ( attributes.getValue(COMPONENT_ATTR_ACTIVATE) != null ) {
                            desc.setActivate(attributes.getValue(COMPONENT_ATTR_ACTIVATE));
                        }
                        if ( attributes.getValue(COMPONENT_ATTR_DEACTIVATE) != null ) {
                            desc.setDeactivate(attributes.getValue(COMPONENT_ATTR_DEACTIVATE));
                        }
                        if ( attributes.getValue(COMPONENT_ATTR_MODIFIED) != null ) {
                            desc.setModified(attributes.getValue(COMPONENT_ATTR_MODIFIED));
                        }
                    }

                    this.currentComponent = desc;
                } else if (localName.equals(IMPLEMENTATION)) {
                    // now we can create the class description and attach the component description
                    // Set the implementation class name (mandatory)
                    final String className = attributes.getValue(IMPLEMENTATION_ATTR_CLASS);
                    Class<?> cl = null;
                    try {
                        cl = this.classLoader.loadClass(className);
                    } catch (final Throwable e) {
                        // this doesn't have an effect as the classes we processed are loaded
                        // anyway.
                    }
                    this.currentClass = new ClassDescription(cl, "classpath:" + className);
                    this.currentClass.add(this.currentComponent);
                    this.components.add(this.currentClass);

                } else if (localName.equals(PROPERTY)) {

                    // read the property, unless it is the service.pid
                    // property which must not be inherited
                    final String propName = attributes.getValue(ATTR_NAME);
                    if (!org.osgi.framework.Constants.SERVICE_PID.equals(propName)) {
                        final PropertyDescription prop = new PropertyDescription(null);

                        prop.setName(propName);
                        final String type = attributes.getValue(PROPERTY_ATTR_TYPE);
                        if ( type != null ) {
                            try {
                                prop.setType(PropertyType.valueOf(type));
                            } catch (final IllegalArgumentException iae) {
                                iLog.addWarning("Invalid value for attribute type : " + type, this.location);
                            }
                        }
                        if ( prop.getType() == null ) {
                            prop.setType(PropertyType.String);
                        }

                        if (attributes.getValue(PROPERTY_ATTR_VALUE) != null) {
                            if ( prop.getType() == PropertyType.Char || prop.getType() == PropertyType.Character ) {
                                final int val = Integer.valueOf(attributes.getValue(PROPERTY_ATTR_VALUE));
                                final Character c = Character.valueOf((char)val);
                                prop.setValue(c.toString());
                            } else {
                                prop.setValue(attributes.getValue(PROPERTY_ATTR_VALUE));
                            }
                            this.currentClass.add(prop);
                        } else {
                            // hold the property pending as we have a multi value
                            this.pendingProperty = prop;
                        }
                        // check for abstract properties
                        prop.setLabel(attributes.getValue(ATTR_LABEL));
                        prop.setDescription(attributes.getValue(ATTR_DESCRIPTION));
                        final String cardinality = attributes.getValue(ATTR_CARDINALITY);
                        prop.setUnbounded(PropertyUnbounded.DEFAULT);
                        if ( cardinality != null ) {
                            prop.setCardinality(Integer.valueOf(cardinality));
                            if ( prop.getCardinality() == Integer.MAX_VALUE ) {
                                prop.setCardinality(0);
                                prop.setUnbounded(PropertyUnbounded.ARRAY);
                            } else if ( prop.getCardinality() == Integer.MIN_VALUE ) {
                                prop.setCardinality(0);
                                prop.setUnbounded(PropertyUnbounded.VECTOR);
                            }
                        }
                        final String pValue = attributes.getValue(PROPERTY_ATTR_PRIVATE);
                        if (pValue != null) {
                            prop.setPrivate(Boolean.valueOf(pValue));
                        }
                    }

                } else if (localName.equals(PROPERTIES)) {

                    // TODO: implement the properties tag

                } else if (localName.equals(SERVICE)) {

                    this.currentService = new ServiceDescription(null);
                    this.currentClass.add(this.currentService);

                    if (attributes.getValue(SERVICE_ATTR_FACTORY) != null) {
                        this.currentService.setServiceFactory(Boolean.valueOf(attributes.getValue(SERVICE_ATTR_FACTORY)));
                    }

                } else if (localName.equals(INTERFACE)) {
                    this.currentService.addInterface(attributes.getValue(INTERFACE_ATTR_NAME));

                } else if (localName.equals(REFERENCE)) {
                    final ReferenceDescription ref = new ReferenceDescription(null);

                    ref.setName(attributes.getValue(ATTR_NAME));
                    ref.setInterfaceName(attributes.getValue(INTERFACE_ATTR_NAME));
                    final String cardinality = attributes.getValue(ATTR_CARDINALITY);
                    if ( cardinality != null ) {
                        ref.setCardinality(ReferenceCardinality.fromValue(cardinality));
                        if ( ref.getCardinality() == null ) {
                            iLog.addWarning("Invalid value for attribute cardinality : " + cardinality, this.location);
                        }
                    }
                    ref.setPolicy(ReferencePolicy.STATIC);
                    final String policy = attributes.getValue(REFERENCE_ATTR_POLICY);
                    if ( policy != null ) {
                        try {
                            ref.setPolicy(ReferencePolicy.valueOf(policy.toUpperCase()));
                        } catch (final IllegalArgumentException iae) {
                            iLog.addWarning("Invalid value for attribute policy : " + policy, this.location);
                        }
                    }
                    ref.setPolicyOption(ReferencePolicyOption.RELUCTANT);
                    final String policyOption = attributes.getValue(REFERENCE_ATTR_POLICY_OPTION);
                    if ( policyOption != null ) {
                        try {
                            ref.setPolicyOption(ReferencePolicyOption.valueOf(policyOption.toUpperCase()));
                        } catch (final IllegalArgumentException iae) {
                            iLog.addWarning("Invalid value for attribute policy-option : " + policyOption, this.location);
                        }
                    }
                    ref.setTarget(attributes.getValue(REFERENCE_ATTR_TARGET));
                    if ( attributes.getValue(REFERENCE_ATTR_BIND) != null ) {
                        ref.setBind(attributes.getValue(REFERENCE_ATTR_BIND));
                    }
                    if ( attributes.getValue(REFERENCE_ATTR_UNBIND) != null ) {
                        ref.setUnbind(attributes.getValue(REFERENCE_ATTR_UNBIND));
                    }
                    if ( attributes.getValue(REFERENCE_ATTR_UPDATED) != null ) {
                        ref.setUnbind(attributes.getValue(REFERENCE_ATTR_UPDATED));
                    }

                    final String strategy = attributes.getValue(REFERENCE_ATTR_STRATEGY);
                    if ( strategy != null ) {
                        try {
                            ref.setStrategy(ReferenceStrategy.valueOf(strategy.toUpperCase()));
                        } catch (final IllegalArgumentException iae) {
                            throw new SAXException("Invalid value for attribute strategy : " + strategy);
                        }
                    }

                    this.currentClass.add(ref);
                }
            }
        }

        /**
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if (this.overrideNamespace != null && "".equals(uri)) {
                uri = this.overrideNamespace;
            }

            if (this.isComponent && "".equals(uri)) {
                uri = SpecVersion.VERSION_1_0.getNamespaceUrl();
            }

            if (SpecVersion.fromNamespaceUrl(uri) != null) {
                if (localName.equals(COMPONENT)) {
                    this.currentClass = null;
                    this.currentComponent = null;
                    this.isComponent = false;
                } else if (localName.equals(PROPERTY) && this.pendingProperty != null) {
                    // now split the value
                    final String text = this.pendingProperty.getValue();
                    if (text != null) {
                        final StringTokenizer st = new StringTokenizer(text);
                        final String[] values = new String[st.countTokens()];
                        int index = 0;
                        while (st.hasMoreTokens()) {
                            values[index] = st.nextToken();
                            if ( this.pendingProperty.getType() == PropertyType.Char || this.pendingProperty.getType() == PropertyType.Character ) {
                                final int val = Integer.valueOf(values[index]);
                                final Character c = Character.valueOf((char)val);
                                values[index] = c.toString();
                            }
                            index++;
                        }
                        this.pendingProperty.setMultiValue(values);
                    }
                    this.currentClass.add(this.pendingProperty);
                    this.pendingProperty = null;
                }
            }
        }

        /**
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (this.pendingProperty != null) {
                final String text = new String(ch, start, length);
                if (this.pendingProperty.getValue() == null) {
                    this.pendingProperty.setValue(text);
                } else {
                    this.pendingProperty.setValue(this.pendingProperty.getValue() + text);
                }
            }
        }
    }

    private static final String PARENT_NAME = "OSGI-INF";

    /**
     * Generate descriptor file(s)
     */
    public static List<String> generateDescriptorFiles(final DescriptionContainer module, final Options options, final Log logger)
            throws SCRDescriptorException, SCRDescriptorFailureException {
        // get the list of all relevant containers
        final List<ComponentContainer> components = new ArrayList<ComponentContainer>();
        for(final ComponentContainer container : module.getComponents()) {
            if (!container.getComponentDescription().isCreateDs()) {
                logger.debug("Ignoring descriptor for DS : " + container);
            } else if (!container.getComponentDescription().isAbstract()) {
                logger.debug("Adding descriptor for DS : " + container);
                components.add(container);
            }
        }

        // check descriptor file
        final File descriptorDir = options.getComponentDescriptorDirectory();

        // terminate if there is nothing else to write
        if (components.isEmpty()) {
            logger.debug("No Service Component Descriptors found in project.");
            // remove files if it exists
            if ( descriptorDir.exists() && !options.isIncremental()) {
                for(final File f : descriptorDir.listFiles()) {
                    if ( f.isFile() ) {
                        logger.debug("Removing obsolete service descriptor " + f);
                        f.delete();
                    }
                }
            }

            return null;
        }

        // finally the descriptors have to be written ....
        descriptorDir.mkdirs(); // ensure parent dir

        final List<String> fileNames = new ArrayList<String>();
        final List<ComponentContainerContainer> containers = ComponentContainerUtil.split(components);
        for(final ComponentContainerContainer ccc : containers) {
            final SpecVersion globalVersion = module.getOptions().getSpecVersion();

            SpecVersion sv = null;
            for(final ComponentContainer cc : ccc.components ) {
                if ( sv == null || sv.ordinal() < cc.getComponentDescription().getSpecVersion().ordinal() ) {
                    sv = cc.getComponentDescription().getSpecVersion();
                }
            }
            module.getOptions().setSpecVersion(sv);
            final File useFile = new File(descriptorDir, ccc.className + ".xml");
            try {
                ComponentDescriptorIO.generateXML(module, ccc.components, useFile, logger);
            } catch (final IOException e) {
                throw new SCRDescriptorException("Unable to generate xml", useFile.toString(), e);
            } catch (final TransformerException e) {
                throw new SCRDescriptorException("Unable to generate xml", useFile.toString(), e);
            } catch (final SAXException e) {
                throw new SCRDescriptorException("Unable to generate xml", useFile.toString(), e);
            }
            fileNames.add(PARENT_NAME + '/' + useFile.getName());

            module.getOptions().setSpecVersion(globalVersion);
        }

        return fileNames;
    }
}
