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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.service.http.runtime.HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT;
import static org.osgi.service.http.runtime.HttpServiceRuntimeConstants.HTTP_SERVICE_ID;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpServiceRuntimeTest extends BaseIntegrationTest
{
    private static final String HTTP_CONTEXT_NAME = "org.osgi.service.http";

    Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    private static final long DEFAULT_SLEEP = 100;

    @After
    public void unregisterServices() throws Exception
    {
        for (ServiceRegistration<?> serviceRegistration : registrations)
        {
            try
            {
                serviceRegistration.unregister();
            }
            catch (Exception e)
            {
                // already unregistered
            }
        }
        registrations.clear();

        Thread.sleep(100);
    }

    private void registerServlet(String name, String path) throws InterruptedException
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        registerServlet(name, path, null, initLatch);
        awaitServiceRegistration(initLatch);
    }

    private void registerServlet(String name, String path, CountDownLatch initLatch)
    {
        registerServlet(name, path, null, initLatch);
    }

    private void registerServlet(String name, String path, String context, CountDownLatch initLatch)
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HTTP_WHITEBOARD_SERVLET_PATTERN, path,
                HTTP_WHITEBOARD_SERVLET_NAME, name,
                HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 4).toArray() : propertyEntries.toArray());

        registrations.add(m_context.registerService(Servlet.class.getName(), new TestServlet(initLatch, null), properties));
    }

    private void registerFilter(String name, String path) throws InterruptedException
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        registerFilter(name, path, initLatch);
        awaitServiceRegistration(initLatch);
    }

    private void registerFilter(String name, String path, CountDownLatch initLatch)
    {
        registerFilter(name, path, null, initLatch);
    }

    private void registerFilter(String name, String path, String context, CountDownLatch initLatch)
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HTTP_WHITEBOARD_FILTER_PATTERN, path,
                HTTP_WHITEBOARD_FILTER_NAME, name,
                HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 4).toArray() : propertyEntries.toArray());

        registrations.add(m_context.registerService(Filter.class.getName(), new TestFilter(initLatch, null), properties));
    }

    private void registerResource(String prefix, String path) throws InterruptedException
    {
        registerResource(prefix, path, null);
    }

    private void registerResource(String prefix, String path, String context) throws InterruptedException
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HTTP_WHITEBOARD_RESOURCE_PATTERN, path,
                HTTP_WHITEBOARD_RESOURCE_PREFIX, prefix,
                HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 4).toArray() : propertyEntries.toArray());

        registrations.add(m_context.registerService(TestResource.class.getName(), new TestResource(), properties));
        awaitServiceRegistration();
    }

    private void registerErrorPage(String name, List<String> errors) throws InterruptedException
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        registerErrorPage(name, errors, initLatch);
        awaitServiceRegistration(initLatch);
    }

    private void registerErrorPage(String name, List<String> errors, CountDownLatch initLatch)
    {
        registerErrorPage(name, errors, null, initLatch);
    }

    private void registerErrorPage(String name, List<String> errors, String context, CountDownLatch initLatch)
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, errors,
                HTTP_WHITEBOARD_SERVLET_NAME, name,
                HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 4).toArray() : propertyEntries.toArray());

        registrations.add(m_context.registerService(Servlet.class.getName(), new TestServlet(initLatch, null), properties));
    }

    private void registerListener(Class<?> listenerClass, boolean useWithWhiteboard) throws InterruptedException
    {
        registerListener(listenerClass, useWithWhiteboard, null);
    }

    private void registerListener(Class<?> listenerClass, boolean useWithWhiteboard, String context) throws InterruptedException
    {
        List<Object> propertyEntries = Arrays.<Object>asList(
                HTTP_WHITEBOARD_LISTENER, useWithWhiteboard ? "true" : "false",
                HTTP_WHITEBOARD_CONTEXT_SELECT, context);

        Dictionary<String, ?> properties = createDictionary(context == null ?
                propertyEntries.subList(0, 2).toArray() : propertyEntries.toArray());

        registrations.add(m_context.registerService(listenerClass.getName(), mock(listenerClass), properties));
        awaitServiceRegistration();
    }

    private ServiceRegistration<?> registerContext(String name, String path) throws InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_CONTEXT_NAME, name,
                HTTP_WHITEBOARD_CONTEXT_PATH, path);

        ServiceRegistration<?> contextRegistration = m_context.registerService(ServletContextHelper.class.getName(), mock(ServletContextHelper.class), properties);
        registrations.add(contextRegistration);
        awaitServiceRegistration();
        return contextRegistration;
    }

    @Before
    public void awaitServiceRuntime() throws Exception
    {
        awaitService(HttpServiceRuntime.class.getName());
    }

    @Test
    public void httpRuntimeServiceIsAvailableAfterBundleActivation() throws Exception
    {
        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        ServiceReferenceDTO serviceDTO = runtimeDTO.serviceDTO;

        assertNotNull(serviceDTO);
        assertNotNull(serviceDTO.properties);
        assertTrue(serviceDTO.properties.containsKey(HTTP_SERVICE_ID));
        assertTrue(serviceDTO.properties.containsKey(HTTP_SERVICE_ENDPOINT));

        assertTrue(serviceDTO.properties.get(HTTP_SERVICE_ID) instanceof Collection);
        final Collection ids = (Collection)serviceDTO.properties.get(HTTP_SERVICE_ID);
        assertTrue(ids.size() == 1);
        assertTrue(ids.iterator().next() instanceof Long);
        assertTrue(0 < (Long)ids.iterator().next());

        assertEquals(0, runtimeDTO.failedErrorPageDTOs.length);
        assertEquals(0, runtimeDTO.failedFilterDTOs.length);
        assertEquals(0, runtimeDTO.failedListenerDTOs.length);
        assertEquals(0, runtimeDTO.failedResourceDTOs.length);
        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(0, runtimeDTO.failedServletDTOs.length);

        ServletContextDTO defaultContext = assertDefaultContext(runtimeDTO);

        assertEquals(0, defaultContext.attributes.size());
        // TODO The default context should have a negative service Id
//        assertTrue(0 > runtimeDTO.servletContextDTOs[0].serviceId);
        // TODO Should be "/" ?
        assertEquals("", defaultContext.contextPath);
        assertEquals(0, defaultContext.initParams.size());

        assertEquals(0, defaultContext.filterDTOs.length);
        assertEquals(0, defaultContext.servletDTOs.length);
        assertEquals(0, defaultContext.resourceDTOs.length);
        assertEquals(0, defaultContext.errorPageDTOs.length);
        assertEquals(0, defaultContext.listenerDTOs.length);
    }

    @Test
    public void dtosForSuccesfullyRegisteredServlets() throws Exception
    {
        //register first servlet
        registerServlet("testServlet 1", "/servlet_1");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstSerlvet = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstSerlvet.failedServletDTOs.length);

        ServletContextDTO contextDTO = assertDefaultContext(runtimeDTOWithFirstSerlvet);
        assertEquals(1, contextDTO.servletDTOs.length);
        assertEquals("testServlet 1", contextDTO.servletDTOs[0].name);

        //register second servlet
        registerServlet("testServlet 2", "/servlet_2");

        RuntimeDTO runtimeDTOWithBothSerlvets = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithBothSerlvets.failedServletDTOs.length);

        contextDTO = assertDefaultContext(runtimeDTOWithBothSerlvets);
        assertEquals(2, contextDTO.servletDTOs.length);
        final Set<String> names = new HashSet<String>();
        names.add(contextDTO.servletDTOs[0].name);
        names.add(contextDTO.servletDTOs[1].name);
        assertTrue(names.contains("testServlet 1"));
        assertTrue(names.contains("testServlet 2"));
    }

    @Test
    public void dtosForSuccesfullyRegisteredFilters() throws Exception
    {
        //register first filter
        registerFilter("testFilter 1", "/servlet_1");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstFilter = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstFilter.failedFilterDTOs.length);

        ServletContextDTO contextDTO = assertDefaultContext(runtimeDTOWithFirstFilter);
        assertEquals(1, contextDTO.filterDTOs.length);
        assertEquals("testFilter 1", contextDTO.filterDTOs[0].name);

        //register second filter
        registerFilter("testFilter 2", "/servlet_1");

        RuntimeDTO runtimeDTOWithBothFilters = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithBothFilters.failedFilterDTOs.length);

        contextDTO = assertDefaultContext(runtimeDTOWithBothFilters);
        assertEquals(2, contextDTO.filterDTOs.length);
        assertEquals("testFilter 1", contextDTO.filterDTOs[0].name);
        assertEquals("testFilter 2", contextDTO.filterDTOs[1].name);
    }

    @Test
    public void dtosForSuccesfullyRegisteredResources() throws Exception
    {
        // register first resource service
        registerResource("/resources", "/resource_1/*");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstResource = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstResource.failedResourceDTOs.length);

        ServletContextDTO contextDTO = assertDefaultContext(runtimeDTOWithFirstResource);
        assertEquals(1, contextDTO.resourceDTOs.length);
        assertEquals("/resources", contextDTO.resourceDTOs[0].prefix);
        assertArrayEquals(new String[] { "/resource_1/*" }, contextDTO.resourceDTOs[0].patterns);

        // register second resource service
        registerResource("/resources", "/resource_2/*");

        RuntimeDTO runtimeDTOWithBothResources = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithBothResources.failedResourceDTOs.length);

        contextDTO = assertDefaultContext(runtimeDTOWithBothResources);
        assertEquals(2, contextDTO.resourceDTOs.length);
        assertEquals("/resources", contextDTO.resourceDTOs[0].prefix);
        assertEquals(1, contextDTO.resourceDTOs[0].patterns.length);
        assertEquals(1, contextDTO.resourceDTOs[1].patterns.length);
        final Set<String> patterns = new HashSet<String>();
        patterns.add(contextDTO.resourceDTOs[0].patterns[0]);
        patterns.add(contextDTO.resourceDTOs[1].patterns[0]);
        assertTrue(patterns.contains("/resource_1/*"));
        assertTrue(patterns.contains("/resource_2/*"));
    }

    @Test
    public void dtosForSuccesfullyRegisteredErrorPages() throws Exception
    {
        // register first error page
        registerErrorPage("error page 1", asList("404", NoSuchElementException.class.getName()));

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstErrorPage = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstErrorPage.failedServletDTOs.length);
        assertEquals(0, runtimeDTOWithFirstErrorPage.failedErrorPageDTOs.length);

        ServletContextDTO contextDTO = assertDefaultContext(runtimeDTOWithFirstErrorPage);
        assertEquals(1, contextDTO.errorPageDTOs.length);
        assertEquals("error page 1", contextDTO.errorPageDTOs[0].name);
        assertArrayEquals(new String[] { NoSuchElementException.class.getName() }, contextDTO.errorPageDTOs[0].exceptions);
        assertArrayEquals(new long[] { 404 }, contextDTO.errorPageDTOs[0].errorCodes);

        // register second error page
        registerErrorPage("error page 2", asList("500", ServletException.class.getName()));

        RuntimeDTO runtimeDTOWithBothErrorPages = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithBothErrorPages.failedServletDTOs.length);
        assertEquals(0, runtimeDTOWithBothErrorPages.failedErrorPageDTOs.length);

        contextDTO = assertDefaultContext(runtimeDTOWithBothErrorPages);
        assertEquals(2, contextDTO.errorPageDTOs.length);
        assertEquals("error page 1", contextDTO.errorPageDTOs[0].name);
        assertEquals("error page 2", contextDTO.errorPageDTOs[1].name);
        assertArrayEquals(new String[] { ServletException.class.getName() }, contextDTO.errorPageDTOs[1].exceptions);
        assertArrayEquals(new long[] { 500 }, contextDTO.errorPageDTOs[1].errorCodes);
    }

    @Test
    public void dtosForSuccesfullyRegisteredErrorPageForClientErrorCodes() throws Exception
    {
        dtosForSuccesfullyRegisteredErrorPageWithWildcardErrorCode("4xx", 400);
    }

    @Test
    public void dtosForSuccesfullyRegisteredErrorPageForClientErrorCodesCaseInsensitive() throws Exception
    {
        dtosForSuccesfullyRegisteredErrorPageWithWildcardErrorCode("4xX", 400);
    }

    @Test
    public void dtosForSuccesfullyRegisteredErrorPageForServerErrorCodes() throws Exception
    {
        dtosForSuccesfullyRegisteredErrorPageWithWildcardErrorCode("5xx", 500);
    }

    @Test
    public void dtosForSuccesfullyRegisteredErrorPageForServerErrorCodesCaseInsensitive() throws Exception
    {
        dtosForSuccesfullyRegisteredErrorPageWithWildcardErrorCode("5XX", 500);
    }

    public void dtosForSuccesfullyRegisteredErrorPageWithWildcardErrorCode(String code, long startCode) throws Exception
    {
        registerErrorPage("error page 1", asList(code));

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithErrorPage = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithErrorPage.failedServletDTOs.length);
        assertEquals(0, runtimeDTOWithErrorPage.failedErrorPageDTOs.length);

        ServletContextDTO contextDTO = assertDefaultContext(runtimeDTOWithErrorPage);
        assertEquals(1, contextDTO.errorPageDTOs.length);
        assertEquals("error page 1", contextDTO.errorPageDTOs[0].name);
        assertContainsAllHundredFrom(startCode, contextDTO.errorPageDTOs[0].errorCodes);
    }

    private void assertContainsAllHundredFrom(Long start, long[] errorCodes)
    {
        assertEquals(100, errorCodes.length);
        SortedSet<Long> distinctErrorCodes = new TreeSet<Long>();
        for (Long code : errorCodes)
        {
            distinctErrorCodes.add(code);
        }
        assertEquals(100, distinctErrorCodes.size());
        assertEquals(start, distinctErrorCodes.first());
        assertEquals(Long.valueOf(start + 99), distinctErrorCodes.last());
    }

    @Test
    public void dtosForSuccesfullyRegisteredListeners() throws Exception
    {
        // register a servlet context listenere as first listener
        registerListener(ServletContextListener.class, true);
        awaitService(ServletContextListener.class.getName());

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstListener = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithFirstListener.failedListenerDTOs.length);

        ServletContextDTO contextDTO = assertDefaultContext(runtimeDTOWithFirstListener);
        assertEquals(1, contextDTO.listenerDTOs.length);
        assertEquals(ServletContextListener.class.getName(), contextDTO.listenerDTOs[0].types[0]);

        // register all other listener types
        registerListener(ServletContextAttributeListener.class, true);
        registerListener(ServletRequestListener.class, true);
        registerListener(ServletRequestAttributeListener.class, true);
        registerListener(HttpSessionListener.class, true);
        registerListener(HttpSessionAttributeListener.class, true);

        awaitService(ServletContextAttributeListener.class.getName());
        awaitService(ServletRequestListener.class.getName());
        awaitService(ServletRequestAttributeListener.class.getName());
        awaitService(HttpSessionListener.class.getName());
        awaitService(HttpSessionAttributeListener.class.getName());

        RuntimeDTO runtimeDTOWithAllListeners = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithAllListeners.failedListenerDTOs.length);

        contextDTO = assertDefaultContext(runtimeDTOWithAllListeners);

        assertEquals(6, contextDTO.listenerDTOs.length);
        assertEquals(ServletContextListener.class.getName(), contextDTO.listenerDTOs[0].types[0]);
        assertEquals(ServletContextAttributeListener.class.getName(), contextDTO.listenerDTOs[1].types[0]);
        assertEquals(ServletRequestListener.class.getName(), contextDTO.listenerDTOs[2].types[0]);
        assertEquals(ServletRequestAttributeListener.class.getName(), contextDTO.listenerDTOs[3].types[0]);
        assertEquals(HttpSessionListener.class.getName(), contextDTO.listenerDTOs[4].types[0]);
        assertEquals(HttpSessionAttributeListener.class.getName(), contextDTO.listenerDTOs[5].types[0]);
    }

    @Test
    public void dtosForSuccesfullyRegisteredContexts() throws Exception
    {
        // register first additional context
        registerContext("contextA", "/contextA");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithAdditionalContext = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithAdditionalContext.failedServletContextDTOs.length);
        assertEquals(3, runtimeDTOWithAdditionalContext.servletContextDTOs.length);

        // default context is last, as it has the lowest service ranking
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTOWithAdditionalContext.servletContextDTOs[0].name);
        assertEquals("", runtimeDTOWithAdditionalContext.servletContextDTOs[0].contextPath);
        assertEquals("contextA", runtimeDTOWithAdditionalContext.servletContextDTOs[1].name);
        assertEquals("/contextA", runtimeDTOWithAdditionalContext.servletContextDTOs[1].contextPath);
        assertEquals("default", runtimeDTOWithAdditionalContext.servletContextDTOs[2].name);
        // TODO should this be "/" ?
        assertEquals("", runtimeDTOWithAdditionalContext.servletContextDTOs[2].contextPath);

        // register second additional context
        registerContext("contextB", "/contextB");

        RuntimeDTO runtimeDTOWithAllContexts = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTOWithAllContexts.failedServletContextDTOs.length);
        assertEquals(4, runtimeDTOWithAllContexts.servletContextDTOs.length);

        // default context is last, as it has the lowest service ranking
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTOWithAdditionalContext.servletContextDTOs[0].name);
        assertEquals("", runtimeDTOWithAdditionalContext.servletContextDTOs[0].contextPath);
        assertEquals("contextA", runtimeDTOWithAllContexts.servletContextDTOs[1].name);
        assertEquals("/contextA", runtimeDTOWithAllContexts.servletContextDTOs[1].contextPath);
        assertEquals("contextB", runtimeDTOWithAllContexts.servletContextDTOs[2].name);
        assertEquals("/contextB", runtimeDTOWithAllContexts.servletContextDTOs[2].contextPath);
        assertEquals("default", runtimeDTOWithAllContexts.servletContextDTOs[3].name);
        assertEquals("", runtimeDTOWithAllContexts.servletContextDTOs[3].contextPath);
    }

    @Test
    public void successfulSetup() throws InterruptedException
    {
        CountDownLatch initLatch = new CountDownLatch(6);

        registerContext("test-context", "/test-context");

        registerServlet("default servlet", "/default", initLatch);
        registerFilter("default filter", "/default", initLatch);
        registerErrorPage("default error page", asList(Exception.class.getName()), initLatch);
        registerResource("/", "/default/resource");
        registerListener(ServletRequestListener.class, true);

        registerServlet("context servlet", "/default", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)", initLatch);
        registerFilter("context filter", "/default", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)", initLatch);
        registerErrorPage("context error page", asList("500"), "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)", initLatch);
        registerResource("/", "/test-contextd/resource", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)");
        registerListener(ServletRequestListener.class, true, "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)");

        awaitServiceRegistration(initLatch);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedErrorPageDTOs.length);
        assertEquals(0, runtimeDTO.failedFilterDTOs.length);
        assertEquals(0, runtimeDTO.failedListenerDTOs.length);
        assertEquals(0, runtimeDTO.failedResourceDTOs.length);
        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(0, runtimeDTO.failedServletDTOs.length);

        assertEquals(3, runtimeDTO.servletContextDTOs.length);
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTO.servletContextDTOs[0].name);
        assertEquals("test-context", runtimeDTO.servletContextDTOs[1].name);
        assertEquals("default", runtimeDTO.servletContextDTOs[2].name);

        ServletContextDTO defaultContextDTO = runtimeDTO.servletContextDTOs[2];
        long contextServiceId = defaultContextDTO.serviceId;

        assertEquals(1, defaultContextDTO.servletDTOs.length);
        assertEquals("default servlet", defaultContextDTO.servletDTOs[0].name);
        assertEquals(contextServiceId, defaultContextDTO.servletDTOs[0].servletContextId);
        assertEquals(1, defaultContextDTO.filterDTOs.length);
        assertEquals("default filter", defaultContextDTO.filterDTOs[0].name);
        assertEquals(contextServiceId, defaultContextDTO.filterDTOs[0].servletContextId);
        assertEquals(1, defaultContextDTO.errorPageDTOs.length);
        assertEquals(Exception.class.getName(), defaultContextDTO.errorPageDTOs[0].exceptions[0]);
        assertEquals(contextServiceId, defaultContextDTO.errorPageDTOs[0].servletContextId);
        assertEquals(1, defaultContextDTO.listenerDTOs.length);
        assertEquals(ServletRequestListener.class.getName(), defaultContextDTO.listenerDTOs[0].types[0]);
        assertEquals(contextServiceId, defaultContextDTO.listenerDTOs[0].servletContextId);

        ServletContextDTO testContextDTO = runtimeDTO.servletContextDTOs[1];
        contextServiceId = testContextDTO.serviceId;

        assertEquals(1, testContextDTO.servletDTOs.length);
        assertEquals("context servlet", testContextDTO.servletDTOs[0].name);
        assertEquals(contextServiceId, testContextDTO.servletDTOs[0].servletContextId);
        assertEquals(1, testContextDTO.filterDTOs.length);
        assertEquals("context filter", testContextDTO.filterDTOs[0].name);
        assertEquals(contextServiceId, testContextDTO.filterDTOs[0].servletContextId);
        assertEquals(1, testContextDTO.errorPageDTOs.length);
        assertEquals(500L, testContextDTO.errorPageDTOs[0].errorCodes[0]);
        assertEquals(contextServiceId, testContextDTO.errorPageDTOs[0].servletContextId);
        assertEquals(1, testContextDTO.listenerDTOs.length);
        assertEquals(ServletRequestListener.class.getName(), testContextDTO.listenerDTOs[0].types[0]);
        assertEquals(contextServiceId, testContextDTO.listenerDTOs[0].servletContextId);
    }

    @Test
    public void exceptionInServletInitAppearsAsFailure() throws ServletException, InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet",
                HTTP_WHITEBOARD_SERVLET_NAME, "servlet");

        CountDownLatch initLatch = new CountDownLatch(1);

        @SuppressWarnings("serial")
        Servlet failingServlet = new TestServlet(initLatch, null) {
            @Override
            public void init() throws ServletException
            {
                super.init();
                throw new ServletException();
            }
        };

        registrations.add(m_context.registerService(Servlet.class.getName(), failingServlet, properties));
        awaitServiceRegistration(initLatch);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.failedServletDTOs.length);
        assertEquals("servlet", runtimeDTO.failedServletDTOs[0].name);
        assertEquals(FAILURE_REASON_EXCEPTION_ON_INIT, runtimeDTO.failedServletDTOs[0].failureReason);
    }

    @Test
    public void exceptionInServletInitDuringServletRemovalAppearsAsFailure() throws ServletException, InterruptedException
    {
        Dictionary<String, ?> properties1 = createDictionary(
            HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet1",
            HTTP_WHITEBOARD_SERVLET_NAME, "servlet1");

        final CountDownLatch initLatch1 = new CountDownLatch(1);

        @SuppressWarnings("serial")
        Servlet failingServlet1 = new TestServlet(initLatch1, null) {
            @Override
            public void init() throws ServletException
            {
                //fail when initialized the second time
                if (initLatch1.getCount() == 0)
                {
                    throw new ServletException();
                }
                super.init();
            }
        };

        Dictionary<String, ?> properties2 = createDictionary(
            HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet2",
            HTTP_WHITEBOARD_SERVLET_NAME, "servlet2");

        final CountDownLatch initLatch2 = new CountDownLatch(1);
        @SuppressWarnings("serial")
        Servlet failingServlet2 = new TestServlet(initLatch2, null) {
            @Override
            public void init() throws ServletException
            {
                //fail when initialized the second time
                if (initLatch2.getCount() == 0)
                {
                    throw new ServletException();
                }
                super.init();
            }
        };

        Dictionary<String, ?> propertiesShadowing = createDictionary(
            HTTP_WHITEBOARD_SERVLET_PATTERN, asList("/servlet1", "/servlet2"),
            HTTP_WHITEBOARD_SERVLET_NAME, "servletShadowing",
            SERVICE_RANKING, Integer.MAX_VALUE);

        CountDownLatch initLatchShadowing = new CountDownLatch(1);
        Servlet servletShadowing = new TestServlet(initLatchShadowing, null);

        registrations.add(m_context.registerService(Servlet.class.getName(), failingServlet1, properties1));
        registrations.add(m_context.registerService(Servlet.class.getName(), failingServlet2, properties2));
        awaitServiceRegistration(initLatch1);
        awaitServiceRegistration(initLatch2);

        ServiceRegistration<?> shadowingRegistration = m_context.registerService(Servlet.class.getName(), servletShadowing, propertiesShadowing);
        registrations.add(shadowingRegistration);
        awaitServiceRegistration(initLatchShadowing);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(2, runtimeDTO.failedServletDTOs.length);
        assertEquals(FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, runtimeDTO.failedServletDTOs[0].failureReason);
        assertEquals(FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, runtimeDTO.failedServletDTOs[1].failureReason);

        shadowingRegistration.unregister();

        runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(2, runtimeDTO.failedServletDTOs.length);
        assertEquals(FAILURE_REASON_EXCEPTION_ON_INIT, runtimeDTO.failedServletDTOs[0].failureReason);
        assertEquals(FAILURE_REASON_EXCEPTION_ON_INIT, runtimeDTO.failedServletDTOs[1].failureReason);
    }

    @Test
    public void exceptionInFilterInitAppearsAsFailure() throws ServletException, InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_FILTER_PATTERN, "/filter",
                HTTP_WHITEBOARD_FILTER_NAME, "filter");

        CountDownLatch initLatch = new CountDownLatch(1);

        Filter failingFilter = new TestFilter(initLatch, null) {
            @Override
            public void init(FilterConfig config) throws ServletException
            {
                super.init(config);
                throw new ServletException();
            }
        };

        registrations.add(m_context.registerService(Filter.class.getName(), failingFilter, properties));
        awaitServiceRegistration(initLatch);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.failedFilterDTOs.length);
        assertEquals("filter", runtimeDTO.failedFilterDTOs[0].name);
        assertEquals(FAILURE_REASON_EXCEPTION_ON_INIT, runtimeDTO.failedFilterDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1 (TODO : exact version)
    @Test
    public void hiddenDefaultContextAppearsAsFailure() throws InterruptedException
    {
        registerContext("default", "");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals("default", runtimeDTO.failedServletContextDTOs[0].name);
        assertDefaultContext(runtimeDTO);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1
    @Test
    public void contextHelperWithDuplicateNameAppearsAsFailure() throws InterruptedException
    {
        ServiceRegistration<?> firstContextReg = registerContext("contextA", "/first");
        registerContext("contextA", "/second");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals("contextA", runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals("/second", runtimeDTO.failedServletContextDTOs[0].contextPath);
        assertEquals(FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, runtimeDTO.failedServletContextDTOs[0].failureReason);

        assertEquals(3, runtimeDTO.servletContextDTOs.length);
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTO.servletContextDTOs[0].name);
        assertEquals("default", runtimeDTO.servletContextDTOs[2].name);

        assertEquals("contextA", runtimeDTO.servletContextDTOs[1].name);
        assertEquals("/first", runtimeDTO.servletContextDTOs[1].contextPath);

        firstContextReg.unregister();

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);

        assertEquals(3, runtimeDTO.servletContextDTOs.length);
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTO.servletContextDTOs[0].name);
        assertEquals("default", runtimeDTO.servletContextDTOs[2].name);

        assertEquals("contextA", runtimeDTO.servletContextDTOs[1].name);
        assertEquals("/second", runtimeDTO.servletContextDTOs[1].contextPath);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1
    @Test
    public void missingContextHelperNameAppearsAsFailure()
    {
        Dictionary<String, ?> properties = createDictionary(HTTP_WHITEBOARD_CONTEXT_PATH, "");

        registrations.add(m_context.registerService(ServletContextHelper.class.getName(), mock(ServletContextHelper.class), properties));

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(null, runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedServletContextDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1
    @Test
    public void invalidContextHelperNameAppearsAsFailure() throws InterruptedException
    {
        registerContext("context A", "");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals("context A", runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedServletContextDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.1
    @Test
    public void invalidContextHelperPathAppearsAsFailure() throws InterruptedException
    {
        registerContext("contextA", "#");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals("#", runtimeDTO.failedServletContextDTOs[0].contextPath);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedServletContextDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.3
    @Test
    public void selectionOfNonExistingContextHelperAppearsAsFailure() throws InterruptedException
    {
        registerServlet("servlet 1", "/", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=contextA)", null);
        awaitServiceRegistration();

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletDTOs.length);
        assertEquals("servlet 1", runtimeDTO.failedServletDTOs[0].name);
        assertEquals(FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING, runtimeDTO.failedServletDTOs[0].failureReason);

        registerContext("contextA", "/contextA");

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        assertEquals(3, runtimeDTO.servletContextDTOs.length);
        assertEquals("contextA", runtimeDTO.servletContextDTOs[1].name);
        assertEquals(1, runtimeDTO.servletContextDTOs[1].servletDTOs.length);
        assertEquals("servlet 1", runtimeDTO.servletContextDTOs[1].servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.3
    @Test
    public void differentTargetIsIgnored() throws InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet",
                HTTP_WHITEBOARD_SERVLET_NAME, "servlet",
                HTTP_WHITEBOARD_TARGET, "(org.osgi.service.http.port=8282)");

        registrations.add(m_context.registerService(Servlet.class.getName(), new TestServlet(), properties));
        awaitServiceRegistration();

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);

        ServletContextDTO defaultContext = assertDefaultContext(runtimeDTO);
        assertEquals(0, defaultContext.servletDTOs.length);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4
    @Test
    public void servletWithoutNameGetsFullyQualifiedName() throws InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet");

        CountDownLatch initLatch = new CountDownLatch(1);
        registrations.add(m_context.registerService(Servlet.class.getName(), new TestServlet(initLatch, null), properties));
        awaitServiceRegistration(initLatch);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);

        ServletContextDTO defaultContext = assertDefaultContext(serviceRuntime.getRuntimeDTO());
        assertEquals(1, defaultContext.servletDTOs.length);
        assertEquals(TestServlet.class.getName(), defaultContext.servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4.1
    @Test
    public void patternAndErrorPageSpecified() throws InterruptedException
    {
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet",
                HTTP_WHITEBOARD_SERVLET_NAME, "servlet",
                HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, asList("400"));

        registrations.add(m_context.registerService(Servlet.class.getName(), new TestServlet(), properties));
        awaitServiceRegistration();

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();
        ServletContextDTO defaultContext = assertDefaultContext(runtimeDTO);

        assertEquals(0, runtimeDTO.failedErrorPageDTOs.length);
        assertEquals(0, runtimeDTO.failedServletDTOs.length);

        assertEquals(1, defaultContext.servletDTOs.length);
        assertEquals(1, defaultContext.errorPageDTOs.length);

        assertEquals("servlet", defaultContext.servletDTOs[0].name);
        assertEquals("servlet", defaultContext.errorPageDTOs[0].name);

        assertArrayEquals(new String[] { "/servlet" }, defaultContext.servletDTOs[0].patterns);
        assertArrayEquals(new long[] { 400 }, defaultContext.errorPageDTOs[0].errorCodes);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4.1
    @Test
    public void multipleServletsForSamePatternChoosenByServiceRankingRules() throws InterruptedException
    {
        registerServlet("servlet 1", "/pathcollision");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        ServletContextDTO defaultContext = assertDefaultContext(runtimeDTO);
        assertEquals(1, defaultContext.servletDTOs.length);

        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_SERVLET_PATTERN, "/pathcollision",
                HTTP_WHITEBOARD_SERVLET_NAME, "servlet 2",
                SERVICE_RANKING, Integer.MAX_VALUE);

        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);
        TestServlet testServlet = new TestServlet(initLatch, destroyLatch);
        ServiceRegistration<?> higherRankingServlet = m_context.registerService(Servlet.class.getName(), testServlet, properties);
        registrations.add(higherRankingServlet);

        RuntimeDTO runtimeWithShadowedServlet = serviceRuntime.getRuntimeDTO();
        awaitServiceRegistration(initLatch);

        defaultContext = assertDefaultContext(runtimeWithShadowedServlet);
        assertEquals(1, defaultContext.servletDTOs.length);

        assertEquals(1, runtimeWithShadowedServlet.failedServletDTOs.length);
        FailedServletDTO failedServletDTO = runtimeWithShadowedServlet.failedServletDTOs[0];
        assertEquals("servlet 1", failedServletDTO.name);
        assertEquals(FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, failedServletDTO.failureReason);

        higherRankingServlet.unregister();
        awaitServiceRegistration(destroyLatch);

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        defaultContext = assertDefaultContext(runtimeDTO);
        assertEquals(1, defaultContext.servletDTOs.length);
        assertEquals("servlet 1", defaultContext.servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4.1
    @Test
    public void multipleErrorPagesForSameErrorCodeChoosenByServiceRankingRules() throws InterruptedException
    {
        registerErrorPage("error page 1", asList(NullPointerException.class.getName(), "500"));

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        ServletContextDTO defaultContext = assertDefaultContext(runtimeDTO);

        assertEquals(0, runtimeDTO.failedErrorPageDTOs.length);
        assertEquals(1, defaultContext.errorPageDTOs.length);

        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, asList("500", IllegalArgumentException.class.getName()),
                HTTP_WHITEBOARD_SERVLET_NAME, "error page 2",
                SERVICE_RANKING, Integer.MAX_VALUE);

        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);
        TestServlet testServlet = new TestServlet(initLatch, destroyLatch);
        ServiceRegistration<?> higherRankingServlet = m_context.registerService(Servlet.class.getName(), testServlet, properties);
        registrations.add(higherRankingServlet);
        awaitServiceRegistration(initLatch);

        RuntimeDTO runtimeWithShadowedErrorPage = serviceRuntime.getRuntimeDTO();

        defaultContext = assertDefaultContext(runtimeWithShadowedErrorPage);

        assertEquals(2, defaultContext.errorPageDTOs.length);
        assertEquals("error page 2", defaultContext.errorPageDTOs[0].name);
        assertArrayEquals(new long[] { 500 }, defaultContext.errorPageDTOs[0].errorCodes);
        assertArrayEquals(new String[] { IllegalArgumentException.class.getName() }, defaultContext.errorPageDTOs[0].exceptions);
        assertEquals("error page 1", defaultContext.errorPageDTOs[1].name);
        assertEquals(0, defaultContext.errorPageDTOs[1].errorCodes.length);
        assertArrayEquals(new String[] { NullPointerException.class.getName() }, defaultContext.errorPageDTOs[1].exceptions);

        assertEquals(1, runtimeWithShadowedErrorPage.failedErrorPageDTOs.length);
        FailedErrorPageDTO failedErrorPageDTO = runtimeWithShadowedErrorPage.failedErrorPageDTOs[0];
        assertEquals("error page 1", failedErrorPageDTO.name);
        assertArrayEquals(new long[] { 500 }, failedErrorPageDTO.errorCodes);
        assertEquals(0, failedErrorPageDTO.exceptions.length);
        assertEquals(FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, failedErrorPageDTO.failureReason);

        higherRankingServlet.unregister();
        awaitServiceRegistration(destroyLatch);

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        defaultContext = assertDefaultContext(runtimeDTO);

        assertEquals(0, runtimeDTO.failedErrorPageDTOs.length);
        assertEquals(1, defaultContext.errorPageDTOs.length);
        assertEquals("error page 1", defaultContext.errorPageDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.4
    @Test
    public void mulitpleServletsWithSamePatternHttpServiceRegistrationWins() throws Exception
    {
        registerServlet("servlet 1", "/pathcollision");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        ServletContextDTO defaultContext = assertDefaultContext(runtimeDTO);
        assertEquals(1, defaultContext.servletDTOs.length);

        CountDownLatch initLatch = new CountDownLatch(1);
        CountDownLatch destroyLatch = new CountDownLatch(1);
        TestServlet testServlet = new TestServlet(initLatch, destroyLatch);
        register("/pathcollision", testServlet);

        RuntimeDTO runtimeWithShadowedServlet = serviceRuntime.getRuntimeDTO();
        awaitServiceRegistration(initLatch);

        defaultContext = assertDefaultContext(runtimeWithShadowedServlet);
        ServletContextDTO httpServiceContext = runtimeWithShadowedServlet.servletContextDTOs[0];
        assertEquals(HTTP_CONTEXT_NAME, httpServiceContext.name);
        assertEquals(1, httpServiceContext.servletDTOs.length);
        assertArrayEquals(new String[] {"/pathcollision"}, httpServiceContext.servletDTOs[0].patterns);

        assertEquals(1, defaultContext.servletDTOs.length);
        ServletDTO servletDTO = defaultContext.servletDTOs[0];
        assertEquals("servlet 1", servletDTO.name);

        // check request info DTO to see which servlet responds
        final RequestInfoDTO infoDTO = serviceRuntime.calculateRequestInfoDTO("/pathcollision");
        assertEquals(httpServiceContext.serviceId, infoDTO.servletDTO.servletContextId);

        unregister(testServlet);
        awaitServiceRegistration(destroyLatch);

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        defaultContext = assertDefaultContext(runtimeDTO);
        assertEquals(1, defaultContext.servletDTOs.length);
        assertEquals("servlet 1", defaultContext.servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.7
    @Test
    public void invalidListenerPopertyValueAppearsAsFailure() throws Exception
    {
        Dictionary<String, ?> properties = createDictionary(HTTP_WHITEBOARD_LISTENER, "invalid");

        registrations.add(m_context.registerService(ServletRequestListener.class.getName(), mock(ServletRequestListener.class), properties));

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedListenerDTOs.length);
        assertEquals(FAILURE_REASON_VALIDATION_FAILED, runtimeDTO.failedListenerDTOs[0].failureReason);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.8
    @Test
    public void contextReplacedWithHigherRankingContext() throws Exception
    {
        ServiceRegistration<?> firstContext = registerContext("test-context", "/first");
        Long firstContextId = (Long) firstContext.getReference().getProperty(Constants.SERVICE_ID);

        CountDownLatch initLatch = new CountDownLatch(1);
        registerServlet("servlet", "/servlet", "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=test-context)", initLatch);
        awaitServiceRegistration(initLatch);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(3, runtimeDTO.servletContextDTOs.length);
        assertEquals(firstContextId.longValue(), runtimeDTO.servletContextDTOs[1].serviceId);
        assertEquals("test-context", runtimeDTO.servletContextDTOs[1].name);
        assertEquals("/first", runtimeDTO.servletContextDTOs[1].contextPath);
        assertEquals("default", runtimeDTO.servletContextDTOs[2].name);
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTO.servletContextDTOs[0].name);

        assertEquals(1, runtimeDTO.servletContextDTOs[1].servletDTOs.length);
        assertEquals("servlet", runtimeDTO.servletContextDTOs[1].servletDTOs[0].name);

        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_CONTEXT_NAME, "test-context",
                HTTP_WHITEBOARD_CONTEXT_PATH, "/second",
                SERVICE_RANKING, Integer.MAX_VALUE);

        ServiceRegistration<?> secondContext = m_context.registerService(ServletContextHelper.class.getName(), mock(ServletContextHelper.class), properties);
        registrations.add(secondContext);
        Long secondContextId = (Long) secondContext.getReference().getProperty(Constants.SERVICE_ID);

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(1, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(firstContextId.longValue(), runtimeDTO.failedServletContextDTOs[0].serviceId);
        assertEquals("test-context", runtimeDTO.failedServletContextDTOs[0].name);
        assertEquals("/first", runtimeDTO.failedServletContextDTOs[0].contextPath);

        assertEquals(3, runtimeDTO.servletContextDTOs.length);

        final List<String> names = new ArrayList<String>();
        for(final ServletContextDTO dto : runtimeDTO.servletContextDTOs)
        {
            names.add(dto.name);
        }
        final int httpContextIndex = names.indexOf(HTTP_CONTEXT_NAME);
        final int secondContextIndex = names.indexOf("test-context");
        final int defaultContextIndex = names.indexOf("default");
        assertEquals(secondContextId.longValue(), runtimeDTO.servletContextDTOs[secondContextIndex].serviceId);
        assertEquals("test-context", runtimeDTO.servletContextDTOs[secondContextIndex].name);
        assertEquals("/second", runtimeDTO.servletContextDTOs[secondContextIndex].contextPath);
        assertEquals("default", runtimeDTO.servletContextDTOs[defaultContextIndex].name);
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTO.servletContextDTOs[httpContextIndex].name);

        assertEquals(1, runtimeDTO.servletContextDTOs[secondContextIndex].servletDTOs.length);
        assertEquals("servlet", runtimeDTO.servletContextDTOs[secondContextIndex].servletDTOs[0].name);

        secondContext.unregister();

        runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        assertEquals(3, runtimeDTO.servletContextDTOs.length);
        assertEquals(firstContextId.longValue(), runtimeDTO.servletContextDTOs[1].serviceId);
        assertEquals("test-context", runtimeDTO.servletContextDTOs[1].name);
        assertEquals("/first", runtimeDTO.servletContextDTOs[1].contextPath);
        assertEquals("default", runtimeDTO.servletContextDTOs[2].name);
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTO.servletContextDTOs[0].name);

        assertEquals(1, runtimeDTO.servletContextDTOs[1].servletDTOs.length);
        assertEquals("servlet", runtimeDTO.servletContextDTOs[1].servletDTOs[0].name);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.9
    @Test
    public void httpServiceIdIsSet()
    {
        ServiceReference<?> httpServiceRef = m_context.getServiceReference(HttpService.class.getName());
        ServiceReference<?> httpServiceRuntimeRef = m_context.getServiceReference(HttpServiceRuntime.class.getName());

        Long expectedId = (Long) httpServiceRef.getProperty(Constants.SERVICE_ID);
        Collection col = (Collection)httpServiceRuntimeRef.getProperty(HTTP_SERVICE_ID);
        Long actualId = (Long) col.iterator().next();

        assertEquals(expectedId, actualId);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.9
    @Test
    public void serviceRegisteredWithHttpServiceHasNegativeServiceId() throws Exception
    {
        CountDownLatch initLatch = new CountDownLatch(1);
        register("/test", new TestServlet(initLatch, null));
        awaitServiceRegistration(initLatch);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(2, runtimeDTO.servletContextDTOs.length);
        assertEquals(1, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertTrue(0 > runtimeDTO.servletContextDTOs[0].servletDTOs[0].serviceId);
    }

    // As specified in OSGi Compendium Release 6, Chapter 140.9
    @Test
    public void serviceWithoutRequiredPropertiesIsIgnored() throws InterruptedException
    {
        // Neither pattern nor error page specified
        Dictionary<String, ?> properties = createDictionary(HTTP_WHITEBOARD_SERVLET_NAME, "servlet");

        registrations.add(m_context.registerService(Servlet.class.getName(), new TestServlet(), properties));
        awaitServiceRegistration();

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
        ServletContextDTO defaultContext = assertDefaultContext(runtimeDTO);
        assertEquals(0, defaultContext.servletDTOs.length);
    }

    @Test
    public void dtosAreIndependentCopies() throws Exception
    {
        //register first servlet
        Dictionary<String, ?> properties = createDictionary(
                HTTP_WHITEBOARD_SERVLET_PATTERN, "/test",
                HTTP_WHITEBOARD_SERVLET_NAME, "servlet 1",
                HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + "test", "testValue");

        CountDownLatch initLatch = new CountDownLatch(1);
        registrations.add(m_context.registerService(Servlet.class.getName(), new TestServlet(initLatch, null), properties));
        awaitServiceRegistration(initLatch);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTOWithFirstSerlvet = serviceRuntime.getRuntimeDTO();

        //register second servlet
        registerServlet("testServlet 2", "/servlet_2");

        RuntimeDTO runtimeDTOWithTwoSerlvets = serviceRuntime.getRuntimeDTO();

        assertNotSame(runtimeDTOWithFirstSerlvet, runtimeDTOWithTwoSerlvets);

        ServletContextDTO defaultContextFirstServlet = assertDefaultContext(runtimeDTOWithFirstSerlvet);
        ServletContextDTO defaultContextTwoServlets = assertDefaultContext(runtimeDTOWithTwoSerlvets);
        assertNotSame(defaultContextFirstServlet.servletDTOs[0].patterns,
                defaultContextTwoServlets.servletDTOs[0].patterns);

        boolean mapsModifiable = true;
        try
        {
            defaultContextTwoServlets.servletDTOs[0].initParams.clear();
        } catch (UnsupportedOperationException e)
        {
            mapsModifiable = false;
        }

        if (mapsModifiable)
        {
            assertNotSame(defaultContextFirstServlet.servletDTOs[0].initParams,
                    defaultContextTwoServlets.servletDTOs[0].initParams);
        }
    }

    @Test
    public void requestInfoDTO() throws Exception
    {
        registerServlet("servlet", "/default");
        registerFilter("filter1", "/default");
        registerFilter("filter2", "/default");

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        ServletContextDTO defaultContext = assertDefaultContext(serviceRuntime.getRuntimeDTO());
        long defaultContextId = defaultContext.serviceId;

        RequestInfoDTO requestInfoDTO = serviceRuntime.calculateRequestInfoDTO("/default");
        assertEquals("/default", requestInfoDTO.path);
        assertEquals(defaultContextId, requestInfoDTO.servletContextId);
        assertEquals("servlet", requestInfoDTO.servletDTO.name);
        assertEquals(2, requestInfoDTO.filterDTOs.length);
        assertEquals("filter1", requestInfoDTO.filterDTOs[0].name);
        assertEquals("filter2", requestInfoDTO.filterDTOs[1].name);
    }

    @Test
    public void serviceEndpointPropertyIsSet()
    {
        // if there is more than one network interface, there might be more than one endpoint!
        final String[] endpoint = (String[]) m_context.getServiceReference(HttpServiceRuntime.class).getProperty(HTTP_SERVICE_ENDPOINT);
        assertNotNull(endpoint);
        assertTrue(Arrays.toString(endpoint), endpoint.length > 0);
        assertTrue(endpoint[0].startsWith("http://"));
        assertTrue(endpoint[0].endsWith(":8080/"));
    }

    /**
     * Test for FELIX-5319
     * @throws Exception
     */
    @Test
    public void testCombinedServletAndResourceRegistration() throws Exception
    {
        // register single component as Servlet and Resource
        final String servletPath = "/hello/sayHello";
        final String servletName = "Hello World";
        final String rsrcPattern = "/hello/static/*";
        final String rsrcPrefix = "/static";

        CountDownLatch initLatch = new CountDownLatch(1);
        List<Object> propertyEntries = Arrays.<Object>asList(
                HTTP_WHITEBOARD_SERVLET_PATTERN, servletPath,
                HTTP_WHITEBOARD_SERVLET_NAME, servletName,
                HTTP_WHITEBOARD_RESOURCE_PATTERN, rsrcPattern,
                HTTP_WHITEBOARD_RESOURCE_PREFIX, rsrcPrefix);

        Dictionary<String, ?> properties = createDictionary(propertyEntries.toArray());

        registrations.add(m_context.registerService(Servlet.class.getName(), new TestServlet(initLatch, null), properties));
        awaitServiceRegistration(initLatch);

        HttpServiceRuntime serviceRuntime = (HttpServiceRuntime) getService(HttpServiceRuntime.class.getName());
        assertNotNull("HttpServiceRuntime unavailable", serviceRuntime);

        RuntimeDTO runtimeDTO = serviceRuntime.getRuntimeDTO();

        assertEquals(0, runtimeDTO.failedServletDTOs.length);
        assertEquals(0, runtimeDTO.failedResourceDTOs.length);

        // check servlet registration
        ServletContextDTO contextDTO = assertDefaultContext(runtimeDTO);
        assertEquals(1, contextDTO.servletDTOs.length);
        assertEquals(servletName, contextDTO.servletDTOs[0].name);
        assertEquals(1, contextDTO.servletDTOs[0].patterns.length);
        assertEquals(servletPath, contextDTO.servletDTOs[0].patterns[0]);

        // check resource registration
        assertEquals(1, contextDTO.resourceDTOs.length);
        assertEquals(1, contextDTO.resourceDTOs[0].patterns.length);
        assertEquals(rsrcPattern, contextDTO.resourceDTOs[0].patterns[0]);
        assertEquals(rsrcPrefix, contextDTO.resourceDTOs[0].prefix);
    }

    private ServletContextDTO assertDefaultContext(RuntimeDTO runtimeDTO)
    {
        assertTrue(1 < runtimeDTO.servletContextDTOs.length);
        assertEquals(HTTP_CONTEXT_NAME, runtimeDTO.servletContextDTOs[0].name);
        assertEquals("default", runtimeDTO.servletContextDTOs[1].name);
        return runtimeDTO.servletContextDTOs[1];
    }

    private void awaitServiceRegistration() throws InterruptedException
    {
        // Wait some time until the whiteboard (hopefully) picked up the service
        Thread.sleep(DEFAULT_SLEEP);
    }

    private void awaitServiceRegistration(CountDownLatch initLatch) throws InterruptedException
    {
        if (!initLatch.await(5, TimeUnit.SECONDS))
        {
            fail("Service was not initialized in time!");
        };
        awaitServiceRegistration();
    }

    public static class TestResource
    {
        // Tagging class
    }
}
