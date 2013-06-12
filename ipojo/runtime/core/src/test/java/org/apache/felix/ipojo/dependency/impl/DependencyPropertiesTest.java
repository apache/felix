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

package org.apache.felix.ipojo.dependency.impl;

import org.apache.felix.framework.FilterImpl;
import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.util.DependencyModel;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.*;

import java.util.Dictionary;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks the interceptor matching pattern.
 */
public class DependencyPropertiesTest {

    private DependencyModel dependency;

    @Before
    public void setup() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("test-bundle");
        when(bundle.getVersion()).thenReturn(new Version(1, 0, 0));

        BundleContext context = mock(BundleContext.class);
        when(context.getBundle()).thenReturn(bundle);

        ComponentFactory factory = mock(ComponentFactory.class);
        when(factory.getFactoryName()).thenReturn("FooFactory");

        ComponentInstance instance = mock(ComponentInstance.class);
        when(instance.getInstanceName()).thenReturn("FooConsumer");
        when(instance.getState()).thenReturn(2);
        when(instance.getFactory()).thenReturn(factory);

        this.dependency = mock(DependencyModel.class);
        when(dependency.getId()).thenReturn("foo");
        when(dependency.getSpecification()).thenReturn(List.class);
        when(dependency.getBundleContext()).thenReturn(context);
        when(dependency.getComponentInstance()).thenReturn(instance);
        when(dependency.getState()).thenReturn(0);

    }

    @Test
    public void testDependencyCreation() {
        Dictionary<String, ?> dictionary = DependencyProperties.getDependencyProperties(dependency);
        assertThat(dictionary.get("dependency.id")).isEqualTo("foo");
        assertThat(dictionary.get("dependency.specification")).isEqualTo(List.class.getName());
        assertThat(dictionary.get("bundle.symbolicName")).isEqualTo("test-bundle");
    }


    @Test
    public void matchByDependencyId() throws InvalidSyntaxException {
        Dictionary<String, ?> dictionary = DependencyProperties.getDependencyProperties(dependency);
        Filter filter = new FilterImpl("(dependency.id=foo)");
        assertThat(filter.match(dictionary));
    }
}
