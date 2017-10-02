/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.maven.osgicheck.impl.checks;

import java.util.List;

import org.apache.felix.maven.osgicheck.impl.Check;
import org.apache.felix.maven.osgicheck.impl.CheckContext;
import org.apache.felix.maven.osgicheck.impl.featureutil.ManifestUtil;
import org.apache.felix.maven.osgicheck.impl.featureutil.PackageInfo;
import org.apache.maven.plugin.MojoExecutionException;
import org.osgi.framework.Version;

/**
 * The following checks are performed:
 * <ul>
 *   <li>Exports without a version (ERROR)
 *   <li>Import without a version (range) (WARNING)
 *   <li>Dynamic import without a version (range) (WARNING)
 *   <li>Dynamic import * (WARNING)
 *   <li>Export of private looking package (WARNING)
 * </ul>
 */
public class ImportExportCheck implements Check {

    @Override
    public String getName() {
        return "packages";
    }

    @Override
    public void check(final CheckContext ctx) throws MojoExecutionException {
        ctx.getLog().info("Checking bundle exports/imports...");
        final List<PackageInfo> exp = ManifestUtil.extractExportedPackages(ctx.getManifest());
        for(final PackageInfo p : exp) {
            if ( p.getPackageVersion().compareTo(Version.emptyVersion) == 0 ) {
                ctx.reportError("Package is exported without version " + p.getName());
            }
            if ( p.getName().contains(".impl.") || p.getName().contains(".internal.") ) {
                ctx.reportWarning("Package seems to export a private package " + p.getName());
            }
        }

        final List<PackageInfo> imp = ManifestUtil.extractImportedPackages(ctx.getManifest());
        for(final PackageInfo p : imp) {
            if ( p.getVersion() == null ) {
                ctx.reportWarning("Package is imported without version (range) " + p.getName());
            }
        }

        final List<PackageInfo> dynImp = ManifestUtil.extractDynamicImportedPackages(ctx.getManifest());
        for(final PackageInfo p : dynImp) {
            if ( "*".equals(p.getName()) ) {
                ctx.reportWarning("Dynamic package import * should be avoided");
            } else {
                if ( p.getVersion() == null ) {
                    ctx.reportWarning("Dynamic package is imported without version (range) " + p.getName());
                }
            }
        }
    }
}
