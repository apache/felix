/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.whiteboard.internal.manager;

import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

@RunWith(MockitoJUnitRunner.class)
public class ServletMappingTest
{

    private static final String SAMPLE_CONTEXT_ID = "some.context.id";

    private static final long BUNDLE_ID = 1L;

    private static final String SERVLET_ALIAS = "/bundle";

    @Mock
    private HttpContext sampleContext;

    @Mock
    private Bundle bundle;

    @Mock
    private ExtenderManagerTest.ExtServlet servlet;

    private ExtenderManagerTest.MockExtHttpService httpService;

    @Before
    public void setup()
    {
        when(bundle.getBundleId()).thenReturn(BUNDLE_ID);

        this.httpService = new ExtenderManagerTest.MockExtHttpService();
    }

    @After
    public void tearDown()
    {
        this.httpService = null;
    }

    @Test
    public void test_with_context()
    {
        ServletMapping sm = new ServletMapping(bundle, servlet, SERVLET_ALIAS);
        TestCase.assertSame(bundle, sm.getBundle());
        TestCase.assertSame(servlet, sm.getServlet());
        TestCase.assertEquals(SERVLET_ALIAS, sm.getAlias());

        TestCase.assertNull(sm.getContext());
        TestCase.assertNotNull(sm.getInitParams());
        TestCase.assertTrue(sm.getInitParams().isEmpty());
        TestCase.assertFalse(sm.isRegistered());

        sm.setContext(sampleContext);
        TestCase.assertSame(sampleContext, sm.getContext());
        TestCase.assertFalse(sm.isRegistered());
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());

        sm.register(this.httpService);
        TestCase.assertSame(sampleContext, sm.getContext());
        TestCase.assertTrue(sm.isRegistered());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet, this.httpService.getServlets().get(SERVLET_ALIAS));
        TestCase.assertSame(sampleContext, servlet.getHttpContext());

        sm.unregister(this.httpService);
        TestCase.assertSame(sampleContext, sm.getContext());
        TestCase.assertFalse(sm.isRegistered());
        TestCase.assertEquals(0, this.httpService.getServlets().size());

        sm.setContext(null);
        TestCase.assertNull(sm.getContext());
        TestCase.assertFalse(sm.isRegistered());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
    }

    @Test
    public void test_context_delayed()
    {
        ServletMapping sm = new ServletMapping(bundle, servlet, SERVLET_ALIAS);
        TestCase.assertSame(bundle, sm.getBundle());
        TestCase.assertSame(servlet, sm.getServlet());
        TestCase.assertEquals(SERVLET_ALIAS, sm.getAlias());

        TestCase.assertNull(sm.getContext());
        TestCase.assertNotNull(sm.getInitParams());
        TestCase.assertTrue(sm.getInitParams().isEmpty());
        TestCase.assertFalse(sm.isRegistered());

        sm.register(this.httpService);
        TestCase.assertNull(sm.getContext());
        TestCase.assertFalse(sm.isRegistered());
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());

        sm.unregister(httpService);
        TestCase.assertNull(sm.getContext());
        TestCase.assertFalse(sm.isRegistered());
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());

        sm.setContext(sampleContext);
        TestCase.assertSame(sampleContext, sm.getContext());
        TestCase.assertFalse(sm.isRegistered());
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
    }

    @Test
    public void test_unset_context()
    {
        ServletMapping sm = new ServletMapping(bundle, servlet, SERVLET_ALIAS);
        TestCase.assertSame(bundle, sm.getBundle());
        TestCase.assertSame(servlet, sm.getServlet());
        TestCase.assertEquals(SERVLET_ALIAS, sm.getAlias());

        TestCase.assertNull(sm.getContext());
        TestCase.assertNotNull(sm.getInitParams());
        TestCase.assertTrue(sm.getInitParams().isEmpty());
        TestCase.assertFalse(sm.isRegistered());

        sm.setContext(sampleContext);
        TestCase.assertSame(sampleContext, sm.getContext());
        TestCase.assertFalse(sm.isRegistered());
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());

        sm.register(this.httpService);
        TestCase.assertSame(sampleContext, sm.getContext());
        TestCase.assertTrue(sm.isRegistered());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet, this.httpService.getServlets().get(SERVLET_ALIAS));
        TestCase.assertSame(sampleContext, servlet.getHttpContext());

        // does not unregister yet
        sm.setContext(null);
        TestCase.assertNull(sm.getContext());
        TestCase.assertTrue(sm.isRegistered());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet, this.httpService.getServlets().get(SERVLET_ALIAS));
        TestCase.assertSame(sampleContext, servlet.getHttpContext());

        sm.unregister(this.httpService);
        TestCase.assertNull(sm.getContext());
        TestCase.assertFalse(sm.isRegistered());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
    }
}
