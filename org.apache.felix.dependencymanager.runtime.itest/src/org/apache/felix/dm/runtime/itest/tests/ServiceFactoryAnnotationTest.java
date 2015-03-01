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
package org.apache.felix.dm.runtime.itest.tests;

import java.util.Hashtable;
import java.util.Set;

import org.junit.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.apache.felix.dm.runtime.itest.components.ServiceFactoryAnnotation;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ServiceFactoryAnnotationTest extends TestBase {
    
    private final Ensure m_ensure = new Ensure();

    public void testServiceFactory() {
        ServiceRegistration sr = register(m_ensure, ServiceFactoryAnnotation.ENSURE);

        DependencyManager m = new DependencyManager(context);
        // Wait for the factory.
        m.add(m.createComponent()
                .setImplementation(this)
                .add(m.createServiceDependency()
                        .setService(Set.class,
                                "(" + Component.FACTORY_NAME + "=" + ServiceFactoryAnnotation.FACTORY + ")")
                        .setRequired(true).setCallbacks("bindFactory", null)));

        // Check if the test.annotation components have been initialized orderly
        m_ensure.waitForStep(10, 5000);
        m.clear();
        sr.unregister();
    }

    void bindFactory(Set factory) {
        // create a service instance with this configuration
        Hashtable conf = new Hashtable();
        conf.put("instance.id", "instance");
        conf.put(".private.param", "private");
        Assert.assertTrue(factory.add(conf));
        m_ensure.waitForStep(4, 5000);

        // update the service instance
        conf.put("instance.modified", "true");
        Assert.assertFalse(factory.add(conf));
        m_ensure.waitForStep(7, 5000);

        // remove instance
        Assert.assertTrue(factory.remove(conf));
        Assert.assertFalse(factory.remove(conf));
    }
}
