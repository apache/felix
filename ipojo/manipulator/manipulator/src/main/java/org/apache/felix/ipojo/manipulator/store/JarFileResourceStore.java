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

package org.apache.felix.ipojo.manipulator.store;

import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.store.mapper.IdentityResourceMapper;
import org.apache.felix.ipojo.manipulator.util.Metadatas;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.metadata.Element;

import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * A {@link JarFileResourceStore} knows how to read and write
 * resources from (to respectively) a Jar File.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class JarFileResourceStore implements ResourceStore {

    /**
     * Source Jar.
     */
    private JarFile m_source;

    /**
     * Target File.
     */
    private File m_target;

    /**
     * Modified resources.
     */
    private Map<String, byte[]> m_content;

    /**
     * Resource Mapper.
     */
    private ResourceMapper m_mapper = new IdentityResourceMapper();

    /**
     * The builder of the updated manifest.
     */
    private ManifestBuilder m_manifestBuilder;

    /**
     * Original manifest to be updated.
     */
    private Manifest m_manifest;

    private ClassLoader classLoader;

    /**
     * Construct a {@link JarFileResourceStore} wrapping the given original bundle,
     * and configured to output in the given target file.
     *
     * @param source original Bundle
     * @param target File where the updated Bundle will be outputted
     * @throws IOException if there is an error retrieving the Manifest from the original JarFile
     */
    public JarFileResourceStore(JarFile source, File target) throws IOException {
        m_source = source;
        m_target = target;

        // TODO ensure File is not null and not an existing file/directory
        this.m_target = target;
        if (source != null) {
            m_manifest = source.getManifest();
        } else {
            m_manifest = new Manifest();
        }
        m_content = new TreeMap<String, byte[]>();
    }

    public void setResourceMapper(ResourceMapper mapper) {
        this.m_mapper = mapper;
    }

    public void setManifestBuilder(ManifestBuilder manifestBuilder) {
        this.m_manifestBuilder = manifestBuilder;
    }

    public void setManifest(Manifest manifest) {
        this.m_manifest = manifest;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public byte[] read(String path) throws IOException {
        ZipEntry entry = m_source.getEntry(getInternalPath(path));
        if (entry == null) {
            // Not in the Jar file, trying from classpath
            return tryToLoadFromClassloader(path);
        }
        return Streams.readBytes(m_source.getInputStream(entry));
    }

    private byte[] tryToLoadFromClassloader(String path) throws IOException {
        if (classLoader != null) {
            byte[] bytes = toByteArray(classLoader.getResource(path));
            if (bytes != null) {
                return bytes;
            }
        }
        throw new IOException("Class not found " + path + ".");
    }

    public static byte[] toByteArray(URL url) throws IOException {
        if (url == null) {
            return null;
        }
        InputStream input = url.openStream();
        try {
            return Streams.readBytes(input);
        } finally {
            Streams.close(input);
        }

    }

    private String getInternalPath(String path) {
        return m_mapper.internalize(path);
    }

    public void accept(ResourceVisitor visitor) {
        List<JarEntry> entries = Collections.list(m_source.entries());
        for (JarEntry entry : entries) {
            String name = entry.getName();
            if (!name.endsWith("/")) {
                // Do not visit directories
                visitor.visit(getExternalName(entry.getName()));
            }
        }
    }

    private String getExternalName(String path) {
        return m_mapper.externalize(path);
    }

    public void open() throws IOException {
        // Nothing to do
    }

    public void writeMetadata(Element metadata) {
        m_manifestBuilder.addMetada(Collections.singletonList(metadata));
        m_manifestBuilder.addReferredPackage(Metadatas.findReferredPackages(metadata));
    }

    public void write(String resourcePath, byte[] resource) throws IOException {
        this.m_content.put(getInternalPath(resourcePath), resource);
    }

    public void close() throws IOException {

        // Update the manifest
        Manifest updated = m_manifestBuilder.build(m_manifest);

        // Create a new Jar file
        FileOutputStream fos = new FileOutputStream(m_target);
        JarOutputStream jos = new JarOutputStream(fos, updated);

        try {
            // Copy classes and resources
            List<JarEntry> entries = Collections.list(m_source.entries());
            for (JarEntry entry : entries) {

                // Ignore some entries (MANIFEST, ...)
                if (isIgnored(entry)) {
                    continue;
                }

                if (isUpdated(entry)) {
                    // Write newer/updated resource (manipulated classes, ...)

                    JarEntry je = new JarEntry(entry.getName());
                    byte[] data = m_content.get(getInternalPath(entry.getName()));
                    jos.putNextEntry(je);
                    jos.write(data);
                    jos.closeEntry();
                } else {
                    // Copy the resource as-is

                    jos.putNextEntry(entry);
                    InputStream is = m_source.getInputStream(entry);
                    try {
                        Streams.transfer(is, jos);
                    } finally {
                        Streams.close(is);
                    }

                    jos.closeEntry();

                }
            }
        } finally {
            try {
                m_source.close();
            } catch (IOException e) {
                // Ignored
            }
            Streams.close(jos, fos);

        }
    }

    private boolean isUpdated(JarEntry entry) {
        // Check if that class was manipulated
        if (entry.getName().endsWith(".class")) {
            // Need to map this into an external+normalized path
            String cleaned = getExternalName(entry.getName());
            return m_content.containsKey(cleaned);
        } else {
            return false;
        }
    }

    private boolean isIgnored(JarEntry entry) {
        return "META-INF/MANIFEST.MF".equals(entry.getName());
    }
}
