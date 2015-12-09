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
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * Checks that BundleTracker actually tracks and untracks a bundle that is entering and then  
 * leaving the observed states
 */
public class BundleTrackerTest {

    @Test
    public void testTracking() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        
        BundleContext context = Mockito.mock(BundleContext.class);
        
        @SuppressWarnings("unchecked")
        BundleTrackerCustomizer<Bundle> customizer = mock(BundleTrackerCustomizer.class);
        when(customizer.addingBundle(Mockito.eq(bundle), Mockito.any(BundleEvent.class))).thenReturn(bundle);
        
        BundleTracker<Bundle> tracker = new BundleTracker<Bundle>(context , Bundle.ACTIVE | Bundle.STARTING, customizer);
        tracker.open();
        ArgumentCaptor<BundleListener> listenerCaptor = ArgumentCaptor.forClass(BundleListener.class);
        verify(context).addBundleListener(listenerCaptor.capture());
        BundleListener listener = listenerCaptor.getValue();
        
        BundleEvent startedEvent = new BundleEvent(BundleEvent.STARTED, bundle);
        listener.bundleChanged(startedEvent);
        verify(customizer).addingBundle(bundle, startedEvent);
        
        when(bundle.getState()).thenReturn(Bundle.INSTALLED);
        BundleEvent stoppedEvent = new BundleEvent(BundleEvent.STOPPED, bundle);
        listener.bundleChanged(stoppedEvent);
        verify(customizer).removedBundle(bundle, stoppedEvent, bundle);
    }
}
