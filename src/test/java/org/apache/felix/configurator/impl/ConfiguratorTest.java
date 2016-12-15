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
package org.apache.felix.configurator.impl;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Requirement;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfiguratorTest {

    private Configurator configurator;

    private BundleContext bundleContext;

    private ConfigurationAdmin configurationAdmin;

    @Before public void setup() throws IOException {
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(new Bundle[0]);
        when(bundleContext.getDataFile("binaries" + File.separatorChar + ".check")).thenReturn(Files.createTempDirectory("test").toFile());
        configurationAdmin = mock(ConfigurationAdmin.class);

        configurator = new Configurator(bundleContext, configurationAdmin);
    }

    private Bundle setupBundle(final long id) {
        final Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(id);
        when(b.getLastModified()).thenReturn(5L);
        final BundleRevision rev = mock(BundleRevision.class);
        when(b.adapt(BundleRevision.class)).thenReturn(rev);
        final Requirement req = mock(Requirement.class);
        when(rev.getRequirements(Util.NS_OSGI_IMPL)).thenReturn(Collections.singletonList(req));
        final Vector<URL> urls = new Vector<>();
        urls.add(this.getClass().getResource("/bundles/" + id + ".json"));
        when(b.findEntries("OSGI-INF/configurator", "*.json", false)).thenReturn(urls.elements());

        return b;
    }

    @Test public void testSimpleAddRemove() throws Exception {
        final Bundle b = setupBundle(1);

        Configuration c1 = mock(Configuration.class);
        Configuration c2 = mock(Configuration.class);
        when(configurationAdmin.getConfiguration("a", "?")).thenReturn(c1);
        when(configurationAdmin.getConfiguration("b", "?")).thenReturn(c2);

        when(c1.getChangeCount()).thenReturn(1L);
        when(c2.getChangeCount()).thenReturn(1L);
        configurator.processAddBundle(b);

        configurator.process();

        when(configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=a)")).thenReturn(new Configuration[] {c1});
        when(configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=b)")).thenReturn(new Configuration[] {c2});

        final Dictionary<String, Object> props1 = new Hashtable<>();
        props1.put("foo", "bar");
        verify(c1).setProperties(props1);
        final Dictionary<String, Object> props2 = new Hashtable<>();
        props2.put("x", "y");
        verify(c2).setProperties(props2);

        configurator.processRemoveBundle(1);
        configurator.process();

        verify(c1).delete();
        verify(c2).delete();
    }

    @Test public void testSimpleRankingRemove() throws Exception {
        final Bundle b1 = setupBundle(1);
        final Bundle b2 = setupBundle(2);

        Configuration c1 = mock(Configuration.class);
        Configuration c2 = mock(Configuration.class);
        when(configurationAdmin.getConfiguration("a", "?")).thenReturn(c1);
        when(configurationAdmin.getConfiguration("b", "?")).thenReturn(c2);

        when(c1.getChangeCount()).thenReturn(1L);
        when(c2.getChangeCount()).thenReturn(1L);
        configurator.processAddBundle(b2);
        configurator.process();

        when(configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=a)")).thenReturn(new Configuration[] {c1});
        when(configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=b)")).thenReturn(new Configuration[] {c2});

        final Dictionary<String, Object> props1 = new Hashtable<>();
        props1.put("foo", "bar2");
        final Dictionary<String, Object> props2 = new Hashtable<>();
        props2.put("x", "y2");

        configurator.processAddBundle(b1);
        configurator.process();

        final Dictionary<String, Object> props3 = new Hashtable<>();
        props3.put("foo", "bar");
        final Dictionary<String, Object> props4 = new Hashtable<>();
        props4.put("x", "y");

        configurator.processRemoveBundle(1);
        configurator.process();

        configurator.processRemoveBundle(2);
        configurator.process();

        InOrder inorder = inOrder(c1, c2);
        inorder.verify(c1).setProperties(props1);
        inorder.verify(c2).setProperties(props2);
        inorder.verify(c1).setProperties(props3);
        inorder.verify(c2).setProperties(props4);
        inorder.verify(c1).setProperties(props1);
        inorder.verify(c2).setProperties(props2);
        inorder.verify(c1).delete();
        inorder.verify(c2).delete();
        inorder.verifyNoMoreInteractions();
    }
}
