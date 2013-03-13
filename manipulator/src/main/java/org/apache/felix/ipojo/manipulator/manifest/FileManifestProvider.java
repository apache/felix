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

package org.apache.felix.ipojo.manipulator.manifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.ManifestProvider;
import org.apache.felix.ipojo.manipulator.util.Streams;

/**
 * A {@code FileManifestProvider} reads a {@link Manifest} from the given input {@link File}.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FileManifestProvider implements ManifestProvider {

    /**
     * Read manifest.
     */
    private Manifest m_manifest;

    /**
     * Read the manifest from the given input file
     * @param resource File pointing to a Manifest file
     * @throws IOException if there is an error during File IO operations
     *         or if the file is not a valid manifest.
     */
    public FileManifestProvider(File resource) throws IOException {
        m_manifest = new Manifest();
        InputStream is = null;
        try {
            is = new FileInputStream(resource);
            m_manifest.read(is);
        } finally {
            Streams.close(is);
        }
    }

    public Manifest getManifest() {
        return m_manifest;
    }
}
