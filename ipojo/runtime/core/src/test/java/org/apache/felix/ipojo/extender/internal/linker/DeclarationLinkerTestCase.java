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

package org.apache.felix.ipojo.extender.internal.linker;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.apache.felix.ipojo.extender.internal.declaration.DefaultTypeDeclaration;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import junit.framework.TestCase;

/**
 * Checks the linker behavior.
 */
public class DeclarationLinkerTestCase extends TestCase {
    @Mock
    private Bundle m_bundle;

    @Mock
    private BundleContext m_bundleContext;

    @Mock
    private Filter filter;

    @Mock
    private ServiceReference m_reference;

    @Mock
    private QueueService m_queueService;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testActivationDeactivation() throws Exception {
/*
        when(filter.match(m_reference)).thenReturn(true);
        when(m_bundleContext.getService(m_reference)).thenReturn(m_extension);
        when(m_extension.getFactoryBuilder()).thenReturn(m_builder);
        when(m_builder.build(any(BundleContext.class), any(Element.class))).thenReturn(factory);
*/
        when(m_bundleContext.getService(m_reference)).thenReturn(new DefaultTypeDeclaration(m_bundleContext, element("component", "test.Hello")));
        when(m_bundleContext.createFilter(anyString())).thenReturn(filter);
        when(m_reference.getBundle()).thenReturn(m_bundle);
        when(m_bundle.getBundleContext()).thenReturn(m_bundleContext);

        DeclarationLinker linker = new DeclarationLinker(m_bundleContext, m_queueService);
        assertNotNull(linker.addingService(m_reference));

/*
        DefaultTypeDeclaration declaration = new DefaultTypeDeclaration(m_bundleContext, element("component", "component.Hello"));
        declaration.start();

        // Declaration is not bound
        assertFalse(declaration.getStatus().isBound());

        verify(m_bundleContext).addServiceListener(captor.capture(), anyString());

        ServiceListener listener = captor.getValue();
        ServiceEvent e = new ServiceEvent(ServiceEvent.REGISTERED, m_reference);
        listener.serviceChanged(e);

        verify(factory).addFactoryStateListener(fslCaptor.capture());
        FactoryStateListener fsl = fslCaptor.getValue();
        fsl.stateChanged(factory, Factory.VALID);

        assertTrue(declaration.getStatus().isBound());

        // The 2nd tracker should have registered its own listener
        verify(m_bundleContext, times(2)).addServiceListener(captor.capture(), anyString());
        ServiceListener listener2 = captor.getValue();
        assertNotSame(listener, listener2);

        ServiceEvent e2 = new ServiceEvent(ServiceEvent.UNREGISTERING, m_reference);
        listener.serviceChanged(e2);

        // After extension removal, the declaration should be unbound
        assertFalse(declaration.getStatus().isBound());
*/
    }


    private Element element(String type, String name) {
        Element root = new Element(type, null);
        root.addAttribute(new Attribute("name", name));
        return root;
    }

}
