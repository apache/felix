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

<<<<<<< HEAD
=======
import java.io.IOException;
import java.io.InputStream;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.net.URL;

public class ArtifactData {

    private final URL m_url;
    private boolean m_isBundle;
    private String m_filename;
    private String m_bundleSymbolicName;
    private String m_bundleVersion;
    private boolean m_isCustomizer;
<<<<<<< HEAD
=======
    private boolean m_needRP;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    private String m_processorPID;
    private boolean m_missing;
    private ResourceFilter m_filter;

    ArtifactData(URL url, String filename) {
        m_url = url;
        m_filename = filename;
    }

<<<<<<< HEAD
=======
    public final InputStream createInputStream() throws IOException {
        if (m_filter != null) {
            return m_filter.createInputStream(m_url);
        }
        return m_url.openStream();
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public String getFilename() {
        return m_filename;
    }

    public ResourceFilter getFilter() {
        return m_filter;
    }

    public String getProcessorPid() {
        return m_processorPID;
    }

    public String getSymbolicName() {
        return m_bundleSymbolicName;
    }

    public URL getURL() {
        return m_url;
    }

    public String getVersion() {
        return m_bundleVersion;
    }

    public boolean isBundle() {
        return m_isBundle;
    }

<<<<<<< HEAD
=======
    public boolean isLocalizationFile() {
        return m_filename.startsWith("OSGI-INF/l10n/");
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean isCustomizer() {
        return m_isCustomizer;
    }

    public boolean isMissing() {
        return m_missing;
    }

<<<<<<< HEAD
=======
    public boolean isResourceProcessorNeeded() {
        return m_needRP;
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    void setArtifactResourceProcessor(String processorPID) {
        m_processorPID = processorPID;
    }

    void setBundleMetadata(String bundleSymbolicName, String bundleVersion) {
        m_isBundle = true;
        m_bundleSymbolicName = bundleSymbolicName;
        m_bundleVersion = bundleVersion;
    }

    void setFilter(ResourceFilter filter) {
        m_filter = filter;
    }

    void setMissing(boolean missing) {
        m_missing = missing;
    }

<<<<<<< HEAD
=======
    void setNeedResourceProcessor(boolean needRP) {
        m_needRP = needRP;
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    void setResourceProcessor(String processorPID) {
        m_isCustomizer = true;
        m_processorPID = processorPID;
    }
}
