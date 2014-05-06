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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.junit.Test;

import test.AnnotatedComponent;

import junit.framework.TestCase;

/**
 * Test cases for Pojoization.directoryManipulation() 
 *
 */
public class DirManipulationTest extends TestCase {

	/**
	 * Test case for FELIX-3466.
	 * 
	 * Checks if directory manipulation, uses the supplied manifest file as
	 * output.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testManifestLocationKept() throws IOException {
		Pojoization pojoizator = new Pojoization();
		File tmpDir = null, manifestFile = null, testClass = null;

		// To obtain OS's temp directory.
		File tmpFile = File.createTempFile("pojodir", ".dir");
		String tmpPath = tmpFile.getAbsolutePath();
		tmpFile.delete();

		// Creating directory on temp location
		tmpDir = new File(tmpPath);
		tmpDir.mkdir();
		tmpDir.deleteOnExit();

		// Create manifest file under temp directory
		manifestFile = new File(tmpDir, "MANIFEST.MF");
		new FileOutputStream(manifestFile).write("Manifest-Version: 1.0\r\n"
				.getBytes());
		manifestFile.deleteOnExit();

		// Just to ensure it is not deleted later from test classes.
		AnnotatedComponent safe;

		// Annotated Class File
		File annotedClassPackage = new File(tmpDir, "test");
		annotedClassPackage.deleteOnExit();
		annotedClassPackage.mkdir();
		testClass = new File(annotedClassPackage, "AnnotatedComponent.class");
		testClass.deleteOnExit();
		FileOutputStream os = new FileOutputStream(testClass);
		os.write(ManipulatorTest.getBytesFromFile(new File(
				"target/test-classes/test/AnnotatedComponent.class")));
		os.close();

		// Issue directory manipulation
		pojoizator.directoryPojoization(tmpDir, null, manifestFile, null);

		// Check if supplied manifest file is altered in place
		BufferedReader fi = new BufferedReader(new FileReader(manifestFile));
		String manifestLine;
		while ((manifestLine = fi.readLine()) != null) {
			if (manifestLine.contains("iPOJO-Components")) {
				assertTrue(true);
				return;
			}
		}

		assertTrue(
				"Directory Manipulation didn't use supplied manifest file as output",
				false);
	}
}
