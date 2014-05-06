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

package org.apache.felix.ipojo.manipulation;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.manipulator.Pojoization;

public class PojoizationTest extends TestCase {

	public void testJarManipulation() {
		Pojoization pojoization = new Pojoization();
        pojoization.setUseLocalXSD();
		File in = new File("target/test-classes/tests.manipulation-no-annotations.jar");
		File out = new File("target/test-classes/tests.manipulation-no-annotations-manipulated.jar");
		out.delete();
		File metadata = new File("target/test-classes/metadata.xml");
		pojoization.pojoization(in, out, metadata, null);

		Assert.assertTrue(out.exists());
	}

	public void testManipulationWithAnnotations() {
		Pojoization pojoization = new Pojoization();
        pojoization.setUseLocalXSD();
		File in = new File("target/test-classes/tests.manipulator-annotations.jar");
		File out = new File("target/test-classes/tests.manipulation-annotations-manipulated.jar");
		out.delete();
		pojoization.pojoization(in, out, (File) null, null);

		Assert.assertTrue(out.exists());
	}

	public void testJarManipulationJava5() {
		Pojoization pojoization = new Pojoization();
        pojoization.setUseLocalXSD();
		File in = new File("target/test-classes/tests.manipulation.java5.jar");
		File out = new File("target/test-classes/tests.manipulation.java5-manipulated.jar");
		out.delete();
		pojoization.pojoization(in, out, (File) null, null);

		Assert.assertTrue(out.exists());
	}

}
