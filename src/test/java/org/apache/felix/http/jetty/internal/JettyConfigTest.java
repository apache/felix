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

import java.net.ServerSocket;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;

/**
 * Unit test for JettyConfig
 */
public class JettyConfigTest extends TestCase
{
    JettyConfig config;
    BundleContext context;

    public void testGetDefaultPort()
    {
        assertEquals("HTTP port", 8080, this.config.getHttpPort());
        assertEquals("HTTPS port", 8443, this.config.getHttpsPort());
    }

    public void testGetPortInRange()
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

    public void testGetPortInvalidRange()
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", "+12000,13000*");
        props.put("org.osgi.service.http.port.secure", "%14000,15000");
        this.config.update(props);

        assertEquals(8080, this.config.getHttpPort());
        assertEquals(8443, this.config.getHttpsPort());
    }

    public void testGetRandomPort()
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", "*");
        props.put("org.osgi.service.http.port.secure", "*");
        this.config.update(props);
        assertTrue(this.config.getHttpPort() != 8080);
        assertTrue(this.config.getHttpsPort() != 433);
    }

    public void testGetSpecificPort() throws Exception
    {
        ServerSocket ss = new ServerSocket(0);
        int port = ss.getLocalPort();
        ss.close();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.osgi.service.http.port", port);
        props.put("org.osgi.service.http.port.secure", port);
        this.config.update(props);
        assertTrue(this.config.getHttpPort() == port);
        assertTrue(this.config.getHttpsPort() == port);
    }

    @Override
    protected void setUp()
    {
        this.context = createNiceMock(BundleContext.class);
        replay(this.context);
        this.config = new JettyConfig(this.context);
    }
}