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
package org.apache.felix.sandbox.scrplugin.xml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.felix.sandbox.scrplugin.om.Component;
import org.apache.felix.sandbox.scrplugin.om.Components;
import org.apache.felix.sandbox.scrplugin.om.Implementation;
import org.apache.felix.sandbox.scrplugin.om.Property;
import org.apache.maven.plugin.MojoExecutionException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.thoughtworks.xstream.XStream;

/**
 * <code>ComponentDescriptorIO</code>
 *
 * is a helper class to read and write component descriptor files.
 *
 */
public class ComponentDescriptorIO {

    public static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";

    private static final String PREFIX = "scr";

    private static final String COMPONENTS = "components";

    private static final String COMPONENT = "component";

    private static final String COMPONENT_QNAME = PREFIX + ':' + COMPONENT;

    private static final String IMPLEMENTATION = "implementation";

    private static final String IMPLEMENTATION_QNAME = PREFIX + ':' + IMPLEMENTATION;

    private static final String SERVICE = "service";

    private static final String SERVICE_QNAME = PREFIX + ':' + SERVICE;

    private static final String PROPERTY = "property";

    private static final String PROPERTY_QNAME = PREFIX + ':' + PROPERTY;

    private static final String REFERENCE = "reference";

    private static final String REFERENCE_QNAME = PREFIX + ':' + REFERENCE;

    private static final String INTERFACE = "provide";

    private static final String INTERFACE_QNAME = PREFIX + ':' + INTERFACE;

    private static final SAXTransformerFactory FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance();

    protected final XStream xstream;

    public ComponentDescriptorIO() {
        this.xstream = new XStream();
        this.xstream.setMode(XStream.NO_REFERENCES);

        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.AbstractObject.class, "tag");

        this.xstream.alias(ComponentDescriptorIO.COMPONENTS, org.apache.felix.sandbox.scrplugin.om.Components.class);
        this.xstream.addImplicitCollection(org.apache.felix.sandbox.scrplugin.om.Components.class, ComponentDescriptorIO.COMPONENTS);

        this.xstream.alias(ComponentDescriptorIO.COMPONENT, org.apache.felix.sandbox.scrplugin.om.Component.class);
        this.xstream.addImplicitCollection(org.apache.felix.sandbox.scrplugin.om.Component.class, "references");
        this.xstream.addImplicitCollection(org.apache.felix.sandbox.scrplugin.om.Component.class, "properties");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Component.class, "name");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Component.class, "enabled");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Component.class, "immediate");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "ocd");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "designate");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "isAbstract");
        this.xstream.omitField(org.apache.felix.sandbox.scrplugin.om.Component.class, "serviceFactory");

        this.xstream.alias("implementation", org.apache.felix.sandbox.scrplugin.om.Implementation.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Implementation.class, "classname");

        this.xstream.alias("property", org.apache.felix.sandbox.scrplugin.om.Property.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Property.class, "name");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Property.class, "value");

        this.xstream.alias("service", org.apache.felix.sandbox.scrplugin.om.Service.class);
        this.xstream.addImplicitCollection(org.apache.felix.sandbox.scrplugin.om.Service.class, "interfaces");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Service.class, "servicefactory");

        this.xstream.alias("provide", org.apache.felix.sandbox.scrplugin.om.Interface.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Interface.class, "interfacename");

        this.xstream.alias("reference", org.apache.felix.sandbox.scrplugin.om.Reference.class);
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "name");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "interfacename");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "target");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "cardinality");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "policy");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "bind");
        this.xstream.useAttributeFor(org.apache.felix.sandbox.scrplugin.om.Reference.class, "unbind");
    }

    public org.apache.felix.sandbox.scrplugin.om.Components read(File file) throws IOException, MojoExecutionException {
        Writer buffer = new StringWriter();
        final TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            IOUtils.copy(new FileReader(file), buffer);
            String xmlDoc = buffer.toString();
            buffer = new StringWriter();
            int pos = xmlDoc.indexOf("?>");
            if ( pos > 0 ) {
                xmlDoc = xmlDoc.substring(pos+2);
            }
            xmlDoc = "<components>" + xmlDoc + "</components>";
            transformer = factory.newTransformer(new StreamSource(this.getClass().getResourceAsStream("/org/apache/felix/sandbox/scrplugin/xml/read.xsl")));
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.transform(new StreamSource(new StringReader(xmlDoc)), new StreamResult(buffer));
            return (org.apache.felix.sandbox.scrplugin.om.Components)this.xstream.fromXML(new StringReader(buffer.toString()));
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to read xml.", e);
        }
    }

    /**
     * Write the component descriptors to the file.
     * @param components
     * @param file
     * @throws MojoExecutionException
     */
    public static void write(org.apache.felix.sandbox.scrplugin.om.Components components, File file)
    throws MojoExecutionException {
        try {
            FileWriter writer = new FileWriter(file);
            final TransformerHandler transformerHandler = FACTORY.newTransformerHandler();
            final Transformer transformer = transformerHandler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformerHandler.setResult(new StreamResult(writer));

            generateXML(components, transformerHandler);
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to write xml to " + file, e);
        } catch (SAXException e) {
            throw new MojoExecutionException("Unable to generate xml for " + file, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write xml to " + file, e);
        }
    }

    /**
     * Generate the xml top level element and start streaming
     * the components.
     * @param components
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Components components, ContentHandler contentHandler)
    throws SAXException {
        contentHandler.startDocument();
        contentHandler.startPrefixMapping(PREFIX, NAMESPACE_URI);

        // wrapper element to generate well formed xml
        contentHandler.startElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS, new AttributesImpl());

        final Iterator i = components.getComponents().iterator();
        while ( i.hasNext() ) {
            final Component component = (Component)i.next();
            generateXML(component, contentHandler);
        }
        // end wrapper element
        contentHandler.endElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS);
        contentHandler.endPrefixMapping(PREFIX);
        contentHandler.endDocument();
    }

    /**
     * Write the xml for a {@link Component}.
     * @param component
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Component component, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "enabled", component.isEnabled());
        addAttribute(ai, "immediate",component.isImmediate());
        addAttribute(ai, "name", component.getName());
        addAttribute(ai, "factory", component.getFactory());

        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME, ai);
        generateXML(component.getImplementation(), contentHandler);
        if ( component.getService() != null ) {
            generateXML(component.getService(), contentHandler);
        }
        if ( component.getProperties() != null ) {
            final Iterator i = component.getProperties().iterator();
            while ( i.hasNext() ) {
                final Property property = (Property)i.next();
                generateXML(property, contentHandler);
            }
        }
        if ( component.getReferences() != null ) {
            final Iterator i = component.getReferences().iterator();
            while ( i.hasNext() ) {
                final Reference reference = (Reference)i.next();
                generateXML(reference, contentHandler);
            }
        }
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME);
    }

    /**
     * Write the xml for a {@link Implementation}.
     * @param implementation
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Implementation implementation, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "class", implementation.getClassame());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION, ComponentDescriptorIO.IMPLEMENTATION_QNAME, ai);
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION, ComponentDescriptorIO.IMPLEMENTATION_QNAME);
    }

    /**
     * Write the xml for a {@link Service}.
     * @param service
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(org.apache.felix.sandbox.scrplugin.om.Service service, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "servicefactory", service.getServicefactory());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME, ai);
        if ( service.getInterfaces() != null ) {
            final Iterator i = service.getInterfaces().iterator();
            while ( i.hasNext() ) {
                final Interface interf = (Interface)i.next();
                generateXML(interf, contentHandler);
            }
        }
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME);
    }

    /**
     * Write the xml for a {@link Interface}.
     * @param interface
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Interface interf, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "interface", interf.getInterfaceame());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME, ai);
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME);
    }

    /**
     * Write the xml for a {@link Property}.
     * @param property
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Property property, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "name", property.getName());
        addAttribute(ai, "type", property.getType());
        addAttribute(ai, "value", property.getValue());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME, ai);
        if ( property.getMultiValue() != null && property.getMultiValue().length > 0 ) {
            for(int i=0; i<property.getMultiValue().length; i++) {
                text(contentHandler, "    ");
                text(contentHandler, property.getMultiValue()[i]);
                text(contentHandler, "\n");
            }
        }
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME);
    }

    /**
     * Write the xml for a {@link Reference}.
     * @param reference
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Reference reference, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "name", reference.getName());
        addAttribute(ai, "interface", reference.getInterfacename());
        addAttribute(ai, "cardinality", reference.getCardinality());
        addAttribute(ai, "policy", reference.getPolicy());
        addAttribute(ai, "target", reference.getTarget());
        addAttribute(ai, "bind", reference.getBind());
        addAttribute(ai, "unbind", reference.getUnbind());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.REFERENCE, ComponentDescriptorIO.REFERENCE_QNAME, ai);
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.REFERENCE, ComponentDescriptorIO.REFERENCE_QNAME);
    }

    /**
     * Helper method to add an attribute.
     * This implementation adds a new attribute with the given name
     * and value. Before adding the value is checked for non-null.
     * @param ai    The attributes impl receiving the additional attribute.
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     */
    protected static void addAttribute(AttributesImpl ai, String name, Object value) {
        if ( value != null ) {
            ai.addAttribute("", name, name, "CDATA", value.toString());
        }
    }

    /**
     * Helper method writing out a string.
     * @param ch
     * @param text
     * @throws SAXException
     */
    protected static void text(ContentHandler ch, String text)
    throws SAXException {
        if ( text != null ) {
            final char[] c = text.toCharArray();
            ch.characters(c, 0, c.length);
        }
    }
}
