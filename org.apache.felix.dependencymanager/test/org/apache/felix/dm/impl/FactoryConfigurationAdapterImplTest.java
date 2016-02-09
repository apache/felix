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
package org.apache.felix.dm.impl;

import static org.mockito.Mockito.mock;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.impl.ConfigurationDependencyImplTest.AManagedService;
import org.apache.felix.dm.impl.ConfigurationDependencyImplTest.FancyService;
import org.apache.felix.dm.impl.ConfigurationDependencyImplTest.MyConfiguration;
import org.apache.felix.dm.impl.ConfigurationDependencyImplTest.PlainService;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ManagedServiceFactory;

import test.Ensure;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FactoryConfigurationAdapterImplTest {

    private static final String CONF_PID = "bogus";

    @Test
    public void testDoNotInvokeFancyUpdatedMethodWithWrongSignatureOk() throws Exception {
        Ensure ensure = createEnsure();
        FancyService service = new FancyService(ensure);

        FactoryConfigurationAdapterImpl cdi = createConfigurationDependency(service, Map.class);
        ensure.step(1);

        ((ManagedServiceFactory) cdi.m_component.getInstance()).updated(CONF_PID, createDictionary());

        TimeUnit.SECONDS.sleep(1L);

        // Our step shouldn't be changed...
        ensure.waitForStep(1, 1000);
    }

    @Test
    public void testInvokeFancyUpdatedMethodOk() throws Exception {
        Ensure ensure = createEnsure();
        FancyService service = new FancyService(ensure);

        FactoryConfigurationAdapterImpl cdi = createConfigurationDependency(service, MyConfiguration.class);

        ((ManagedServiceFactory) cdi.m_component.getInstance()).updated(CONF_PID, createDictionary());

        ensure.waitForStep(1, 1000);

        ((ManagedServiceFactory) cdi.m_component.getInstance()).deleted(CONF_PID);

        ensure.waitForStep(2, 1000);
    }

    @Test
    public void testInvokeManagedServiceUpdatedMethodOk() throws Exception {
        Ensure ensure = createEnsure();
        AManagedService service = new AManagedService(ensure);

        FactoryConfigurationAdapterImpl cdi = createConfigurationDependency(service);

        ((ManagedServiceFactory) cdi.m_component.getInstance()).updated(CONF_PID, createDictionary());

        ensure.waitForStep(1, 1000);

        ((ManagedServiceFactory) cdi.m_component.getInstance()).deleted(CONF_PID);

        ensure.waitForStep(2, 1000);
    }

    @Test
    public void testInvokePlainUpdatedMethodOk() throws Exception {
        Ensure ensure = createEnsure();
        PlainService service = new PlainService(ensure);

        FactoryConfigurationAdapterImpl cdi = createConfigurationDependency(service);

        ((ManagedServiceFactory) cdi.m_component.getInstance()).updated(CONF_PID, createDictionary());

        ensure.waitForStep(1, 1000);

        ((ManagedServiceFactory) cdi.m_component.getInstance()).deleted(CONF_PID);

        ensure.waitForStep(2, 1000);
    }


    private FactoryConfigurationAdapterImpl createConfigurationDependency(Object service) {
        return createConfigurationDependency(service, null);
    }
    
    private FactoryConfigurationAdapterImpl createConfigurationDependency(Object service, Class<?> configType) {
        BundleContext bc = mock(BundleContext.class);

        DependencyManager dm = new DependencyManager(bc);

        Component result = dm.createFactoryConfigurationAdapterService("does.not.matter", "updated", false, service, configType);
        
        // Normally, when creating a factory pid adapter, you specify the class of the adapter implementation which will be instantiated
        // for each created factory pid. To do so, you invoke the setImplementation(Object impl) method, and this methods
        // accepts a class parameter, or an object instance. Usually, you always pass a class, because the intent of a factory pid adapter is to
        // create a component instance for each created factory pid. But in our case, the "service" parameter represents our adapter instance, 
        // so just use it as the factory adapter implementation instance: 
        result.setImplementation(service); 
        
        // *Important note:* the semantic of the factory conf pid adapter is really similar to a ManagedServiceFactory: 
        // - when the factory pid is created, a component is created; called in updated; and called in start().
        // - when the factory pid is updated, the component is called in updated().
        // - but when the factory pid is removed, updated(null) is not invoked (unlike in case of ConfigurationDependency), and the component is simply 
        //   stopped. This is actually the same semantic as ManagedServiceFactory: when factory pid is removed, ManagedServiceFactory.deleted() is called
        //   and the deleted() method is assumed to stop and unregister the service that was registered for the pid being removed.
        dm.add(result);
        return (FactoryConfigurationAdapterImpl) result;
    }

    private Dictionary<?,?> createDictionary() {
        Dictionary<String, Object> result = new Hashtable<>();
        result.put("true", "true");
        result.put("value", "42");
        result.put("longValue", "1234567890");
        result.put("pi", "3.141");
        result.put("argArray", "[a, b, c]");
        result.put("argList", "[d, e, f]");
        result.put("map.foo", "bar");
        result.put("map.qux", "123");
        result.put("map.quu", "[x, y, z]");
        result.put("message", "hello world!");
        return result;
    }

    private Ensure createEnsure() {
        return new Ensure(false);
    }
}
