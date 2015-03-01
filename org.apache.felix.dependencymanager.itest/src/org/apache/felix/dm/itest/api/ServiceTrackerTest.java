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
package org.apache.felix.dm.itest.api;

import java.util.Hashtable;

import org.junit.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.ServiceUtil;
import org.apache.felix.dm.itest.util.TestBase;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class ServiceTrackerTest extends TestBase {
    public void testPlainServiceTracker() {
        ServiceTracker st = new ServiceTracker(context, ServiceInterface.class.getName(), null);
        st.open();
        ServiceRegistration sr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(), null);
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        sr.unregister();
        Assert.assertNull("There should be no service that matches the tracker", st.getServices());
        st.close();
    }
    
    public void testAspectServiceTracker() {
        ServiceTracker st = new ServiceTracker(context, ServiceInterface.class.getName(), null);
        st.open();

        ServiceRegistration sr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(), null);
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        
        final long sid = ServiceUtil.getServiceId(sr.getReference());
        ServiceRegistration asr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(),
            new Hashtable() {{ put(DependencyManager.ASPECT, sid); put(Constants.SERVICE_RANKING, 10); }});
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        Assert.assertEquals("Service ranking should be 10", Integer.valueOf(10), (Integer) st.getServiceReference().getProperty(Constants.SERVICE_RANKING));

        ServiceRegistration asr2 = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(),
            new Hashtable() {{ put(DependencyManager.ASPECT, sid); put(Constants.SERVICE_RANKING, 20); }});
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        Assert.assertEquals("Service ranking should be 20", Integer.valueOf(20), (Integer) st.getServiceReference().getProperty(Constants.SERVICE_RANKING));
        
        asr.unregister();
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        Assert.assertEquals("Service ranking should be 20", Integer.valueOf(20), (Integer) st.getServiceReference().getProperty(Constants.SERVICE_RANKING));
        
        asr2.unregister();
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        Assert.assertNull("Service should not have a ranking", st.getServiceReference().getProperty(Constants.SERVICE_RANKING));
        
        sr.unregister();
        Assert.assertNull("There should be no service that matches the tracker", st.getServices());
        
        st.close();
    }
    
    public void testExistingAspectServiceTracker() {
        ServiceTracker st = new ServiceTracker(context, ServiceInterface.class.getName(), null);
        ServiceRegistration sr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(), null);
        final long sid = ServiceUtil.getServiceId(sr.getReference());
        ServiceRegistration asr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(),
            new Hashtable() {{ put(DependencyManager.ASPECT, sid); put(Constants.SERVICE_RANKING, 10); }});
        ServiceRegistration asr2 = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(),
            new Hashtable() {{ put(DependencyManager.ASPECT, sid); put(Constants.SERVICE_RANKING, 20); }});

        st.open();
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        Assert.assertEquals("Service ranking should be 20", Integer.valueOf(20), (Integer) st.getServiceReference().getProperty(Constants.SERVICE_RANKING));
        
        asr2.unregister();
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        Assert.assertEquals("Service ranking should be 10", Integer.valueOf(10), (Integer) st.getServiceReference().getProperty(Constants.SERVICE_RANKING));
        
        asr.unregister();
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        Assert.assertNull("Service should not have a ranking", st.getServiceReference().getProperty(Constants.SERVICE_RANKING));
        
        sr.unregister();
        Assert.assertNull("There should be no service that matches the tracker", st.getServices());
        
        st.close();
    }
    
    static interface ServiceInterface {
        public void invoke();
    }

    static class ServiceProvider implements ServiceInterface {
        public void invoke() {
        }
    }
}
