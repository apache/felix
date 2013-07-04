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

package org.apache.felix.ipojo.bnd;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.StreamMetadataProvider;
import org.apache.felix.ipojo.metadata.Element;

import aQute.bnd.osgi.Resource;

/**
 * A {@code ResourceMetadataProvider} is ...
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceMetadataProvider implements MetadataProvider {

    private Resource m_resource;

    private List<Element> m_cached;

    private boolean m_validateUsingLocalSchemas = false;

    private Reporter m_reporter;

    public ResourceMetadataProvider(Resource resource, Reporter reporter) {
        m_resource = resource;
        m_reporter = reporter;
    }

    public void setValidateUsingLocalSchemas(boolean validateUsingLocalSchemas) {
        this.m_validateUsingLocalSchemas = validateUsingLocalSchemas;
    }

    public List<Element> getMetadatas() throws IOException {
        if (m_cached == null) {
            m_cached = new ArrayList<Element>();
            InputStream stream = null;
            try {
                stream = m_resource.openInputStream();
            } catch (Exception e) {
                m_reporter.error("%s", e.getMessage());
                throw new IOException("Cannot read metadata", e);
            }
            StreamMetadataProvider provider = new StreamMetadataProvider(stream, m_reporter);
            provider.setValidateUsingLocalSchemas(m_validateUsingLocalSchemas);
            m_cached.addAll(provider.getMetadatas());
        }

        return m_cached;
    }
}
