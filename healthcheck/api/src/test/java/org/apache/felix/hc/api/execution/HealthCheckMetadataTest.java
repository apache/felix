/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.api.execution;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Vector;

import org.apache.felix.hc.api.HealthCheck;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class HealthCheckMetadataTest {

    @Mock
    ServiceReference<HealthCheck> hcServiceRef;
    
    @Before
    public void setup() {
        initMocks(this);
        when(hcServiceRef.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
    }
    
    @Test
    public void testTagObjectConversion() {

        when(hcServiceRef.getProperty(HealthCheck.TAGS)).thenReturn("singleTag");
        assertEquals(Arrays.asList("singleTag"), new HealthCheckMetadata(hcServiceRef).getTags());

        when(hcServiceRef.getProperty(HealthCheck.TAGS)).thenReturn(new String[] {"tag1", "tag2"});
        assertEquals(Arrays.asList("tag1", "tag2"), new HealthCheckMetadata(hcServiceRef).getTags());

        Vector<String> tags = new Vector<>();
        tags.addAll(Arrays.asList("tag1", "tag2"));
        when(hcServiceRef.getProperty(HealthCheck.TAGS)).thenReturn(tags);
        assertEquals(Arrays.asList("tag1", "tag2"), new HealthCheckMetadata(hcServiceRef).getTags());

    }

}
