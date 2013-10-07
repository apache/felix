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

package org.apache.felix.ipojo.bnd;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.util.Constants;
import org.apache.felix.ipojo.manipulator.util.Metadatas;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.metadata.Element;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.service.reporter.Reporter;

public class BndJarResourceStore implements ResourceStore {

    private Analyzer m_analyzer;
    private Reporter m_reporter;

    private MetadataRenderer m_renderer = new MetadataRenderer();

    private List<Element> m_metadata;
    private boolean m_includeEmbedComponents;

    public BndJarResourceStore(Analyzer analyzer, Reporter reporter) {
        m_metadata = new ArrayList<Element>();
        m_analyzer = analyzer;
        m_reporter = reporter;
    }

    public byte[] read(String path) throws IOException {

        // Find the resource either in the global jar or in one of the embed dependencies
        Resource resource = m_analyzer.getJar().getResource(path);
        if (resource == null) {
            Jar embed = findJar(path);
            if (embed == null) {
                throw new IOException(format("Cannot find resource %s in jar and classpath", path));
            }
            resource = embed.getResource(path);
        }
        InputStream is = null;
        try {
            is = resource.openInputStream();
        } catch (Exception e) {
            throw new IOException("Cannot read " + path);
        }
        return Streams.readBytes(is);
    }

    public void accept(ResourceVisitor visitor) {

        try {
            // Collect all annotated classes
            Collection<Clazz> classes = m_analyzer.getClasses("",
                    Clazz.QUERY.CLASSANNOTATIONS.name());

            classes = filter(classes);

            // Iterates over discovered resources
            for (Clazz clazz : classes) {
                visitor.visit(clazz.getAbsolutePath());
            }
        } catch (Exception e) {
            m_reporter.error("Cannot find iPOJO annotated types: " + e.getMessage());
        }
    }

    private Collection<Clazz> filter(Collection<Clazz> classes) throws Exception {
        Set<Clazz> manipulable = new HashSet<Clazz>();
        for (Clazz clazz : classes) {

            // If it is in the main jar, simply use it
            if (m_analyzer.getJar().getResource(clazz.getAbsolutePath()) != null) {
                manipulable.add(clazz);
                continue;
            }

            if (m_includeEmbedComponents) {
                // Otherwise ...
                // Try to see if it is in an embed dependencies
                Jar jar = findJar(clazz.getAbsolutePath());
                if (jar == null) {
                    m_reporter.error("Resource for class %s not found in classpath", clazz.getFQN());
                    continue;
                }

                // Is it a Bundle ?
                if (jar.getBsn() != null) {
                    // OSGi Bundle case

                    // Check if the bundle was manipulated before
                    Attributes attr = jar.getManifest().getMainAttributes();
                    if (Manifests.getComponents(attr) != null) {
                        // Bundle has been previously manipulated
                        // TODO We should ignore the resource since it was already manipulated
                        // TODO But we should also merge its IPOJO-Components header
                    } else {
                        // Bundle was not manipulated
                        manipulable.add(clazz);
                    }

                } else  {
                    // Simple Jar file with iPOJO annotations
                    m_reporter.warning("Class %s found in a non-Bundle archive %s", clazz.getFQN(), jar.getName());
                    continue;
                }
            } else {
                m_reporter.warning("Embed components are excluded, Component %s will not be manipulated", clazz.getFQN());
            }
        }
        return manipulable;
    }

    private Jar findJar(String path) {
        for (Jar jar : m_analyzer.getClasspath()) {
            if (jar.getResource(path) != null) {
                return jar;
            }
        }
        return null;
    }

    public void open() throws IOException {
        // nothing to do
    }

    public void writeMetadata(Element metadata) {
        m_metadata.add(metadata);

        // Find referred packages and add them into Bnd
        for (String referred : Metadatas.findReferredPackages(metadata)) {
            if (!m_analyzer.getReferred().containsFQN(referred)) {
                // The given package is not referred ATM
                m_analyzer.getReferred().put(m_analyzer.getPackageRef(referred),
                                             new Attrs());
            }
        }

        // IPOJO-Components will be written during the close method.
    }

    public void write(String resourcePath, byte[] resource) throws IOException {
        Jar jar = m_analyzer.getJar();
        jar.putResource(resourcePath, new ByteArrayResource(resource));
    }

    public void close() throws IOException {

        // Write the iPOJO header (including manipulation metadata)
        StringBuilder builder = new StringBuilder();

        if (m_includeEmbedComponents) {
            // Incorporate metadata of embed dependencies (if any)
            for (Jar jar : m_analyzer.getClasspath()) {
                try {
                    Manifest manifest = jar.getManifest();
                    Attributes main = manifest.getMainAttributes();
                    String components = Manifests.getComponents(main);
                    if (components != null) {
                        m_reporter.trace("Merging components from %s", jar.getName());
                        builder.append(components);
                    }
                } catch (Exception e) {
                    m_reporter.warning("Cannot open MANIFEST of %s", jar.getName());
                }
            }
        }

        for (Element metadata : m_metadata) {
            builder.append(m_renderer.render(metadata));
        }

        if (builder.length() != 0) {
            m_analyzer.setProperty("IPOJO-Components", builder.toString());
        }

        // Add some mandatory imported packages
        Attrs version = new Attrs();
        version.put("version:Version", Constants.getPackageImportClause());

        if (!m_analyzer.getReferred().containsFQN("org.apache.felix.ipojo")) {
            m_analyzer.getReferred().put(m_analyzer.getPackageRef("org.apache.felix.ipojo"),
                                         version);
        }
        if (!m_analyzer.getReferred().containsFQN("org.apache.felix.ipojo.architecture")) {
            m_analyzer.getReferred().put(m_analyzer.getPackageRef("org.apache.felix.ipojo.architecture"),
                                         version);
        }
        if (!m_analyzer.getReferred().containsFQN("org.osgi.service.cm")) {
            Attrs cm = new Attrs();
            cm.put("version:Version", "1.2");
            m_analyzer.getReferred().put(m_analyzer.getPackageRef("org.osgi.service.cm"),
                                         cm);
        }
        if (!m_analyzer.getReferred().containsFQN("org.osgi.service.log")) {
            Attrs log = new Attrs();
            log.put("version:Version", "1.3");
            m_analyzer.getReferred().put(m_analyzer.getPackageRef("org.osgi.service.log"),
                                         log);
        }


    }

    public void setIncludeEmbedComponents(boolean excludeEmbedComponents) {
        m_includeEmbedComponents = excludeEmbedComponents;
    }
}
