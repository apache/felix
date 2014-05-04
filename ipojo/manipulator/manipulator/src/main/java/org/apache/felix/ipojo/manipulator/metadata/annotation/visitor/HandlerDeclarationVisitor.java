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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parse the @HandlerDeclaration annotation.
 * @see org.apache.felix.ipojo.annotations.HandlerDeclaration
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class HandlerDeclarationVisitor extends AnnotationVisitor {

    /**
     * XML accepted by the handler.
     */
    private String m_value;

    private DocumentBuilder builder;

    private ComponentWorkbench workbench;
    private Reporter reporter;

    public HandlerDeclarationVisitor(ComponentWorkbench workbench, DocumentBuilder builder, Reporter reporter) {
        super(Opcodes.ASM5);
        this.workbench = workbench;
        this.builder = builder;
        this.reporter = reporter;
    }

    /**
     * Parses the value attribute.
     * @param name 'value'
     * @param value the value
     * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
     */
    public void visit(String name, Object value) {
        // there is only a 'value' attribute
        this.m_value = (String) value;
    }

    /**
     * End of the visit.
     * Builds the XML document.
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    public void visitEnd() {
        // The value is an XML document
        InputStream is = new ByteArrayInputStream(m_value.getBytes());
        Document document = null;
        try {
            document = builder.parse(is);
            Element e = convertDOMElements(document.getDocumentElement());
            workbench.getElements().put(e, null);
        } catch (Exception e) {
            reporter.warn("Cannot convert {} to iPOJO Elements.", m_value, e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                reporter.trace("Cannot close correctly the value input stream ({}).", m_value, e);
            }
        }
    }

    /**
     * Converts recursively the given XML Element into an iPOJO Element.
     * @param xmlElement DOM Element to be converted
     */
    private static Element convertDOMElements(final org.w3c.dom.Element xmlElement) {

        // Create an equivalent iPOJO element
        Element converted = transformElement(xmlElement);

        convertDOMElements(converted, xmlElement);

        return converted;
    }

    /**
     * Converts recursively the given XML Element into an iPOJO Element.
     * @param root iPOJO root Element
     * @param xmlElement DOM Element to be converted
     */
    private static void convertDOMElements(final Element root,
                                           final org.w3c.dom.Element xmlElement) {

        // Convert attributes if any
        if (xmlElement.hasAttributes()) {
            NamedNodeMap attributes = xmlElement.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attr = (Attr) attributes.item(i);
                if (!"xmlns".equals(attr.getPrefix())) {
                    root.addAttribute(transformAttribute(attr));
                }
            }
        }

        // Convert child elements if any
        if (xmlElement.hasChildNodes()) {
            NodeList children = xmlElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {

                // Create an equivalent iPOJO element
                org.w3c.dom.Element child = (org.w3c.dom.Element) children.item(i);
                Element converted = transformElement(child);

                // Add converted element as a root's child
                root.addElement(converted);

                // Recursive call
                convertDOMElements(converted, child);
            }
        }

    }

    private static Attribute transformAttribute(Attr attr) {
        return new Attribute(attr.getLocalName(),
                attr.getNamespaceURI(),
                attr.getValue());
    }

    private static Element transformElement(org.w3c.dom.Element xmlElement) {
        return new Element(xmlElement.getLocalName(), xmlElement.getNamespaceURI());
    }

}

