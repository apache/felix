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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides a builder for creating {@link ArtifactData} instances.
 */
public abstract class ArtifactDataBuilder<TYPE extends ArtifactDataBuilder<?>> {
    
    protected URL m_url;
    protected String m_filename;
    protected ResourceFilter m_filter;
    protected boolean m_missing;
    
    ArtifactDataBuilder() {
        m_filter = null;
        m_missing = false;
    }
    
    public TYPE setFilename(String filename) {
        m_filename = filename;
        return getThis();
    }
    
    public TYPE setFilter(ResourceFilter filter) {
        m_filter = filter;
        return getThis();
    }
    
    public TYPE setMissing() {
        return setMissing(true);
    }
    
    public TYPE setMissing(boolean missing) {
        m_missing = missing;
        return getThis();
    }

    public TYPE setUrl(String url) throws MalformedURLException {
        return setUrl(new URL(url));
    }
 
    public TYPE setUrl(URL url) {
        m_url = url;
        return getThis();
    }
    
    ArtifactData build() {
        validate();

        ArtifactData result = new ArtifactData(m_url, m_filename);
        result.setFilter(m_filter);
        result.setMissing(m_missing);
        return result;
    }
    
    abstract TYPE getThis(); 
    
    void validate() throws RuntimeException {
        if (m_url == null) {
            throw new RuntimeException("URL is missing!");
        }
        if (m_filename == null || "".equals(m_filename.trim())) {
            throw new RuntimeException("Filename is missing!");
        }
    }
}
