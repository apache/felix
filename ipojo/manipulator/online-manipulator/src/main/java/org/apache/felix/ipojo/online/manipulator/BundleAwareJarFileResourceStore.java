/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.online.manipulator;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.store.JarFileResourceStore;
import org.apache.felix.ipojo.manipulator.store.builder.DefaultManifestBuilder;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

/**
 * User: guillaume
 * Date: 28/02/2014
 * Time: 19:16
 */
public class BundleAwareJarFileResourceStore extends JarFileResourceStore {

    private final BundleContext bundleContext;

    /**
     * @param source original Bundle
     * @param target File where the updated Bundle will be outputted
     * @throws java.io.IOException if there is an error retrieving the Manifest from the original JarFile
     */
    public BundleAwareJarFileResourceStore(final JarFile source,
                                           final File target,
                                           final BundleContext bundleContext) throws IOException {
        super(source, target);
        this.bundleContext = bundleContext;
        DefaultManifestBuilder manifestBuilder = new DefaultManifestBuilder();
        manifestBuilder.setMetadataRenderer(new MetadataRenderer());
        setManifestBuilder(manifestBuilder);
    }

    @Override
    public byte[] read(final String path) throws IOException {
        try {
            return super.read(path);
        } catch (IOException e) {
            // An IOException is thrown when no resource can be found
            // We swallow this exception and try to load the resource from the installed bundles
        }

        String directory = path.substring(0, path.lastIndexOf('/'));
        String filename = path.substring(path.lastIndexOf('/') + 1);

        for (Bundle bundle : bundleContext.getBundles()) {

            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            List<URL> entries = wiring.findEntries(directory, filename, 0);
            for (URL url : entries) {
                try {
                    return Streams.readBytes(url.openStream());
                } catch (IOException e) {
                    // Ignore and move to the next source
                    e.printStackTrace();
                }
            }

        }

        throw new IOException(format("Could not find %s in currently available bundles", path));
    }
}
