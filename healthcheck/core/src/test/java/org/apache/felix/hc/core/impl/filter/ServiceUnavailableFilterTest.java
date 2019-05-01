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
package org.apache.felix.hc.core.impl.filter;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ServiceUnavailableFilterTest {

    @Mock
    BundleContext bundleContext;

    @Mock
    Bundle bundle1;

    @Mock
    Bundle bundle2;

    ServiceUnavailableFilter serviceUnavailableFilter = new ServiceUnavailableFilter();

    @Before
    public void setup() {
        initMocks(this);

        doReturn(new Bundle[] { bundle1, bundle2 }).when(bundleContext).getBundles();
        doReturn("bundle1").when(bundle1).getSymbolicName();
        doReturn("bundle2").when(bundle2).getSymbolicName();

    }

    @Test
    public void testGetResponseTextSimple() {

        assertEquals("test", serviceUnavailableFilter.getResponseText(bundleContext, "test"));
    }

    @Test
    public void testGetResponseTextFromBundle() throws IOException {

        mockUrl(bundle1, "/path.html", "testFromFile");
        assertEquals("testFromFile", serviceUnavailableFilter.getResponseText(bundleContext, "classpath:bundle1:/path.html"));
    }

    @Test
    public void testGetResponseTextFromBundleErrors() throws IOException {

        mockUrl(bundle1, "/path.html", "testFromFile");

        assertThat(serviceUnavailableFilter.getResponseText(bundleContext, "classpath:bundle2:/path.html"), containsString("file not found"));
        assertThat(serviceUnavailableFilter.getResponseText(bundleContext, "classpath:bundle3:/path.html"), containsString("bundle not found"));
    }

    private void mockUrl(Bundle bundle, String path, String result) throws IOException {
        URLConnection mockConnection = Mockito.mock(URLConnection.class);
        doReturn(new ByteArrayInputStream(result.getBytes())).when(mockConnection).getInputStream();

        final URLStreamHandler handler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(final URL arg0)
                    throws IOException {
                return mockConnection;
            }
        };

        doReturn(new URL("file:/" + path, "host", 80, "", handler)).when(bundle1).getEntry(path);
    }

}
