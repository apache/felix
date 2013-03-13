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

package org.apache.felix.ipojo.manipulator.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.xml.parser.ParseException;
import org.apache.felix.ipojo.xml.parser.SchemaResolver;
import org.apache.felix.ipojo.xml.parser.XMLMetadataParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A {@code StreamManifestProvider} knows how to load metadata from an InputStream.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class StreamMetadataProvider implements MetadataProvider {

    private InputStream m_stream;

    private Reporter m_reporter;

    private boolean m_validateUsingLocalSchemas = false;

    public StreamMetadataProvider(InputStream stream, Reporter reporter) {
        m_stream = stream;
        m_reporter = reporter;
    }

    /**
     * Force XML schemas resolution to be performed locally
     */
    public void setValidateUsingLocalSchemas(boolean validateUsingLocalSchemas) {
        m_validateUsingLocalSchemas = validateUsingLocalSchemas;
    }

    public List<Element> getMetadatas() throws IOException {

        // First time we've been called: parse the XML Stream
        List<Element> metadata = new ArrayList<Element>();

        try {
            XMLMetadataParser handler = new XMLMetadataParser();

            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(handler);
            parser.setFeature("http://xml.org/sax/features/validation", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);
            parser.setErrorHandler(handler);
            if (m_validateUsingLocalSchemas) {
                parser.setEntityResolver(new SchemaResolver());
            }

            // Parse the XML
            InputSource is = new InputSource(m_stream);
            parser.parse(is);
            Element[] meta = handler.getMetadata();

            // Add parsed metadata
            if (meta != null) {
                metadata.addAll(Arrays.asList(meta));
            }

            // Output a warning message if no metadata was found
            if (meta == null || meta.length == 0) {
                m_reporter.warn("Neither component types, nor instances in the XML metadata");
            }

        } catch (IOException e) {
            m_reporter.error("Cannot open the metadata input stream: " + e.getMessage());
        } catch (ParseException e) {
            m_reporter.error("Parsing error when parsing the XML file: " + e.getMessage());
        } catch (SAXParseException e) {
            m_reporter.error("Error during metadata parsing at line " + e.getLineNumber() + " : " + e.getMessage());
        } catch (SAXException e) {
            m_reporter.error("Parsing error when parsing (Sax Error) the XML file: " + e.getMessage());
        } finally {
            Streams.close(m_stream);
        }

        return metadata;

    }
}
