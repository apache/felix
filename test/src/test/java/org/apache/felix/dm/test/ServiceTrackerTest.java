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
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.util.Properties;

import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceUtil;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

@RunWith(JUnit4TestRunner.class)
public class ServiceTrackerTest extends Base {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    

    @Test
    public void testPlainServiceTracker(BundleContext context) {
        ServiceTracker st = new ServiceTracker(context, ServiceInterface.class.getName(), null);
        st.open();
        ServiceRegistration sr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(), null);
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        sr.unregister();
        Assert.assertNull("There should be no service that matches the tracker", st.getServices());
        st.close();
    }
    
    @Test
    public void testAspectServiceTracker(BundleContext context) {
        ServiceTracker st = new ServiceTracker(context, ServiceInterface.class.getName(), null);
        st.open();

        ServiceRegistration sr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(), null);
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        
        final long sid = ServiceUtil.getServiceId(sr.getReference());
        ServiceRegistration asr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(),
            new Properties() {{ put(DependencyManager.ASPECT, sid); put(Constants.SERVICE_RANKING, 10); }});
        Assert.assertEquals("There should be one service that matches the tracker", 1, st.getServices().length);
        Assert.assertEquals("Service ranking should be 10", Integer.valueOf(10), (Integer) st.getServiceReference().getProperty(Constants.SERVICE_RANKING));

        ServiceRegistration asr2 = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(),
            new Properties() {{ put(DependencyManager.ASPECT, sid); put(Constants.SERVICE_RANKING, 20); }});
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
    
    @Test
    public void testExistingAspectServiceTracker(BundleContext context) {
        ServiceTracker st = new ServiceTracker(context, ServiceInterface.class.getName(), null);
        ServiceRegistration sr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(), null);
        final long sid = ServiceUtil.getServiceId(sr.getReference());
        ServiceRegistration asr = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(),
            new Properties() {{ put(DependencyManager.ASPECT, sid); put(Constants.SERVICE_RANKING, 10); }});
        ServiceRegistration asr2 = context.registerService(ServiceInterface.class.getName(), new ServiceProvider(),
            new Properties() {{ put(DependencyManager.ASPECT, sid); put(Constants.SERVICE_RANKING, 20); }});

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
