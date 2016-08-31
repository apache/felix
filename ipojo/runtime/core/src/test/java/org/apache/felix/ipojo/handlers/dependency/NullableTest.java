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
package org.apache.felix.ipojo.handlers.dependency;

import java.lang.reflect.Proxy;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.Nullable;
import org.apache.felix.ipojo.handlers.providedservice.ComponentTestWithSuperClass;
import org.apache.felix.ipojo.test.MockBundle;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import junit.framework.Assert;
import junit.framework.TestCase;

public class NullableTest extends TestCase {

    public void testOnGet_whenNullableEnabled_returnsAnInstanceOfNullableAndSpecification() {
        Bundle bundle = new MockBundle(Dependency.class.getClassLoader());
        BundleContext context = Mockito.mock(BundleContext.class);
        InstanceManager im = Mockito.mock(InstanceManager.class);
        Mockito.when(im.getClazz()).thenReturn(ComponentTestWithSuperClass.class);
        DependencyHandler handler = Mockito.mock(DependencyHandler.class);
        Mockito.when(handler.getInstanceManager()).thenReturn(im);
        Dependency dependency = new Dependency(handler, "a_field", TestSpecification.class, null, true, false, true,
                false, "dep", context, Dependency.DYNAMIC_BINDING_POLICY, null, null, null);
        dependency.start();

        Object service = dependency.onGet(null, null, null);

        Assert.assertTrue(service instanceof Nullable);
        Assert.assertTrue(service instanceof TestSpecification);
    }

    public void testOnGet_whenNullableEnabled_returnsAProxyWithNullableObjectAsInvocationHandler() {
        Bundle bundle = new MockBundle(Dependency.class.getClassLoader());
        BundleContext context = Mockito.mock(BundleContext.class);
        InstanceManager im = Mockito.mock(InstanceManager.class);
        Mockito.when(im.getClazz()).thenReturn(ComponentTestWithSuperClass.class);
        DependencyHandler handler = Mockito.mock(DependencyHandler.class);
        Mockito.when(handler.getInstanceManager()).thenReturn(im);
        Dependency dependency = new Dependency(handler, "a_field", TestSpecification.class, null, true, false, true,
            false, "dep", context, Dependency.DYNAMIC_BINDING_POLICY, null, null, null);
        dependency.start();

        Object service = dependency.onGet(null, null, null);

        Assert.assertTrue(service instanceof Proxy);
        Assert.assertTrue(Proxy.getInvocationHandler(service) instanceof NullableObject);
    }
}