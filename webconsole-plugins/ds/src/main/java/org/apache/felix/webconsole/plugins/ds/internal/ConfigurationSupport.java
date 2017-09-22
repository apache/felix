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
package org.apache.felix.webconsole.plugins.ds.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigurationSupport {

    private final ServiceTracker<Object, Object> configAdminTracker;

    private final ServiceTracker<Object, Object> metatypeTracker;

    public ConfigurationSupport(final BundleContext bundleContext)
    {
        this.configAdminTracker = new ServiceTracker<Object, Object>(bundleContext, "org.osgi.service.cm.ConfigurationAdmin", null);
        this.metatypeTracker = new ServiceTracker<Object, Object>(bundleContext, "org.osgi.service.metatype.MetaTypeService", null);

        configAdminTracker.open();
        this.metatypeTracker.open();
    }

    public void close()
    {
        this.configAdminTracker.close();
        this.metatypeTracker.close();
    }

    /**
     * Check if the component with the specified pid is
     * configurable
     * @param providingBundle The Bundle providing the component. This may be
     *      theoretically be <code>null</code>.
     * @param pid A non null pid
     * @return <code>true</code> if the component is configurable.
     */
    public boolean isConfigurable(final Bundle providingBundle, final String pid)
    {
        // we first check if the config admin has something for this pid
        final Object ca = this.configAdminTracker.getService();
        if (ca != null)
        {
            if ( new ConfigurationAdminSupport().check(ca, pid) )
            {
                return true;
            }
        }
        // second check is using the meta type service
        if (providingBundle != null)
        {
            final Object mts = this.metatypeTracker.getService();
            if (mts != null)
            {
                return new MetatypeSupport().check(mts, providingBundle, pid);
            }
        }
        return false;
    }
}