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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.metadata.Element;

/**
 * A {@code FileMetadataProvider} is responsible of loading all the {@literal .xml} files in the given directory.
 * It also accepts a direct reference to a {@literal metadata.xml} file.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FileMetadataProvider implements MetadataProvider {

    /**
     * Metadata source (file or directory).
     */
    private File m_source;

    /**
     * Feedback reporter.
     */
    private Reporter m_reporter;

    /**
     * Validate using local schemas or not ?
     */
    private boolean m_validateUsingLocalSchemas = false;

    /**
     * Constructs a metadata provider using the given source File (directory
     * or file) to load iPOJO metadata.
     * @param source source of the metadata
     * @param reporter feedback reporter
     */
    public FileMetadataProvider(File source, Reporter reporter) {
        m_source = source;
        m_reporter = reporter;
    }

    public void setValidateUsingLocalSchemas(boolean validateUsingLocalSchemas) {
        m_validateUsingLocalSchemas = validateUsingLocalSchemas;
    }

    public List<Element> getMetadatas() throws IOException {

        List<Element> metadata = new ArrayList<Element>();
        traverse(m_source, metadata);
        return metadata;
    }

    private void traverse(File file, List<Element> metadata) {
        if (file.isDirectory()) {
            // Traverse the directory and parse all files.
            File[] files = file.listFiles();
            for (File child : files) {
                traverse(child, metadata);
            }
        } else if (file.getName().endsWith(".xml")) { // Detect XML by extension,
                                                      // others are ignored.
            loadFileMetadata(file, metadata);
        }

    }

    private void loadFileMetadata(File file, List<Element> metadata) {
        try {
            // Sanity check, should be OK, but we never know ...
            InputStream stream = null;
            URL url = file.toURI().toURL();
            stream = url.openStream();
            StreamMetadataProvider provider = new StreamMetadataProvider(stream, m_reporter);
            provider.setValidateUsingLocalSchemas(m_validateUsingLocalSchemas);
            metadata.addAll(provider.getMetadatas());
        } catch (MalformedURLException e) {
            m_reporter.error("Cannot open the metadata input stream from " + m_source.getAbsolutePath() + ": " + e.getMessage());
        } catch (IOException e) {
            m_reporter.error("Cannot open the metadata input stream: " + m_source.getAbsolutePath() + ": " + e.getMessage());
        }

    }
}
