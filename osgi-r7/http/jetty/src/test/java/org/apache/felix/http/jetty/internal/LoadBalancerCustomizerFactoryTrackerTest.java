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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.http.jetty.LoadBalancerCustomizerFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.Request;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class LoadBalancerCustomizerFactoryTrackerTest
{

    @Test public void testTrackerOrdering() throws Exception
    {
        final BundleContext bc = mock(BundleContext.class);
        final CustomizerWrapper wrapper = new CustomizerWrapper();

        final List<String> result = new ArrayList<>();

        final LoadBalancerCustomizerFactoryTracker tracker = new LoadBalancerCustomizerFactoryTracker(bc, wrapper);

        wrapper.customize(null, null, null);
        assertTrue(result.isEmpty());

        final ServiceReference<LoadBalancerCustomizerFactory> refA = create(bc, result, 5, "A");
        final ServiceReference<LoadBalancerCustomizerFactory> refB = create(bc, result, 15, "B");
        final ServiceReference<LoadBalancerCustomizerFactory> refC = create(bc, result, 25, "C");

        // just A
        tracker.addingService(refA);

        wrapper.customize(null, null, null);
        assertEquals(1, result.size());
        assertEquals("A", result.get(0));
        result.clear();

        // add B, B is highest
        tracker.addingService(refB);

        wrapper.customize(null, null, null);
        assertEquals(1, result.size());
        assertEquals("B", result.get(0));
        result.clear();

        // add C, C is highest
        tracker.addingService(refC);

        wrapper.customize(null, null, null);
        assertEquals(1, result.size());
        assertEquals("C", result.get(0));
        result.clear();

        // remove B, C is still highest
        tracker.removedService(refB, refB);

        wrapper.customize(null, null, null);
        assertEquals(1, result.size());
        assertEquals("C", result.get(0));
        result.clear();

        // remove C, A is highest
        tracker.removedService(refC, refC);

        wrapper.customize(null, null, null);
        assertEquals(1, result.size());
        assertEquals("A", result.get(0));
        result.clear();

        // remove A, no customizer
        tracker.removedService(refA, refA);

        wrapper.customize(null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test public void testTrackerCreateFailures() throws Exception
    {
        final BundleContext bc = mock(BundleContext.class);
        final CustomizerWrapper wrapper = new CustomizerWrapper();

        final List<String> result = new ArrayList<>();

        final LoadBalancerCustomizerFactoryTracker tracker = new LoadBalancerCustomizerFactoryTracker(bc, wrapper);

        wrapper.customize(null, null, null);
        assertTrue(result.isEmpty());

        final ServiceReference<LoadBalancerCustomizerFactory> refA = create(bc, result, 5, "A");
        final ServiceReference<LoadBalancerCustomizerFactory> refB = create(bc, result, 15, null);
        final ServiceReference<LoadBalancerCustomizerFactory> refC = create(bc, result, 25, "C");

        // add A, C, B - C is highest
        tracker.addingService(refA);
        tracker.addingService(refC);
        tracker.addingService(refB);

        wrapper.customize(null, null, null);
        assertEquals(1, result.size());
        assertEquals("C", result.get(0));
        result.clear();

        // remove C, B returns null, therefore A is used
        tracker.removedService(refC, refC);

        wrapper.customize(null, null, null);
        assertEquals(1, result.size());
        assertEquals("A", result.get(0));
        result.clear();

        // remove A, no wrapper
        tracker.removedService(refA, refA);
        wrapper.customize(null, null, null);
        assertTrue(result.isEmpty());

        // remove B, no wrapper
        tracker.removedService(refB, refB);
        wrapper.customize(null, null, null);
        assertTrue(result.isEmpty());
    }

    private ServiceReference<LoadBalancerCustomizerFactory> create(final BundleContext bc,
            final List<String> result,
            final int ranking,
            final String identifier)
    {

        final ServiceReference<LoadBalancerCustomizerFactory> refA = new ServiceReferenceImpl(ranking);
        when(bc.getService(refA)).thenReturn(new LoadBalancerCustomizerFactory()
        {

            @Override
            public Customizer createCustomizer()
            {
                if ( identifier == null )
                {
                    return null;
                }
                return new Customizer()
                {

                    @Override
                    public void customize(Connector connector, HttpConfiguration channelConfig, Request request)
                    {
                        result.add(identifier);
                    }
                };
            }
        });

        return refA;
    }

    private static class ServiceReferenceImpl implements ServiceReference<LoadBalancerCustomizerFactory>
    {

        private final int serviceRanking;

        public ServiceReferenceImpl(final int ranking)
        {
            this.serviceRanking = ranking;
        }

        @Override
        public Object getProperty(String key)
        {
            return null;
        }

        @Override
        public String[] getPropertyKeys()
        {
            return null;
        }

        @Override
        public Bundle getBundle()
        {
            return null;
        }

        @Override
        public Bundle[] getUsingBundles()
        {
            return null;
        }

        @Override
        public boolean isAssignableTo(Bundle bundle, String className)
        {
            return false;
        }

        @Override
        public int compareTo(Object reference)
        {
            final ServiceReferenceImpl o = (ServiceReferenceImpl)reference;
            if ( serviceRanking < o.serviceRanking )
            {
                return -1;
            }
            else if ( serviceRanking > o.serviceRanking )
            {
                return 1;
            }
            return 0;
        }

    };
}
