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
public class FilterMappingTest
{

    private static final String SAMPLE_CONTEXT_ID = "some.context.id";

    private static final long BUNDLE_ID = 1L;

    private static final String FILTER_PATTERN = "/sample/*";

    private static final int FILTER_RANKING = 1234;

    @Mock
    private HttpContext sampleContext;

    @Mock
    private Bundle bundle;

    @Mock
    private ExtenderManagerTest.ExtFilter filter;

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
        FilterMapping fm = new FilterMapping(bundle, filter, FILTER_PATTERN, FILTER_RANKING);
        TestCase.assertSame(bundle, fm.getBundle());
        TestCase.assertSame(filter, fm.getFilter());
        TestCase.assertEquals(FILTER_PATTERN, fm.getPattern());
        TestCase.assertEquals(FILTER_RANKING, fm.getRanking());

        TestCase.assertNull(fm.getContext());
        TestCase.assertNotNull(fm.getInitParams());
        TestCase.assertTrue(fm.getInitParams().isEmpty());
        TestCase.assertFalse(fm.isRegistered());

        fm.setContext(sampleContext);
        TestCase.assertSame(sampleContext, fm.getContext());
        TestCase.assertFalse(fm.isRegistered());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        fm.register(this.httpService);
        TestCase.assertSame(sampleContext, fm.getContext());
        TestCase.assertTrue(fm.isRegistered());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter, this.httpService.getFilters().get(FILTER_PATTERN));
        TestCase.assertSame(sampleContext, filter.getHttpContext());

        fm.unregister(this.httpService);
        TestCase.assertSame(sampleContext, fm.getContext());
        TestCase.assertFalse(fm.isRegistered());
        TestCase.assertEquals(0, this.httpService.getFilters().size());

        fm.setContext(null);
        TestCase.assertNull(fm.getContext());
        TestCase.assertFalse(fm.isRegistered());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
    }

    @Test
    public void test_context_delayed()
    {
        FilterMapping fm = new FilterMapping(bundle, filter, FILTER_PATTERN, FILTER_RANKING);
        TestCase.assertSame(bundle, fm.getBundle());
        TestCase.assertSame(filter, fm.getFilter());
        TestCase.assertEquals(FILTER_PATTERN, fm.getPattern());
        TestCase.assertEquals(FILTER_RANKING, fm.getRanking());

        TestCase.assertNull(fm.getContext());
        TestCase.assertNotNull(fm.getInitParams());
        TestCase.assertTrue(fm.getInitParams().isEmpty());
        TestCase.assertFalse(fm.isRegistered());

        fm.register(this.httpService);
        TestCase.assertNull(fm.getContext());
        TestCase.assertFalse(fm.isRegistered());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        fm.unregister(httpService);
        TestCase.assertNull(fm.getContext());
        TestCase.assertFalse(fm.isRegistered());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        fm.setContext(sampleContext);
        TestCase.assertSame(sampleContext, fm.getContext());
        TestCase.assertFalse(fm.isRegistered());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());
    }

    @Test
    public void test_unset_context()
    {
        FilterMapping fm = new FilterMapping(bundle, filter, FILTER_PATTERN, FILTER_RANKING);
        TestCase.assertSame(bundle, fm.getBundle());
        TestCase.assertSame(filter, fm.getFilter());
        TestCase.assertEquals(FILTER_PATTERN, fm.getPattern());
        TestCase.assertEquals(FILTER_RANKING, fm.getRanking());

        TestCase.assertNull(fm.getContext());
        TestCase.assertNotNull(fm.getInitParams());
        TestCase.assertTrue(fm.getInitParams().isEmpty());
        TestCase.assertFalse(fm.isRegistered());

        fm.setContext(sampleContext);
        TestCase.assertSame(sampleContext, fm.getContext());
        TestCase.assertFalse(fm.isRegistered());
        TestCase.assertTrue(this.httpService.getFilters().isEmpty());

        fm.register(this.httpService);
        TestCase.assertSame(sampleContext, fm.getContext());
        TestCase.assertTrue(fm.isRegistered());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter, this.httpService.getFilters().get(FILTER_PATTERN));
        TestCase.assertSame(sampleContext, filter.getHttpContext());

        // does not unregister yet
        fm.setContext(null);
        TestCase.assertNull(fm.getContext());
        TestCase.assertTrue(fm.isRegistered());
        TestCase.assertEquals(1, this.httpService.getFilters().size());
        TestCase.assertSame(filter, this.httpService.getFilters().get(FILTER_PATTERN));
        TestCase.assertSame(sampleContext, filter.getHttpContext());

        fm.unregister(this.httpService);
        TestCase.assertNull(fm.getContext());
        TestCase.assertFalse(fm.isRegistered());
        TestCase.assertEquals(0, this.httpService.getFilters().size());
    }
}
