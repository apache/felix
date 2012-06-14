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

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.MethodDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.ReferenceCardinality;
import org.apache.felix.scrplugin.description.ReferenceDescription;
import org.apache.felix.scrplugin.description.ReferencePolicy;
import org.apache.felix.scrplugin.description.ReferenceStrategy;
import org.apache.felix.scrplugin.description.ServiceDescription;
import org.apache.felix.scrplugin.helper.IssueLog;
import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.felix.scrplugin.om.Interface;
import org.apache.felix.scrplugin.om.Property;
import org.apache.felix.scrplugin.om.Reference;
import org.apache.felix.scrplugin.om.Service;
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

    /** Component: The name attribute. */
    private static final String COMPONENT_ATTR_NAME = "name";

    /** Component: The activate attribute. */
    private static final String COMPONENT_ATTR_ACTIVATE = "activate";

    /** Component: The deactivate attribute. */
    private static final String COMPONENT_ATTR_DEACTIVATE = "deactivate";

    /** Component: The modified attribute. */
    private static final String COMPONENT_ATTR_MODIFIED = "modified";

    private static final String IMPLEMENTATION = "implementation";

    private static final String IMPLEMENTATION_QNAME = IMPLEMENTATION;

    private static final String SERVICE = "service";

    private static final String SERVICE_QNAME = SERVICE;

    private static final String PROPERTY = "property";

    private static final String PROPERTY_QNAME = PROPERTY;

    private static final String REFERENCE = "reference";

    private static final String REFERENCE_QNAME = REFERENCE;

    private static final String INTERFACE = "provide";

    private static final String INTERFACE_QNAME = INTERFACE;

    public static List<ClassDescription> read(final InputStream file,
                    final ClassLoader classLoader,
                    final IssueLog iLog, final String location) throws SCRDescriptorException {
        try {
            final XmlHandler xmlHandler = new XmlHandler(classLoader, iLog, location);
            IOUtils.parse(file, xmlHandler);
            return xmlHandler.components;
        } catch (final TransformerException e) {
            throw new SCRDescriptorException("Unable to read xml", "[stream]", 0, e);
        }
    }

    /**
     * Write the component descriptors to the file.
     *
     * @param components
     * @param file
     * @throws SCRDescriptorException
     */
    public static void write(Components components, File file) throws SCRDescriptorException {
        try {
            generateXML(components, IOUtils.getSerializer(file));
        } catch (TransformerException e) {
            throw new SCRDescriptorException("Unable to write xml", file.toString(), 0, e);
        } catch (SAXException e) {
            throw new SCRDescriptorException("Unable to generate xml", file.toString(), 0, e);
        } catch (IOException e) {
            throw new SCRDescriptorException("Unable to write xml", file.toString(), 0, e);
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
    protected static void generateXML(Components components, ContentHandler contentHandler) throws SAXException {
        // detect namespace to use
        final String namespace = components.getSpecVersion().getNamespaceUrl();

        contentHandler.startDocument();
        contentHandler.startPrefixMapping(PREFIX, namespace);

        // wrapper element to generate well formed xml
        contentHandler.startElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS, new AttributesImpl());
        IOUtils.newline(contentHandler);

        for (final Component component : components.getComponents()) {
            if (component.isDs()) {
                generateXML(namespace, component, contentHandler);
            }
        }
        // end wrapper element
        contentHandler.endElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS);
        IOUtils.newline(contentHandler);
        contentHandler.endPrefixMapping(PREFIX);
        contentHandler.endDocument();
    }

    /**
     * Write the xml for a {@link Component}.
     *
     * @param component
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(final String namespace, final Component component, final ContentHandler contentHandler)
                    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, COMPONENT_ATTR_ENABLED, component.isEnabled());
        IOUtils.addAttribute(ai, COMPONENT_ATTR_IMMEDIATE, component.isImmediate());
        IOUtils.addAttribute(ai, COMPONENT_ATTR_NAME, component.getName());
        IOUtils.addAttribute(ai, COMPONENT_ATTR_FACTORY, component.getFactory());

        // attributes new in 1.1
        if (component.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
            if ( component.getConfigurationPolicy() != ComponentConfigurationPolicy.OPTIONAL ) {
                IOUtils.addAttribute(ai, COMPONENT_ATTR_POLICY, component.getConfigurationPolicy().name());
            }
            IOUtils.addAttribute(ai, COMPONENT_ATTR_ACTIVATE, component.getActivate());
            IOUtils.addAttribute(ai, COMPONENT_ATTR_DEACTIVATE, component.getDeactivate());
            IOUtils.addAttribute(ai, COMPONENT_ATTR_MODIFIED, component.getModified());
        }

        IOUtils.indent(contentHandler, 1);
        contentHandler.startElement(namespace, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME, ai);
        IOUtils.newline(contentHandler);
        generateImplementationXML(component, contentHandler);
        if (component.getService() != null) {
            generateServiceXML(component.getService(), contentHandler);
        }
        if (component.getProperties() != null) {
            for (final Property property : component.getProperties()) {
                generatePropertyXML(property, contentHandler);
            }
        }
        if (component.getReferences() != null) {
            for (final Reference reference : component.getReferences()) {
                generateReferenceXML(component, reference, contentHandler);
            }
        }
        IOUtils.indent(contentHandler, 1);
        contentHandler.endElement(namespace, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Implementation}.
     *
     * @param implementation
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateImplementationXML(Component component, ContentHandler contentHandler) throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "class", component.getClassDescription().getDescribedClass().getName());
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION,
                        ComponentDescriptorIO.IMPLEMENTATION_QNAME, ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION,
                        ComponentDescriptorIO.IMPLEMENTATION_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Service}.
     *
     * @param service
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateServiceXML(
                    final Service service,
                    final ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "servicefactory", String.valueOf(service.isServiceFactory()));
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME, ai);
        if (service.getInterfaces() != null && service.getInterfaces().size() > 0) {
            IOUtils.newline(contentHandler);
            for (final Interface interf : service.getInterfaces()) {
                generateXML(interf, contentHandler);
            }
            IOUtils.indent(contentHandler, 2);
        }
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Interface}.
     *
     * @param interf
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Interface interf, ContentHandler contentHandler) throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "interface", interf.getInterfaceName());
        IOUtils.indent(contentHandler, 3);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME,
                        ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Property}.
     *
     * @param property
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generatePropertyXML(Property property, ContentHandler contentHandler) throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "name", property.getName());
        IOUtils.addAttribute(ai, "type", property.getType());
        IOUtils.addAttribute(ai, "value", property.getValue());

        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME, ai);
        if (property.getMultiValue() != null && property.getMultiValue().length > 0) {
            // generate a new line first
            IOUtils.text(contentHandler, "\n");
            for (int i = 0; i < property.getMultiValue().length; i++) {
                IOUtils.indent(contentHandler, 3);
                IOUtils.text(contentHandler, property.getMultiValue()[i]);
                IOUtils.newline(contentHandler);
            }
            IOUtils.indent(contentHandler, 2);
        }
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Reference}.
     *
     * @param reference
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateReferenceXML(final Component component,
                    final Reference reference,
                    final ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "name", reference.getName());
        IOUtils.addAttribute(ai, "interface", reference.getInterfacename());
        IOUtils.addAttribute(ai, "cardinality", reference.getCardinality().getCardinalityString());
        IOUtils.addAttribute(ai, "policy", reference.getPolicy().name());
        IOUtils.addAttribute(ai, "target", reference.getTarget());
        IOUtils.addAttribute(ai, "bind", reference.getBind());
        IOUtils.addAttribute(ai, "unbind", reference.getUnbind());

        // attributes new in 1.1-felix (FELIX-1893)
        if (component.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1_FELIX.ordinal() ) {
            IOUtils.addAttribute(ai, "updated", reference.getUpdated());
        }

        IOUtils.indent(contentHandler, 2);
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
                    desc.setName(attributes.getValue(COMPONENT_ATTR_NAME));

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
                                desc.setConfigurationPolicy(ComponentConfigurationPolicy.valueOf(policy));
                            } catch (final IllegalArgumentException iae) {
                                iLog.addWarning("Invalid value for attribute " + COMPONENT_ATTR_POLICY + " : " + policy, this.location);
                            }
                        }
                        if ( attributes.getValue(COMPONENT_ATTR_ACTIVATE) != null ) {
                            desc.setActivate(new MethodDescription(attributes.getValue(COMPONENT_ATTR_ACTIVATE)));
                        }
                        if ( attributes.getValue(COMPONENT_ATTR_DEACTIVATE) != null ) {
                            desc.setDeactivate(new MethodDescription(attributes.getValue(COMPONENT_ATTR_DEACTIVATE)));
                        }
                        if ( attributes.getValue(COMPONENT_ATTR_MODIFIED) != null ) {
                            desc.setModified(new MethodDescription(attributes.getValue(COMPONENT_ATTR_MODIFIED)));
                        }
                    }
                } else if (localName.equals(IMPLEMENTATION)) {
                    // now we can create the class description and attach the component description
                    // Set the implementation class name (mandatory)
                    try {
                        this.currentClass = new ClassDescription(this.classLoader.loadClass(attributes.getValue("class")), null);
                    } catch (final ClassNotFoundException e) {
                        iLog.addError("Unable to load class " + attributes.getValue("class") + " from dependencies.", this.location);
                    }
                    this.currentClass.add(this.currentComponent);

                } else if (localName.equals(PROPERTY)) {

                    // read the property, unless it is the service.pid
                    // property which must not be inherited
                    final String propName = attributes.getValue("name");
                    if (!org.osgi.framework.Constants.SERVICE_PID.equals(propName)) {
                        final PropertyDescription prop = new PropertyDescription(null);

                        prop.setName(propName);
                        final String type = attributes.getValue("type");
                        if ( type != null ) {
                            try {
                                prop.setType(PropertyType.valueOf(type));
                            } catch (final IllegalArgumentException iae) {
                                iLog.addWarning("Invalid value for attribute type : " + type, this.location);
                            }
                        }

                        if (attributes.getValue("value") != null) {
                            prop.setValue(attributes.getValue("value"));
                            this.currentClass.add(prop);
                        } else {
                            // hold the property pending as we have a multi value
                            this.pendingProperty = prop;
                        }
                        // check for abstract properties
                        prop.setLabel(attributes.getValue("label"));
                        prop.setDescription(attributes.getValue("description"));
                        final String cardinality = attributes.getValue("cardinality");
                        if ( cardinality != null ) {
                            prop.setCardinality(Integer.valueOf(cardinality));
                        }
                        final String pValue = attributes.getValue("private");
                        if (pValue != null) {
                            prop.setPrivate(Boolean.valueOf(pValue));
                        }
                    }

                } else if (localName.equals("properties")) {

                    // TODO: implement the properties tag

                } else if (localName.equals(SERVICE)) {

                    this.currentService = new ServiceDescription(null);
                    this.currentClass.add(this.currentService);

                    if (attributes.getValue("servicefactory") != null) {
                        this.currentService.setServiceFactory(Boolean.valueOf(attributes.getValue("servicefactory")));
                    }

                } else if (localName.equals(INTERFACE)) {
                    this.currentService.addInterface(attributes.getValue("interface"));

                } else if (localName.equals(REFERENCE)) {
                    final ReferenceDescription ref = new ReferenceDescription(null);

                    ref.setName(attributes.getValue("name"));
                    ref.setInterfaceName(attributes.getValue("interface"));
                    final String cardinality = attributes.getValue("cardinality");
                    if ( cardinality != null ) {
                        ref.setCardinality(ReferenceCardinality.fromValue(cardinality));
                        if ( ref.getCardinality() == null ) {
                            iLog.addWarning("Invalid value for attribute cardinality : " + cardinality, this.location);
                        }
                    }
                    final String policy = attributes.getValue("policy");
                    if ( policy != null ) {
                        try {
                            ref.setPolicy(ReferencePolicy.valueOf(policy));
                        } catch (final IllegalArgumentException iae) {
                            iLog.addWarning("Invalid value for attribute policy : " + policy, this.location);
                        }
                    }
                    ref.setTarget(attributes.getValue("target"));
                    if ( attributes.getValue("bind") != null ) {
                        ref.setBind(new MethodDescription(attributes.getValue("bind")));
                    }
                    if ( attributes.getValue("unbind") != null ) {
                        ref.setUnbind(new MethodDescription(attributes.getValue("unbind")));
                    }

                    final String strategy = attributes.getValue("strategy");
                    if ( strategy != null ) {
                        try {
                            ref.setStrategy(ReferenceStrategy.valueOf(strategy));
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
}
