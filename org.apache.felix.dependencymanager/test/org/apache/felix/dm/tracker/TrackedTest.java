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
package org.apache.felix.dm.tracker;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.apache.felix.dm.tracker.ServiceTrackerCustomizer;
import org.apache.felix.dm.tracker.ServiceTracker.Tracked;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TrackedTest {

	@Test
	public void testSetInitialHideAspects() {
		System.out.println("testSetInitialHideAspects");
		TestCustomizer customizer = new TestCustomizer();
		
		ServiceTracker tracker = new TestTracker(customizer);
		tracker.open();
		Tracked tracked = tracker.getTracked();

		Object[] initialReferences = new Object[] {
				createServiceReference(1L),
				createServiceReference(2L, 1L, 10),
				createServiceReference(3L),
				createServiceReference(4L, 1L, 5),
				createServiceReference(5L, 3L, 5),
		};
		tracked.setInitial(initialReferences);
		tracked.trackInitial();
		tracked.getExecutor().execute();
		assertArrayEquals(new Long[] { 2L, 5L }, customizer.getServiceReferenceIds());
	}
	
	@Test
	public void testUnHideAspect() {
		System.out.println("testUnhideAspect");
		TestCustomizer customizer = new TestCustomizer();
		
		ServiceTracker tracker = new TestTracker(customizer);
		tracker.open();
		Tracked tracked = tracker.getTracked();
		
		ServiceReference[] initialReferences = new ServiceReference[] {
				createServiceReference(1L),
				createServiceReference(2L, 1L, 10),
				createServiceReference(3L),
				createServiceReference(4L, 1L, 5),
				createServiceReference(5L, 3L, 5),
		};
		tracked.setInitial(initialReferences);
		tracked.trackInitial();
		tracked.getExecutor().execute();
		assertArrayEquals(new Long[] { 2L, 5L }, customizer.getServiceReferenceIds());
		
		// create a service event that unregisters service with id 2, we would expect it to be swapped with 4.
		ServiceEvent event = new ServiceEvent(ServiceEvent.UNREGISTERING, initialReferences[1]);
		tracked.serviceChanged(event);
		assertArrayEquals(new Long[] { 5L, 4L }, customizer.getServiceReferenceIds());
		// create a service event that unregisters service with id 4, we would expect it to be swapped with 1.
		event = new ServiceEvent(ServiceEvent.UNREGISTERING, initialReferences[3]);
		tracked.serviceChanged(event);
		assertArrayEquals(new Long[] { 5L, 1L }, customizer.getServiceReferenceIds());
	}	
	
	@Test
	public void testHideAspect() {
		System.out.println("testHideAspect");
		TestCustomizer customizer = new TestCustomizer();
		
		ServiceTracker tracker = new TestTracker(customizer);
		tracker.open();
		Tracked tracked = tracker.getTracked();
		
		ServiceReference[] initialReferences = new ServiceReference[] {
				createServiceReference(1L),
				createServiceReference(2L, 1L, 10),
				createServiceReference(3L),
				createServiceReference(4L, 1L, 5),
				createServiceReference(5L, 3L, 5),
		};
		tracked.setInitial(initialReferences);
		tracked.trackInitial();
		tracked.getExecutor().execute();
		assertArrayEquals(new Long[] { 2L, 5L }, customizer.getServiceReferenceIds());
		
		// create a service event that registers another but lower ranked aspect for service with id 1. 
		ServiceReference newReference = createServiceReference(6L, 1L, 8);
		ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, newReference);
		tracked.serviceChanged(event);
		assertArrayEquals(new Long[] { 2L, 5L }, customizer.getServiceReferenceIds());
		
		// create a service event that unregisters service with id 2, we would expect it to be swapped with 6.
		event = new ServiceEvent(ServiceEvent.UNREGISTERING, initialReferences[1]);
		tracked.serviceChanged(event);
		assertArrayEquals(new Long[] { 5L, 6L }, customizer.getServiceReferenceIds());
		
		// create a service event that unregisters service with id 6, we would expect it to be swapped with 4.
		event = new ServiceEvent(ServiceEvent.UNREGISTERING, newReference);
		tracked.serviceChanged(event);
		assertArrayEquals(new Long[] { 5L, 4L }, customizer.getServiceReferenceIds());	
		
		// create a service event that registers a higher ranked aspect for service with id 1.
		ServiceReference higherRankedReference = createServiceReference(7L, 1L, 15);
		ServiceEvent addHigherRankedEvent = new ServiceEvent(ServiceEvent.REGISTERED, higherRankedReference);
		tracked.serviceChanged(addHigherRankedEvent);
		assertArrayEquals(new Long[] { 5L, 7L }, customizer.getServiceReferenceIds());	
	}		
	
	@Test
	public void testSetInitialTrackAspects() {
		System.out.println("testSetInitialTrackAspects");
		TestCustomizer customizer = new TestCustomizer();
		
		ServiceTracker tracker = new TestTracker(customizer);
		tracker.open(false, true);
		Tracked tracked = tracker.getTracked();
		
		Object[] initialReferences = new Object[] {
				createServiceReference(1L),
				createServiceReference(2L, 1L, 10),
				createServiceReference(3L, 1L, 5)
		};
		tracked.setInitial(initialReferences);
		tracked.trackInitial();
		tracked.getExecutor().execute();
		assertArrayEquals(new Long[] { 1L, 2L, 3L }, customizer.getServiceReferenceIds());
	}	
	
	private static BundleContext createBundleContext() {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(Constants.FRAMEWORK_VERSION)).thenReturn(null);
		return context;
	}
	
	private ServiceReference createServiceReference(Long serviceId) {
		return createServiceReference(serviceId, null, null);
	}

	private ServiceReference createServiceReference(Long serviceId, Long aspectId, Integer ranking) {
		return new TestServiceReference(serviceId, aspectId, ranking);
	}

	class TestTracker extends ServiceTracker {

		public TestTracker(ServiceTrackerCustomizer customizer) {
			super(createBundleContext(), "(objectClass=*)", customizer);
		}
		
	}
	
	class TestCustomizer implements ServiceTrackerCustomizer {
		
		List<ServiceReference> serviceReferences = new ArrayList<>();

		@Override
		public Object addingService(ServiceReference reference) {
			System.out.println("adding service: " + reference);
			return new Object();
		}

		@Override
		public void addedService(ServiceReference reference, Object service) {
			System.out.println("added service: " + reference);
			serviceReferences.add(reference);
		}

		@Override
		public void modifiedService(ServiceReference reference, Object service) {
			System.out.println("modified service: " + reference);
		}

		@Override
		public void swappedService(ServiceReference reference, Object service,
				ServiceReference newReference, Object newService) {
			System.out.println("swapped service: " + reference);
			serviceReferences.remove(reference);
			serviceReferences.add(newReference);
		}

		@Override
		public void removedService(ServiceReference reference, Object service) {
			System.out.println("removed service: " + reference);
			serviceReferences.remove(reference);
		}
		
		public Long[] getServiceReferenceIds() {
			Long[] ids = new Long[serviceReferences.size()];
			for (int i = 0; i < serviceReferences.size(); i++) {
				ids[i] = (Long) serviceReferences.get(i).getProperty(Constants.SERVICE_ID);
			}
			return ids;
		}
		
	}
	
	class TestServiceReference implements ServiceReference {
		
		Properties props = new Properties();

		public TestServiceReference(Long serviceId, Long aspectId,
				Integer ranking) {
			props.put(Constants.SERVICE_ID, serviceId);
			if (aspectId != null) {
				props.put(DependencyManager.ASPECT, aspectId);
			}
			if (ranking != null) {
				props.put(Constants.SERVICE_RANKING, ranking);
			}
		}

		@Override
		public Object getProperty(String key) {
			return props.get(key);
		}

		@Override
		public String[] getPropertyKeys() {
			return props.keySet().toArray(new String[]{});
		}

		@Override
		public Bundle getBundle() {
			return null;
		}

		@Override
		public Bundle[] getUsingBundles() {
			return null;
		}

		@Override
		public boolean isAssignableTo(Bundle bundle, String className) {
			return false;
		}

		@Override
		public int compareTo(Object reference) // Kindly borrowed from the Apache Felix ServiceRegistrationImpl.ServiceReferenceImpl
        {
            ServiceReference other = (ServiceReference) reference;

            Long id = (Long) getProperty(Constants.SERVICE_ID);
            Long otherId = (Long) other.getProperty(Constants.SERVICE_ID);

            if (id.equals(otherId))
            {
                return 0; // same service
            }

            Object rankObj = getProperty(Constants.SERVICE_RANKING);
            Object otherRankObj = other.getProperty(Constants.SERVICE_RANKING);

            // If no rank, then spec says it defaults to zero.
            rankObj = (rankObj == null) ? new Integer(0) : rankObj;
            otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

            // If rank is not Integer, then spec says it defaults to zero.
            Integer rank = (rankObj instanceof Integer)
                ? (Integer) rankObj : new Integer(0);
            Integer otherRank = (otherRankObj instanceof Integer)
                ? (Integer) otherRankObj : new Integer(0);

            // Sort by rank in ascending order.
            if (rank.compareTo(otherRank) < 0)
            {
                return -1; // lower rank
            }
            else if (rank.compareTo(otherRank) > 0)
            {
                return 1; // higher rank
            }

            // If ranks are equal, then sort by service id in descending order.
            return (id.compareTo(otherId) < 0) ? 1 : -1;
        }

		@Override
		public String toString() {
			return "TestServiceReference [props=" + props + "]";
		}

	}
	
}
