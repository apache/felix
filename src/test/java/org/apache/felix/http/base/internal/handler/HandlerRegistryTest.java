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
package org.apache.felix.http.base.internal.handler;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.http.NamespaceException;

public class HandlerRegistryTest
{
    @Test
    public void testAddRemoveServlet() throws Exception
    {
        HandlerRegistry hr = new HandlerRegistry();

        Servlet servlet = Mockito.mock(Servlet.class);
        final ServletInfo info = new ServletInfo("foo", "/foo", 0, null, servlet, null);
        ServletHandler handler = new ServletHandler(null, info);
        assertEquals("Precondition", 0, hr.getServlets().length);
        hr.addServlet(handler);
        Mockito.verify(servlet, Mockito.times(1)).init(Mockito.any(ServletConfig.class));
        assertEquals(1, hr.getServlets().length);
        assertSame(handler, hr.getServlets()[0]);

        final ServletInfo info2 = new ServletInfo("bar", "/bar", 0, null, servlet, null);
        ServletHandler handler2 = new ServletHandler(null, info2);
        try
        {
            hr.addServlet(handler2);
            // TODO
//            fail("Should not have allowed to add the same servlet twice");
        }
        catch (ServletException se)
        {
            // good
        }
        assertArrayEquals(new ServletHandler[] {handler2, handler}, hr.getServlets());

        final ServletInfo info3 = new ServletInfo("zar", "/foo", 0, null, Mockito.mock(Servlet.class), null);
        ServletHandler handler3 = new ServletHandler(null,info3);

        try
        {
            hr.addServlet(handler3);
            fail("Should not have allowed to add the same alias twice");
        }
        catch (NamespaceException ne) {
            // good
        }
        assertArrayEquals(new ServletHandler[] {handler2, handler}, hr.getServlets());

        assertSame(servlet, hr.getServletByAlias("/foo"));

        Mockito.verify(servlet, Mockito.never()).destroy();
        hr.removeServlet(servlet, true);
        Mockito.verify(servlet, Mockito.times(2)).destroy();
        assertEquals(0, hr.getServlets().length);
    }

    @Test
    public void testAddServletWhileSameServletAddedDuringInit() throws Exception
    {
        final HandlerRegistry hr = new HandlerRegistry();

        Servlet servlet = Mockito.mock(Servlet.class);
        final ServletInfo info = new ServletInfo("bar", "/bar", 0, null, servlet, null);
        final ServletHandler otherHandler = new ServletHandler(null, info);

        Mockito.doAnswer(new Answer<Void>()
        {
            boolean registered = false;
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                if (!registered)
                {
                    registered = true;
                    // sneakily register another handler with this servlet before this
                    // one has finished calling init()
                    hr.addServlet(otherHandler);
                }
                return null;
            }
        }).when(servlet).init(Mockito.any(ServletConfig.class));

        final ServletInfo info2 = new ServletInfo("foo", "/foo", 0, null, servlet, null);
        ServletHandler handler = new ServletHandler(null, info2);
        try
        {
            hr.addServlet(handler);

            // TODO
//            fail("Should not have allowed the servlet to be added as it was already "
//                    + "added before init was finished");

        }
        catch (ServletException ne)
        {
            // good
        }
        assertArrayEquals(new ServletHandler[] {otherHandler, handler}, hr.getServlets());
    }

    @Test
    public void testAddServletWhileSameAliasAddedDuringInit() throws Exception
    {
        final HandlerRegistry hr = new HandlerRegistry();

        Servlet otherServlet = Mockito.mock(Servlet.class);
        final ServletInfo info = new ServletInfo("bar", "/foo", 0, null, otherServlet, null);
        final ServletHandler otherHandler = new ServletHandler(null, info);

        Servlet servlet = Mockito.mock(Servlet.class);
        Mockito.doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                // sneakily register another servlet before this one has finished calling init()
                hr.addServlet(otherHandler);
                return null;
            }
        }).when(servlet).init(Mockito.any(ServletConfig.class));

        final ServletInfo info2 = new ServletInfo("foo", "/foo", 0, null, servlet, null);
        ServletHandler handler = new ServletHandler(null, info2);

        try
        {
            hr.addServlet(handler);
            fail("Should not have allowed the servlet to be added as another one got in there with the same alias");
        }
        catch (NamespaceException ne)
        {
            // good
        }
        assertArrayEquals(new ServletHandler[] {otherHandler}, hr.getServlets());
        Mockito.verify(servlet, Mockito.times(1)).destroy();

        assertSame(otherServlet, hr.getServletByAlias("/foo"));
    }

    @Test
    public void testAddRemoveFilter() throws Exception
    {
        HandlerRegistry hr = new HandlerRegistry();

        Filter filter = Mockito.mock(Filter.class);
        final FilterInfo info = new FilterInfo();
        info.name = "oho";
        info.patterns = new String[] {"/aha"};
        info.ranking = 1;

        FilterHandler handler = new FilterHandler(null, filter, info);
        assertEquals("Precondition", 0, hr.getFilters().length);
        hr.addFilter(handler);
        Mockito.verify(filter, Mockito.times(1)).init(Mockito.any(FilterConfig.class));
        assertEquals(1, hr.getFilters().length);
        assertSame(handler, hr.getFilters()[0]);

        final FilterInfo info2 = new FilterInfo();
        info2.name = "haha";
        info2.patterns = new String[] {"/hihi"};
        info2.ranking = 2;
        FilterHandler handler2 = new FilterHandler(null, filter, info2);
        try
        {
            hr.addFilter(handler2);
            fail("Should not have allowed the same filter to be added twice");
        }
        catch(ServletException se)
        {
            // good
        }
        assertArrayEquals(new FilterHandler[] {handler}, hr.getFilters());

        Mockito.verify(filter, Mockito.never()).destroy();
        hr.removeFilter(filter, true);
        Mockito.verify(filter, Mockito.times(1)).destroy();
        assertEquals(0, hr.getServlets().length);
    }

    @Test
    public void testAddFilterWhileSameFilterAddedDuringInit() throws Exception
    {
        final HandlerRegistry hr = new HandlerRegistry();

        Filter filter = Mockito.mock(Filter.class);
        final FilterInfo info = new FilterInfo();
        info.name = "two";
        info.patterns = new String[] {"/two"};
        info.ranking = 99;
        final FilterHandler otherHandler = new FilterHandler(null, filter, info);

        Mockito.doAnswer(new Answer<Void>()
        {
            boolean registered = false;
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                if (!registered)
                {
                    registered = true;
                    // sneakily register another handler with this filter before this
                    // one has finished calling init()
                    hr.addFilter(otherHandler);
                }
                return null;
            }
        }).when(filter).init(Mockito.any(FilterConfig.class));

        final FilterInfo info2 = new FilterInfo();
        info2.name = "one";
        info2.patterns = new String[] {"/one"};
        info2.ranking = 1;
        FilterHandler handler = new FilterHandler(null, filter, info2);

        try
        {
            hr.addFilter(handler);
            fail("Should not have allowed the filter to be added as it was already "
                    + "added before init was finished");
        }
        catch (ServletException se)
        {
            // good
        }
        assertArrayEquals(new FilterHandler[] {otherHandler}, hr.getFilters());
    }

    @Test
    public void testRemoveAll() throws Exception
    {
        HandlerRegistry hr = new HandlerRegistry();

        Servlet servlet = Mockito.mock(Servlet.class);
        final ServletInfo info = new ServletInfo("f", "/f", 0, null, servlet, null);
        ServletHandler servletHandler = new ServletHandler(null, info);
        hr.addServlet(servletHandler);
        Servlet servlet2 = Mockito.mock(Servlet.class);
        final ServletInfo info2 = new ServletInfo("ff", "/ff", 0, null, servlet2, null);
        ServletHandler servletHandler2 = new ServletHandler(null, info2);
        hr.addServlet(servletHandler2);
        Filter filter = Mockito.mock(Filter.class);
        final FilterInfo fi = new FilterInfo();
        fi.name = "f";
        fi.patterns = new String[] {"/f"};
        fi.ranking = 0;
        FilterHandler filterHandler = new FilterHandler(null, filter, fi);
        hr.addFilter(filterHandler);

        assertEquals(2, hr.getServlets().length);
        assertEquals("Most specific Alias should come first",
                "/ff", hr.getServlets()[0].getAlias());
        assertEquals("/f", hr.getServlets()[1].getAlias());
        assertEquals(1, hr.getFilters().length);
        assertSame(filter, hr.getFilters()[0].getFilter());

        Mockito.verify(servlet, Mockito.never()).destroy();
        Mockito.verify(servlet2, Mockito.never()).destroy();
        Mockito.verify(filter, Mockito.never()).destroy();
        hr.removeAll();
        Mockito.verify(servlet, Mockito.times(1)).destroy();
        Mockito.verify(servlet2, Mockito.times(1)).destroy();
        Mockito.verify(filter, Mockito.times(1)).destroy();

        assertEquals(0, hr.getServlets().length);
        assertEquals(0, hr.getFilters().length);
    }
}
