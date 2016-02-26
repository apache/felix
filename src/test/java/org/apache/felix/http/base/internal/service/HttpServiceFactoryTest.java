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
package org.apache.felix.http.base.internal.service;

import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.registry.HandlerRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpService;

public class HttpServiceFactoryTest {
    @Test
    public void testGetServiceActiveInActive() throws Exception {
        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.createFilter(Mockito.anyString())).then(new Answer<Filter>() {
            @Override
            public Filter answer(InvocationOnMock invocation) throws Throwable {
                return FrameworkUtil.createFilter((String) invocation.getArguments()[0]);
            }
        });
        HttpServiceFactory hsf = new HttpServiceFactory(bc, new HandlerRegistry());

        Assert.assertNull("Not yet active",
                hsf.getService(Mockito.mock(Bundle.class), null));

        ServletContext sctx = Mockito.mock(ServletContext.class);
        hsf.start(sctx);
        HttpService svc = hsf.getService(Mockito.mock(Bundle.class), null);
        Assert.assertNotNull(svc);

        hsf.stop();
        Assert.assertNull("Not active any more",
                hsf.getService(Mockito.mock(Bundle.class), null));
    }
}
