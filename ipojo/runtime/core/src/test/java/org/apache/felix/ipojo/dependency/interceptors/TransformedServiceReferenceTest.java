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

package org.apache.felix.ipojo.dependency.interceptors;

import org.apache.felix.ipojo.dependency.impl.TransformedServiceReferenceImpl;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests service reference transformation
 */
public class TransformedServiceReferenceTest {

    @Test
    public void addPropertyToReference() {
        ServiceReference reference = mock(ServiceReference.class);
        when(reference.getPropertyKeys()).thenReturn(new String[] {"service.id", "foo"});
        when(reference.getProperty("service.id")).thenReturn(42);
        when(reference.getProperty("foo")).thenReturn("test");

        ServiceReference newReference = new TransformedServiceReferenceImpl(reference).addProperty("location",
                "kitchen");

        assertThat(newReference.getPropertyKeys()).contains("location");
        assertThat(newReference.getProperty("location")).isEqualTo("kitchen");
    }

    @Test
    public void removePropertyToReference() {
        ServiceReference<List> reference = mock(ServiceReference.class);
        when(reference.getPropertyKeys()).thenReturn(new String[] {"service.id", "foo"});
        when(reference.getProperty("service.id")).thenReturn(42l);
        when(reference.getProperty("foo")).thenReturn("test");

        ServiceReference newReference = new TransformedServiceReferenceImpl<List>(reference).removeProperty("foo");

        assertThat(newReference.getPropertyKeys()).containsOnly("service.id");
        assertThat(newReference.getProperty("foo")).isNull();
    }

    @Test
    public void equals() {
        ServiceReference<List> reference = mock(ServiceReference.class);
        when(reference.getPropertyKeys()).thenReturn(new String[] {"service.id", "foo"});
        when(reference.getProperty("service.id")).thenReturn((long) 42);
        when(reference.getProperty("foo")).thenReturn("test");

        ServiceReference newReference1 = new TransformedServiceReferenceImpl<List>(reference).removeProperty("foo");
        ServiceReference newReference2 = new TransformedServiceReferenceImpl<List>(reference).removeProperty("foo");

        assertThat(newReference1).isEqualTo(newReference2);
    }

    @Test
    public void list() {
        ServiceReference<List> reference = mock(ServiceReference.class);
        when(reference.getPropertyKeys()).thenReturn(new String[] {"service.id", "foo"});
        when(reference.getProperty("service.id")).thenReturn(42);
        when(reference.getProperty("foo")).thenReturn("test");

        ServiceReference newReference1 = new TransformedServiceReferenceImpl<List>(reference).removeProperty("foo");
        ServiceReference newReference2 = new TransformedServiceReferenceImpl<List>(reference).removeProperty("foo");

        List<ServiceReference> references = new ArrayList<ServiceReference>();
        references.add(newReference1);

        assertThat(references.contains(newReference1));
        assertThat(references.contains(reference));
        assertThat(references.contains(newReference2));
    }
}
