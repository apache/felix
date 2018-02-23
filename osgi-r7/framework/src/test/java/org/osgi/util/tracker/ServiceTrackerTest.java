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
package org.osgi.util.tracker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class ServiceTrackerTest {

    @Test
    public void testTracking() throws InvalidSyntaxException {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        
        BundleContext context = Mockito.mock(BundleContext.class);
        
        @SuppressWarnings("unchecked")
        ServiceTrackerCustomizer<Runnable, Runnable> customizer = mock(ServiceTrackerCustomizer.class);
        @SuppressWarnings("unchecked")
        ServiceReference<Runnable> ref = mock(ServiceReference.class);
        Runnable service = mock(Runnable.class);
        when(customizer.addingService(ref)).thenReturn(service);
        
        Filter filter = mock(Filter.class);
        String filterString = "(objectClass=java.lang.Runnable)";
        when(context.createFilter(Mockito.eq(filterString))).thenReturn(filter);
        
        ServiceTracker<Runnable, Runnable> tracker = new ServiceTracker<Runnable, Runnable>(context, Runnable.class, customizer);
        tracker.open();

        ArgumentCaptor<ServiceListener> listenerCaptor = ArgumentCaptor.forClass(ServiceListener.class);
        verify(context).addServiceListener(listenerCaptor.capture(), Mockito.eq(filterString));
        ServiceListener listener = listenerCaptor.getValue();
        
        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref));
        verify(customizer).addingService(ref);
        
        listener.serviceChanged(new ServiceEvent(ServiceEvent.MODIFIED, ref));
        verify(customizer).modifiedService(ref, service);
        
        listener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref));
        verify(customizer).removedService(ref, service);
        
        tracker.close();
    }
}
