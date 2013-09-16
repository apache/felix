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

import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks the dependency identifier computation.
 */
public class DependencyIdentifierTest {

    @Test
    public void testDependencyWithAllInformation() {
        Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.when(dependency.getId()).thenReturn("id1");
        Mockito.when(dependency.getField()).thenReturn("fs");
        DependencyCallback[] callbacks = new DependencyCallback[2];
        callbacks[0] = Mockito.mock(DependencyCallback.class);
        Mockito.when(callbacks[0].getMethodName()).thenReturn("bindFooService");
        callbacks[1] = Mockito.mock(DependencyCallback.class);
        Mockito.when(callbacks[1].getMethodName()).thenReturn("unbindFooService");
        Mockito.when(dependency.getCallbacks()).thenReturn(callbacks);
        Mockito.when(dependency.getSpecification()).thenReturn(List.class);

        String identifier = DependencyHandler.getDependencyIdentifier(dependency);
        assertThat(identifier).startsWith("{");
        assertThat(identifier).endsWith("}");
        assertThat(identifier).contains("id=" + "id1");
        assertThat(identifier).contains("field=" + "fs");
        assertThat(identifier).contains("specification=" + List.class.getName());
        assertThat(identifier).contains("method=" + "bindFooService");

        System.out.println(identifier);
    }

    @Test
    public void testDependencyWithIdOnly() {
        Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.when(dependency.getId()).thenReturn("id1");
        Mockito.when(dependency.getField()).thenReturn(null);
        Mockito.when(dependency.getCallbacks()).thenReturn(null);
        Mockito.when(dependency.getSpecification()).thenReturn(null);

        String identifier = DependencyHandler.getDependencyIdentifier(dependency);
        assertThat(identifier).startsWith("{");
        assertThat(identifier).endsWith("}");
        assertThat(identifier).contains("id=" + "id1");
        assertThat(identifier).isEqualTo("{id=" + "id1}");
        assertThat(identifier).doesNotContain("field");
        assertThat(identifier).doesNotContain("specification");
        assertThat(identifier).doesNotContain("method");
    }

    @Test
    public void testDependencyWithFieldOnly() {
        Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.when(dependency.getField()).thenReturn("fs");
        Mockito.when(dependency.getId()).thenReturn(null);
        Mockito.when(dependency.getCallbacks()).thenReturn(null);
        Mockito.when(dependency.getSpecification()).thenReturn(null);

        String identifier = DependencyHandler.getDependencyIdentifier(dependency);
        assertThat(identifier).isEqualTo("{field=" + "fs}");
    }

    @Test
    public void testDependencyWithSpecificationOnly() {
        Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.when(dependency.getField()).thenReturn(null);
        Mockito.when(dependency.getId()).thenReturn(null);
        Mockito.when(dependency.getCallbacks()).thenReturn(null);
        Mockito.when(dependency.getSpecification()).thenReturn(List.class);

        String identifier = DependencyHandler.getDependencyIdentifier(dependency);
        assertThat(identifier).isEqualTo("{specification=" + List.class.getName() + "}");
    }

    @Test
    public void testDependencyWithNothing() {
        Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.when(dependency.getField()).thenReturn(null);
        Mockito.when(dependency.getId()).thenReturn(null);
        Mockito.when(dependency.getCallbacks()).thenReturn(null);
        Mockito.when(dependency.getSpecification()).thenReturn(null);

        String identifier = DependencyHandler.getDependencyIdentifier(dependency);
        assertThat(identifier).isEqualTo("{}");
    }

    @Test
    public void testDependencyWithCallbackOnly() {
        Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.when(dependency.getField()).thenReturn(null);
        Mockito.when(dependency.getId()).thenReturn(null);
        Mockito.when(dependency.getSpecification()).thenReturn(null);
        DependencyCallback[] callbacks = new DependencyCallback[2];
        callbacks[0] = Mockito.mock(DependencyCallback.class);
        Mockito.when(callbacks[0].getMethodName()).thenReturn("bindFooService");
        callbacks[1] = Mockito.mock(DependencyCallback.class);
        Mockito.when(callbacks[1].getMethodName()).thenReturn("unbindFooService");
        Mockito.when(dependency.getCallbacks()).thenReturn(callbacks);

        String identifier = DependencyHandler.getDependencyIdentifier(dependency);
        assertThat(identifier).isEqualTo("{method=bindFooService}");
    }

    @Test
    public void testDependencyWithIdAndField() {
        Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.when(dependency.getField()).thenReturn("fs");
        Mockito.when(dependency.getId()).thenReturn("id1");
        Mockito.when(dependency.getCallbacks()).thenReturn(null);
        Mockito.when(dependency.getSpecification()).thenReturn(null);

        String identifier = DependencyHandler.getDependencyIdentifier(dependency);
        assertThat(identifier).isEqualTo("{id=id1, field=fs}");
    }

    @Test
    public void testDependencyWithFieldAndMethod() {
        Dependency dependency = Mockito.mock(Dependency.class);
        Mockito.when(dependency.getField()).thenReturn("fs");
        Mockito.when(dependency.getId()).thenReturn(null);
        DependencyCallback[] callbacks = new DependencyCallback[2];
        callbacks[0] = Mockito.mock(DependencyCallback.class);
        Mockito.when(callbacks[0].getMethodName()).thenReturn("bindFooService");
        callbacks[1] = Mockito.mock(DependencyCallback.class);
        Mockito.when(callbacks[1].getMethodName()).thenReturn("unbindFooService");
        Mockito.when(dependency.getCallbacks()).thenReturn(callbacks);
        Mockito.when(dependency.getSpecification()).thenReturn(null);

        String identifier = DependencyHandler.getDependencyIdentifier(dependency);
        assertThat(identifier).isEqualTo("{field=fs, method=bindFooService}");
    }


}
