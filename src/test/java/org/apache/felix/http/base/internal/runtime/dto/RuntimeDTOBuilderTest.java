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
package org.apache.felix.http.base.internal.runtime.dto;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.sort;
import static java.util.Collections.emptyMap;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.ID_COUNTER;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createContextInfo;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createErrorPage;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createErrorPageWithServiceId;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createFilterInfo;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createServletInfo;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createTestFilter;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createTestFilterWithServiceId;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createTestServlet;
import static org.apache.felix.http.base.internal.runtime.WhiteboardServiceHelper.createTestServletWithServiceId;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.HandlerRuntime;
import org.apache.felix.http.base.internal.runtime.HandlerRuntime.ErrorPage;
import org.apache.felix.http.base.internal.runtime.RegistryRuntime;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.whiteboard.ContextHandler;
import org.apache.felix.http.base.internal.whiteboard.ListenerRegistry;
import org.apache.felix.http.base.internal.whiteboard.PerContextEventListener;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

@RunWith(MockitoJUnitRunner.class)
public class RuntimeDTOBuilderTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final Long ID_0 = -ID_COUNTER.incrementAndGet();
    private static final Long ID_A = ID_COUNTER.incrementAndGet();
    private static final Long ID_B = ID_COUNTER.incrementAndGet();

    private static final Long ID_LISTENER_1 = ID_COUNTER.incrementAndGet();
    private static final Long ID_LISTENER_2 = ID_COUNTER.incrementAndGet();

    private static final List<String> CONTEXT_NAMES = Arrays.asList("0", "A", "B");

    @SuppressWarnings("serial")
    private static final Map<String, List<String>> CONTEXT_ENTITY_NAMES = new HashMap<String, List<String>>()
            {
                {
                    put("0", Arrays.asList("1"));
                    put("A", Arrays.asList("A_1"));
                    put("B", Arrays.asList("B_1", "B_2"));
                }
            };

    @Mock private Bundle bundle;

    @Mock private DTO testDTO;

    @Mock private ExtServletContext context_0;
    @Mock private ExtServletContext context_A;
    @Mock private ExtServletContext context_B;

    @Mock private ServiceReference<?> listener_1;
    @Mock private ServiceReference<?> listener_2;

    private RegistryRuntime registry;
    private Map<String, Object> runtimeAttributes;
	private ListenerRegistry listenerRegistry;

    @Before
    public void setUp()
    {
        registry = null;
        runtimeAttributes = Collections.emptyMap();
        listenerRegistry = new ListenerRegistry(bundle);
    }

	public ContextHandler setupContext(ServletContext context, String name, long serviceId)
    {
        when(context.getServletContextName()).thenReturn(name);

        String path = "/" + name;
        when(context.getContextPath()).thenReturn(path);

        List<String> initParameterNames = asList("param_1", "param_2");
        when(context.getInitParameterNames()).thenReturn(Collections.enumeration(initParameterNames));
        when(context.getInitParameter("param_1")).thenReturn("init_val_1");
        when(context.getInitParameter("param_2")).thenReturn("init_val_2");

        Map<String, String> initParemters = new HashMap<String, String>();
        initParemters.put("param_1", "init_val_1");
        initParemters.put("param_2", "init_val_2");
        ServletContextHelperInfo contextInfo = createContextInfo(0, serviceId, name, path, initParemters);

        PerContextEventListener eventListener = listenerRegistry.addContext(contextInfo);
        ContextHandler contextHandler = new ContextHandler(contextInfo, context, eventListener, bundle);

        ServletContext sharedContext = contextHandler.getSharedContext();
        sharedContext.setAttribute("intAttr", 1);
        sharedContext.setAttribute("dateAttr", new Date());
        sharedContext.setAttribute("stringAttr", "one");
        sharedContext.setAttribute("dtoAttr", testDTO);

        return contextHandler;
    }

    @SuppressWarnings("unchecked")
    public Map<Long, Collection<ServiceReference<?>>> setupListeners()
    {
        Map<Long, Collection<ServiceReference<?>>> listenerRuntimes = new HashMap<Long, Collection<ServiceReference<?>>>();
        listenerRuntimes.put(ID_0, asList(listener_1, listener_2));
        listenerRuntimes.put(ID_A, Arrays.<ServiceReference<?>> asList(listener_1));
        listenerRuntimes.put(ID_B, asList(listener_1, listener_2));

        when(listener_1.getProperty(Constants.SERVICE_ID)).thenReturn(ID_LISTENER_1);
        when(listener_1.getProperty(Constants.OBJECTCLASS))
                .thenReturn(new String[] { "org.test.interface_1" });

        when(listener_2.getProperty(Constants.SERVICE_ID)).thenReturn(ID_LISTENER_2);
        when(listener_2.getProperty(Constants.OBJECTCLASS))
                .thenReturn(new String[] { "org.test.interface_1", "org.test.interface_2" });

        return listenerRuntimes;
    }

    public void setupRegistry(List<ContextHandler> contexts,
            List<HandlerRuntime> contextRuntimes,
            Map<Long, Collection<ServiceReference<?>>> listenerRuntimes)
    {
        registry = new RegistryRuntime(contexts, contextRuntimes, listenerRuntimes);
    }

    @Test
    public void buildRuntimeDTO()
    {
        ContextHandler contextHelper_0 = setupContext(context_0, "0", ID_0);
        ContextHandler contextHelper_A = setupContext(context_A, "A", ID_A);
        ContextHandler contextHelper_B = setupContext(context_B, "B", ID_B);

        List<ServletHandler> servlets_0 = asList(createTestServlet("1", context_0));
        List<FilterHandler> filters_0 = asList(createTestFilter("1", context_0));
        List<ErrorPage> errorPages_0 = asList(createErrorPage("E_1", context_0));
        HandlerRuntime contextRuntime_0 = new HandlerRuntime(servlets_0, filters_0, errorPages_0, ID_0);

        List<ServletHandler> servlets_A = asList(createTestServlet("A_1", context_A));
        List<FilterHandler> filters_A = asList(createTestFilter("A_1", context_A));
        List<ErrorPage> errorPages_A = asList(createErrorPage("E_A_1", context_A));
        HandlerRuntime contextRuntime_A = new HandlerRuntime(servlets_A, filters_A, errorPages_A, ID_A);

        List<ServletHandler> servlets_B = asList(createTestServletWithServiceId("B_1", context_B),
                createTestServletWithServiceId("B_2", context_B));
        List<FilterHandler> filters_B = asList(createTestFilterWithServiceId("B_1", context_B),
                createTestFilterWithServiceId("B_2", context_B));
        List<ErrorPage> errorPages_B = asList(createErrorPageWithServiceId("E_B_1", context_B),
                createErrorPageWithServiceId("E_B_2", context_B));
        HandlerRuntime contextRuntime_B = new HandlerRuntime(servlets_B, filters_B, errorPages_B, ID_B);

        Map<Long, Collection<ServiceReference<?>>> listenerRuntimes = setupListeners();

        setupRegistry(asList(contextHelper_0, contextHelper_A, contextHelper_B),
                asList(contextRuntime_0, contextRuntime_A, contextRuntime_B),
                listenerRuntimes);

        RuntimeDTO runtimeDTO = new RuntimeDTOBuilder(registry, runtimeAttributes).build();

        assertServletContextDTOs(runtimeDTO);
    }

    private void assertServletContextDTOs(RuntimeDTO runtimeDTO)
    {
        SortedSet<Long> seenServiceIds = new TreeSet<Long>();

        assertEquals(3, runtimeDTO.servletContextDTOs.length);

        for (ServletContextDTO servletContextDTO : runtimeDTO.servletContextDTOs)
        {
            String contextName = servletContextDTO.name;
            assertTrue(CONTEXT_NAMES.contains(contextName));
            if (contextName.equals("0"))
            {
                assertTrue(contextName,
                        servletContextDTO.serviceId < 0);
                assertEquals(contextName,
                        1, servletContextDTO.servletDTOs.length);
                assertEquals(contextName,
                        1, servletContextDTO.filterDTOs.length);
                assertEquals(contextName,
                        1, servletContextDTO.errorPageDTOs.length);
                assertEquals(contextName,
                        2, servletContextDTO.listenerDTOs.length);
            }
            else
            {
                int expectedId = CONTEXT_NAMES.indexOf(contextName) + 1;
                assertEquals(contextName,
                        expectedId, servletContextDTO.serviceId);

                int expectedChildren = CONTEXT_NAMES.indexOf(contextName);
                assertEquals(contextName,
                        expectedChildren, servletContextDTO.servletDTOs.length);
                assertEquals(contextName,
                        expectedChildren, servletContextDTO.filterDTOs.length);
                assertEquals(contextName,
                        expectedChildren, servletContextDTO.errorPageDTOs.length);
                assertEquals(contextName,
                        expectedChildren, servletContextDTO.listenerDTOs.length);
            }
            seenServiceIds.add(servletContextDTO.serviceId);

            assertEquals(contextName,
                    3, servletContextDTO.attributes.size());
            assertEquals(contextName,
                    1, servletContextDTO.attributes.get("intAttr"));
            assertEquals(contextName,
                    "one", servletContextDTO.attributes.get("stringAttr"));
            assertEquals(contextName,
                    testDTO, servletContextDTO.attributes.get("dtoAttr"));

            assertEquals(contextName,
                    2, servletContextDTO.initParams.size());
            assertEquals(contextName,
                    "init_val_1", servletContextDTO.initParams.get("param_1"));
            assertEquals(contextName,
                    "init_val_2", servletContextDTO.initParams.get("param_2"));

            assertEquals(contextName,
                    "/" + contextName + "/" + contextName, servletContextDTO.contextPath);

            Collection<Long> serviceIds = assertServletDTOs(contextName,
                    servletContextDTO.serviceId, servletContextDTO.servletDTOs);
            seenServiceIds.addAll(serviceIds);

            serviceIds = assertFilterDTOs(contextName,
                    servletContextDTO.serviceId, servletContextDTO.filterDTOs);
            seenServiceIds.addAll(serviceIds);

            serviceIds = assertErrorPageDTOs(contextName,
                    servletContextDTO.serviceId, servletContextDTO.errorPageDTOs);
            seenServiceIds.addAll(serviceIds);

            serviceIds = assertListenerDTOs(contextName,
                    servletContextDTO.serviceId, servletContextDTO.listenerDTOs);
            seenServiceIds.addAll(serviceIds);
        }
        assertEquals(10, seenServiceIds.tailSet(0L).size());
        assertEquals(7, seenServiceIds.headSet(0L).size());
    }

    private Collection<Long> assertServletDTOs(String contextName, long contextId, ServletDTO[] dtos) {
        List<Long> serviceIds = new ArrayList<Long>();
        for (ServletDTO servletDTO : dtos)
        {
            String name = servletDTO.name;
            assertTrue(CONTEXT_ENTITY_NAMES.get(contextName).contains(name));

            if (contextId != ID_B)
            {
                assertTrue(name,
                        servletDTO.serviceId < 0);
            }
            else
            {
                assertTrue(name,
                        servletDTO.serviceId > 0);
            }
            serviceIds.add(servletDTO.serviceId);

            assertEquals(name,
                    contextId, servletDTO.servletContextId);

            assertTrue(name,
                    servletDTO.asyncSupported);

            assertEquals(name,
                    2, servletDTO.initParams.size());
            assertEquals(name,
                    "valOne_" + name, servletDTO.initParams.get("paramOne_" + name));
            assertEquals(name,
                    "valTwo_" + name, servletDTO.initParams.get("paramTwo_" + name));

            assertEquals(name,
                    1, servletDTO.patterns.length);
            assertEquals(name,
                    "/" + name, servletDTO.patterns[0]);

            assertEquals(name,
                    "info_" + name, servletDTO.servletInfo);
        }

        return serviceIds;
    }

    private Collection<Long> assertFilterDTOs(String contextName, long contextId, FilterDTO[] dtos) {
        List<Long> serviceIds = new ArrayList<Long>();
        for (FilterDTO filterDTO : dtos)
        {
            String name = filterDTO.name;
            assertTrue(CONTEXT_ENTITY_NAMES.get(contextName).contains(name));

            if (contextId != ID_B)
            {
                assertTrue(name,
                        filterDTO.serviceId < 0);
            }
            else
            {
                assertTrue(name,
                        filterDTO.serviceId > 0);
            }
            serviceIds.add(filterDTO.serviceId);

            assertEquals(name,
                    contextId, filterDTO.servletContextId);

            assertTrue(name,
                    filterDTO.asyncSupported);

            assertEquals(name,
                    2, filterDTO.initParams.size());
            assertEquals(name,
                    "valOne_" + name, filterDTO.initParams.get("paramOne_" + name));
            assertEquals(name,
                    "valTwo_" + name, filterDTO.initParams.get("paramTwo_" + name));

            assertEquals(name,
                    1, filterDTO.patterns.length);
            assertEquals(name,
                    "/" + name, filterDTO.patterns[0]);

            assertEquals(name,
                    1, filterDTO.regexs.length);
            assertEquals(name,
                    "." + name, filterDTO.regexs[0]);

            assertEquals(name,
                    2, filterDTO.dispatcher.length);
            assertEquals(name,
                    "ASYNC", filterDTO.dispatcher[0]);
            assertEquals(name,
                    "REQUEST", filterDTO.dispatcher[1]);
        }

        return serviceIds;
    }

    private Collection<Long> assertErrorPageDTOs(String contextName, long contextId, ErrorPageDTO[] dtos)
    {
        List<Long> serviceIds = new ArrayList<Long>();
        for (ErrorPageDTO  errorPageDTO : dtos)
        {
            String name = errorPageDTO.name;
            assertTrue(CONTEXT_ENTITY_NAMES.get(contextName).contains(name.substring(2)));

            if (contextId != ID_B)
            {
                assertTrue(name,
                        errorPageDTO.serviceId < 0);
            }
            else
            {
                assertTrue(name,
                        errorPageDTO.serviceId > 0);
            }
            serviceIds.add(errorPageDTO.serviceId);

            assertEquals(name,
                    contextId, errorPageDTO.servletContextId);

            assertTrue(name,
                    errorPageDTO.asyncSupported);

            assertEquals(name,
                    2, errorPageDTO.initParams.size());
            assertEquals(name,
                    "valOne_" + name, errorPageDTO.initParams.get("paramOne_" + name));
            assertEquals(name,
                    "valTwo_" + name, errorPageDTO.initParams.get("paramTwo_" + name));

            assertEquals(name,
                    "info_" + name, errorPageDTO.servletInfo);

            assertEquals(name,
                    2, errorPageDTO.errorCodes.length);
            long[] errorCodes = copyOf(errorPageDTO.errorCodes, 2);
            sort(errorCodes);
            assertEquals(name,
                    400, errorCodes[0]);
            assertEquals(name,
                    500, errorCodes[1]);

            assertEquals(name,
                    2, errorPageDTO.exceptions.length);
            String[] exceptions = copyOf(errorPageDTO.exceptions, 2);
            sort(exceptions);
            assertEquals(name,
                    "Bad request", exceptions[0]);
            assertEquals(name,
                    "Error", exceptions[1]);
        }

        return serviceIds;
    }

    private Collection<Long> assertListenerDTOs(String contextName, long contextId, ListenerDTO[] dtos)
    {
        Set<Long> serviceIds = new HashSet<Long>();
        for (ListenerDTO listenerDTO : dtos)
        {
            assertEquals(contextId, listenerDTO.servletContextId);
            serviceIds.add(listenerDTO.serviceId);
        }

        assertEquals(ID_LISTENER_1.longValue(), dtos[0].serviceId);
        assertArrayEquals(new String[] { "org.test.interface_1" },
                dtos[0].types);
        if (dtos.length > 1)
        {
            assertEquals(ID_LISTENER_2.longValue(), dtos[1].serviceId);
            assertArrayEquals(new String[] { "org.test.interface_1", "org.test.interface_2" }, dtos[1].types);
        }

        return serviceIds;
    }

    @Test
    public void nullValuesInEntities() {
        ContextHandler contextHandler = setupContext(context_0, "0", ID_0);

        ServletInfo servletInfo = createServletInfo(0,
                ID_COUNTER.incrementAndGet(),
                "1",
                new String[] { "/*" },
                null,
                true,
                Collections.<String, String>emptyMap());
        Servlet servlet = mock(Servlet.class);
        ServletHandler servletHandler = new ServletHandler(null, context_0, servletInfo, servlet);
        when(servlet.getServletInfo()).thenReturn("info_0");

        FilterInfo filterInfo = createFilterInfo(0,
                ID_COUNTER.incrementAndGet(),
                "1",
                null,
                null,
                null,
                true,
                null,
                Collections.<String, String>emptyMap());
        FilterHandler filterHandler = new FilterHandler(null, context_0, mock(Filter.class), filterInfo);

        HandlerRuntime contextRuntime = new HandlerRuntime(asList(servletHandler), asList(filterHandler), Collections.<ErrorPage>emptyList(), ID_0);
        setupRegistry(asList(contextHandler), asList(contextRuntime),
                Collections.<Long, Collection<ServiceReference<?>>>emptyMap());

        RuntimeDTO runtimeDTO = new RuntimeDTOBuilder(registry, runtimeAttributes).build();

        assertEquals(1, runtimeDTO.servletContextDTOs.length);
        assertEquals(1, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals(1, runtimeDTO.servletContextDTOs[0].filterDTOs.length);

        assertEquals(emptyMap(), runtimeDTO.servletContextDTOs[0].servletDTOs[0].initParams);

        assertEquals(emptyMap(), runtimeDTO.servletContextDTOs[0].filterDTOs[0].initParams);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].filterDTOs[0].patterns.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].filterDTOs[0].regexs.length);
    }

    @Test
    public void contextWithNoEntities() {
        ContextHandler contextHandler_0 = setupContext(context_0, "0", ID_0);
        ContextHandler contextHandler_A = setupContext(context_A, "A", ID_A);

        setupRegistry(asList(contextHandler_0, contextHandler_A),
                asList(HandlerRuntime.empty(ID_0), HandlerRuntime.empty(ID_A)),
                Collections.<Long, Collection<ServiceReference<?>>>emptyMap());

        RuntimeDTO runtimeDTO = new RuntimeDTOBuilder(registry, runtimeAttributes).build();

        assertEquals(2, runtimeDTO.servletContextDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].servletDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[0].filterDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[1].servletDTOs.length);
        assertEquals(0, runtimeDTO.servletContextDTOs[1].filterDTOs.length);
    }

    @Test
    public void missingPatternInServletThrowsException()
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("patterns");

        ContextHandler contextHandler = setupContext(context_0, "0", ID_0);

        ServletInfo servletInfo = createServletInfo(0,
                ID_COUNTER.incrementAndGet(),
                "1",
                null,
                null,
                true,
                Collections.<String, String>emptyMap());
        Servlet servlet = mock(Servlet.class);
        ServletHandler servletHandler = new ServletHandler(null, context_0, servletInfo, servlet);
        when(servlet.getServletInfo()).thenReturn("info_0");

        HandlerRuntime contextRuntime = new HandlerRuntime(asList(servletHandler),
                Collections.<FilterHandler>emptyList(),
                Collections.<ErrorPage>emptyList(),
                ID_0);
        setupRegistry(asList(contextHandler), asList(contextRuntime),
                Collections.<Long, Collection<ServiceReference<?>>> emptyMap());

        new RuntimeDTOBuilder(registry, runtimeAttributes).build();
    }
}