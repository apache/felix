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

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Resource;
import aQute.libg.reporter.Reporter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.util.Metadatas;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.metadata.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class BndJarResourceStore implements ResourceStore {

    private Analyzer analyzer;
    private Reporter reporter;

    private MetadataRenderer renderer = new MetadataRenderer();

    public BndJarResourceStore(Analyzer analyzer, Reporter reporter) {
        this.analyzer = analyzer;
        this.reporter = reporter;
    }

    public byte[] read(String path) throws IOException {
        Resource resource = analyzer.getJar().getResource(path);
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
            // Only visit classes annotated with @Component or @Handler
            String annotations = Component.class.getName() + "|" + Handler.class.getName();

            Collection<Clazz> classes = analyzer.getClasses("",
                    Clazz.QUERY.ANNOTATION.name(), annotations,
                    Clazz.QUERY.NAMED.name(), "*");

            // Iterates over discovered resources
            for (Clazz clazz : classes) {
                visitor.visit(clazz.getPath());
            }
        } catch (Exception e) {
            reporter.error("Cannot find iPOJO annotated types: " + e.getMessage());
        }
    }

    public void open() throws IOException {
        // nothing to do
    }

    public void writeMetadata(Element metadata) {

        // Find referred packages and add them into Bnd
        for (String referred : Metadatas.findReferredPackages(metadata)) {
            if (analyzer.getReferred().get(referred) == null) {
                // The given package is not referred ATM
                analyzer.getReferred().put(referred, new HashMap<String, String>());
            }
        }

        // Write the iPOJO header
        String components = analyzer.getProperty("IPOJO-Components");
        StringBuilder builder = new StringBuilder();

        if (components != null) {
            builder.append(components);
        }
        builder.append(renderer.render(metadata));

        if (builder.length() != 0) {
            analyzer.setProperty("IPOJO-Components", builder.toString());
        }


    }

    public void write(String resourcePath, byte[] resource) throws IOException {
        Jar jar = analyzer.getJar();
        jar.putResource(resourcePath, new ByteArrayResource(resource));
    }

    public void close() throws IOException {

        // Add some mandatory imported packages
        Map<String, String> version = new TreeMap<String, String>();
        version.put("version", Pojoization.IPOJO_PACKAGE_VERSION);

        if (analyzer.getReferred().get("org.apache.felix.ipojo") == null) {
            analyzer.getReferred().put("org.apache.felix.ipojo", version);
        }
        if (analyzer.getReferred().get("org.apache.felix.ipojo.architecture") == null) {
            analyzer.getReferred().put("org.apache.felix.ipojo.architecture", version);
        }
        if (analyzer.getReferred().get("org.osgi.service.cm") == null) {
            Map<String, String> cm = new TreeMap<String, String>();
            cm.put("version", "1.2");
            analyzer.getReferred().put("org.osgi.service.cm", cm);
        }
        if (analyzer.getReferred().get("org.osgi.service.log") == null) {
            Map<String, String> log = new TreeMap<String, String>();
            log.put("version", "1.3");
            analyzer.getReferred().put("org.osgi.service.log", log);
        }

    }
}
