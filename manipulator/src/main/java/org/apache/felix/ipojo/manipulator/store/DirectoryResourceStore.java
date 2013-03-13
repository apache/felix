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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.store.mapper.FileSystemResourceMapper;
import org.apache.felix.ipojo.manipulator.store.mapper.IdentityResourceMapper;
import org.apache.felix.ipojo.manipulator.util.Metadatas;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.metadata.Element;

/**
 * A {@link DirectoryResourceStore} knows how to read and write
 * resources from (to respectively) a File directory.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DirectoryResourceStore implements ResourceStore {

    /**
     * Source directory where bytecode is read.
     */
    private File m_source;

    /**
     * Target directory.
     */
    private File m_target;

    /**
     * The builder of the updated manifest.
     */
    private ManifestBuilder m_manifestBuilder;

    /**
     * Original manifest to be updated.
     */
    private Manifest m_manifest;
    
    /**
     * Location of the manifest file to update.
     */
    private File m_manifest_file;

    /**
     * Resource Mapper.
     */
    private ResourceMapper m_mapper = new FileSystemResourceMapper(new IdentityResourceMapper());

    public DirectoryResourceStore(File source) {
        this(source, source);
    }

    public DirectoryResourceStore(File source, File target) {
        m_source = source;
        m_target = target;
    }

    public void setResourceMapper(ResourceMapper mapper) {
        m_mapper = new FileSystemResourceMapper(mapper);
    }

    public void setManifestBuilder(ManifestBuilder manifestBuilder) {
        m_manifestBuilder = manifestBuilder;
    }

    public void setManifest(Manifest manifest) {
        m_manifest = manifest;
    }
    
    public void setManifestFile(File manifestFile){
    	m_manifest_file = manifestFile;
    }

    public byte[] read(String path) throws IOException {
        File resource = new File(m_source, m_mapper.internalize(path));
        if (!resource.isFile()) {
            throw new IOException("File '" + resource + "' is not found (for class " + path + ").");
        }
        return Streams.readBytes(new FileInputStream(resource));
    }

    public void accept(ResourceVisitor visitor) {
        traverseDirectory(m_source, visitor);
    }

    private void traverseDirectory(File directory, ResourceVisitor visitor) {
        for (File child : directory.listFiles()) {
            if (child.isDirectory()) {
                traverseDirectory(child, visitor);
            } else {
                visitor.visit(getRelativeName(child));
            }
        }
    }

    private String getRelativeName(File file) {
        String relative = file.getPath().substring(m_source.getPath().length());
        return m_mapper.externalize(relative);
    }

    public void open() throws IOException {

        // Update the manifest
        Manifest updated = m_manifestBuilder.build(m_manifest);
        
        // Write it to disk
        OutputStream os = new FileOutputStream(m_manifest_file);
        try {
            updated.write(os);
        } finally {
            Streams.close(os);
        }

    }

    public void writeMetadata(Element metadata) {
        m_manifestBuilder.addMetada(Collections.singletonList(metadata));
        m_manifestBuilder.addReferredPackage(Metadatas.findReferredPackages(metadata));
    }

    public void write(String resourcePath, byte[] bytecode) throws IOException {

        // Internalize the name
        File resource = new File(m_target, m_mapper.internalize(resourcePath));

        // Create intermediate directories if needed
        if (!resource.getParentFile().exists()) {
            resource.getParentFile().mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(resource);
        try {
            fos.write(bytecode);
            fos.flush();
        } finally {
            Streams.close(fos);
        }
    }

    public void close() throws IOException {
        // Nothing to do
    }
}
