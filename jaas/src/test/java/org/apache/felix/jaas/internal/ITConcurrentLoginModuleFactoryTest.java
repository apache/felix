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

package org.apache.felix.jaas.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.felix.jaas.LoginModuleFactory;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@Ignore("FELIX-5502")
@RunWith(Parameterized.class)
public class ITConcurrentLoginModuleFactoryTest
{
    private static final String TEST_REALM_NAME = "FELIX";
    @Rule
    public OsgiContext context = new OsgiContext();

    @Parameterized.Parameters
    public static List<Object[]> data()
    {
        return Arrays.asList(new Object[25][0]);
    }

    @Test
    public void concurrentLoginFactoryRegs() throws Exception
    {
        Logger log = new Logger(context.bundleContext());

        BundleContext mock = spy(context.bundleContext());
        doReturn(mock(LoginModuleFactory.class)).when(mock).getService(
            any(ServiceReference.class));

        ConfigSpiOsgi spi = new ConfigSpiOsgi(mock, log);

        int numOfServices = 20;
        Queue<ServiceReference> references = new ArrayBlockingQueue<ServiceReference>(numOfServices);
        for (int i = 0; i < numOfServices; i++)
        {
            references.add(newReference());
        }

        CountDownLatch latch = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 3; i++)
        {
            Thread t = new Thread(new ServiceAdder(latch, references, spi));
            threads.add(t);
            t.start();
        }

        latch.countDown();

        for (Thread t : threads)
        {
            t.join();
        }

        assertEquals(numOfServices,
            spi.engineGetAppConfigurationEntry(TEST_REALM_NAME).length);
    }

    private static class ServiceAdder implements Runnable
    {
        private final CountDownLatch latch;
        private final Queue<ServiceReference> references;
        private final ConfigSpiOsgi spi;

        ServiceAdder(CountDownLatch latch, Queue<ServiceReference> references, ConfigSpiOsgi spi)
        {
            this.latch = latch;
            this.references = references;
            this.spi = spi;
        }

        @Override
        public void run()
        {
            try
            {
                latch.await();
            }
            catch (InterruptedException ignore)
            {
                return;
            }
            while (!references.isEmpty())
            {
                ServiceReference reference = references.poll();
                if (reference != null)
                {
                    spi.addingService(reference);
                }
            }
        }
    }

    private static ServiceReference newReference()
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(LoginModuleFactory.JAAS_CONTROL_FLAG, "REQUIRED");
        props.put(LoginModuleFactory.JAAS_REALM_NAME, TEST_REALM_NAME);

        ServiceReference sr = mock(ServiceReference.class);
        when(sr.getProperty(any(String.class))).thenAnswer(new Answer<Object>()
        {
            @SuppressWarnings("SuspiciousMethodCalls")
            @Override
            public Object answer(InvocationOnMock i) throws Throwable
            {
                return props.get(i.getArguments()[0]);
            }
        });
        return sr;
    }
}
