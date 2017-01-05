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
package org.apache.felix.scr.impl.helper;

import org.apache.felix.scr.impl.manager.ComponentActivator;
import org.apache.felix.scr.impl.manager.RegionConfigurationSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConfigAdminTracker
{
    public static final String CONFIGURATION_ADMIN = "org.osgi.service.cm.ConfigurationAdmin";

    private final ServiceTracker<ConfigurationAdmin, RegionConfigurationSupport> configAdminTracker;

    public ConfigAdminTracker(final ComponentActivator componentActivator)
    {

        //TODO this assumes that there is 0 or 1 ca service visible to the bundle being extended.
        //Is this sure to be true?
        configAdminTracker = new ServiceTracker<ConfigurationAdmin, RegionConfigurationSupport>(
            componentActivator.getBundleContext(), CONFIGURATION_ADMIN,
            new ServiceTrackerCustomizer<ConfigurationAdmin, RegionConfigurationSupport>()
            {

                public RegionConfigurationSupport addingService(ServiceReference<ConfigurationAdmin> reference)
                {
                    return componentActivator.setRegionConfigurationSupport( reference );
                }

                public void modifiedService(ServiceReference<ConfigurationAdmin> reference,
                    RegionConfigurationSupport service)
                {
                }

                public void removedService(ServiceReference<ConfigurationAdmin> reference,
                    RegionConfigurationSupport rcs)
                {
                    componentActivator.unsetRegionConfigurationSupport( rcs );
                }
            } );

        configAdminTracker.open();
    }

    public void dispose()
    {
        configAdminTracker.close();
    }

}
