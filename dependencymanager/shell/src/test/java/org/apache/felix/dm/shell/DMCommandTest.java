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
package org.apache.felix.dm.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class DMCommandTest {
    
    /** Setup a ByteArrayOutputStream to capture the system out printlines */
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    
    private DependencyManager dm;
    @Spy @InjectMocks private DMCommand dme;
    @Mock private BundleContext m_bundleContext;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        m_bundleContext = mock(BundleContext.class);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        dm = new DependencyManager(m_bundleContext);
        dme = new DMCommand(m_bundleContext);
        DependencyManager.getDependencyManagers().add(dm);
    }

    @After
    public void cleanUp() {
        System.setOut(null);
        System.setErr(null);
        DependencyManager.getDependencyManagers().remove(dm);
    }

    @Test
    public void testWithoutAnyDependcyManagersShouldNotCrash() {
        setupEmptyBundles();
        
        dme.wtf();
        assertEquals("No missing dependencies found.\n", outContent.toString());
    }

    @Test
    public void testASingleComponentShouldNotRegisterAsFailure() {
        setupEmptyBundles();
        
        dm.add(dm.createComponent()
            .setImplementation(Object.class)
            .setInterface(Object.class.getName(), null)
            );
        dme.wtf();
        assertEquals("No missing dependencies found.\n", outContent.toString());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testComponentThatDependsOnAOtheComponentShouldRegisterAsFailure() {
        setupEmptyBundles();
        DependencyManager dm = new DependencyManager(m_bundleContext);
        DependencyManager.getDependencyManagers().add(dm);
        
        Component component = dm.createComponent()
            .setImplementation(Object.class)
            .setInterface(Object.class.getName(), null)
            .add(dm.createServiceDependency().setService(Math.class).setRequired(true));
        dm.add(component);
        
        dme.wtf();
        String output = outContent.toString();
        assertTrue(output.contains("1 missing"));
        assertTrue(output.contains("java.lang.Math"));
        
        // remove the mess
        dm.remove(component);
    }
    
    @Test
    public void testCanFindRootFailure() {
        setupEmptyBundles();
        
        Component component1 = dm.createComponent()
            .setImplementation(Object.class)
            .setInterface(Object.class.getName(), null)
            .add(dm.createServiceDependency().setService(Math.class).setRequired(true));
        dm.add(component1);
        
        Component component2 = dm.createComponent()
            .setImplementation(Math.class)
            .setInterface(Math.class.getName(), null)
            .add(dm.createServiceDependency().setService(String.class).setRequired(true));
        dm.add(component2);
        
        dme.wtf();
        String output = outContent.toString();
        assertTrue(output.contains("2 missing"));
        assertTrue(output.contains("java.lang.String"));
        
        // remove the mess
        dm.remove(component1);
        dm.remove(component2);
    }
    
    @Test
    public void testInstalledBundleListing() {
        Bundle bundle1 = mock(Bundle.class);
        when(bundle1.getState()).thenReturn(Bundle.INSTALLED);
        when(bundle1.getSymbolicName()).thenReturn("BadBundle");
        
        setupBundles(bundle1);
        
        dme.wtf();
        String output = outContent.toString();
        assertTrue(output.contains("following bundles are in the INSTALLED"));
        assertTrue(output.contains("[0] BadBundle"));
        // Will print null if it gets bundle 2, that should not happen
        assertFalse(output.contains("null"));
    }
    
    @Test
    public void testResolvedBundleListing() {
        Bundle bundle1 = mock(Bundle.class);
        when(bundle1.getState()).thenReturn(Bundle.RESOLVED);
        when(bundle1.getSymbolicName()).thenReturn("BadBundle");
        Properties headers = new Properties();
        when(bundle1.getHeaders()).thenReturn(headers);
        
        setupBundles(bundle1);
        
        dme.wtf();
        String output = outContent.toString();
        assertTrue(output.contains("following bundles are in the RESOLVED"));
        assertTrue(output.contains("[0] BadBundle"));
        assertFalse(output.contains("null"));
    }
    
    @Test
    public void testResolvedBundleListingButNoFragements() {
        Bundle bundle1 = mock(Bundle.class);
        when(bundle1.getState()).thenReturn(Bundle.RESOLVED);
        when(bundle1.getSymbolicName()).thenReturn("BadBundle");
        Properties headers = new Properties();
        headers.put("Fragment-Host", "some value");
        when(bundle1.getHeaders()).thenReturn(headers);
        setupBundles(bundle1);
        
        dme.wtf();
        String output = outContent.toString();
        assertFalse(output.contains("following bundles are in the RESOLVED"));
        // Will print null if it gets bundle 2, that should not happen
        assertFalse(output.contains("null"));
    }

    private void setupBundles( Bundle bundle1) {
        Bundle bundle2 = mock(Bundle.class);
        when(bundle2.getState()).thenReturn(Bundle.ACTIVE);
        
        when(m_bundleContext.getBundles()).thenReturn(new Bundle[] { bundle1, bundle2});
    }
    
    /** Sets up the bundle context without any bundles */
    private void setupEmptyBundles() {
        when(m_bundleContext.getBundles()).thenReturn(new Bundle[] {});
    }

}
