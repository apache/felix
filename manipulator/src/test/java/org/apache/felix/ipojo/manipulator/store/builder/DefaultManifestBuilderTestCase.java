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

package org.apache.felix.ipojo.manipulator.store.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.store.ManifestBuilder;
import org.apache.felix.ipojo.manipulator.store.builder.DefaultManifestBuilder;
import org.apache.felix.ipojo.manipulator.util.Constants;

public class DefaultManifestBuilderTestCase extends TestCase {

    private static final String ORG_OSGI_FRAMEWORK_VERSION_1_5 = "org.osgi.framework;version=1.5";
    private Manifest manifest;
    private ManifestBuilder builder;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue("Import-Package", ORG_OSGI_FRAMEWORK_VERSION_1_5);
        attributes.putValue("Created-By", "TestCase");

        builder = new DefaultManifestBuilder();
        builder.addReferredPackage(Collections.singleton("org.osgi.service.http"));
    }

    public void testManifestIsModified() throws Exception {
        Manifest modified = builder.build(manifest);

        // Created by header was properly modified
        Assert.assertEquals("TestCase & iPOJO " + Constants.getVersion(),
                            modified.getMainAttributes().getValue("Created-By"));

        // As there was no metadata provided, no iPOJO-Components header should be present
        Assert.assertNull(modified.getMainAttributes().getValue("iPOJO-Components"));

        // Check that default manipulation introduced packages are present
        String imported = modified.getMainAttributes().getValue("Import-Package");
        Assert.assertTrue(imported.contains(ORG_OSGI_FRAMEWORK_VERSION_1_5));
        Assert.assertTrue(imported.contains("org.apache.felix.ipojo"));
        Assert.assertTrue(imported.contains("org.apache.felix.ipojo.architecture"));
        Assert.assertTrue(imported.contains("org.osgi.service.cm"));
        Assert.assertTrue(imported.contains("org.osgi.service.log"));
    }


    public void testCreatedByHeaderDoesNotContainIPojoTwice() throws Exception {

        manifest.getMainAttributes().putValue("Created-By", "TestCase for iPOJO");

        Manifest modified = builder.build(manifest);

        // Created by header was properly not changed
        Assert.assertEquals("TestCase for iPOJO",
                            modified.getMainAttributes().getValue("Created-By"));
    }

    public void testReferredPackagesAreProperlyAdded() throws Exception {

        List<String> p = Arrays.asList("org.apache.felix.ipojo.test", "org.apache.felix.ipojo.log");
        builder.addReferredPackage(new HashSet<String>(p));

        Manifest modified = builder.build(manifest);

        // Check that referred packages are present
        String imported = modified.getMainAttributes().getValue("Import-Package");
        Assert.assertTrue(imported.contains("org.apache.felix.ipojo.test"));
        Assert.assertTrue(imported.contains("org.apache.felix.ipojo.log"));
    }

}
