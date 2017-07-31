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
package org.apache.felix.dm.index.itest.tests;
//import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartupFor;
//import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import org.junit.Assert;

import java.util.function.Consumer;

import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX3057_EmptyServiceReferenceArray extends TestBase {
	
	private String m_systemConf;

    @SuppressWarnings("unchecked")
	public void testWithoutIndex() throws Exception {
        // backup currently configured filter index
        BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    	m_systemConf = context.getProperty(DependencyManager.SERVICEREGISTRY_CACHE_INDICES);

    	// Reset filter indices    	
    	Consumer<String> reset = (Consumer<String>) System.getProperties().get("org.apache.felix.dependencymanager.filterindex.reset");
        reset.accept(null); // clear filter index
        
        executeTest(context); // no filter index used
        
        reset.accept(m_systemConf); // reset filer index configured in our bnd.bnd
    }
    
    public void testWithIndex() throws Exception {
        executeTest(context); // SERVICEREGISTRY_CACHE_INDICES system property is configured in our bnd.bnd  
    }

    private void executeTest(BundleContext context) throws InvalidSyntaxException {
        DependencyManager m = new DependencyManager(context);
        Assert.assertNull("Looking up a non-existing service should return null.", m.getBundleContext().getServiceReferences(Service.class.getName(), "(objectClass=*)"));
        Assert.assertNull("Looking up a non-existing service should return null.", m.getBundleContext().getAllServiceReferences(Service.class.getName(), "(objectClass=*)"));
        Assert.assertNull("Looking up a non-existing service should return null.", m.getBundleContext().getServiceReference(Service.class.getName()));
        m.clear();
    }

    /** Dummy interface for lookup. */
    public static interface Service {}
}
