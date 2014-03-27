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
package org.apache.felix.deploymentadmin.itest.util;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Provides a bundle data builder.
 */
public class BundleDataBuilder<TYPE extends ArtifactDataBuilder<?>> extends ArtifactDataBuilder<TYPE> {

    private String m_symbolicName;
    private String m_version;

    public BundleDataBuilder() {
        super();
    }

    public TYPE setSymbolicName(String symbolicName) {
        m_symbolicName = symbolicName;
        return getThis();
    }

    public TYPE setVersion(String version) {
        m_version = version;
        return getThis();
    }

    @Override
    ArtifactData build() {
        ArtifactData result = super.build();
        result.setBundleMetadata(m_symbolicName, m_version);
        return result;
    }
    
    String getRequiredHeader(Attributes mainAttributes, String headerName) throws RuntimeException {
        String value = mainAttributes.getValue(headerName);
        if (value == null || value.equals("")) {
            throw new RuntimeException("Missing or invalid " + headerName + " header.");
        }
        return value;
    }

    @Override
    TYPE getThis() {
        return (TYPE) this;
    }

    void retrieveAndSetBundleInformation() throws RuntimeException {
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(m_url.openStream());

            Manifest bundleManifest = jis.getManifest();
            if (bundleManifest == null) {
                throw new RuntimeException("Not a valid manifest in: " + m_url);
            }

            Attributes attributes = bundleManifest.getMainAttributes();

            if (m_symbolicName == null || "".equals(m_symbolicName.trim())) {
                setSymbolicName(getRequiredHeader(attributes, "Bundle-SymbolicName"));
            }

            if (m_version == null || "".equals(m_version.trim())) {
                setVersion(getRequiredHeader(attributes, "Bundle-Version"));
            }
            
            if (m_filename == null || "".equals(m_filename.trim())) {
                setFilename(new File(m_url.getFile()).getName());
            }
            
            setAdditionalBundleInformation(bundleManifest);
        }
        catch (IOException exception) {
            throw new RuntimeException("Failed to retrieve bundle information; set symbolic name and version!", exception);
        }
        finally {
            if (jis != null) {
                try {
                    jis.close();
                }
                catch (IOException exception) {
                    // Ignore...
                }
            }
        }
    }

    void setAdditionalBundleInformation(Manifest bundleManifest) {
        // Nop
    }

    @Override
    void validate() throws RuntimeException {
        retrieveAndSetBundleInformation();
        
        super.validate();
    }
}
