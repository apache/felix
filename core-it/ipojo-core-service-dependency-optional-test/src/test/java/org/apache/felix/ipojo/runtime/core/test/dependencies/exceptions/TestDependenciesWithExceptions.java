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

package org.apache.felix.ipojo.runtime.core.test.dependencies.exceptions;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Checks the behavior of the exception attribute.
 */
public class TestDependenciesWithExceptions extends Common {

    ComponentInstance consumer;
    ComponentInstance provider;

    @Before
    public void setUp() {
        try {
            Properties prov = new Properties();
            prov.put("instance.name", "FooProvider");
            provider = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov);
            provider.stop();

            Properties i1 = new Properties();
            i1.put("instance.name", "Consumer");
            consumer = ipojoHelper
                    .getFactory("org.apache.felix.ipojo.runtime.core.test.components.exceptions.ExceptionAwareCheckServiceProvider")
                    .createComponentInstance(i1);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {
        consumer.dispose();
        provider.dispose();
        consumer = null;
        provider = null;
    }

    /**
     * A simple test checking that the exception is thrown.
     */
    @Test
    public void testExceptionWhenServiceUnavailable() {
        assertNull(osgiHelper.getServiceObject(FooService.class));
        osgiHelper.waitForService(CheckService.class, "(instance.name=" + consumer.getInstanceName() + ")", 1000);
        CheckService cs = osgiHelper.getServiceObject(CheckService.class,
                "(instance.name=" + consumer.getInstanceName()+")");
        assertNotNull(cs);

        // the exception is caught by the implementation, and false is returned.
        assertFalse(cs.check());

        // we start the provider.
        provider.start();
        assertNotNull(osgiHelper.getServiceObject(FooService.class));

        // This time everything is fine.
        assertTrue(cs.check());
    }


}
