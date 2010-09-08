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
package org.apache.felix.ipojo.test.scenarios.service.dependency;

import java.util.AbstractMap;
import java.util.HashMap;

import junit.framework.Assert;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.osgi.framework.ServiceRegistration;

public class NonProxiedNotInterfaceDependencies extends OSGiTestCase {

    InstanceManager instance1;
    ComponentInstance fooProvider;

    ServiceRegistration reg, reg2;
    IPOJOHelper helper;

    public void setUp() {
    	helper = new IPOJOHelper(this);
    	reg = context.registerService(String.class.getName(), "ahahah", null);
    	reg2 = context.registerService(AbstractMap.class.getName(), new HashMap(), null);

    }

    public void tearDown() {
    	if (reg != null) {
    		reg.unregister();
    	}
    	if (reg2 != null) {
    		reg2.unregister();
    	}
    }

    public void testInstanceCreation() {
    	instance1 = (InstanceManager) helper
    		.createComponentInstance("org.apache.felix.ipojo.test.scenarios.service.dependency.proxy.CheckServiceUsingStringService");
    	Assert.assertTrue(instance1.getState() == ComponentInstance.VALID);
    	Assert.assertTrue(((CheckService) instance1.getPojoObject()).check());
    }

}
