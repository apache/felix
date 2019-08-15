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
package org.apache.felix.webconsole.internal.servlet;

import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class OsgiManagerHttpContextTest {
    @Test
    public void testAuthenticate() throws Exception {
        BundleContext bc = Mockito.mock(BundleContext.class);
        HttpService svc = Mockito.mock(HttpService.class);
        OsgiManagerHttpContext ctx = new OsgiManagerHttpContext(bc, svc, null, "foo", "bar", "blah");

        Method authenticateMethod = OsgiManagerHttpContext.class.getDeclaredMethod(
                "authenticate", new Class [] {Object.class, String.class, byte[].class});
        authenticateMethod.setAccessible(true);

        assertEquals(true, authenticateMethod.invoke(ctx, null, "foo", "bar".getBytes()));
        assertEquals(false, authenticateMethod.invoke(ctx, null, "foo", "blah".getBytes()));

        WebConsoleSecurityProvider sp = new TestSecurityProvider();
        assertEquals(true, authenticateMethod.invoke(ctx, sp, "xxx", "yyy".getBytes()));
        assertEquals("The default username and password should not be accepted with security provider",
                false, authenticateMethod.invoke(ctx, sp, "foo", "bar".getBytes()));
    }

    @Test
    public void testAuthenticatePwdDisabledWithRequiredSecurityProvider() throws Exception {
        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getProperty(OsgiManager.FRAMEWORK_PROP_SECURITY_PROVIDERS)).thenReturn("a");

        HttpService svc = Mockito.mock(HttpService.class);
        OsgiManagerHttpContext ctx = new OsgiManagerHttpContext(bc, svc, null, "foo", "bar", "blah");

        Method authenticateMethod = OsgiManagerHttpContext.class.getDeclaredMethod(
                "authenticate", new Class [] {Object.class, String.class, byte[].class});
        authenticateMethod.setAccessible(true);

        assertEquals("A required security provider is configured, logging in using "
                + "username and password should be disabled",
                false, authenticateMethod.invoke(ctx, null, "foo", "bar".getBytes()));
        assertEquals(false, authenticateMethod.invoke(ctx, null, "foo", "blah".getBytes()));
        assertEquals(false, authenticateMethod.invoke(ctx, null, "blah", "bar".getBytes()));

        WebConsoleSecurityProvider sp = new TestSecurityProvider();
        assertEquals(true, authenticateMethod.invoke(ctx, sp, "xxx", "yyy".getBytes()));
        assertEquals(false, authenticateMethod.invoke(ctx, sp, "foo", "bar".getBytes()));
    }

    private static class TestSecurityProvider implements WebConsoleSecurityProvider {
        @Override
        public Object authenticate(String username, String password) {
            if ("xxx".equals(username) && "yyy".equals(password))
                return new Object();
            return null;
        }

        @Override
        public boolean authorize(Object user, String role) {
            return false;
        }
    }
}
