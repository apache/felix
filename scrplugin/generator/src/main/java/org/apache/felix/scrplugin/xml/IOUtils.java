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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Utility class for xml/sax handling.
 */
public class IOUtils {

    /** The transformer factory. */
    private static final SAXTransformerFactory FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance();

    /**
     * Parse a file and send the sax events to the content handler.
     * @param file
     * @param handler
     * @throws TransformerException
     */
    public static final void parse(final InputStream file, final ContentHandler handler)
    throws TransformerException {
        final Transformer transformer = FACTORY.newTransformer();
        transformer.transform( new StreamSource( file ), new SAXResult( handler ) );
    }

    /**
     * Get a serializer to write XML to a file.
     */
    public static ContentHandler getSerializer(final File file)
    throws TransformerException, IOException {
        final Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");

        final TransformerHandler transformerHandler = FACTORY.newTransformerHandler();
        final Transformer transformer = transformerHandler.getTransformer();

        final Properties format = new Properties();
        format.put(OutputKeys.METHOD, "xml");
        format.put(OutputKeys.OMIT_XML_DECLARATION, "no");
        format.put(OutputKeys.ENCODING, "UTF-8");
        format.put(OutputKeys.INDENT, "yes");

        transformer.setOutputProperties(format);

        transformerHandler.setResult(new StreamResult(writer));

        return transformerHandler;
    }

    /**
     * Helper method to add an attribute.
     * This implementation adds a new attribute with the given name
     * and value. Before adding the value is checked for non-null.
     * @param ai    The attributes impl receiving the additional attribute.
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     */
    public static void addAttribute(final AttributesImpl ai, final String name, final Object value) {
        if ( value != null ) {
            ai.addAttribute("", name, name, "CDATA", value.toString());
        }
    }

    /**
     * Helper method writing out a string.
     * @param ch    The content handler.
     * @param text
     * @throws SAXException
     */
    public static void text(final ContentHandler ch, final String text)
    throws SAXException {
        if ( text != null ) {
            final char[] c = text.toCharArray();
            ch.characters(c, 0, c.length);
        }
    }

    /**
     * Helper method to indent the xml elements.
     * Each level is indented with four spaces.
     * @param ch    The content handler.
     * @param level The level of indention.
     */
    public static void indent(final ContentHandler ch, final int level)
    throws SAXException {
        for(int i=0;i<level;i++) {
            IOUtils.text(ch, "    ");
        }
    }

    /**
     * Helper method to create a new line.
     * @param ch    The content handler.
     * @throws SAXException
     */
    public static void newline(final ContentHandler ch)
    throws SAXException {
        IOUtils.text(ch, "\n");
    }
}
