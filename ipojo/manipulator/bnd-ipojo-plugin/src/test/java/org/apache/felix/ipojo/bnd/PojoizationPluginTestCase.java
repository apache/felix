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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import java.util.HashMap;
import java.util.Map;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.URLResource;
import aQute.service.reporter.Reporter;
import junit.framework.TestCase;

public class PojoizationPluginTestCase extends TestCase {

    @Mock
    private Reporter reporter;

    @Spy
    private Analyzer analyzer = new Analyzer();

    @Spy
    private Jar jar = new Jar("mock.jar");

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testAnalysisWithComponentOnlyMetadataXml() throws Exception {
        PojoizationPlugin plugin = new PojoizationPlugin();

        Map<String, String> props = new HashMap<String, String>();

        Resource resource = new URLResource(getClass().getResource("/metadata-components-only.xml"));
        doReturn(jar).when(analyzer).getJar();
        doReturn(resource).when(jar).getResource(eq("META-INF/metadata.xml"));

        plugin.setReporter(reporter);
        plugin.setProperties(props);

        plugin.analyzeJar(analyzer);

        assertEquals("component { $class=\"com.acme.Thermometer\" }",
                analyzer.getProperty("IPOJO-Components"));
    }

    public void testAnalysisWithInstanceOnlyMetadataXml() throws Exception {
        PojoizationPlugin plugin = new PojoizationPlugin();

        Map<String, String> props = new HashMap<String, String>();

        Resource resource = new URLResource(getClass().getResource("/metadata-instances-only.xml"));
        doReturn(jar).when(analyzer).getJar();
        doReturn(resource).when(jar).getResource(eq("META-INF/metadata.xml"));

        plugin.setReporter(reporter);
        plugin.setProperties(props);

        plugin.analyzeJar(analyzer);

        assertEquals("instance { $component=\"com.acme.Thermometer\" }",
                analyzer.getProperty("IPOJO-Components"));
    }

    public void testAnalysisWithComponentsAndInstancesMetadataXml() throws Exception {
        PojoizationPlugin plugin = new PojoizationPlugin();

        Map<String, String> props = new HashMap<String, String>();

        Resource resource = new URLResource(getClass().getResource("/metadata-components-and-instances.xml"));
        doReturn(jar).when(analyzer).getJar();
        doReturn(resource).when(jar).getResource(eq("META-INF/metadata.xml"));

        plugin.setReporter(reporter);
        plugin.setProperties(props);

        plugin.analyzeJar(analyzer);

        assertEquals("component { $class=\"com.acme.Thermometer\" }" +
                     "instance { $component=\"com.acme.Thermometer\" }" +
                     "instance { $component=\"com.acme.Thermometer\" }",
                     analyzer.getProperty("IPOJO-Components"));
    }
}
