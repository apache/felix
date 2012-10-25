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

package org.apache.felix.useradmin.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Role;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(JUnit4TestRunner.class)
public class FileStoreInitializationTest extends BaseIntegrationTest {

	/**
	 * Provides a mock file store that does nothing but track the number of
	 * times the {@link #initialize()} and {@link #close()} methods are called.
	 */
	public static class MockRoleRepositoryStore implements RoleRepositoryStore {

		final AtomicInteger m_initCount = new AtomicInteger(0);
		final AtomicInteger m_closeCount = new AtomicInteger(0);

		@Override
		public boolean addRole(Role role) throws IOException {
			return false;
		}

		@Override
		public void close() throws IOException {
			m_closeCount.incrementAndGet();
		}

		@Override
		public Role[] getAllRoles() throws IOException {
			return null;
		}

		@Override
		public Role getRoleByName(String roleName) throws IOException {
			return null;
		}

		@Override
		public void initialize() throws IOException {
			m_initCount.incrementAndGet();
		}

		@Override
		public boolean removeRole(Role role) throws IOException {
			return false;
		}
	}

	/**
	 * Tests that initialization and closing of the repository store is
	 * performed correctly.
	 */
	@Test
	public void testStoreIsInitializedAndClosedProperlyOk() throws Exception {
		final String serviceName = RoleRepositoryStore.class.getName();
		final MockRoleRepositoryStore mockStore = new MockRoleRepositoryStore();

		// Stop the file store...
		Bundle fileStoreBundle = findBundle(ORG_APACHE_FELIX_USERADMIN_FILESTORE);
		assertNotNull(fileStoreBundle);
		fileStoreBundle.stop();

		// Manually register our mock store...
		ServiceRegistration serviceReg = m_context.registerService(serviceName, mockStore, null);

		// Wait until it becomes available...
		awaitService(serviceName);

		assertEquals(1, mockStore.m_initCount.get());
		assertEquals(0, mockStore.m_closeCount.get());

		serviceReg.unregister();

		Thread.sleep(100); // sleep a tiny bit to allow service to be properly unregistered...

		assertEquals(1, mockStore.m_initCount.get());
		assertEquals(1, mockStore.m_closeCount.get());

		// Re-register the service again...
		serviceReg = m_context.registerService(serviceName, mockStore, null);

		assertEquals(2, mockStore.m_initCount.get());
		assertEquals(1, mockStore.m_closeCount.get());

		// Stop & start the UserAdmin bundle to verify the initialization is
		// still only performed once...
		Bundle userAdminBundle = findBundle(ORG_APACHE_FELIX_USERADMIN);
		assertNotNull(userAdminBundle);
		userAdminBundle.stop();

		Thread.sleep(100); // sleep a tiny bit to allow service to be properly unregistered...

		userAdminBundle.start();

		assertEquals(3, mockStore.m_initCount.get());
		assertEquals(2, mockStore.m_closeCount.get());
	}
}
