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
    private File source;

    /**
     * Target directory.
     */
    private File target;

    /**
     * The builder of the updated manifest.
     */
    private ManifestBuilder manifestBuilder;

    /**
     * Original manifest to be updated.
     */
    private Manifest manifest;

    /**
     * Resource Mapper.
     */
    private ResourceMapper mapper = new FileSystemResourceMapper(new IdentityResourceMapper());

    public DirectoryResourceStore(File source) {
        this(source, source);
    }

    public DirectoryResourceStore(File source, File target) {
        this.source = source;
        this.target = target;
    }

    public void setResourceMapper(ResourceMapper mapper) {
        this.mapper = new FileSystemResourceMapper(mapper);
    }

    public void setManifestBuilder(ManifestBuilder manifestBuilder) {
        this.manifestBuilder = manifestBuilder;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }

    public byte[] read(String path) throws IOException {
        File resource = new File(source, mapper.internalize(path));
        if (!resource.isFile()) {
            throw new IOException("File '" + resource + "' is not found (for class " + path + ").");
        }
        return Streams.readBytes(new FileInputStream(resource));
    }

    public void accept(ResourceVisitor visitor) {
        traverseDirectory(source, visitor);
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
        String relative = file.getPath().substring(source.getPath().length());
        return mapper.externalize(relative);
    }

    public void open() throws IOException {

        // Update the manifest
        Manifest updated = manifestBuilder.build(manifest);

        // Write it on disk
        File metaInf = new File(target, "META-INF");
        File resource = new File(metaInf, "MANIFEST.MF");
        OutputStream os = new FileOutputStream(resource);
        try {
            updated.write(os);
        } finally {
            Streams.close(os);
        }

    }

    public void writeMetadata(Element metadata) {
        manifestBuilder.addMetada(Collections.singletonList(metadata));
        manifestBuilder.addReferredPackage(Metadatas.findReferredPackages(metadata));
    }

    public void write(String resourcePath, byte[] bytecode) throws IOException {

        // Internalize the name
        File resource = new File(target, mapper.internalize(resourcePath));

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
