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

import java.util.Dictionary;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import junit.framework.TestCase;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.whiteboard.HttpWhiteboardConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

@RunWith(MockitoJUnitRunner.class)
public class ExtenderManagerTest
{

    private static final String SAMPLE_CONTEXT_ID = "some.context.id";

    private static final long BUNDLE_1_ID = 1L;

    private static final long BUNDLE_2_ID = 2L;

    private static final String SERVLET_1_ALIAS = "/servet1";

    private static final String SERVLET_1_1_ALIAS = "/servet1_1";

    private static final String SERVLET_2_ALIAS = "/servet2";

    private MockExtHttpService httpService;

    @Mock
    private HttpContext sampleContext;

    @Mock
    private Bundle bundle1;

    @Mock
    private Bundle bundle2;

    @Mock
    private ExtServlet servlet1;

    @Mock
    private ExtServlet servlet1_1;

    @Mock
    private ExtServlet servlet2;

    @Mock
    private ExtFilter filter1;

    @Mock
    private ExtFilter filter1_1;


    @Mock
    private ExtFilter filter2;

    @Mock
    private ServiceReference servlet1Reference;

    @Mock
    private ServiceReference servlet1_1Reference;

    @Mock
    private ServiceReference servlet2Reference;

    @Mock
    private ServiceReference filter1Reference;

    @Mock
    private ServiceReference filter1_1Reference;

    @Mock
    private ServiceReference filter2Reference;

    @Mock
    private ServiceReference filterAndServletReference;

    @Mock
    private ServiceReference httpContextReference;

    @Before
    public void setup()
    {
        when(bundle1.getBundleId()).thenReturn(BUNDLE_1_ID);
        when(bundle2.getBundleId()).thenReturn(BUNDLE_2_ID);
        when(httpContextReference.getBundle()).thenReturn(bundle1);

        when(servlet1Reference.getBundle()).thenReturn(bundle1);
        when(servlet1Reference.getPropertyKeys()).thenReturn(new String[0]);
        when(servlet1Reference.getProperty(HttpWhiteboardConstants.ALIAS)).thenReturn(SERVLET_1_ALIAS);
        when(servlet1Reference.getProperty(Constants.SERVICE_ID)).thenReturn(1L);

        when(servlet1_1Reference.getBundle()).thenReturn(bundle1);
        when(servlet1_1Reference.getPropertyKeys()).thenReturn(new String[0]);
        when(servlet1_1Reference.getProperty(HttpWhiteboardConstants.ALIAS)).thenReturn(SERVLET_1_1_ALIAS);
        when(servlet1_1Reference.getProperty(Constants.SERVICE_ID)).thenReturn(2L);

        when(servlet2Reference.getBundle()).thenReturn(bundle2);
        when(servlet2Reference.getPropertyKeys()).thenReturn(new String[0]);
        when(servlet2Reference.getProperty(HttpWhiteboardConstants.ALIAS)).thenReturn(SERVLET_2_ALIAS);
        when(servlet2Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        when(servlet2Reference.getProperty(Constants.SERVICE_ID)).thenReturn(3L);

        when(filter1Reference.getBundle()).thenReturn(bundle1);
        when(filter1Reference.getPropertyKeys()).thenReturn(new String[0]);
        when(filter1Reference.getProperty(HttpWhiteboardConstants.PATTERN)).thenReturn(SERVLET_1_ALIAS);
        when(filter1Reference.getProperty(Constants.SERVICE_ID)).thenReturn(4L);

        when(filter1_1Reference.getBundle()).thenReturn(bundle1);
        when(filter1_1Reference.getPropertyKeys()).thenReturn(new String[0]);
        when(filter1_1Reference.getProperty(HttpWhiteboardConstants.PATTERN)).thenReturn(SERVLET_1_1_ALIAS);
        when(filter1_1Reference.getProperty(Constants.SERVICE_ID)).thenReturn(5L);

        when(filter2Reference.getBundle()).thenReturn(bundle2);
        when(filter2Reference.getPropertyKeys()).thenReturn(new String[0]);
        when(filter2Reference.getProperty(HttpWhiteboardConstants.PATTERN)).thenReturn(SERVLET_2_ALIAS);
        when(filter2Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        when(filter2Reference.getProperty(Constants.SERVICE_ID)).thenReturn(6L);

        when(filterAndServletReference.getBundle()).thenReturn(bundle1);
        when(filterAndServletReference.getPropertyKeys()).thenReturn(new String[0]);
        when(filterAndServletReference.getProperty(HttpWhiteboardConstants.PATTERN)).thenReturn(SERVLET_2_ALIAS);
        when(filterAndServletReference.getProperty(HttpWhiteboardConstants.ALIAS)).thenReturn(SERVLET_2_ALIAS);
        when(filterAndServletReference.getProperty(Constants.SERVICE_ID)).thenReturn(7L);

        this.httpService = new MockExtHttpService();
    }

    @After
    public void tearDown()
    {
        this.httpService = null;
    }

    @Test
    public void test_no_servlets_no_filters()
    {
        ExtenderManager em = new ExtenderManager();

        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        em.setHttpService(null);
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertTrue(em.getMappings().isEmpty());
    }

    @Test
    public void test_servlet_per_bundle()
    {
        ExtenderManager em = new ExtenderManager();

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // setup a context without context ID
        em.add(sampleContext, httpContextReference);
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        em.remove(sampleContext);

        // set up a context with context ID and not shared
        final String id = HttpContextManagerTest.createId(bundle1, SAMPLE_CONTEXT_ID);
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(sampleContext, httpContextReference);
        TestCase.assertEquals(1, em.getHttpContexts().size());

        // register servlet1 from bundle1
        when(servlet1Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(servlet1, servlet1Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet1, this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(sampleContext, servlet1.getHttpContext());

        // register servlet2 from bundle2
        em.add(servlet2, servlet2Reference);

        TestCase.assertEquals(2, em.getMappings().size());
        TestCase.assertSame(servlet2, getServletMapping(em, servlet2Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertNull(this.httpService.getServlets().get(SERVLET_2_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getServletMapping(em, servlet2Reference)));

        // unregister servlet2
        em.removeServlet(servlet2Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet1, this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());

        // unregister servlet1
        em.removeServlet(servlet1Reference);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().get(id).getMappings().isEmpty());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());

        // unregister context
        em.remove(sampleContext);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_servlet_shared()
    {
        ExtenderManager em = new ExtenderManager();

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // set up a context with context ID and shared
        final String id = HttpContextManagerTest.createId(SAMPLE_CONTEXT_ID);
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_SHARED)).thenReturn("true");
        em.add(sampleContext, httpContextReference);
        TestCase.assertEquals(1, em.getHttpContexts().size());

        // register servlet1 from bundle1
        when(servlet1Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(servlet1, servlet1Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet1, this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(sampleContext, servlet1.getHttpContext());

        // register servlet2 from bundle2
        em.add(servlet2, servlet2Reference);

        TestCase.assertEquals(2, em.getMappings().size());
        TestCase.assertSame(servlet2, getServletMapping(em, servlet2Reference).getServlet());
        TestCase.assertEquals(2, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(2, this.httpService.getServlets().size());
        TestCase.assertSame(servlet2, this.httpService.getServlets().get(SERVLET_2_ALIAS));
        TestCase.assertEquals(0, em.getOrphanMappings().size());
        TestCase.assertSame(sampleContext, servlet2.getHttpContext());

        // unregister servlet2
        em.removeServlet(servlet2Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet1, this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());

        // unregister servlet1
        em.removeServlet(servlet1Reference);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().get(id).getMappings().isEmpty());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());

        // unregister context
        em.remove(sampleContext);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_servlet_no_context_id()
    {
        ExtenderManager em = new ExtenderManager();
        final String id1 = HttpContextManagerTest.createId(bundle1, null);
        final String id2 = HttpContextManagerTest.createId(bundle2, null);

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());
        TestCase.assertEquals(0, em.getHttpContexts().size());

        // register servlet1 from bundle1
        em.add(servlet1, servlet1Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id1).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet1, this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(DefaultHttpContext.class, servlet1.getHttpContext().getClass());

        // register servlet2 from bundle2
        when(servlet2Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn("");
        em.add(servlet2, servlet2Reference);

        TestCase.assertEquals(2, em.getMappings().size());
        TestCase.assertSame(servlet2, getServletMapping(em, servlet2Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id2).getMappings().size());
        TestCase.assertEquals(2, this.httpService.getServlets().size());
        TestCase.assertSame(servlet2, this.httpService.getServlets().get(SERVLET_2_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(DefaultHttpContext.class, servlet2.getHttpContext().getClass());

        // different HttpContext instances per servlet/per bundle
        TestCase.assertNotSame(servlet1.getHttpContext(), servlet2.getHttpContext());

        // register servlet 1_1 from bundle 1
        em.add(servlet1_1, servlet1_1Reference);

        TestCase.assertEquals(3, em.getMappings().size());
        TestCase.assertSame(servlet1_1, getServletMapping(em, servlet1_1Reference).getServlet());
        TestCase.assertEquals(2, em.getHttpContexts().get(id1).getMappings().size());
        TestCase.assertEquals(3, this.httpService.getServlets().size());
        TestCase.assertSame(servlet1_1, this.httpService.getServlets().get(SERVLET_1_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(DefaultHttpContext.class, servlet1_1.getHttpContext().getClass());

        // same HttpContext instances per servlet in same bundle
        TestCase.assertSame(servlet1.getHttpContext(), servlet1_1.getHttpContext());
    }

    @Test
    public void test_servlet_before_context_per_bundle()
    {
        ExtenderManager em = new ExtenderManager();
        final String id = HttpContextManagerTest.createId(bundle1, SAMPLE_CONTEXT_ID);

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // register servlet1 from bundle1
        when(servlet1Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(servlet1, servlet1Reference);

        // servlet not registered with HttpService yet
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(0, em.getHttpContexts().size());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertNull(this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getServletMapping(em, servlet1Reference)));

        // set up a context with context ID and not shared
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(sampleContext, httpContextReference);
        TestCase.assertEquals(1, em.getHttpContexts().size());

        // servlet registered with HttpService
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet1, this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(sampleContext, servlet1.getHttpContext());

        // unregister context
        em.remove(sampleContext);
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(0, em.getHttpContexts().size());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertNull(this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getServletMapping(em, servlet1Reference)));

        // unregister servlet1
        em.removeServlet(servlet1Reference);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_servlet_before_context_shared()
    {
        ExtenderManager em = new ExtenderManager();
        final String id = HttpContextManagerTest.createId(SAMPLE_CONTEXT_ID);

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // register servlet1 from bundle1
        when(servlet1Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(servlet1, servlet1Reference);

        // servlet not registered with HttpService yet
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(0, em.getHttpContexts().size());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertNull(this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getServletMapping(em, servlet1Reference)));

        // set up a context with context ID and not shared
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_SHARED)).thenReturn(true);
        em.add(sampleContext, httpContextReference);
        TestCase.assertEquals(1, em.getHttpContexts().size());

        // servlet registered with HttpService
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertSame(servlet1, this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(sampleContext, servlet1.getHttpContext());

        // unregister context
        em.remove(sampleContext);
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(servlet1, getServletMapping(em, servlet1Reference).getServlet());
        TestCase.assertEquals(0, em.getHttpContexts().size());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertNull(this.httpService.getServlets().get(SERVLET_1_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getServletMapping(em, servlet1Reference)));

        // unregister servlet1
        em.removeServlet(servlet1Reference);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_filter_per_bundle()
    {
        ExtenderManager em = new ExtenderManager();

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // setup a context without context ID
        em.add(sampleContext, httpContextReference);
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        em.remove(sampleContext);

        // set up a context with context ID and not shared
        final String id = HttpContextManagerTest.createId(bundle1, SAMPLE_CONTEXT_ID);
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(sampleContext, httpContextReference);
        TestCase.assertEquals(1, em.getHttpContexts().size());

        // register filter1 from bundle1
        when(filter1Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(filter1, filter1Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter1, this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(sampleContext, filter1.getHttpContext());

        // register filter2 from bundle2
        em.add(filter2, filter2Reference);

        TestCase.assertEquals(2, em.getMappings().size());
        TestCase.assertSame(filter2, getFilterMapping(em, filter2Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertNull(this.httpService.getFilters().get(SERVLET_2_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getFilterMapping(em, filter2Reference)));

        // unregister filter2
        em.removeFilter(filter2Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter1, this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());

        // unregister filter1
        em.removeFilter(filter1Reference);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().get(id).getMappings().isEmpty());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());

        // unregister context
        em.remove(sampleContext);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_filter_shared()
    {
        ExtenderManager em = new ExtenderManager();

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // set up a context with context ID and shared
        final String id = HttpContextManagerTest.createId(SAMPLE_CONTEXT_ID);
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_SHARED)).thenReturn("true");
        em.add(sampleContext, httpContextReference);
        TestCase.assertEquals(1, em.getHttpContexts().size());

        // register filter1 from bundle1
        when(filter1Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(filter1, filter1Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter1, this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(sampleContext, filter1.getHttpContext());

        // register filter2 from bundle2
        em.add(filter2, filter2Reference);

        TestCase.assertEquals(2, em.getMappings().size());
        TestCase.assertSame(filter2, getFilterMapping(em, filter2Reference).getFilter());
        TestCase.assertEquals(2, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(2, this.httpService.getFilters().size());
        TestCase.assertSame(filter2, this.httpService.getFilters().get(SERVLET_2_ALIAS));
        TestCase.assertEquals(0, em.getOrphanMappings().size());
        TestCase.assertSame(sampleContext, filter2.getHttpContext());

        // unregister filter2
        em.removeFilter(filter2Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter1, this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());

        // unregister filter1
        em.removeFilter(filter1Reference);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().get(id).getMappings().isEmpty());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());

        // unregister context
        em.remove(sampleContext);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_filter_no_context_id()
    {
        ExtenderManager em = new ExtenderManager();
        final String id1 = HttpContextManagerTest.createId(bundle1, null);
        final String id2 = HttpContextManagerTest.createId(bundle2, null);

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());
        TestCase.assertEquals(0, em.getHttpContexts().size());

        // register filter1 from bundle1
        em.add(filter1, filter1Reference);

        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id1).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter1, this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(DefaultHttpContext.class, filter1.getHttpContext().getClass());

        // register filter2 from bundle2
        when(filter2Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn("");
        em.add(filter2, filter2Reference);

        TestCase.assertEquals(2, em.getMappings().size());
        TestCase.assertSame(filter2, getFilterMapping(em, filter2Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id2).getMappings().size());
        TestCase.assertEquals(2, this.httpService.getFilters().size());
        TestCase.assertSame(filter2, this.httpService.getFilters().get(SERVLET_2_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(DefaultHttpContext.class, filter2.getHttpContext().getClass());

        // different HttpContext instances per servlet/per bundle
        TestCase.assertNotSame(filter1.getHttpContext(), filter2.getHttpContext());

        // register servlet 1_1 from bundle 1
        em.add(filter1_1, filter1_1Reference);

        TestCase.assertEquals(3, em.getMappings().size());
        TestCase.assertSame(filter1_1, getFilterMapping(em, filter1_1Reference).getFilter());
        TestCase.assertEquals(2, em.getHttpContexts().get(id1).getMappings().size());
        TestCase.assertEquals(3, this.httpService.getFilters().size());
        TestCase.assertSame(filter1_1, this.httpService.getFilters().get(SERVLET_1_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(DefaultHttpContext.class, filter1_1.getHttpContext().getClass());

        // same HttpContext instances per servlet in same bundle
        TestCase.assertSame(filter1.getHttpContext(), filter1_1.getHttpContext());
    }

    @Test
    public void test_filter_before_context_per_bundle()
    {
        ExtenderManager em = new ExtenderManager();
        final String id = HttpContextManagerTest.createId(bundle1, SAMPLE_CONTEXT_ID);

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // register filter1 from bundle1
        when(filter1Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(filter1, filter1Reference);

        // servlet not registered with HttpService yet
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(0, em.getHttpContexts().size());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertNull(this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getFilterMapping(em, filter1Reference)));

        // set up a context with context ID and not shared
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(sampleContext, httpContextReference);
        TestCase.assertEquals(1, em.getHttpContexts().size());

        // servlet registered with HttpService
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter1, this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(sampleContext, filter1.getHttpContext());

        // unregister context
        em.remove(sampleContext);
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(0, em.getHttpContexts().size());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertNull(this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getFilterMapping(em, filter1Reference)));

        // unregister filter1
        em.removeFilter(filter1Reference);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_filter_before_context_shared()
    {
        ExtenderManager em = new ExtenderManager();
        final String id = HttpContextManagerTest.createId(SAMPLE_CONTEXT_ID);

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // register filter1 from bundle1
        when(filter1Reference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        em.add(filter1, filter1Reference);

        // servlet not registered with HttpService yet
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(0, em.getHttpContexts().size());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertNull(this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getFilterMapping(em, filter1Reference)));

        // set up a context with context ID and not shared
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_ID)).thenReturn(SAMPLE_CONTEXT_ID);
        when(httpContextReference.getProperty(HttpWhiteboardConstants.CONTEXT_SHARED)).thenReturn(true);
        em.add(sampleContext, httpContextReference);
        TestCase.assertEquals(1, em.getHttpContexts().size());

        // servlet registered with HttpService
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(1, em.getHttpContexts().get(id).getMappings().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter1, this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
        TestCase.assertSame(sampleContext, filter1.getHttpContext());

        // unregister context
        em.remove(sampleContext);
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filter1Reference).getFilter());
        TestCase.assertEquals(0, em.getHttpContexts().size());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertNull(this.httpService.getFilters().get(SERVLET_1_ALIAS));
        TestCase.assertEquals(1, em.getOrphanMappings().size());
        TestCase.assertEquals(1, em.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(em.getOrphanMappings().get(SAMPLE_CONTEXT_ID)
            .contains(getFilterMapping(em, filter1Reference)));

        // unregister filter1
        em.removeFilter(filter1Reference);
        TestCase.assertTrue(em.getMappings().isEmpty());
        TestCase.assertTrue(em.getHttpContexts().isEmpty());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertTrue(em.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_filter_and_servlet_in_one_service()
    {
        ExtenderManager em = new ExtenderManager();

        // prepare with http service
        em.setHttpService(this.httpService);
        TestCase.assertTrue(this.httpService.getServlets().isEmpty());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        // register filter1 from bundle1
        em.add(filter1, filterAndServletReference);
        em.add(servlet1, filterAndServletReference);

        TestCase.assertEquals(2, em.getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter1, getFilterMapping(em, filterAndServletReference).getFilter());
        TestCase.assertSame(servlet1, getServletMapping(em, filterAndServletReference).getServlet());

        em.removeFilter(filterAndServletReference);
        TestCase.assertEquals(1, em.getMappings().size());
        TestCase.assertEquals(1, this.httpService.getServlets().size());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertNull(getFilterMapping(em, filterAndServletReference));
        TestCase.assertSame(servlet1, getServletMapping(em, filterAndServletReference).getServlet());

        em.removeServlet(filterAndServletReference);
        TestCase.assertEquals(0, em.getMappings().size());
        TestCase.assertEquals(0, this.httpService.getServlets().size());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
        TestCase.assertNull(getFilterMapping(em, filterAndServletReference));
        TestCase.assertNull(getServletMapping(em, filterAndServletReference));
    }

    private FilterMapping getFilterMapping(final ExtenderManager em, final ServiceReference ref) {
        return (FilterMapping) em.getMappings().get(ref.getProperty(Constants.SERVICE_ID).toString() + ExtenderManager.TYPE_FILTER);
    }

    private ServletMapping getServletMapping(final ExtenderManager em, final ServiceReference ref) {
        return (ServletMapping) em.getMappings().get(ref.getProperty(Constants.SERVICE_ID).toString() + ExtenderManager.TYPE_SERVLET);
    }

    static interface ExtFilter extends Filter
    {
        HttpContext getHttpContext();
    }

    static interface ExtServlet extends Servlet
    {
        HttpContext getHttpContext();
    }

    static final class MockExtHttpService implements ExtHttpService
    {

        private final BidiMap /* <String, Servlet> */servlets = new DualHashBidiMap();
        private final BidiMap /* <String, Filter> */filters = new DualHashBidiMap();

        /**
         * @return BidiMap<String, Servlet>
         */
        public BidiMap getServlets()
        {
            return servlets;
        }

        /**
         * @return BidiMap<String, Filter>
         */
        public BidiMap getFilters()
        {
            return filters;
        }

        public void registerServlet(String alias, Servlet servlet, @SuppressWarnings("rawtypes") Dictionary initparams,
            HttpContext context)

        {
            // always expect a non-null HttpContext here !!
            TestCase.assertNotNull(context);

            this.servlets.put(alias, servlet);

            // make HttpContext available
            when(((ExtServlet) servlet).getHttpContext()).thenReturn(context);
        }

        public void registerResources(String alias, String name, HttpContext context)
        {
            // not used here
        }

        public void unregister(String alias)
        {
            Object servlet = this.servlets.remove(alias);
            if (servlet instanceof ExtServlet)
            {
                when(((ExtServlet) servlet).getHttpContext()).thenReturn(null);
            }
        }

        public HttpContext createDefaultHttpContext()
        {
            // not used here
            return null;
        }

        public void registerFilter(Filter filter, String pattern, @SuppressWarnings("rawtypes") Dictionary initParams,
            int ranking, HttpContext context)
        {
            // always expect a non-null HttpContext here !!
            TestCase.assertNotNull(context);

            this.filters.put(pattern, filter);

            // make HttpContext available
            when(((ExtFilter) filter).getHttpContext()).thenReturn(context);
        }

        public void unregisterFilter(Filter filter)
        {
            this.filters.removeValue(filter);
            when(((ExtFilter) filter).getHttpContext()).thenReturn(null);
        }

        public void unregisterServlet(Servlet servlet)
        {
            this.servlets.removeValue(servlet);
            when(((ExtServlet) servlet).getHttpContext()).thenReturn(null);
        }
    }
}