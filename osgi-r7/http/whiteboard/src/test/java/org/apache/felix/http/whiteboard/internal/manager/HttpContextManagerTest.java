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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.felix.http.whiteboard.internal.manager.HttpContextManager.HttpContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HttpContextManagerTest
{

    private static final String SAMPLE_CONTEXT_ID = "some.context.id";

    private static final long BUNDLE_1_ID = 1L;

    private static final String BUNDLE_1_ALIAS = "/bundle1";

    private static final long BUNDLE_2_ID = 2L;

    private static final String BUNDLE_2_ALIAS = "/bundle2";

    @Mock
    private HttpContext sampleContext;

    @Mock
    private Bundle bundle1;

    @Mock
    private Bundle bundle2;

    @Before
    public void setup()
    {
        when(bundle1.getBundleId()).thenReturn(BUNDLE_1_ID);
        when(bundle2.getBundleId()).thenReturn(BUNDLE_2_ID);
    }

    @Test
    public void test_HttpContextHolder()
    {
        TestCase.assertNotNull(sampleContext);

        final HttpContextHolder h1 = new HttpContextHolder(sampleContext);
        TestCase.assertSame(sampleContext, h1.getContext());
        TestCase.assertTrue(h1.getMappings().isEmpty());

        ServletMapping sm = new ServletMapping(bundle1, null, "");
        h1.addMapping(sm);
        TestCase.assertSame(sampleContext, sm.getContext());
        TestCase.assertEquals(1, h1.getMappings().size());
        TestCase.assertTrue(h1.getMappings().contains(sm));

        h1.removeMapping(sm);
        TestCase.assertNull(sm.getContext());
        TestCase.assertTrue(h1.getMappings().isEmpty());
    }

    @Test
    public void test_add_remove_HttpContext_per_Bundle()
    {
        final HttpContextManager hcm = new HttpContextManager();
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());

        Collection<AbstractMapping> mappings = hcm.addHttpContext(bundle1, SAMPLE_CONTEXT_ID, sampleContext);
        TestCase.assertNotNull(mappings);
        TestCase.assertTrue(mappings.isEmpty());

        String holderId = createId(bundle1, SAMPLE_CONTEXT_ID);
        Map<String, HttpContextHolder> holders = hcm.getHttpContexts();
        TestCase.assertEquals(1, holders.size());
        TestCase.assertSame(sampleContext, holders.get(holderId).getContext());
        TestCase.assertEquals(mappings, holders.get(holderId).getMappings());

        Collection<AbstractMapping> removedMappings = hcm.removeHttpContext(sampleContext);
        TestCase.assertNotNull(removedMappings);
        TestCase.assertTrue(removedMappings.isEmpty());
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());
    }

    @Test
    public void test_add_remove_HttpContext_shared()
    {
        final HttpContextManager hcm = new HttpContextManager();
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());

        Collection<AbstractMapping> mappings = hcm.addHttpContext(null, SAMPLE_CONTEXT_ID, sampleContext);
        TestCase.assertNotNull(mappings);
        TestCase.assertTrue(mappings.isEmpty());

        String holderId = createId(SAMPLE_CONTEXT_ID);
        Map<String, HttpContextHolder> holders = hcm.getHttpContexts();
        TestCase.assertEquals(1, holders.size());
        TestCase.assertSame(sampleContext, holders.get(holderId).getContext());
        TestCase.assertEquals(mappings, holders.get(holderId).getMappings());

        Collection<AbstractMapping> removedMappings = hcm.removeHttpContext(sampleContext);
        TestCase.assertNotNull(removedMappings);
        TestCase.assertTrue(removedMappings.isEmpty());
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());
    }

    @Test
    public void test_get_unget_HttpContext_per_bundle_same_bundle()
    {
        final HttpContextManager hcm = new HttpContextManager();
        final String id = createId(bundle1, SAMPLE_CONTEXT_ID);
        hcm.addHttpContext(bundle1, SAMPLE_CONTEXT_ID, sampleContext);

        // Servlet 1 gets the context
        final ServletMapping bundle1Servlet = new ServletMapping(bundle1, null, BUNDLE_1_ALIAS);
        HttpContext ctx1 = hcm.getHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        TestCase.assertNotNull(ctx1);
        TestCase.assertSame(ctx1, bundle1Servlet.getContext());
        TestCase.assertSame(sampleContext, ctx1);
        TestCase.assertTrue(hcm.getHttpContexts().get(id).getMappings().contains(bundle1Servlet));
        Map<String, Set<AbstractMapping>> orphans1 = hcm.getOrphanMappings();
        TestCase.assertTrue(orphans1.isEmpty());

        // unregister servlet again --> all references clear
        hcm.ungetHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        TestCase.assertNull(bundle1Servlet.getContext());
        TestCase.assertTrue(hcm.getHttpContexts().get(id).getMappings().isEmpty());
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());

        // register servlet, unregister context --> orphan
        hcm.getHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        hcm.removeHttpContext(sampleContext);
        TestCase.assertNull(bundle1Servlet.getContext());
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());
        TestCase.assertEquals(1, hcm.getOrphanMappings().size());
        TestCase.assertEquals(1, hcm.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(hcm.getOrphanMappings().get(SAMPLE_CONTEXT_ID).contains(bundle1Servlet));

        // cleanup
        hcm.ungetHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        TestCase.assertNull(bundle1Servlet.getContext());
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());
    }

    public void test_get_unget_HttpContext_per_bundle_other_bundle()
    {
        final HttpContextManager hcm = new HttpContextManager();

        final String id1 = createId(bundle1, SAMPLE_CONTEXT_ID);
        hcm.addHttpContext(bundle1, SAMPLE_CONTEXT_ID, sampleContext);

        // Servlet 2 is an orphan
        final ServletMapping bundle2Servlet = new ServletMapping(bundle2, null, BUNDLE_2_ALIAS);
        HttpContext ctx2 = hcm.getHttpContext(bundle2, SAMPLE_CONTEXT_ID, bundle2Servlet);
        TestCase.assertNull(ctx2);
        TestCase.assertNull(bundle2Servlet.getContext());
        TestCase.assertTrue(hcm.getHttpContexts().get(id1).getMappings().isEmpty());
        Map<String, Set<AbstractMapping>> orphans2 = hcm.getOrphanMappings();
        TestCase.assertEquals(1, orphans2.size());
        TestCase.assertEquals(1, orphans2.get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(orphans2.get(SAMPLE_CONTEXT_ID).contains(bundle2Servlet));

        // unregister unused context for bundle1
        hcm.removeHttpContext(sampleContext);
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());

        // register context for bundle2
        final String id2 = createId(bundle1, SAMPLE_CONTEXT_ID);
        hcm.addHttpContext(bundle2, SAMPLE_CONTEXT_ID, sampleContext);
        TestCase.assertEquals(1, hcm.getHttpContexts().size());
        TestCase.assertSame(sampleContext, hcm.getHttpContexts().get(id2).getContext());

        TestCase.assertSame(sampleContext, bundle2Servlet.getContext());
        TestCase.assertEquals(1, hcm.getHttpContexts().get(id2).getMappings().size());
        TestCase.assertTrue(hcm.getHttpContexts().get(id2).getMappings().contains(bundle2Servlet));
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());

        // cleanup
        hcm.ungetHttpContext(bundle2, SAMPLE_CONTEXT_ID, bundle2Servlet);
        TestCase.assertNull(bundle2Servlet.getContext());
        TestCase.assertTrue(hcm.getHttpContexts().get(id2).getMappings().isEmpty());
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_get_unget_HttpContext_shared()
    {
        final HttpContextManager hcm = new HttpContextManager();
        final String id = createId(SAMPLE_CONTEXT_ID);
        hcm.addHttpContext(null, SAMPLE_CONTEXT_ID, sampleContext);

        // Servlet 1 gets the context
        final ServletMapping bundle1Servlet = new ServletMapping(bundle1, null, BUNDLE_1_ALIAS);
        HttpContext ctx1 = hcm.getHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        TestCase.assertNotNull(ctx1);
        TestCase.assertSame(ctx1, bundle1Servlet.getContext());
        TestCase.assertSame(sampleContext, ctx1);
        TestCase.assertEquals(1, hcm.getHttpContexts().get(id).getMappings().size());
        TestCase.assertTrue(hcm.getHttpContexts().get(id).getMappings().contains(bundle1Servlet));
        Map<String, Set<AbstractMapping>> orphans1 = hcm.getOrphanMappings();
        TestCase.assertTrue(orphans1.isEmpty());

        // unregister serlvet 1 --> all references clear
        hcm.ungetHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        TestCase.assertNull(bundle1Servlet.getContext());
        TestCase.assertTrue(hcm.getHttpContexts().get(id).getMappings().isEmpty());
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());

        // Servlet 2 gets the context
        final ServletMapping bundle2Servlet = new ServletMapping(bundle2, null, BUNDLE_2_ALIAS);
        HttpContext ctx2 = hcm.getHttpContext(bundle2, SAMPLE_CONTEXT_ID, bundle2Servlet);
        TestCase.assertNotNull(ctx2);
        TestCase.assertSame(ctx2, bundle2Servlet.getContext());
        TestCase.assertSame(sampleContext, ctx2);
        TestCase.assertEquals(1, hcm.getHttpContexts().get(id).getMappings().size());
        TestCase.assertTrue(hcm.getHttpContexts().get(id).getMappings().contains(bundle2Servlet));
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());

        // register Servlet 1 again --> gets context
        hcm.getHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        HttpContext ctx3 = hcm.getHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        TestCase.assertNotNull(ctx3);
        TestCase.assertSame(ctx3, bundle1Servlet.getContext());
        TestCase.assertSame(sampleContext, ctx3);
        TestCase.assertEquals(2, hcm.getHttpContexts().get(id).getMappings().size());
        TestCase.assertTrue(hcm.getHttpContexts().get(id).getMappings().contains(bundle1Servlet));
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());

        // unregister context --> all references clear
        hcm.removeHttpContext(sampleContext);
        TestCase.assertNull(bundle1Servlet.getContext());
        TestCase.assertNull(bundle2Servlet.getContext());
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());
        TestCase.assertEquals(1, hcm.getOrphanMappings().size());
        TestCase.assertEquals(2, hcm.getOrphanMappings().get(SAMPLE_CONTEXT_ID).size());
        TestCase.assertTrue(hcm.getOrphanMappings().get(SAMPLE_CONTEXT_ID).contains(bundle1Servlet));
        TestCase.assertTrue(hcm.getOrphanMappings().get(SAMPLE_CONTEXT_ID).contains(bundle2Servlet));

        // register context --> servlets 1, 2 get context
        hcm.addHttpContext(null, SAMPLE_CONTEXT_ID, sampleContext);
        TestCase.assertSame(sampleContext, bundle1Servlet.getContext());
        TestCase.assertSame(sampleContext, bundle2Servlet.getContext());
        TestCase.assertEquals(2, hcm.getHttpContexts().get(id).getMappings().size());
        TestCase.assertTrue(hcm.getHttpContexts().get(id).getMappings().contains(bundle1Servlet));
        TestCase.assertTrue(hcm.getHttpContexts().get(id).getMappings().contains(bundle2Servlet));
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());

        // cleanup
        hcm.removeHttpContext(sampleContext);
        hcm.ungetHttpContext(bundle1, SAMPLE_CONTEXT_ID, bundle1Servlet);
        hcm.ungetHttpContext(bundle2, SAMPLE_CONTEXT_ID, bundle2Servlet);
        TestCase.assertNull(bundle1Servlet.getContext());
        TestCase.assertNull(bundle2Servlet.getContext());
        TestCase.assertTrue(hcm.getHttpContexts().isEmpty());
        TestCase.assertTrue(hcm.getOrphanMappings().isEmpty());
    }

    @Test
    public void test_createId_Bundle_String()
    {
        TestCase.assertEquals(BUNDLE_1_ID + "-", createId(bundle1, null));
        TestCase.assertEquals(BUNDLE_1_ID + "-", createId(bundle1, ""));
        TestCase.assertEquals(BUNDLE_1_ID + "-" + SAMPLE_CONTEXT_ID, createId(bundle1, SAMPLE_CONTEXT_ID));
    }

    @Test
    public void test_createId_String()
    {
        TestCase.assertEquals("shared-", createId(null));
        TestCase.assertEquals("shared-", createId(""));
        TestCase.assertEquals("shared-" + SAMPLE_CONTEXT_ID, createId(SAMPLE_CONTEXT_ID));
    }

    static String createId(String contextId)
    {
        try
        {
            Method m = HttpContextManager.class.getDeclaredMethod("createId", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, contextId);
        }
        catch (Throwable t)
        {
            TestCase.fail(t.toString());
            return null; // compiler satisfaction
        }
    }

    static String createId(Bundle bundle, String contextId)
    {
        try
        {
            Method m = HttpContextManager.class.getDeclaredMethod("createId", Bundle.class, String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, bundle, contextId);
        }
        catch (Throwable t)
        {
            TestCase.fail(t.toString());
            return null; // compiler satisfaction
        }
    }
}
