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
package test;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.apache.felix.dm.impl.index.multiproperty.MultiPropertyFilterIndex;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class MultiPropertyFilterIndexPerformanceTest {

	final int testSize = 5000;

	final int iterations = 10;
	
	@Test
	public void MultiPropertyFilterIndexTest() {
		MultiPropertyFilterIndex stringIndex;

		for (int i = 0; i < iterations; i++) {
			stringIndex = new MultiPropertyFilterIndex("component-identifier,model,concept,role");
			testPerformance(stringIndex, testSize, "bystring");
		}
	}

	
	@SuppressWarnings("rawtypes")
	private void testPerformance(MultiPropertyFilterIndex filterIndex, int runSize, String indexName) {
		System.gc();
		long start = System.currentTimeMillis();
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		for (int i = 0; i < runSize; i++) {
			TestReference newReference = new TestReference();
			String[] multiValue = { "CREATES", "UPDATES" };
			String[] mv2 = { "extra1", "extra2", "extra3" };

			newReference.addProperty("component-identifier",
					"org.acme.xyz.platform.interfaces.modeldrivenservices.DialogService");
			newReference.addProperty("model", "//Housing benefit request/Housing benefit request.model" + i);
			newReference.addProperty("concept",
					"//Housing benefit request/2000 Requests/40000/Housing benefit request.model#concept" + i);
			newReference.addProperty("role", multiValue);
			newReference.addProperty("extra", mv2);

			filterIndex.addedService(newReference, new Object());
		}

		double writeTime = ((System.currentTimeMillis() - start) / 1000.0);
		long startReading = System.currentTimeMillis();

		for (int i = 0; i < runSize; i++) {
			List<ServiceReference> allServiceReferences = filterIndex.getAllServiceReferences(null,
					"(&(component-identifier=org.acme.xyz.platform.interfaces.modeldrivenservices.DialogService)"
							+ "(model=//Housing benefit request/Housing benefit request.model" + i + ")"
							+ "(concept=//Housing benefit request/2000 Requests/40000/Housing benefit request.model#concept"
							+ i + ")" + "(role=CREATES))");

			if (allServiceReferences.size() != 1) {
				throw new AssertionError("Failed to find reference in cache");
			}

		}

		// Sanitiy check
		List<ServiceReference> allServiceReferences = filterIndex.getAllServiceReferences(null,
				"(&(model=mymodel1)(concept=abracadabrra)(role=CREATES))");
		if (allServiceReferences.size() != 0) {
			assertFalse(allServiceReferences.size() != 0);
		}

		double readTime = ((System.currentTimeMillis() - startReading) / 1000.0);

		System.gc();
		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		double consumed = (memoryAfter - memoryBefore) / 1048576.0;

		System.err.println("w: " + writeTime + ", r: " + readTime + ",m: " + consumed + " # of iterations: " + runSize + " -- "
				+ indexName);
	}


}
