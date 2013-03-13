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

package org.apache.felix.ipojo.manipulator.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.metadata.Element;

/**
 * A {@code CacheableMetadataProvider} provides simple caching methods to avoid
 * calling {@link MetadataProvider#getMetadatas()} twice.
 *
 * TODO Do we really need this ? ATM it is not used ...
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CacheableMetadataProvider implements MetadataProvider {

    /**
     * Delegate.
     */
    private MetadataProvider m_delegate;

    /**
     * Cached elements.
     */
    private List<Element> m_cached;

    public CacheableMetadataProvider(MetadataProvider delegate) {
        m_delegate = delegate;
    }

    public List<Element> getMetadatas() throws IOException {

        // Only ask the delegate if cache is empty
        if (m_cached == null) {
            m_cached = new ArrayList<Element>();
            m_cached.addAll(m_delegate.getMetadatas());
        }
        return m_cached;
    }
}
