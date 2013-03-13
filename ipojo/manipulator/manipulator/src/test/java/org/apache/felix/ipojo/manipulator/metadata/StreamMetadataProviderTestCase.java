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
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.metadata.Element;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StreamMetadataProviderTestCase extends TestCase {

    public void testGetMetadatas() throws Exception {
        File metadata = new File(new File("target", "test-classes"), "metadata.xml");
        FileInputStream fis = new FileInputStream(metadata);
        Reporter reporter = mock(Reporter.class);

        StreamMetadataProvider provider = new StreamMetadataProvider(fis, reporter);
        provider.setValidateUsingLocalSchemas(true);

        List<Element> meta = provider.getMetadatas();
        assertEquals(3, meta.size());
    }

    public void testWithEmptyMetadataXml() throws Exception {
        File metadata = new File(new File("target", "test-classes"), "empty-metadata.xml");
        FileInputStream fis = new FileInputStream(metadata);
        Reporter reporter = mock(Reporter.class);

        StreamMetadataProvider provider = new StreamMetadataProvider(fis, reporter);
        provider.setValidateUsingLocalSchemas(true);

        List<Element> meta = provider.getMetadatas();
        assertEquals(0, meta.size());
        verify(reporter).warn(anyString());
    }
}
