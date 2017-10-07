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
package org.apache.felix.fileinstall.plugins.resolver.impl;

import java.net.URI;

import junit.framework.TestCase;

public class URIUtilsTest extends TestCase {
	public void testResolveRelativeHttpUri() {
		URI resolved = URIUtils.resolve(URI.create("http://example.org/repository/index.xml"), URI.create("bundle/bundle.jar"));
		assertEquals("http://example.org/repository/bundle/bundle.jar", resolved.toString());
	}

	public void testResolveAbsoluteFileBaseUri() {
		URI resolved = URIUtils.resolve(URI.create("file:/Users/bob/repository/index.xml"), URI.create("bundle/bundle.jar"));
		assertEquals("file:/Users/bob/repository/bundle/bundle.jar", resolved.toString());
	}

	public void testResolveJarUri() throws Exception {
		URI base = new URI("jar:http://example.com/systems/example.jar!/sys/system.xml");

		// Sibling resource within the JAR
		assertEquals("jar:http://example.com/systems/example.jar!/sys/repo/index-nim.xml", URIUtils.resolve(base, new URI("repo/index-nim.xml")).toString());

		// Move up a path within the JAR
		assertEquals("jar:http://example.com/systems/example.jar!/repo/index-nim.xml", URIUtils.resolve(base, new URI("../repo/index-nim.xml")).toString());

		// Here's the tricky bit... move up a path *outside* the JAR
		assertEquals("http://example.com/systems/index.xml", URIUtils.resolve(base, new URI("../../index.xml")).toString());
		assertEquals("http://example.com/repos/index.xml", URIUtils.resolve(base, new URI("../../../repos/index.xml")).toString());
		
		// Even trickier... move up a path to another JAR, and then into it
		assertEquals("jar:http://example.com/systems/other.jar!/index.xml", URIUtils.resolve(base, new URI("../../other.jar!/index.xml")).toString());
	}
	
	public void testResolveJarUriNoNavigateUp() throws Exception {
		URI base = new URI("jar:http://example.com/systems/example.jar!/sys/system.xml?navigate=false");

		// Sibling resource within the JAR
		assertEquals("jar:http://example.com/systems/example.jar!/sys/repo/index-nim.xml", URIUtils.resolve(base, new URI("repo/index-nim.xml")).toString());

		// Move up a path within the JAR
		assertEquals("jar:http://example.com/systems/example.jar!/repo/index-nim.xml", URIUtils.resolve(base, new URI("../repo/index-nim.xml")).toString());

		// Moving outside the JAR is not allowed
		try {
			URIUtils.resolve(base, new URI("../../index.xml"));
			fail("Should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testGetFileName() {
		assertEquals("system.xml", URIUtils.getFileName(URI.create("http://www.example.com/foo/bar/system.xml")));
		assertEquals("system.xml", URIUtils.getFileName(URI.create("jar:http://www.example.com/foo/bar/system.jar!/bar/foo/system.xml")));

		try {
			URIUtils.getFileName(URI.create("blah:this/could/be/anything.xml"));
			fail("Should throw IllArgExc");
		} catch (IllegalArgumentException e) {
			// expected
		}

		try {
			URIUtils.getFileName(URI.create("jar:http://www.example.com/foo/bar/system.jar"));
			fail("Should throw IllArgExc");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
}
