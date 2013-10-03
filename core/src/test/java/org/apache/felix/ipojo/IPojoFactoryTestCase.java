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

package org.apache.felix.ipojo;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 03/10/13
 * Time: 11:53
 */
public class IPojoFactoryTestCase extends TestCase {
    @Mock
    private IPojoFactory.NameGenerator m_delegate;
    @Mock
    private Factory m_factory;
    @Mock
    private BundleContext m_bundleContext;
    @Mock
    private Bundle m_bundle;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testRetryNameGenerator() throws Exception {
        when(m_factory.getName()).thenReturn("test");
        when(m_delegate.generate(eq(m_factory), anyList())).thenReturn("my.instance");

        IPojoFactory.RetryNameGenerator retry = new IPojoFactory.RetryNameGenerator(m_delegate);

        assertEquals("my.instance", retry.generate(m_factory, Collections.<String>emptyList()));
    }

    public void testRetryNameGeneratorWithCollisions() throws Exception {
        when(m_factory.getName()).thenReturn("test");
        when(m_delegate.generate(eq(m_factory), anyList())).thenReturn("my.instance", "my.instance2");

        IPojoFactory.RetryNameGenerator retry = new IPojoFactory.RetryNameGenerator(m_delegate);

        assertEquals("my.instance2", retry.generate(m_factory, Arrays.asList("my.instance")));
    }

    public void testRetryNameGeneratorDoNotGenerateStackOverflow() throws Exception {
        when(m_factory.getName()).thenReturn("test");
        when(m_delegate.generate(eq(m_factory), anyList())).thenReturn("my.instance");
        when(m_factory.getBundleContext()).thenReturn(m_bundleContext);
        when(m_bundleContext.getBundle()).thenReturn(m_bundle);
        when(m_bundle.getBundleId()).thenReturn(42l);

        IPojoFactory.RetryNameGenerator retry = new IPojoFactory.RetryNameGenerator(m_delegate);
        retry.setMaximum(100);

        try {
            retry.generate(m_factory, Arrays.asList("my.instance"));
        } catch (UnacceptableConfiguration unacceptableConfiguration) {
            return;
        }
        fail("Expecting an Exception");
    }


    public void testDefaultNameGenerator() throws Exception {
        when(m_factory.getName()).thenReturn("test");

        IPojoFactory.DefaultNameGenerator generator = new IPojoFactory.DefaultNameGenerator();

        assertEquals("test-0", generator.generate(m_factory, null));
        assertEquals("test-1", generator.generate(m_factory, null));
        assertEquals("test-2", generator.generate(m_factory, null));
        assertEquals("test-3", generator.generate(m_factory, null));
        assertEquals("test-4", generator.generate(m_factory, null));
    }

    public void testDefaultNameGeneratorWithVersion() throws Exception {
        when(m_factory.getName()).thenReturn("test");
        when(m_factory.getVersion()).thenReturn("1.2.3");

        IPojoFactory.DefaultNameGenerator generator = new IPojoFactory.DefaultNameGenerator();

        assertEquals("test/1.2.3-0", generator.generate(m_factory, null));
        assertEquals("test/1.2.3-1", generator.generate(m_factory, null));
        assertEquals("test/1.2.3-2", generator.generate(m_factory, null));
        assertEquals("test/1.2.3-3", generator.generate(m_factory, null));
        assertEquals("test/1.2.3-4", generator.generate(m_factory, null));
    }

}
