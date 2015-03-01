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
package org.apache.felix.dm.itest.api;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX4158_DependencyDeclarationTest extends TestBase {
    public void testServiceDependencyDeclaration() {
        DependencyManager m = getDM();
        Component c = m.createComponent()
            .setImplementation(new Object())
            .add(m.createServiceDependency().setService(LogService.class, "(foo=bar)"));
        
        ComponentDeclaration cd = c.getComponentDeclaration();
        ComponentDependencyDeclaration[] cdds = cd.getComponentDependencies();
        Assert.assertNotNull(cdds);
        Assert.assertNotNull(cdds.length == 1);
        Assert.assertEquals(cdds[0].getName(), "org.osgi.service.log.LogService (foo=bar)");
        Assert.assertEquals(cdds[0].getSimpleName(), "org.osgi.service.log.LogService");
        Assert.assertNotNull(cdds[0].getFilter());
        Assert.assertEquals(cdds[0].getFilter(), "(foo=bar)");
        m.clear();
    }
    
    public void testConfigurationDependencyDeclaration() {
        DependencyManager m = getDM();
        Component c = m.createComponent()
            .setImplementation(new Object())
            .add(m.createConfigurationDependency().setPid("foo"));
        
        ComponentDeclaration cd = c.getComponentDeclaration();
        ComponentDependencyDeclaration[] cdds = cd.getComponentDependencies();
        Assert.assertNotNull(cdds);
        Assert.assertNotNull(cdds.length == 1);
        Assert.assertEquals(cdds[0].getName(), "foo");
        Assert.assertEquals(cdds[0].getSimpleName(), "foo");
        Assert.assertNull(cdds[0].getFilter());
        m.clear();
    }
    
    public void testResourceDependencyDeclaration() throws MalformedURLException {
        DependencyManager m = getDM();
        Component c = m.createComponent()
            .setImplementation(new Object())
            .add(m.createResourceDependency()
                  .setResource(new URL("file://localhost/path/to/file1.txt")));
        
        ComponentDeclaration cd = c.getComponentDeclaration();
        ComponentDependencyDeclaration[] cdds = cd.getComponentDependencies();
        Assert.assertNotNull(cdds);
        Assert.assertNotNull(cdds.length == 1);
        Assert.assertEquals(cdds[0].getName(), "file://localhost/path/to/file1.txt");
        Assert.assertNotNull(cdds[0].getSimpleName());
        Assert.assertEquals(cdds[0].getSimpleName(), "file://localhost/path/to/file1.txt");
        Assert.assertNull(cdds[0].getFilter());
        m.clear();
    }
    
    public void testResourceDependencyDeclarationWithFilter() {
        DependencyManager m = getDM();
        Component c = m.createComponent()
            .setImplementation(new Object())
            .add(m.createResourceDependency()
                .setFilter("(&(path=/path/to/*.txt)(host=localhost))"));
        
        ComponentDeclaration cd = c.getComponentDeclaration();
        ComponentDependencyDeclaration[] cdds = cd.getComponentDependencies();
        Assert.assertNotNull(cdds);
        Assert.assertNotNull(cdds.length == 1);
        Assert.assertEquals(cdds[0].getName(), ("(&(path=/path/to/*.txt)(host=localhost))"));
        Assert.assertNull(cdds[0].getSimpleName());
        Assert.assertNotNull(cdds[0].getFilter());
        Assert.assertEquals(cdds[0].getFilter(), "(&(path=/path/to/*.txt)(host=localhost))");
        m.clear();
    }
    
    public void testBundleDependencyDeclaration() throws MalformedURLException {
        DependencyManager m = getDM();
        Component c = m.createComponent()
            .setImplementation(new Object())
            .add(m.createBundleDependency());
        
        ComponentDeclaration cd = c.getComponentDeclaration();
        ComponentDependencyDeclaration[] cdds = cd.getComponentDependencies();
        Assert.assertNotNull(cdds);
        Assert.assertNotNull(cdds.length == 1);
        Assert.assertEquals(cdds[0].getName(), "active installed resolved");
        Assert.assertNotNull(cdds[0].getSimpleName());
        Assert.assertEquals(cdds[0].getSimpleName(), "active installed resolved");
        Assert.assertNull(cdds[0].getFilter());
        m.clear();
    }

    public void testBundleDependencyDeclarationWithMask() throws MalformedURLException {
        DependencyManager m = getDM();
        Component c = m.createComponent()
            .setImplementation(new Object())
            .add(m.createBundleDependency()
                .setStateMask( Bundle.ACTIVE | Bundle.RESOLVED));
        
        ComponentDeclaration cd = c.getComponentDeclaration();
        ComponentDependencyDeclaration[] cdds = cd.getComponentDependencies();
        Assert.assertNotNull(cdds);
        Assert.assertNotNull(cdds.length == 1);
        Assert.assertEquals(cdds[0].getName(), "active resolved");
        Assert.assertNotNull(cdds[0].getSimpleName());
        Assert.assertEquals(cdds[0].getSimpleName(), "active resolved");
        Assert.assertNull(cdds[0].getFilter());
        m.clear();
    }
    
    public void testBundleDependencyDeclarationWithFilter() throws MalformedURLException {
        DependencyManager m = getDM();
        Component c = m.createComponent()
            .setImplementation(new Object())
            .add(m.createBundleDependency()
                .setStateMask( Bundle.ACTIVE )
                .setFilter("(DependencyManager-Component=*)"));
        
        ComponentDeclaration cd = c.getComponentDeclaration();
        ComponentDependencyDeclaration[] cdds = cd.getComponentDependencies();
        Assert.assertNotNull(cdds);
        Assert.assertNotNull(cdds.length == 1);
        Assert.assertEquals(cdds[0].getName(), "active (DependencyManager-Component=*)");
        Assert.assertNotNull(cdds[0].getSimpleName());
        Assert.assertEquals(cdds[0].getSimpleName(), "active");
        Assert.assertNotNull(cdds[0].getFilter());
        Assert.assertEquals(cdds[0].getFilter(), "(DependencyManager-Component=*)");
        m.clear();
    }
}
