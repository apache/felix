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
package org.apache.felix.scr.impl.config;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.ComponentRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConfigAdminTracker
{
	public static final String CONFIGURATION_ADMIN = "org.osgi.service.cm.ConfigurationAdmin";
	
	private final ServiceTracker<ConfigurationAdmin, RegionConfigurationSupport> configAdminTracker;

	static ConfigAdminTracker getRegionConfigurationSupport(final BundleComponentActivator bundleComponentActivator, final ComponentRegistry registry, final Bundle dsBundle)
	{
		Class<?> ourCA;
		Class<?> theirCA;
		try
		{
			ourCA = dsBundle.loadClass(CONFIGURATION_ADMIN);
		}
		catch (ClassNotFoundException e)
		{
			return null;
		}
		try
		{
			Bundle bundle = bundleComponentActivator.getBundleContext().getBundle();
			if ( bundle == null )
			{
				return null;
			}
			theirCA = dsBundle.loadClass(CONFIGURATION_ADMIN);
		}
		catch (ClassNotFoundException e)
		{
			return null;
		}
		if ( ourCA != theirCA )
		{
			return null;
		}
		ConfigAdminTracker tracker = new ConfigAdminTracker(bundleComponentActivator, registry);
		return tracker;
	}

	public ConfigAdminTracker(final BundleComponentActivator bundleComponentActivator, final ComponentRegistry registry)
	{
		
		//TODO this assumes that there is 0 or 1 ca service visible to the bundle being extended.
		//Is this sure to be true?
		configAdminTracker = new ServiceTracker<ConfigurationAdmin, RegionConfigurationSupport>(bundleComponentActivator.getBundleContext(), 
				CONFIGURATION_ADMIN, 
				new ServiceTrackerCustomizer<ConfigurationAdmin, RegionConfigurationSupport>() 
				{

			public RegionConfigurationSupport addingService(
					ServiceReference<ConfigurationAdmin> reference) {
				RegionConfigurationSupport trialRcs = new RegionConfigurationSupport(reference, registry);
				RegionConfigurationSupport rcs = registry.registerRegionConfigurationSupport(trialRcs);
				bundleComponentActivator.setRegionConfigurationSupport(rcs);
				return rcs;
			}

			public void modifiedService(
					ServiceReference<ConfigurationAdmin> reference,
					RegionConfigurationSupport service) {
			}

			public void removedService(
					ServiceReference<ConfigurationAdmin> reference,
					RegionConfigurationSupport rcs) {
				registry.unregisterRegionConfigurationSupport(rcs);
				bundleComponentActivator.unsetRegionConfigurationSupport(rcs);
			}
				});

		configAdminTracker.open();
	}

	public void dispose()
	{
		configAdminTracker.close();
	}
	
}
