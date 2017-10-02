/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.maven.osgicheck.impl.featureutil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.sling.commons.osgi.ManifestHeader;
import org.osgi.framework.Constants;

/*
 * This is a simplified copy of the Apache Sling Feature ManifestUtil class.
 * We keep this copy until the feature module is released in Sling.
 */
public class ManifestUtil {

    /**
     * Get the manifest from the artifact.
     * @param artifact The file
     * @throws IOException If the manifest can't be read
     */
    public static Manifest getManifest(final File artifact) throws IOException {
        try (final JarFile file = new JarFile(artifact) ) {
            return file.getManifest();
        }
    }

    public static List<PackageInfo> extractPackages(final Manifest m,
            final String headerName,
            final String defaultVersion,
            final boolean checkOptional) {
        final String pckInfo = m.getMainAttributes().getValue(headerName);
        if (pckInfo != null) {
            final ManifestHeader header = ManifestHeader.parse(pckInfo);

            final List<PackageInfo> pcks = new ArrayList<>();
            for(final ManifestHeader.Entry entry : header.getEntries()) {
                String version = entry.getAttributeValue("version");
                if ( version == null ) {
                    version = defaultVersion;
                }
                boolean optional = false;
                if ( checkOptional ) {
                    final String resolution = entry.getDirectiveValue("resolution");
                    optional = "optional".equalsIgnoreCase(resolution);
                }
                final PackageInfo pck = new PackageInfo(entry.getValue(),
                        version,
                        optional);
                pcks.add(pck);
            }

            return pcks;
        }
        return Collections.emptyList();
    }

    public static List<PackageInfo> extractExportedPackages(final Manifest m) {
        return extractPackages(m, Constants.EXPORT_PACKAGE, "0.0.0", false);
    }

    public static List<PackageInfo> extractImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.IMPORT_PACKAGE, null, true);
    }

    public static List<PackageInfo> extractDynamicImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.DYNAMICIMPORT_PACKAGE, null, false);
    }
}
