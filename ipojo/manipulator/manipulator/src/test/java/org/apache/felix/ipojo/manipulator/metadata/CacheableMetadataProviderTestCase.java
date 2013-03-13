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

import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.metadata.Element;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class CacheableMetadataProviderTestCase extends TestCase {
    public void testGetMetadatas() throws Exception {
        MetadataProvider delegate = mock(MetadataProvider.class);
        CacheableMetadataProvider provider = new CacheableMetadataProvider(delegate);

        Element returned = new Element("test", null);
        when(delegate.getMetadatas()).thenReturn(Collections.singletonList(returned));

        provider.getMetadatas();
        provider.getMetadatas();
        provider.getMetadatas();
        provider.getMetadatas();

        verify(delegate, atMost(1)).getMetadatas();

    }
}
