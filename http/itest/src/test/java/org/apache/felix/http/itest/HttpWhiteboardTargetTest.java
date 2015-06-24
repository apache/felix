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

package org.apache.felix.http.itest;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpWhiteboardTargetTest extends BaseIntegrationTest
{

	private static final String SERVICE_HTTP_PORT = "org.osgi.service.http.port";

	/**]
	 * Test that a servlet with the org.osgi.http.whiteboard.target property not set
	 * is registered with the whiteboard
	 */
	@Test
	public void testServletNoTargetProperty() throws Exception
	{
		CountDownLatch initLatch = new CountDownLatch(1);
		CountDownLatch destroyLatch = new CountDownLatch(1);

		TestServlet servlet = new TestServlet(initLatch, destroyLatch)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("It works!");
				resp.flushBuffer();
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servletAlias");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servletName");

		ServiceRegistration<?> reg = m_context.registerService(Servlet.class.getName(), servlet, props);

		try {
			assertTrue(initLatch.await(5, TimeUnit.SECONDS));
			URL testURL = createURL("/servletAlias");
            assertContent("It works!", testURL);
		} finally {
				reg.unregister();
		}
		assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
	}

	/**
	 * Test that a servlet with the org.osgi.http.whiteboard.target property matching the
	 * HttpServiceRuntime properties is registered with the whiteboard.
	 *
	 * In the current implementation the HttpServiceRuntime properties are the same as the
	 * HttpService properties.
	 *
	 */
	@Test
	public void testServletTargetMatchPort() throws Exception
	{
		CountDownLatch initLatch = new CountDownLatch(1);
		CountDownLatch destroyLatch = new CountDownLatch(1);

		TestServlet servlet = new TestServlet(initLatch, destroyLatch)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("matchingServlet");
				resp.flushBuffer();
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servletAlias");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servletName");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(" + SERVICE_HTTP_PORT + "=8080" + ")");

		ServiceRegistration<?> reg = m_context.registerService(Servlet.class.getName(), servlet, props);

		try {
			assertTrue(initLatch.await(5, TimeUnit.SECONDS));
			URL testURL = createURL("/servletAlias");
            assertContent("matchingServlet", testURL);
		} finally {
			reg.unregister();
		}

		assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
	}

	/**
	 * Test that a servlet with the org.osgi.http.whiteboard.target property not matching
	 * the properties of the HttpServiceRuntime is not registered with the whiteboard.
	 *
	 */
	@Test
	public void testServletTargetNotMatchPort() throws Exception
	{
		CountDownLatch initLatch = new CountDownLatch(1);
		CountDownLatch destroyLatch = new CountDownLatch(1);

		TestServlet nonMatchingServlet = new TestServlet(initLatch, destroyLatch)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("nonMatchingServlet");
				resp.flushBuffer();
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servletAlias");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servletName");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(" + SERVICE_HTTP_PORT + "=8282" + ")");

		ServiceRegistration<?> reg = m_context.registerService(Servlet.class.getName(), nonMatchingServlet, props);

		try {
			// the servlet will not be registered, its init method will not be called, await must return false due to timeout
			assertFalse(initLatch.await(5, TimeUnit.SECONDS));
			URL testURL = createURL("/servletAlias");
			assertResponseCode(404, testURL);
		} finally {
			reg.unregister();
		}
	}

	/**
	 * Test that a filter with no target property set is correctly registered with the whiteboard
	 *
	 */
	@Test
	public void testFilterNoTargetProperty() throws Exception
	{
		CountDownLatch initLatch = new CountDownLatch(3);
		CountDownLatch destroyLatch = new CountDownLatch(3);

		TestServlet servlet1 = new TestServlet(initLatch, destroyLatch)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("servlet1");
				resp.flushBuffer();
			}
		};
		Dictionary<String, Object> props1 = new Hashtable<String, Object>();
		props1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet/1");
		props1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servlet1");
		props1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(" + SERVICE_HTTP_PORT + "=8080" + ")");

		TestServlet servlet2 = new TestServlet(initLatch, destroyLatch)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("servlet2");
				resp.flushBuffer();
			}
		};
		Dictionary<String, Object> props2 = new Hashtable<String, Object>();
		props2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet/2");
		props2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servle2");
		props2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(" + SERVICE_HTTP_PORT + "=8080" + ")");

		TestFilter filter = new TestFilter(initLatch, destroyLatch)
		{
			@Override
			protected void filter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException
			{
				String param = req.getParameter("param");
				if("forbidden".equals(param))
				{
					resp.reset();
					resp.sendError(SC_FORBIDDEN);
					resp.flushBuffer();
				}
				else
				{
					chain.doFilter(req, resp);
				}
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/servlet/1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servletName");

		ServiceRegistration<?> reg1 = m_context.registerService(Servlet.class.getName(), servlet1, props1);
		ServiceRegistration<?> reg2 = m_context.registerService(Servlet.class.getName(), servlet2, props2);
		ServiceRegistration<?> reg = m_context.registerService(Filter.class.getName(), filter, props);

		assertTrue(initLatch.await(5, TimeUnit.SECONDS));

		assertResponseCode(SC_FORBIDDEN, createURL("/servlet/1?param=forbidden"));
		assertContent("servlet1", createURL("/servlet/1?param=any"));
		assertContent("servlet1", createURL("/servlet/1"));

		assertResponseCode(SC_OK, createURL("/servlet/2?param=forbidden"));
		assertContent("servlet2", createURL("/servlet/2?param=forbidden"));

		reg1.unregister();
		reg2.unregister();
		reg.unregister();

		assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
	}

	@Test
	public void testFilterTargetMatchPort() throws Exception
	{
		CountDownLatch initLatch = new CountDownLatch(2);
		CountDownLatch destroyLatch = new CountDownLatch(2);

		TestServlet servlet = new TestServlet(initLatch, destroyLatch)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("servlet");
				resp.flushBuffer();
			}
		};
		Dictionary<String, Object> sprops = new Hashtable<String, Object>();
		sprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet");
		sprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servlet1");
		sprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(" + SERVICE_HTTP_PORT + "=8080" + ")");

		TestFilter filter = new TestFilter(initLatch, destroyLatch)
		{
			@Override
			protected void filter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException
			{
				String param = req.getParameter("param");
				if("forbidden".equals(param))
				{
					resp.reset();
					resp.sendError(SC_FORBIDDEN);
					resp.flushBuffer();
				}
				else
				{
					chain.doFilter(req, resp);
				}
			}
		};

		Dictionary<String, Object> fprops = new Hashtable<String, Object>();
		fprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/servlet");
		fprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servletName");
		fprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(" + SERVICE_HTTP_PORT + "=8080" + ")");

		ServiceRegistration<?> sreg = m_context.registerService(Servlet.class.getName(), servlet, sprops);
		ServiceRegistration<?> freg = m_context.registerService(Filter.class.getName(), filter, fprops);

		assertTrue(initLatch.await(5, TimeUnit.SECONDS));

		assertResponseCode(SC_FORBIDDEN, createURL("/servlet?param=forbidden"));
		assertContent("servlet", createURL("/servlet?param=any"));
		assertContent("servlet", createURL("/servlet"));

		sreg.unregister();
		freg.unregister();

		assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));
	}

	@Test
	public void testFilterTargetNotMatchPort() throws Exception
	{
		CountDownLatch servletInitLatch = new CountDownLatch(1);
		CountDownLatch servletDestroyLatch = new CountDownLatch(1);

		CountDownLatch filterInitLatch = new CountDownLatch(1);
		CountDownLatch filterDestroyLatch = new CountDownLatch(1);

		TestServlet servlet = new TestServlet(servletInitLatch, servletDestroyLatch)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
			{
				resp.getWriter().print("servlet");
				resp.flushBuffer();
			}
		};
		Dictionary<String, Object> sprops = new Hashtable<String, Object>();
		sprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet");
		sprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servlet1");
		sprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(" + SERVICE_HTTP_PORT + "=8080" + ")");

		TestFilter filter = new TestFilter(filterInitLatch, filterDestroyLatch)
		{
			@Override
			protected void filter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException
			{
				String param = req.getParameter("param");
				if("forbidden".equals(param))
				{
					resp.reset();
					resp.sendError(SC_FORBIDDEN);
					resp.flushBuffer();
				}
				else
				{
					chain.doFilter(req, resp);
				}
			}
		};

		Dictionary<String, Object> fprops = new Hashtable<String, Object>();
		fprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/servlet");
		fprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + ".myname", "servletName");
		fprops.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(" + SERVICE_HTTP_PORT + "=8181" + ")");

		ServiceRegistration<?> sreg = m_context.registerService(Servlet.class.getName(), servlet, sprops);
		ServiceRegistration<?> freg = m_context.registerService(Filter.class.getName(), filter, fprops);

		// servlet is registered
		assertTrue(servletInitLatch.await(5, TimeUnit.SECONDS));
		// fitler is not registered, timeout occurs
		assertFalse(filterInitLatch.await(5, TimeUnit.SECONDS));

		assertResponseCode(SC_OK, createURL("/servlet?param=forbidden"));
		assertContent("servlet", createURL("/servlet?param=forbidden"));
		assertContent("servlet", createURL("/servlet?param=any"));
		assertContent("servlet", createURL("/servlet"));

		sreg.unregister();
		freg.unregister();

		assertTrue(servletDestroyLatch.await(5, TimeUnit.SECONDS));
		assertFalse(filterDestroyLatch.await(5, TimeUnit.SECONDS));
	}
}
