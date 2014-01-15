/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.coordinator.impl;

import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;

public class Activator implements BundleActivator
{

    private CoordinationMgr mgr;

    private ServiceRegistration coordinatorService;

    public void start(final BundleContext context)
    {
        LogWrapper.setContext(context);

        mgr = new CoordinationMgr();

        final ServiceFactory factory = new CoordinatorFactory(mgr);
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION, "Coordinator Service Implementation");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        coordinatorService = context.registerService(Coordinator.class.getName(), factory, props);
    }

    public void stop(final BundleContext context)
    {
        if (coordinatorService != null)
        {
            coordinatorService.unregister();
            coordinatorService = null;
        }

        mgr.cleanUp();

        LogWrapper.setContext(null);
    }

    static final class CoordinatorFactory implements ServiceFactory
    {

        private final CoordinationMgr mgr;

        CoordinatorFactory(final CoordinationMgr mgr)
        {
            this.mgr = mgr;
        }

        public Object getService(final Bundle bundle, final ServiceRegistration registration)
        {
            return new CoordinatorImpl(bundle, mgr);
        }

        public void ungetService(final Bundle bundle, final ServiceRegistration registration, final Object service)
        {
            ((CoordinatorImpl) service).dispose();
        }

    }
}
