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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.naming.directory.SearchControls;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.runtime.HttpServiceRuntime;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpServiceTest extends BaseIntegrationTest
{

    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    private CountDownLatch initLatch;
    private CountDownLatch destroyLatch;

    public void setupLatches(int count)
    {
        initLatch = new CountDownLatch(count);
        destroyLatch = new CountDownLatch(count);
    }

    public void setupOldWhiteboardFilter(final String pattern) throws Exception
    {
        Dictionary<String, Object> servletProps = new Hashtable<String, Object>();
        servletProps.put("pattern", pattern);

        final Filter f = new Filter()
        {

            @Override
            public void init(FilterConfig filterConfig) throws ServletException
            {
                initLatch.countDown();
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException
            {
                response.getWriter().print("FILTER-");
                response.flushBuffer();
                chain.doFilter(request, response);
            }

            @Override
            public void destroy()
            {
                destroyLatch.countDown();
            }
        };

        registrations.add(m_context.registerService(Filter.class.getName(), f, servletProps));
    }

    @After
    public void unregisterServices() throws InterruptedException
    {
        for (ServiceRegistration<?> serviceRegistration : registrations)
        {
            serviceRegistration.unregister();
        }

        assertTrue(destroyLatch.await(5, TimeUnit.SECONDS));

        Thread.sleep(500);
    }

    @Test
    public void testServletAndOldWhiteboardFilter() throws Exception
    {
        final HttpService service = this.getHttpService();
        service.registerServlet("/tesths", new TestServlet()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException
            {
                resp.getWriter().print("helloworld");
                resp.flushBuffer();
            }
        }, null, null);

        this.setupLatches(1);
        this.setupOldWhiteboardFilter(".*");
        assertTrue(initLatch.await(5, TimeUnit.SECONDS));
        final HttpServiceRuntime rt = this.getService(HttpServiceRuntime.class.getName());
        System.out.println(rt.getRuntimeDTO());
        try
        {
            assertContent("FILTER-helloworld", createURL("/tesths"));
        }
        finally
        {
            service.unregister("/tesths");
        }
    }

    /**
     * Tests the starting of Jetty.
     */
    @Test
    public void testHttpServiceCapabiltiy() throws Exception
    {
    	setupLatches(0);
    	
        Bundle httpJettyBundle = getHttpJettyBundle();
        
        BundleWiring wiring = httpJettyBundle.adapt(BundleWiring.class);
        
		List<BundleCapability> capabilities = wiring.getCapabilities("osgi.service");
		
		assertFalse(capabilities.isEmpty());
		
		boolean found = false;
		
		for (BundleCapability capability : capabilities) {
			@SuppressWarnings("unchecked")
			List<String> objectClass = (List<String>) capability.getAttributes().get(Constants.OBJECTCLASS);
			
			assertNotNull(objectClass);
			
			if(objectClass.contains(HttpService.class.getName())) {
				String uses = capability.getDirectives().get("uses");
				
				assertNotNull(uses);
				
				assertTrue(uses.contains(HttpService.class.getPackage().getName()));
				
				found = true;
				break;
			}
		}
		
		assertTrue("Missing HttpService capability", found);
    }
    
}
