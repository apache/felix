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
package org.apache.felix.http.base.internal.context;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class ServletContextImplTest
{
    private Bundle bundle;
    private HttpContext httpContext;
    private AttributeListener listener;
    private ServletContextImpl context;

    @Before
    public void setUp()
    {
        this.bundle = Mockito.mock(Bundle.class);
        ServletContext globalContext = Mockito.mock(ServletContext.class);
        this.httpContext = Mockito.mock(HttpContext.class);
        this.listener = new AttributeListener();
        this.context = new ServletContextImpl(this.bundle, globalContext, this.httpContext, this.listener, false);
    }

    @Test
    public void testGetResource()
        throws Exception
    {
        URL url = getClass().getResource("resource.txt");
        Assert.assertNotNull(url);

        Mockito.when(this.httpContext.getResource("resource.txt")).thenReturn(url);
        Assert.assertNull(this.context.getResource("/notfound.txt"));
        Assert.assertEquals(url, this.context.getResource("/resource.txt"));
    }

    @Test
    public void testGetResourceAsStream()
        throws Exception
    {
        URL url = getClass().getResource("resource.txt");
        Assert.assertNotNull(url);

        Mockito.when(this.httpContext.getResource("resource.txt")).thenReturn(url);
        Assert.assertNull(this.context.getResourceAsStream("/notfound.txt"));
        Assert.assertNotNull(this.context.getResourceAsStream("/resource.txt"));
    }

    @Test
    public void testGetResourcePaths()
    {
        HashSet<String> paths = new HashSet<String>(Arrays.asList("/some/path/1", "/some/path/2"));
        Mockito.when(this.bundle.getEntryPaths("some/path")).thenReturn(Collections.enumeration(paths));

        Set set = this.context.getResourcePaths("/some/path");
        Assert.assertNotNull(set);
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.contains("/some/path/1"));
        Assert.assertTrue(set.contains("/some/path/2"));
    }

    @Test
    public void testGetRealPath()
    {
        Assert.assertNull(this.context.getRealPath("path"));
    }

    @Test
    public void testGetInitParameter()
    {
        Assert.assertNull(this.context.getInitParameter("key1"));
    }

    @Test
    public void testGetInitParameterNames()
    {
        Enumeration e = this.context.getInitParameterNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetAttribute()
    {
        Assert.assertNull(this.context.getAttribute("key1"));

        this.context.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        Assert.assertEquals("value1", this.context.getAttribute("key1"));

        this.context.removeAttribute("key1");
        this.listener.checkRemoved("key1", "value1");
        Assert.assertNull(this.context.getAttribute("key1"));

        this.context.setAttribute("key1", null);
        this.listener.checkNull();
        Assert.assertNull(this.context.getAttribute("key1"));

        this.context.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        Assert.assertEquals("value1", this.context.getAttribute("key1"));

        this.context.setAttribute("key1", "newValue");
        this.listener.checkReplaced("key1", "value1");
        Assert.assertEquals("newValue", this.context.getAttribute("key1"));
    }

    @Test
    public void testGetAttributeNames()
    {
        Enumeration e = this.context.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());

        this.context.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        e = this.context.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertTrue(e.hasMoreElements());
        Assert.assertEquals("key1", e.nextElement());
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetSharedAttribute()
    {
        ServletContext globalContext = new MockServletContext();
        ServletContext ctx1 = new ServletContextImpl(bundle, globalContext, httpContext, listener, true);
        ServletContext ctx2 = new ServletContextImpl(bundle, globalContext, httpContext, listener, true);

        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertNull(ctx2.getAttribute("key1"));
        Assert.assertNull(globalContext.getAttribute("key1"));

        // Operations on ctx1 and check results

        ctx1.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        Assert.assertEquals("value1", ctx1.getAttribute("key1"));
        Assert.assertEquals("value1", ctx2.getAttribute("key1"));
        Assert.assertEquals("value1", globalContext.getAttribute("key1"));

        ctx1.removeAttribute("key1");
        this.listener.checkRemoved("key1", "value1");
        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertNull(ctx2.getAttribute("key1"));
        Assert.assertNull(globalContext.getAttribute("key1"));

        ctx1.setAttribute("key1", null);
        this.listener.checkNull();
        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertNull(ctx2.getAttribute("key1"));
        Assert.assertNull(globalContext.getAttribute("key1"));

        ctx1.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        Assert.assertEquals("value1", ctx1.getAttribute("key1"));
        Assert.assertEquals("value1", ctx2.getAttribute("key1"));
        Assert.assertEquals("value1", globalContext.getAttribute("key1"));

        ctx1.setAttribute("key1", "newValue");
        this.listener.checkReplaced("key1", "value1");
        Assert.assertEquals("newValue", ctx1.getAttribute("key1"));
        Assert.assertEquals("newValue", ctx2.getAttribute("key1"));
        Assert.assertEquals("newValue", globalContext.getAttribute("key1"));

        ctx1.removeAttribute("key1");

        // Operations on ctx2 and check results

        ctx2.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        Assert.assertEquals("value1", ctx1.getAttribute("key1"));
        Assert.assertEquals("value1", ctx2.getAttribute("key1"));
        Assert.assertEquals("value1", globalContext.getAttribute("key1"));

        ctx2.removeAttribute("key1");
        this.listener.checkRemoved("key1", "value1");
        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertNull(ctx2.getAttribute("key1"));
        Assert.assertNull(globalContext.getAttribute("key1"));

        ctx2.setAttribute("key1", null);
        this.listener.checkNull();
        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertNull(ctx2.getAttribute("key1"));
        Assert.assertNull(globalContext.getAttribute("key1"));

        ctx2.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        Assert.assertEquals("value1", ctx1.getAttribute("key1"));
        Assert.assertEquals("value1", ctx2.getAttribute("key1"));
        Assert.assertEquals("value1", globalContext.getAttribute("key1"));

        ctx2.setAttribute("key1", "newValue");
        this.listener.checkReplaced("key1", "value1");
        Assert.assertEquals("newValue", ctx1.getAttribute("key1"));
        Assert.assertEquals("newValue", ctx2.getAttribute("key1"));
        Assert.assertEquals("newValue", globalContext.getAttribute("key1"));

        ctx2.removeAttribute("key1");

        // Operations on globalContext and check results

        globalContext.setAttribute("key1", "value1");
        Assert.assertEquals("value1", ctx1.getAttribute("key1"));
        Assert.assertEquals("value1", ctx2.getAttribute("key1"));
        Assert.assertEquals("value1", globalContext.getAttribute("key1"));

        globalContext.removeAttribute("key1");
        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertNull(ctx2.getAttribute("key1"));
        Assert.assertNull(globalContext.getAttribute("key1"));

        globalContext.setAttribute("key1", null);
        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertNull(ctx2.getAttribute("key1"));
        Assert.assertNull(globalContext.getAttribute("key1"));

        globalContext.setAttribute("key1", "value1");
        Assert.assertEquals("value1", ctx1.getAttribute("key1"));
        Assert.assertEquals("value1", ctx2.getAttribute("key1"));
        Assert.assertEquals("value1", globalContext.getAttribute("key1"));

        globalContext.setAttribute("key1", "newValue");
        Assert.assertEquals("newValue", ctx1.getAttribute("key1"));
        Assert.assertEquals("newValue", ctx2.getAttribute("key1"));
        Assert.assertEquals("newValue", globalContext.getAttribute("key1"));

        globalContext.removeAttribute("key1");
    }

    @Test
    public void testGetSharedAttributeNames()
    {
        ServletContext globalContext = new MockServletContext();
        ServletContext ctx1 = new ServletContextImpl(bundle, globalContext, httpContext, listener, true);
        ServletContext ctx2 = new ServletContextImpl(bundle, globalContext, httpContext, listener, true);

        Enumeration e = ctx1.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
        e = ctx2.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
        e = globalContext.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());

        ctx1.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        e = ctx1.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertTrue(e.hasMoreElements());
        Assert.assertEquals("key1", e.nextElement());
        Assert.assertFalse(e.hasMoreElements());
        e = ctx2.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertTrue(e.hasMoreElements());
        Assert.assertEquals("key1", e.nextElement());
        Assert.assertFalse(e.hasMoreElements());
        e = globalContext.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertTrue(e.hasMoreElements());
        Assert.assertEquals("key1", e.nextElement());
        Assert.assertFalse(e.hasMoreElements());
    }


    @Test
    public void testGetUnsharedAttribute()
    {
        ServletContext globalContext = new MockServletContext();
        ServletContext ctx1 = new ServletContextImpl(bundle, globalContext, httpContext, listener, false);
        ServletContext ctx2 = new ServletContextImpl(bundle, globalContext, httpContext, listener, false);

        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertNull(ctx2.getAttribute("key1"));
        Assert.assertNull(globalContext.getAttribute("key1"));

        // Operations on ctx1 and check results

        ctx2.setAttribute("key1", "ctx2_private_value");
        globalContext.setAttribute("key1", "globalContext_private_value");
        ctx1.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        Assert.assertEquals("value1", ctx1.getAttribute("key1"));
        Assert.assertEquals("ctx2_private_value", ctx2.getAttribute("key1"));
        Assert.assertEquals("globalContext_private_value", globalContext.getAttribute("key1"));

        ctx1.removeAttribute("key1");
        this.listener.checkRemoved("key1", "value1");
        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertEquals("ctx2_private_value", ctx2.getAttribute("key1"));
        Assert.assertEquals("globalContext_private_value", globalContext.getAttribute("key1"));

        ctx1.setAttribute("key1", null);
        this.listener.checkNull();
        Assert.assertNull(ctx1.getAttribute("key1"));
        Assert.assertEquals("ctx2_private_value", ctx2.getAttribute("key1"));
        Assert.assertEquals("globalContext_private_value", globalContext.getAttribute("key1"));

        ctx1.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        Assert.assertEquals("value1", ctx1.getAttribute("key1"));
        Assert.assertEquals("ctx2_private_value", ctx2.getAttribute("key1"));
        Assert.assertEquals("globalContext_private_value", globalContext.getAttribute("key1"));

        ctx1.setAttribute("key1", "newValue");
        this.listener.checkReplaced("key1", "value1");
        Assert.assertEquals("newValue", ctx1.getAttribute("key1"));
        Assert.assertEquals("ctx2_private_value", ctx2.getAttribute("key1"));
        Assert.assertEquals("globalContext_private_value", globalContext.getAttribute("key1"));
    }

    @Test
    public void testGetUnsharedAttributeNames()
    {
        ServletContext globalContext = new MockServletContext();
        ServletContext ctx1 = new ServletContextImpl(bundle, globalContext, httpContext, listener, false);
        ServletContext ctx2 = new ServletContextImpl(bundle, globalContext, httpContext, listener, false);

        Enumeration e = ctx1.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
        e = ctx2.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
        e = globalContext.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());

        ctx1.setAttribute("key1", "value1");
        this.listener.checkAdded("key1", "value1");
        e = ctx1.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertTrue(e.hasMoreElements());
        Assert.assertEquals("key1", e.nextElement());
        Assert.assertFalse(e.hasMoreElements());
        e = ctx2.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
        e = globalContext.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetServlet()
        throws Exception
    {
        Assert.assertNull(this.context.getServlet("test"));
    }

    @Test
    public void testGetServletNames()
    {
        Enumeration e = this.context.getServletNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetServlets()
    {
        Enumeration e = this.context.getServlets();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetMimeType()
    {
        Mockito.when(this.httpContext.getMimeType("file.xml")).thenReturn("some-other-format");
        Assert.assertEquals("some-other-format", this.context.getMimeType("file.xml"));
        Assert.assertEquals("text/plain", this.context.getMimeType("file.txt"));
    }

    @Test
    public void testHandleSecurity()
        throws Exception
    {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);

        Mockito.when(this.httpContext.handleSecurity(req, res)).thenReturn(true);
        Assert.assertTrue(this.context.handleSecurity(req, res));

        Mockito.when(this.httpContext.handleSecurity(req, res)).thenReturn(false);
        Assert.assertFalse(this.context.handleSecurity(req, res));
    }

    private static class AttributeListener implements ServletContextAttributeListener
    {

        private int type;

        private String name;

        private Object value;

        public void attributeAdded(ServletContextAttributeEvent scab)
        {
            setData(1, scab);
        }

        public void attributeRemoved(ServletContextAttributeEvent scab)
        {
            setData(2, scab);
        }

        public void attributeReplaced(ServletContextAttributeEvent scab)
        {
            setData(3, scab);
        }

        private void setData(int type, ServletContextAttributeEvent scab)
        {
            this.type = type;
            this.name = scab.getName();
            this.value = scab.getValue();
        }

        void checkAdded(String name, Object value)
        {
            check(1, name, value);
        }

        void checkRemoved(String name, Object value)
        {
            check(2, name, value);
        }

        void checkReplaced(String name, Object value)
        {
            check(3, name, value);
        }

        void checkNull()
        {
            check(0, null, null);
        }

        private void check(int type, String name, Object value)
        {
            try
            {
                Assert.assertEquals(type, this.type);
                Assert.assertEquals(name, this.name);
                Assert.assertEquals(value, this.value);
            }
            finally
            {
                this.type = 0;
                this.name = null;
                this.value = null;
            }
        }
    }

    private class MockServletContext implements ServletContext {

        private Dictionary attributes = new Hashtable();

        public Object getAttribute(String name)
        {
            return attributes.get(name);
        }

        public Enumeration getAttributeNames()
        {
            return attributes.keys();
        }

        public void setAttribute(String name, Object object)
        {
            if (object != null)
            {
                attributes.put(name, object);
            }
            else
            {
                removeAttribute(name);
            }
        }

        public void removeAttribute(String name)
        {
            attributes.remove(name);
        }

        public String getContextPath()
        {
            return null;
        }

        public ServletContext getContext(String uripath)
        {
            return null;
        }

        public int getMajorVersion()
        {
            return 0;
        }

        public int getMinorVersion()
        {
            return 0;
        }

        public String getMimeType(String file)
        {
            return null;
        }

        public Set getResourcePaths(String path)
        {
            return null;
        }

        public URL getResource(String path)
        {
            return null;
        }

        public InputStream getResourceAsStream(String path)
        {
            return null;
        }

        public RequestDispatcher getRequestDispatcher(String path)
        {
            return null;
        }

        public RequestDispatcher getNamedDispatcher(String name)
        {
            return null;
        }

        public Servlet getServlet(String name)
        {
            return null;
        }

        public Enumeration getServlets()
        {
            return null;
        }

        public Enumeration getServletNames()
        {
            return null;
        }

        public void log(String msg)
        {
        }

        public void log(Exception exception, String msg)
        {
        }

        public void log(String message, Throwable throwable)
        {
        }

        public String getRealPath(String path)
        {
            return null;
        }

        public String getServerInfo()
        {
            return null;
        }

        public String getInitParameter(String name)
        {
            return null;
        }

        public Enumeration getInitParameterNames()
        {
            return null;
        }

        public String getServletContextName()
        {
            return null;
        }
    }
}
