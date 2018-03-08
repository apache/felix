/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.installer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.felix.fileinstall.plugins.installer.impl.DeploymentInstaller;
import org.junit.Before;
import org.junit.Test;

public class CompositeBundleInstallerTest {

	private TestLogService log;
	private DeploymentInstaller installer;
	
	@Before
	public void setup() {
		log = new TestLogService();
		installer = new DeploymentInstaller();
		installer.log = log;
	}

	@Test
	public void testValidArchiveCanBeHandled() {
		File jar = new File("target/test-classes/valid1.bar");

		assertTrue("test archive not found", jar.isFile());
		assertTrue("test archive not handled", installer.canHandle(jar));
		
		assertEquals(1, log.logged.size());
		assertTrue("missing info message", log.logged.get(0).startsWith("INFO: Detected valid bundle archive"));
	}
	
	@Test
	public void testIndexPathInManifest() {
		File jar = new File("target/test-classes/valid2.bar");

		assertTrue("test archive not found", jar.isFile());
		assertTrue("test archive not handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing info message", log.logged.get(0).startsWith("INFO: Detected valid bundle archive"));
	}
	
	@Test
	public void testInvalidExtension() {
		File jar = new File("target/test-classes/invalid-wrong-extension.xxx");

		assertTrue("test archive not found", jar.isFile());
		assertFalse("invalid test archive should not be handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing debug message", log.logged.get(0).startsWith("DEBUG: Ignoring"));
	}

	@Test
	public void testMissingRequireHeader() {
		File jar = new File("target/test-classes/invalid-missing-requires.bar");

		assertTrue("test archive not found", jar.isFile());
		assertFalse("invalid test archive should not be handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing warning message", log.logged.get(0).startsWith("WARNING: Not a valid bundle archive"));
	}

	@Test
	public void testNoIndex() {
		File jar = new File("target/test-classes/invalid-no-index.bar");

		assertTrue("test archive not found", jar.isFile());
		assertFalse("invalid test archive should not be handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing warning message", log.logged.get(0).startsWith("WARNING: Not a valid bundle archive"));
	}

	@Test
	public void testMissingIndexPath() {
		File jar = new File("target/test-classes/invalid-missing-index-path.bar");

		assertTrue("test archive not found", jar.isFile());
		assertFalse("invalid test archive should not be handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing warning message", log.logged.get(0).startsWith("WARNING: Not a valid bundle archive"));
	}

}
