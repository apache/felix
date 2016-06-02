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
package org.apache.felix.scr.impl.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.spi.ServiceRegistry;

import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.inject.ComponentMethodsImpl;
import org.apache.felix.scr.impl.manager.AbstractComponentManager.State;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

public class SingleComponentManagerTest
{
    
    private ServiceRegistration serviceRegistration = Mockito.mock(ServiceRegistration.class);
    private ServiceReference serviceReference = Mockito.mock(ServiceReference.class);
    private ComponentActivator componentActivator = new ComponentActivator() {

        public boolean isLogEnabled(int level)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public void log(int level, String pattern, Object[] arguments, ComponentMetadata metadata, Long componentId,
            Throwable ex)
        {
            // TODO Auto-generated method stub
            
        }

        public void log(int level, String message, ComponentMetadata metadata, Long componentId, Throwable ex)
        {
            // TODO Auto-generated method stub
            
        }

        public void addServiceListener(String className, Filter filter,
            ExtendedServiceListener<ExtendedServiceEvent> listener)
        {
            // TODO Auto-generated method stub
            
        }

        public void removeServiceListener(String className, Filter filter,
            ExtendedServiceListener<ExtendedServiceEvent> listener)
        {
            // TODO Auto-generated method stub
            
        }

        public BundleContext getBundleContext()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isActive()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public ScrConfiguration getConfiguration()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void schedule(Runnable runnable)
        {
            // TODO Auto-generated method stub
            
        }

        public long registerComponentId(AbstractComponentManager<?> sAbstractComponentManager)
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public void unregisterComponentId(AbstractComponentManager<?> sAbstractComponentManager)
        {
            // TODO Auto-generated method stub
            
        }

        public <T> boolean enterCreate(ServiceReference<T> reference)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public <T> void leaveCreate(ServiceReference<T> reference)
        {
            // TODO Auto-generated method stub
            
        }

        public <S, T> void registerMissingDependency(DependencyManager<S, T> dependencyManager,
            ServiceReference<T> serviceReference, int trackingCount)
        {
            // TODO Auto-generated method stub
            
        }

        public <T> void missingServicePresent(ServiceReference<T> serviceReference)
        {
            // TODO Auto-generated method stub
            
        }

        public void enableComponent(String name)
        {
            // TODO Auto-generated method stub
            
        }

        public void disableComponent(String name)
        {
            // TODO Auto-generated method stub
            
        }

        public RegionConfigurationSupport setRegionConfigurationSupport(ServiceReference<ConfigurationAdmin> reference)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void unsetRegionConfigurationSupport(RegionConfigurationSupport rcs)
        {
            // TODO Auto-generated method stub
            
        }
        
    };
    
    @Test
    public void testGetService() throws Exception {
        ComponentMetadata cm = new ComponentMetadata(DSVersion.DS13);
        cm.setImplementationClassName("foo.bar.SomeClass");
        cm.validate(null);

        @SuppressWarnings("unchecked")
        ComponentContainer<Object> cc = Mockito.mock(ComponentContainer.class);
        Mockito.when(cc.getComponentMetadata()).thenReturn(cm);
        Mockito.when(cc.getActivator()).thenReturn(componentActivator);

        SingleComponentManager<Object> scm = new SingleComponentManager<Object>(cc, new ComponentMethodsImpl()) {
            @Override
            boolean getServiceInternal(ServiceRegistration<Object> serviceRegistration)
            {
                return true;
            }
        };

        BundleContext bc = Mockito.mock(BundleContext.class);
        Bundle b = Mockito.mock(Bundle.class);
        Mockito.when(b.getBundleContext()).thenReturn(bc);

        ComponentContextImpl<Object> cci = new ComponentContextImpl<Object>(scm, b, null);
        Object implObj = new Object();
        cci.setImplementationObject(implObj);
        cci.setImplementationAccessible(true);

        Field f = SingleComponentManager.class.getDeclaredField("m_componentContext");
        f.setAccessible(true);
        f.set(scm, cci);

        scm.setState(scm.getState(), State.unsatisfiedReference);
        assertSame(implObj, scm.getService(b, serviceRegistration));

        Field u = SingleComponentManager.class.getDeclaredField("m_useCount");
        u.setAccessible(true);
        AtomicInteger use = (AtomicInteger) u.get(scm);
        assertEquals(1, use.get());
    }

    @Test
    public void testGetServiceWithNullComponentContext() throws Exception
    {
        ComponentMetadata cm = new ComponentMetadata(DSVersion.DS13);
        cm.setImplementationClassName("foo.bar.SomeClass");
        cm.validate(null);

        @SuppressWarnings("unchecked")
        ComponentContainer<Object> cc = Mockito.mock(ComponentContainer.class);
        Mockito.when(cc.getComponentMetadata()).thenReturn(cm);
        Mockito.when(cc.getActivator()).thenReturn(componentActivator);

        SingleComponentManager<?> scm = new SingleComponentManager<Object>(cc, new ComponentMethodsImpl()) {
            @Override
            boolean getServiceInternal(ServiceRegistration<Object> serviceRegistration)
            {
                return true;
            }
        };
        BundleContext bc = Mockito.mock(BundleContext.class);
        Bundle b = Mockito.mock(Bundle.class);
        Mockito.when(b.getBundleContext()).thenReturn(bc);

        scm.setState(scm.getState(), State.unsatisfiedReference);
        assertNull("m_componentContext is null, this should not cause an NPE",
                scm.getService(b, serviceRegistration));

        Field u = SingleComponentManager.class.getDeclaredField("m_useCount");
        u.setAccessible(true);
        AtomicInteger use = (AtomicInteger) u.get(scm);
        assertEquals(0, use.get());
    }
}
