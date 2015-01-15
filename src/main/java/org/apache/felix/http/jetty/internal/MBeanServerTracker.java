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
package org.apache.felix.http.jetty.internal;

import javax.management.MBeanServer;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class MBeanServerTracker extends ServiceTracker
{

    private final Server server;

    public MBeanServerTracker(final BundleContext context, final Server server)
    {
        super(context, MBeanServer.class.getName(), null);
        this.server = server;
    }

    @Override
    public Object addingService(ServiceReference reference)
    {
        MBeanServer server = (MBeanServer) super.addingService(reference);
        MBeanContainer mBeanContainer = new MBeanContainer(server);
        this.server.addEventListener(mBeanContainer);
        return mBeanContainer;
    }

    @Override
    public void removedService(ServiceReference reference, Object service)
    {
        MBeanContainer mBeanContainer = (MBeanContainer) service;
        this.server.removeEventListener(mBeanContainer);
        super.removedService(reference, mBeanContainer.getMBeanServer());
    }
}
