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
package org.apache.felix.dependencymanager.test2.integration.api;
//import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartupFor;
//import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;


import org.ops4j.pax.exam.junit.PaxExam;
import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;

@RunWith(PaxExam.class)
public class FELIX3057_EmptyServiceReferenceArray extends TestBase {
    @Test
    public void testWithoutIndex() throws Exception {
        executeTest(context);
    }
    
    @Test
    public void testWithIndex() throws Exception {
        System.setProperty(DependencyManager.SERVICEREGISTRY_CACHE_INDICES, "objectClass");
        executeTest(context);
    }

    private void executeTest(BundleContext context) throws InvalidSyntaxException {
        DependencyManager m = new DependencyManager(context);
        Assert.assertNull("Looking up a non-existing service should return null.", m.getBundleContext().getServiceReferences(Service.class.getName(), "(objectClass=*)"));
        Assert.assertNull("Looking up a non-existing service should return null.", m.getBundleContext().getAllServiceReferences(Service.class.getName(), "(objectClass=*)"));
        Assert.assertNull("Looking up a non-existing service should return null.", m.getBundleContext().getServiceReference(Service.class.getName()));
    }

    /** Dummy interface for lookup. */
    public static interface Service {}
}
