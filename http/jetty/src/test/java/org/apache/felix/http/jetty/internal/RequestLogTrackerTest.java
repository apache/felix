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
package org.apache.felix.http.jetty.internal;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RequestLogTrackerTest {
    
    @Mock
    BundleContext context;
    
    @Test
    public void testInvokeRequestLog() throws Exception {
        RequestLogTracker tracker = new RequestLogTracker(context, null);

        RequestLog mockRequestLog = mock(RequestLog.class);

        ServiceReference<RequestLog> mockSvcRef = mock(ServiceReference.class);
        when(context.getService(mockSvcRef)).thenReturn(mockRequestLog);

        // These invocations not passed through to the mock because it is not registered yet
        for (int i = 0; i < 10; i++)
            tracker.log(null, null);

        tracker.addingService(mockSvcRef);

        // These will pass through
        for (int i = 0; i < 15; i++)
            tracker.log(null, null);

        tracker.removedService(mockSvcRef, mockRequestLog);

        // And these will not.
        for (int i = 0; i < 50; i++)
            tracker.log(null, null);

        verify(mockRequestLog, times(15)).log(isNull(Request.class), isNull(Response.class));
    }

    @Test
    public void testNaughtyService() throws Exception {
        RequestLogTracker tracker = new RequestLogTracker(context, null);

        AtomicInteger counter = new AtomicInteger(0);
        RequestLog mockRequestLog = new RequestLog() {
            @Override
            public void log(Request request, Response response) {
                counter.addAndGet(1);
                throw new RuntimeException("This service always explodes");
            }
        };
        ServiceReference<RequestLog> mockSvcRef = mock(ServiceReference.class);
        Bundle mockBundle = mock(Bundle.class);
        when(mockSvcRef.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getSymbolicName()).thenReturn("org.example");
        when(mockBundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(context.getService(mockSvcRef)).thenReturn(mockRequestLog);

        tracker.addingService(mockSvcRef);

        // Invoke 200 times
        for (int i = 0; i < 200; i++)
            tracker.log(null, null);

        tracker.removedService(mockSvcRef, mockRequestLog);

        // Invoked 100 times and then removed
        assertEquals(100, counter.get());
    }
}
