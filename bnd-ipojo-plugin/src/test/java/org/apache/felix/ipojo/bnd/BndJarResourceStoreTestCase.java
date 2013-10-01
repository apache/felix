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

package org.apache.felix.ipojo.bnd;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.URLResource;
import aQute.service.reporter.Reporter;
import junit.framework.TestCase;

/**
 * Checks the Resource Store from the BND plugin.
 */
public class BndJarResourceStoreTestCase extends TestCase {
    @Mock
    private Reporter reporter;

    @Spy
    private Analyzer analyzer = new Analyzer();

    @Spy
    private Jar dot = new Jar("root.jar");

    @Spy
    private Jar embed = new Jar("embed.jar");

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testAnalysisWithOnlyEmbedComponents() throws Exception {
        PojoizationPlugin plugin = new PojoizationPlugin();

        Map<String, String> props = new HashMap<String, String>();
        props.put("include-embed-bundles", "true");

        Resource resource = new URLResource(getClass().getResource("/EMBED-MANIFEST.MF"));
        doReturn(dot).when(analyzer).getJar();
        doReturn(resource).when(embed).getResource(eq("META-INF/MANIFEST.MF"));
        analyzer.setClasspath(new Jar[] {embed});

        plugin.setReporter(reporter);
        plugin.setProperties(props);

        plugin.analyzeJar(analyzer);

        assertContains("instance { $component=\"org.apache.felix.ipojo.IPOJOURLHandler\" }",
                analyzer.getProperty("IPOJO-Components"));
    }

    public void testAnalysisWithExcludedEmbedComponents() throws Exception {
        PojoizationPlugin plugin = new PojoizationPlugin();

        Map<String, String> props = new HashMap<String, String>();

        Resource resource = new URLResource(getClass().getResource("/EMBED-MANIFEST.MF"));
        doReturn(dot).when(analyzer).getJar();
        doReturn(resource).when(embed).getResource(eq("META-INF/MANIFEST.MF"));
        analyzer.setClasspath(new Jar[] {embed});

        plugin.setReporter(reporter);
        plugin.setProperties(props);

        plugin.analyzeJar(analyzer);

        assertNull(analyzer.getProperty("IPOJO-Components"));
    }


    public void testAnalysisWithBothLocalAndEmbedComponents() throws Exception {
        PojoizationPlugin plugin = new PojoizationPlugin();

        Map<String, String> props = new HashMap<String, String>();
        props.put("include-embed-bundles", "true");

        Resource resource = new URLResource(getClass().getResource("/EMBED-MANIFEST.MF"));
        Resource resource2 = new URLResource(getClass().getResource("/metadata-components-only.xml"));
        doReturn(dot).when(analyzer).getJar();
        doReturn(resource).when(embed).getResource(eq("META-INF/MANIFEST.MF"));
        doReturn(resource2).when(dot).getResource(eq("META-INF/metadata.xml"));
        analyzer.setClasspath(new Jar[]{embed});

        plugin.setReporter(reporter);
        plugin.setProperties(props);

        plugin.analyzeJar(analyzer);

        assertContains("instance { $component=\"org.apache.felix.ipojo.IPOJOURLHandler\" }",
                analyzer.getProperty("IPOJO-Components"));
        assertContains("component { $class=\"com.acme.Thermometer\" }",
                analyzer.getProperty("IPOJO-Components"));
    }


    public void testAnalysisWithLocallyDefinedComponentAndEmbedResource() throws Exception {
        PojoizationPlugin plugin = new PojoizationPlugin();

        Map<String, String> props = new HashMap<String, String>();
        props.put("include-embed-bundles", "true");
        String path = EmptyComponent.class.getName().replace('.', '/').concat(".class");

        Resource resource2 = new URLResource(getClass().getResource("/metadata-test-component.xml"));
        doReturn(dot).when(analyzer).getJar();
        doReturn(resource2).when(dot).getResource(eq("META-INF/metadata.xml"));

        Collection<Clazz> classes = new ArrayList<Clazz>();
        Resource typeResource = new URLResource(getClass().getResource("EmptyComponent.class"));
        Clazz clazz = new Clazz(analyzer, path, typeResource);
        clazz.parseClassFile();
        classes.add(clazz);
        doReturn(classes).when(analyzer).getClasses(Matchers.<String[]>anyVararg());

        Resource resource = new URLResource(getClass().getResource("/EMBED-MANIFEST-EMPTY.MF"));
        doReturn(resource).when(embed).getResource(eq("META-INF/MANIFEST.MF"));
        doReturn(typeResource).when(embed).getResource(path);
        doReturn("aaa").when(embed).getBsn();

        analyzer.setClasspath(new Jar[] {embed});

        plugin.setReporter(reporter);
        plugin.setProperties(props);

        plugin.analyzeJar(analyzer);

        assertContains("component { $classname=\"org.apache.felix.ipojo.bnd.EmptyComponent\" manipulation { $classname=\"org.apache.felix.ipojo.bnd.EmptyComponent\" method { $name=\"$init\" }}}",
                analyzer.getProperty("IPOJO-Components"));
        verify(dot).putResource(eq(path), any(Resource.class));
    }

    private void assertContains(String expected, String actual) {
        System.out.println("Actual: " + actual);
        assertTrue(actual.contains(expected));
    }

}
