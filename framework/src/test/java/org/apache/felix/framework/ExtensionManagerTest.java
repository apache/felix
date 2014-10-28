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
package org.apache.felix.framework;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.framework.util.FelixConstants;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleCapability;

/**
 * 
 * Test Classes for the ExtentionManager
 *
 */
public class ExtensionManagerTest {

	/**
	 * 
	 * 
	 * Ensure Native Bundle Capabilities are properly formed based on
	 * Framework properties.
	 * 
	 */
	@Test
	public void testBuildNativeCapabilities() {
		Logger logger = new Logger();
		Map<String, String> configMap = new HashMap<String, String>();
		configMap.put(FelixConstants.FELIX_VERSION_PROPERTY, "1.0");
		configMap.put(FelixConstants.FRAMEWORK_LANGUAGE, "en");
		configMap.put(FelixConstants.FRAMEWORK_PROCESSOR, "x86_64");
		configMap.put(FelixConstants.FRAMEWORK_OS_NAME, "windows8");
		configMap.put(FelixConstants.FRAMEWORK_OS_VERSION, "6.3");
		ExtensionManager extensionManager = new ExtensionManager(logger,
				configMap, null);
		BundleCapability nativeBundleCapability = extensionManager
				.buildNativeCapabilites();
		assertEquals(
				"Native Language should be same as framework Language",
				"en",
				nativeBundleCapability.getAttributes().get(
						NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE));
		assertEquals(
				"Native Processor should be same as framework Processor",
				"x86_64",
				nativeBundleCapability.getAttributes().get(
						NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE));
		assertEquals(
				"Native OS Name should be the same as the framework os name",
				"windows8",
				nativeBundleCapability.getAttributes().get(
						NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE));
		assertEquals(
				"Native OS Version should be the same as the framework OS Version",
				new Version("6.3"),
				nativeBundleCapability.getAttributes().get(
						NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE));
	}

}
