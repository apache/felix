/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.jetty.internal;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/**
 * Unit test for JettyConfig
 */
public class JettyConfigTest
{
    JettyConfig config;
    BundleContext context;

    @Test public void testGetDefaultPort()
    {
        assertEquals("HTTP port", 8080, this.config.getHttpPort());
        assertEquals("HTTPS port", 8443, this.config.getHttpsPort());
    }

    @Test public void testGetPortInRange()
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", "[8000,9000]");
        props.put("org.osgi.service.http.port.secure", "[10000,11000)");
        this.config.update(props);

        assertTrue(this.config.getHttpPort() >= 8000 && this.config.getHttpPort() <= 9000);
        assertTrue(this.config.getHttpsPort() >= 10000 && this.config.getHttpsPort() < 11000);

        props.put("org.osgi.service.http.port", "(12000,13000]");
        props.put("org.osgi.service.http.port.secure", "(14000,15000)");
        this.config.update(props);

        assertTrue(this.config.getHttpPort() > 12000 && this.config.getHttpPort() <= 13000);
        assertTrue(this.config.getHttpsPort() > 14000 && this.config.getHttpsPort() < 15000);

        props.put("org.osgi.service.http.port", "[,9000]");
        props.put("org.osgi.service.http.port.secure", "[9000,)");
        this.config.update(props);

        assertTrue(this.config.getHttpPort() >= 1 && this.config.getHttpPort() <= 9000);
        assertTrue(this.config.getHttpsPort() >= 9000 && this.config.getHttpsPort() < 65535);
    }

    @Test public void testGetPortInvalidRange()
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", "+12000,13000*");
        props.put("org.osgi.service.http.port.secure", "%14000,15000");
        this.config.update(props);

        assertEquals(8080, this.config.getHttpPort());
        assertEquals(8443, this.config.getHttpsPort());
    }

    @Test public void testGetSpecificPortOne() throws Exception
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", "1");
        this.config.update(props);
        assertTrue(this.config.getHttpPort() == 1);
    }

    @Test public void testGetRandomPort()
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", "*");
        props.put("org.osgi.service.http.port.secure", "*");
        this.config.update(props);
        assertTrue(this.config.getHttpPort() != 8080);
        assertTrue(this.config.getHttpsPort() != 433);
    }

    @Test public void testGetRandomPortZero() throws Exception
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", "0");
        this.config.update(props);
        assertTrue(this.config.getHttpPort() != 0);
    }

    @Test public void testGetSpecificPort() throws Exception
    {
        int port = 80;

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", port);
        props.put("org.osgi.service.http.port.secure", port);
        this.config.update(props);
        assertTrue(this.config.getHttpPort() == port);
        assertTrue(this.config.getHttpsPort() == port);
    }

    @Test public void testParseStringArrayProperty() {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("org.apache.felix.https.jetty.ciphersuites.excluded",
                  "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDH_anon_WITH_RC4_128_SHA");
        this.config.update(props);
        String[] expecteds = {"TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDH_anon_WITH_RC4_128_SHA"};
        assertArrayEquals(expecteds, this.config.getExcludedCipherSuites());
    }

    @Before
    public void setUp()
    {
        this.context = createNiceMock(BundleContext.class);
        replay(this.context);
        this.config = new JettyConfig(this.context);
    }
}
