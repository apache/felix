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

import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.diagnostics.CircularDependency;
import org.apache.felix.dm.diagnostics.DependencyGraph;
import org.apache.felix.dm.diagnostics.DependencyGraph.ComponentState;
import org.apache.felix.dm.diagnostics.DependencyGraph.DependencyState;
import org.apache.felix.dm.diagnostics.MissingDependency;
import org.apache.felix.dm.itest.util.TestBase;

public class DiagnosticsTest extends TestBase {
	
	// there are two components (TestComponent and HelloWorldServiceFactory) created by 
    // org.apache.felix.dm.itest.bundle
	// These component is always registered, so we only need to take them into 
	// account when we build the graph with ALL components
	private static final int InitialComponentCount = 2;
	
	private boolean checkComponentCount(int expected, int count) {
		return count == expected + InitialComponentCount;
	}
	
	public void testWithoutComponents() throws Exception {
		
		DependencyGraph graph = DependencyGraph.getGraph(
				ComponentState.ALL, 
				DependencyState.ALL);
	
		assertTrue(checkComponentCount(0, graph.getAllComponents().size()));
		assertTrue(graph.getAllDependencies().isEmpty());
	}
	
	public void testSingleComponent() throws Exception {
		
		DependencyManager dm = getDM();
		
		Component component = dm.createComponent()
								.setImplementation(Object.class);
							
		dm.add(component);
		
		DependencyGraph graph = DependencyGraph.getGraph(
				ComponentState.ALL, DependencyState.ALL);
		
		assertTrue(checkComponentCount(1, graph.getAllComponents().size()));
		assertTrue(graph.getAllDependencies().isEmpty());
		
		graph = DependencyGraph.getGraph(
				ComponentState.UNREGISTERED, DependencyState.ALL_UNAVAILABLE);
		
		assertTrue(graph.getAllComponents().isEmpty());
		assertTrue(graph.getAllDependencies().isEmpty());
	}

	public void testServiceDependencyMissing() throws Exception {
		
		DependencyManager dm = getDM();
		
		ServiceDependency serviceDependency1 = dm.createServiceDependency()
				.setService(S1.class)
				.setRequired(true);
		ServiceDependency serviceDependency2 = dm.createServiceDependency()
				.setService(S2.class)
				.setRequired(true);
		
		Component component1 = dm.createComponent()
				.setImplementation(C0.class)
				.add(serviceDependency1);
		Component component2 = dm.createComponent()
				.setImplementation(S1Impl1.class)
				.setInterface(S1.class.getName(), null)
				.add(serviceDependency2);
		
		dm.add(component1);
		dm.add(component2);
		
		DependencyGraph graph = DependencyGraph.getGraph(
				ComponentState.UNREGISTERED, DependencyState.REQUIRED_UNAVAILABLE);
		
		assertEquals(2, graph.getAllComponents().size());
		assertEquals(2, graph.getAllDependencies().size());
		
		List<MissingDependency> missingDependencies = graph.getMissingDependencies("service");
		assertEquals(1, missingDependencies.size());
		assertEquals(S2.class.getName(), missingDependencies.get(0).getName());
		
		assertTrue(graph.getMissingDependencies("configuration").isEmpty());
		assertTrue(graph.getMissingDependencies("bundle").isEmpty());
		assertTrue(graph.getMissingDependencies("resource").isEmpty());
		
	}
	
	public void testConfigurationDependencyMissing() throws Exception {
		
		DependencyManager dm = getDM();
		
		ConfigurationDependency configurationDependency1 = dm.createConfigurationDependency()
				.setPid("missing.configuration.pid");
		
		Component component1 = dm.createComponent()
				.setImplementation(Object.class)
				.add(configurationDependency1);
		m_dm.add(component1);
		
		DependencyGraph graph = DependencyGraph.getGraph(ComponentState.UNREGISTERED, DependencyState.REQUIRED_UNAVAILABLE);
		
		assertEquals(1, graph.getAllComponents().size());
		assertEquals(1, graph.getAllDependencies().size());
		
		List<MissingDependency> missingServiceDependencies = graph.getMissingDependencies("service");
		assertTrue(missingServiceDependencies.isEmpty());
		
		List<MissingDependency> missingConfigDependencies = graph.getMissingDependencies("configuration");
		assertEquals(1, missingConfigDependencies.size());
		
		MissingDependency missingConfigDependency = missingConfigDependencies.get(0);
		assertEquals("missing.configuration.pid", missingConfigDependency.getName());
	}

	public void testProvidersWithoutProperties() throws Exception {
		DependencyManager dm = getDM();
		
		ServiceDependency serviceDependency1 = dm.createServiceDependency()
				.setService(S1.class)
				.setRequired(true);
		
		Component component1 = dm.createComponent()
				.setImplementation(C0.class)
				.add(serviceDependency1);
		
		Component component2 = dm.createComponent()
				.setImplementation(S1Impl1.class)
				.setInterface(S1.class.getName(), null);
		Component component3 = dm.createComponent()
				.setImplementation(S1Impl2.class)
				.setInterface(S1.class.getName(), null);
		
		dm.add(component1);
		dm.add(component2);
		dm.add(component3);
		
		DependencyGraph graph = DependencyGraph.getGraph(ComponentState.ALL, DependencyState.ALL);
		
		List<ComponentDeclaration> providers = graph.getProviders(serviceDependency1);
		assertEquals(2, providers.size());
		assertTrue(providers.contains(component2));
		assertTrue(providers.contains(component3));
	}
	
	public void testProvidersWithProperties() throws Exception {
		
		DependencyManager dm = getDM();
		
		ServiceDependency serviceDependency1 = dm.createServiceDependency()
				.setService(S1.class, "(key=value)")
				.setRequired(true);
		
		Component component1 = dm.createComponent()
				.setImplementation(C0.class)
				.add(serviceDependency1);

		Properties component2Properties = new Properties();
		component2Properties.put("key", "value");
		Properties component4Properties = new Properties();
		component4Properties.put("key", "otherValue");

		Component component2 = dm.createComponent()
				.setImplementation(S1Impl1.class)
				.setInterface(S1.class.getName(), component2Properties);
		Component component3 = dm.createComponent()
				.setImplementation(S1Impl2.class)
				.setInterface(S1.class.getName(), null);
		Component component4 = dm.createComponent()
				.setImplementation(S1Impl3.class)
				.setInterface(S1.class.getName(), component4Properties);

		m_dm.add(component1);
		m_dm.add(component2);
		m_dm.add(component3);
		m_dm.add(component4);
		
		DependencyGraph graph = DependencyGraph.getGraph(ComponentState.ALL, DependencyState.ALL);
		
		List<ComponentDeclaration> providers = graph.getProviders(serviceDependency1);
		assertEquals(1, providers.size());
		assertTrue(providers.contains(component2));
		assertFalse(providers.contains(component3));
		assertFalse(providers.contains(component4));
		
	}

	public void testCircularDependencies() throws Exception {
		
		DependencyManager dm = getDM();
		
		Component component0 = dm.createComponent()
				.setImplementation(C0.class)
				.add(dm.createServiceDependency()
						.setService(S1.class)
						.setRequired(true));

		Component component1 = dm.createComponent()
				.setImplementation(S1Impl1.class)
				.setInterface(S1.class.getName(), null)
				.add(dm.createServiceDependency()
						.setService(S2.class)
						.setRequired(true));
		Component component2 = dm.createComponent()
				.setImplementation(S2Impl1.class)
				.setInterface(S2.class.getName(), null)
				.add(dm.createServiceDependency()
						.setService(S1.class)
						.setRequired(true));
		
		m_dm.add(component0);
		m_dm.add(component1);
		m_dm.add(component2);
		
		DependencyGraph graph = DependencyGraph.getGraph(ComponentState.UNREGISTERED,DependencyState.REQUIRED_UNAVAILABLE);
		List<CircularDependency> circularDependencies = graph.getCircularDependencies();
		
		assertEquals(1, circularDependencies.size());
		
		List<ComponentDeclaration> circularDependencyComponents = circularDependencies.get(0).getComponents(); 
		assertTrue(circularDependencyComponents.contains(component1));
		assertTrue(circularDependencyComponents.contains(component2));
		assertFalse(circularDependencyComponents.contains(component0));
		
	}
	
	public void testWithTwoProvidersOneUnavailable() {
		DependencyManager dm = getDM();
		
		Component component0 = dm.createComponent()
				.setImplementation(C0.class)
				.add(dm.createServiceDependency()
						.setService(S1.class)
						.setRequired(true));
		Component component1 = dm.createComponent()
				.setImplementation(S1Impl1.class)
				.setInterface(S1.class.getName(), null);
		Component component2 = dm.createComponent()
				.setImplementation(S1Impl2.class)
				.setInterface(S1.class.getName(), null)
				.add(dm.createServiceDependency()
						.setService(S2.class)
						.setRequired(true));
		
		dm.add(component0);
		dm.add(component1);
		dm.add(component2);
		
		DependencyGraph graph = DependencyGraph.getGraph(ComponentState.UNREGISTERED, DependencyState.REQUIRED_UNAVAILABLE);
		
		assertEquals(1, graph.getAllComponents().size());
		List<MissingDependency> missingDependencies = graph.getMissingDependencies("service");
		assertEquals(1, missingDependencies.size());
	
		MissingDependency missingDependency = missingDependencies.get(0);
		assertTrue(missingDependency.getName().equals(S2.class.getName()));
		
	}
	
	public void testGraphsWithSeveralComponentDependencyCombinations() throws Exception {
		
		DependencyManager dm = getDM();
		
		Component component0 = dm.createComponent()
				.setImplementation(C0.class)
				.add(dm.createServiceDependency()
						.setService(S1.class)
						.setRequired(true))
				.add(dm.createServiceDependency()
						.setService(S3.class)
						.setRequired(true))
				.add(dm.createServiceDependency()
						.setService(S4.class)
						.setRequired(true));
		Component component1 = dm.createComponent()
				.setImplementation(C1.class)
				.add(dm.createServiceDependency()
						.setService(S5.class)
						.setRequired(true))
				.add(dm.createServiceDependency()
						.setService(S6.class)
						.setRequired(true))
				.add(dm.createServiceDependency()
						.setService(S7.class)
						.setRequired(true));
		Component s1Impl1 = dm.createComponent()
				.setImplementation(S1Impl1.class)
				.setInterface(S1.class.getName(), null)
				.add(dm.createServiceDependency()
						.setService(S2.class)
						.setRequired(true));
		Component s1Impl2 = dm.createComponent()
				.setImplementation(S1Impl2.class)
				.setInterface(S1.class.getName(), null);

		Component s3Impl1 = dm.createComponent()
				.setImplementation(S3Impl1.class)
				.setInterface(S3.class.getName(), null)
				.add(dm.createConfigurationDependency()
						.setPid("missing.config.pid"));
		Component s3s4Impl = dm.createComponent()
				.setImplementation(S3S4Impl.class)
				.setInterface( new String[] {S3.class.getName(), S4.class.getName()}, null);
		Component s4Impl1 = dm.createComponent()
				.setImplementation(S4Impl1.class)
				.setInterface(S4.class.getName(), null);
		Component s5Impl1 = dm.createComponent()
				.setImplementation(S5Impl1.class)
				.setInterface(S5.class.getName(), null);
		Component s6s7Impl = dm.createComponent()
				.setImplementation(S6S7Impl.class)
				.setInterface( new String[] {S6.class.getName(), S7.class.getName()}, null)
				.add(dm.createServiceDependency()
						.setService(S8.class)
						.setRequired(true));
		Component s8Impl1 = dm.createComponent()
				.setImplementation(S8Impl1.class)
				.setInterface(S8.class.getName(), null)
				.add(dm.createServiceDependency()
						.setService(S6.class)
						.setRequired(true));
		dm.add(component0);
		dm.add(component1);
		dm.add(s1Impl1); dm.add(s1Impl2);
		dm.add(s3Impl1); dm.add(s3s4Impl);
		dm.add(s4Impl1); dm.add(s5Impl1);
		dm.add(s6s7Impl); dm.add(s8Impl1);
		
		
		// graph containing all components and dependencies
		DependencyGraph graph = DependencyGraph.getGraph(ComponentState.ALL, DependencyState.ALL);
		
		List<ComponentDeclaration> allComponents = graph.getAllComponents();
		assertTrue(checkComponentCount(10, allComponents.size()));
		
		List<MissingDependency> missingDependencies = graph.getMissingDependencies("service");
		assertEquals(1, missingDependencies.size());
		
		missingDependencies = graph.getMissingDependencies("configuration");
		assertEquals(1, missingDependencies.size());
		
		List<CircularDependency> circularDependencies = graph.getCircularDependencies();
		assertEquals(1, circularDependencies.size());
		CircularDependency circularDependency = circularDependencies.get(0);

		assertEquals(3, circularDependency.getComponents().size());
		assertTrue(circularDependency.getComponents().contains(s6s7Impl));
		assertTrue(circularDependency.getComponents().contains(s8Impl1));

		// graph containing unregistered components and unavailable required dependencies
		graph = null;
		graph = DependencyGraph.getGraph(ComponentState.UNREGISTERED, DependencyState.REQUIRED_UNAVAILABLE);
	
		List<ComponentDeclaration> unregComponents = graph.getAllComponents();
		assertEquals(5, unregComponents.size());
		assertTrue(unregComponents.contains(s1Impl1));
		assertTrue(unregComponents.contains(s3Impl1));
		assertTrue(unregComponents.contains(component1));
		assertTrue(unregComponents.contains(s6s7Impl));
		assertTrue(unregComponents.contains(s8Impl1));
		assertFalse(unregComponents.contains(component0));
		
		circularDependencies = graph.getCircularDependencies();
		assertEquals(1, circularDependencies.size());
		circularDependency = circularDependencies.get(0);

		assertEquals(3, circularDependency.getComponents().size());
		assertTrue(circularDependency.getComponents().contains(s6s7Impl));
		assertTrue(circularDependency.getComponents().contains(s8Impl1));
		
		missingDependencies = graph.getMissingDependencies("service");
		assertEquals(1, missingDependencies.size());	
		
		missingDependencies = graph.getMissingDependencies("configuration");
		assertEquals(1, missingDependencies.size());

		// call getCircularDependencies again on the same graph
		circularDependencies = graph.getCircularDependencies();
		assertEquals(1, circularDependencies.size());
		circularDependency = circularDependencies.get(0);

		assertEquals(3, circularDependency.getComponents().size());
		assertTrue(circularDependency.getComponents().contains(s6s7Impl));
		assertTrue(circularDependency.getComponents().contains(s8Impl1));
		
		List<MissingDependency> allMissingDependencies = graph.getMissingDependencies(null);
		assertEquals(2, allMissingDependencies.size());

	}
	
	static interface S1 {}
	static interface S2 {}
	static interface S3 {}
	static interface S4 {}
	static interface S5 {}
	static interface S6 {}
	static interface S7 {}
	static interface S8 {}
	static class C0 {
		public C0() {}
	}
	static class C1 {
		public C1() {}
	}
	static class S1Impl1 implements S1 {
		public S1Impl1(){}
	}
	static class S1Impl2 implements S1 {
		public S1Impl2() {}
	}
	static class S1Impl3 implements S1 {
		public S1Impl3() {}
	}
	static class S2Impl1 implements S2 {
		public S2Impl1() {}
	}
	static class S3Impl1 implements S3 {
		public S3Impl1() {}
	}
	static class S3S4Impl implements S3, S4 {
		public S3S4Impl() {}
	}
	static class S4Impl1 implements S4 {
		public S4Impl1() {}
	}
	static class S5Impl1 implements S5 {
		public S5Impl1() {}
	}
	static class S6S7Impl implements S6, S7 {
		public S6S7Impl() {}
	}
	static class S8Impl1 implements S8 {
		public S8Impl1() {}
	}

}
