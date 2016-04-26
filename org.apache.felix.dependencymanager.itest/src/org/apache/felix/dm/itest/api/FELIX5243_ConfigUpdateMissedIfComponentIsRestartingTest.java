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
package org.apache.felix.dm.itest.api;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * This test reproduces the following race condition:
 * 
 * A "Pojo" component depends on a required "MyDependency" service.
 * The Pojo also has a configuration dependency.
 * 
 * Now, concurrently, the MyDependency is restarted and the configuration is updated.
 * This means that the Pojo is restarted and should also get notified for the configuration update.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5243_ConfigUpdateMissedIfComponentIsRestartingTest extends TestBase {
	final static String PID = "ConfigurationDependencyTest.pid";

	public void testOptionalConfigurationConsumer() throws InterruptedException {
		DependencyManager m = getDM();
		Ensure e = new Ensure();

		UpdateController controller = new UpdateController();
		Pojo pojo = new Pojo(controller);
		Configurator configurator = new Configurator(e, "my.pid");
		ThreadPoolExecutor tpool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

		try {
			Component pojoComp = m.createComponent().setImplementation(pojo)
					.add(m.createServiceDependency().setService(MyDependency.class).setRequired(true))
					.add(m.createConfigurationDependency().setCallback("updated", MyConfig.class).setPid("my.pid"));

			Component configuratorComp = m.createComponent().setImplementation(configurator)
					.add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));

			Component myDep = m.createComponent().setImplementation(new MyDependency())
					.setInterface(MyDependency.class.getName(), null);

			m.add(configuratorComp);
			m.add(pojoComp);
			m.add(myDep);

			e.waitForStep(1, 5000); // configurator is ready.
			
			// make sure pojo is updated with property=2
			controller.setExpectedUpdatedProperty(2);
			configurator.update(2);
			controller.awaitUpdated();
			
			// Now, make a loop: for each loop:
			// - concurrently remove/add the dependency in order to restart the pojo.
			// - update the configuration.
			// - and check if the component has not missed the configuration update

			for (int i = 3; i < 10000; i ++) {
				controller.setExpectedUpdatedProperty(i);
				tpool.execute(() -> {
					m.remove(myDep);
					m.add(myDep);
				});
				configurator.update(i);
				controller.awaitUpdated();
			}
			
			m.remove(configuratorComp);
			m.remove(pojoComp);
			m.remove(myDep);
		} finally {
			tpool.shutdown();
		}
	}

	static interface MyConfig {
		int getProperty();
	}

	static class UpdateController {
		volatile CountDownLatch m_latch;
		volatile int m_expected;
		
		public void setExpectedUpdatedProperty(int expected) {
			m_expected = expected;
			m_latch = new CountDownLatch(1);
		}

		public void updated(int property) {
			if (property == m_expected) {
				m_latch.countDown();
			}
		}
		
		public void awaitUpdated() throws InterruptedException {
			if (! m_latch.await(5000, TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException("property not updated to value " + m_expected);
			}
			if (m_expected % 100 == 0) {
				System.out.println("#updated " + m_expected);
			}
		}
	}

	static class MyDependency {
	}

	static class Pojo {
		final UpdateController m_controller;
		volatile MyDependency m_dependency;

		Pojo(UpdateController controller) {
			m_controller = controller;
		}

		void updated(MyConfig cnf) {
			if (cnf != null) {
				m_controller.updated(cnf.getProperty());
			}
		}
	}

	static class Configurator {
		volatile ConfigurationAdmin m_ca;
		volatile Configuration m_conf;
		final String m_pid;
		final Ensure m_e;

		public Configurator(Ensure e, String pid) {
			m_pid = pid;
			m_e = e;
		}

		public void start() throws IOException {
			m_conf = m_ca.getConfiguration(m_pid, null);
			m_e.step(1);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void update(int property) {
			try {
				Hashtable props = new Properties();
				props.put("property", property);
				m_conf.update(props);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void destroy() throws IOException {
			m_conf.delete();
		}
	}
}
