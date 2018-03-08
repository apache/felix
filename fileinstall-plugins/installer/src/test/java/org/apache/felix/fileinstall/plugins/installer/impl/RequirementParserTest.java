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

import static org.junit.Assert.*;

import java.util.List;

import org.apache.felix.fileinstall.plugins.installer.impl.RequirementParser;
import org.junit.Test;
import org.osgi.resource.Requirement;

public class RequirementParserTest {

	@Test
	public void testParseRequireBundle() {
		List<Requirement> actual = RequirementParser.parseRequireBundle("foo;bundle-version=1.0.0, bar;bundle-version=\"[1.0,1.1)\", baz;bundle-version=\"[2.0,3.0)\", fnarg");
		
		assertEquals(4, actual.size());
		assertEquals("(&(osgi.wiring.bundle=foo)(bundle-version>=1.0.0))", actual.get(0).getDirectives().get("filter"));
		assertEquals("(&(osgi.wiring.bundle=bar)(bundle-version>=1.0.0)(!(bundle-version>=1.1.0)))", actual.get(1).getDirectives().get("filter"));
		assertEquals("(&(osgi.wiring.bundle=baz)(bundle-version>=2.0.0)(!(bundle-version>=3.0.0)))", actual.get(2).getDirectives().get("filter"));
		assertEquals("(osgi.wiring.bundle=fnarg)", actual.get(3).getDirectives().get("filter"));
	}

	@Test
	public void testParseRequireCapability() {
		List<Requirement> actual = RequirementParser.parseRequireCapability("osgi.extender; filter:=\"(&(osgi.extender=osgi.ds)(version>=1.0))\"; effective:=active, osgi.service; filter:=\"(objectClass=org.example.Foo)\"");
		
		assertEquals(2, actual.size());
		assertEquals("(&(osgi.extender=osgi.ds)(version>=1.0))", actual.get(0).getDirectives().get("filter"));
		assertEquals("active", actual.get(0).getDirectives().get("effective"));
	}

}
