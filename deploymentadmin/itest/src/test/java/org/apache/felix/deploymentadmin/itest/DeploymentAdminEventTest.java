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
package org.apache.felix.deploymentadmin.itest;

import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.deploymentadmin.Constants;
import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Test cases for FELIX-4466 - DA does not always fire events.
 */
@RunWith(PaxExam.class)
public class DeploymentAdminEventTest extends BaseIntegrationTest
{
    /**
     * FELIX-4466 - test that an event is fired when an installation of a DP fails.
     */
    @Test
    public void testFailedInstallationCausesCompletionEventOk() throws Exception
    {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        // incluse two different versions of the same bundle (with the same BSN), this is *not* allowed per the DA spec...
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi1", "bundleapi1", "1.0.0")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi2", "bundleapi2", "2.0.0")));

        final AtomicReference<Event> completionEventRef = new AtomicReference<Event>();
        final AtomicReference<Event> installEventRef = new AtomicReference<Event>();
        final CountDownLatch cdl = new CountDownLatch(1);

        EventHandler eventHandler = new EventHandler()
        {
            @Override
            public void handleEvent(Event event)
            {
                if (Constants.EVENTTOPIC_COMPLETE.equals(event.getTopic()))
                {
                    assertTrue("Multiple events received?!", completionEventRef.compareAndSet(null, event));
                    cdl.countDown();
                }
                else if (Constants.EVENTTOPIC_INSTALL.equals(event.getTopic()))
                {
                    assertTrue("Multiple events received?!", installEventRef.compareAndSet(null, event));
                }
            }
        };

        Dictionary props = new Properties();
        props.put(EventConstants.EVENT_TOPIC, new String[] { Constants.EVENTTOPIC_COMPLETE, Constants.EVENTTOPIC_INSTALL });

        ServiceRegistration sreg = m_context.registerService(EventHandler.class, eventHandler, props);

        try
        {
            installDeploymentPackage(dpBuilder);
            fail("DeploymentException expected!");
        }
        catch (DeploymentException e)
        {
            // Ok; expected...
            assertTrue("Not all events were received in time?!", cdl.await(5, TimeUnit.SECONDS));

            Event event;
            // Verify we've got the expected events...
            event = installEventRef.get();
            // The install event is send *after* the DP have been created, which fails in this test...
            assertNull("No install event received?!", event);
            
            event = completionEventRef.get();
            assertNotNull("No completion event received?!", event);
            assertTrue("Completion property set to true?!", Boolean.FALSE.equals(event.getProperty(Constants.EVENTPROPERTY_SUCCESSFUL)));
        }
        finally
        {
            sreg.unregister();
        }
    }

    /**
     * FELIX-4466 - test that an event is fired when an installation of a DP succeeds.
     */
    @Test
    public void testSuccessfulInstallationCausesCompletionEventOk() throws Exception
    {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi1", "bundleapi1", "1.0.0")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleimpl1", "bundleimpl1", "1.0.0")));

        final AtomicReference<Event> completionEventRef = new AtomicReference<Event>();
        final AtomicReference<Event> installEventRef = new AtomicReference<Event>();
        final CountDownLatch cdl = new CountDownLatch(2);
        
        EventHandler eventHandler = new EventHandler()
        {
            @Override
            public void handleEvent(Event event)
            {
                if (Constants.EVENTTOPIC_COMPLETE.equals(event.getTopic()))
                {
                    assertTrue("Multiple events received?!", completionEventRef.compareAndSet(null, event));
                    cdl.countDown();
                }
                else if (Constants.EVENTTOPIC_INSTALL.equals(event.getTopic()))
                {
                    assertTrue("Multiple events received?!", installEventRef.compareAndSet(null, event));
                    cdl.countDown();
                }
            }
        };

        Dictionary props = new Properties();
        props.put(EventConstants.EVENT_TOPIC, new String[] { Constants.EVENTTOPIC_COMPLETE, Constants.EVENTTOPIC_INSTALL });

        ServiceRegistration sreg = m_context.registerService(EventHandler.class, eventHandler, props);

        try
        {
            installDeploymentPackage(dpBuilder);

            assertTrue("Not all events were received in time?!", cdl.await(5, TimeUnit.SECONDS));
            
            Event event;
            // Verify we've got the expected events...
            event = installEventRef.get();
            assertNotNull("No install event received?!", event);
            
            event = completionEventRef.get();
            assertNotNull("No completion event received?!", event);
            assertTrue("Completion property set to false?!", Boolean.TRUE.equals(event.getProperty(Constants.EVENTPROPERTY_SUCCESSFUL)));
        }
        finally
        {
            sreg.unregister();
        }
    }

    /**
     * FELIX-4466 - test that an event is fired when a DP is uninstalled.
     */
    @Test
    public void testSuccessfulUninstallationCausesCompletionEventOk() throws Exception
    {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi1", "bundleapi1", "1.0.0")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleimpl1", "bundleimpl1", "1.0.0")));

        final AtomicReference<Event> completionEventRef = new AtomicReference<Event>();
        final AtomicReference<Event> uninstallEventRef = new AtomicReference<Event>();
        final CountDownLatch cdl = new CountDownLatch(2);
        
        EventHandler eventHandler = new EventHandler()
        {
            @Override
            public void handleEvent(Event event)
            {
                if (Constants.EVENTTOPIC_COMPLETE.equals(event.getTopic()))
                {
                    assertTrue("Multiple events received?!", completionEventRef.compareAndSet(null, event));
                    cdl.countDown();
                }
                else if (Constants.EVENTTOPIC_UNINSTALL.equals(event.getTopic()))
                {
                    assertTrue("Multiple events received?!", uninstallEventRef.compareAndSet(null, event));
                    cdl.countDown();
                }
            }
        };

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull(dp);
        
        awaitRefreshPackagesEvent();

        Dictionary props = new Properties();
        props.put(EventConstants.EVENT_TOPIC, new String[] { Constants.EVENTTOPIC_COMPLETE, Constants.EVENTTOPIC_UNINSTALL });

        ServiceRegistration sreg = m_context.registerService(EventHandler.class, eventHandler, props);

        try
        {
            dp.uninstall();

            assertTrue("Not all events were received in time?!", cdl.await(5, TimeUnit.SECONDS));

            Event event;
            // Verify we've got the expected events...
            event = uninstallEventRef.get();
            assertNotNull("No uninstall event received?!", event);
            
            event = completionEventRef.get();
            assertNotNull("No completion event received?!", event);
            assertTrue("Completion property set to false?!", Boolean.TRUE.equals(event.getProperty(Constants.EVENTPROPERTY_SUCCESSFUL)));
        }
        finally
        {
            sreg.unregister();
        }
    }

    /**
     * FELIX-4466 - test that an event is fired when a DP is uninstalled, but fails.
     */
    @Test
    public void testFailedUninstallationCausesCompletionEventOk() throws Exception
    {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        final AtomicReference<Event> completionEventRef = new AtomicReference<Event>();
        final AtomicReference<Event> uninstallEventRef = new AtomicReference<Event>();
        final CountDownLatch cdl = new CountDownLatch(2);

        EventHandler eventHandler = new EventHandler()
        {
            @Override
            public void handleEvent(Event event)
            {
                if (Constants.EVENTTOPIC_COMPLETE.equals(event.getTopic()))
                {
                    assertTrue("Multiple events received?!", completionEventRef.compareAndSet(null, event));
                    cdl.countDown();
                }
                else if (Constants.EVENTTOPIC_UNINSTALL.equals(event.getTopic()))
                {
                    assertTrue("Multiple events received?!", uninstallEventRef.compareAndSet(null, event));
                    cdl.countDown();
                }
            }
        };

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        // Should cause the uninstall of the DP to fail...
        dp.getBundle(getSymbolicName("rp1")).uninstall();

        Dictionary props = new Properties();
        props.put(EventConstants.EVENT_TOPIC, new String[] { Constants.EVENTTOPIC_COMPLETE, Constants.EVENTTOPIC_UNINSTALL });

        ServiceRegistration sreg = m_context.registerService(EventHandler.class, eventHandler, props);

        try
        {
            dp.uninstall();
            fail("DeploymentException expected!");
        }
        catch (DeploymentException e)
        {
            // Ok, expected...
            assertTrue("Not all events were received in time?!", cdl.await(5, TimeUnit.SECONDS));

            Event event;
            // Verify we've got the expected events...
            event = uninstallEventRef.get();
            assertNotNull("No uninstall event received?!", event);

            event = completionEventRef.get();
            assertNotNull("No completion event received?!", event);
            assertTrue("Completion property set to true?!", Boolean.FALSE.equals(event.getProperty(Constants.EVENTPROPERTY_SUCCESSFUL)));
        }
        finally
        {
            sreg.unregister();
        }
    }
}
