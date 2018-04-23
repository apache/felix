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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class JettyManagedServiceFactory implements ManagedServiceFactory
{
	private final Map<String, JettyServiceStarter> services = new HashMap<>();
	private final BundleContext context;

	JettyManagedServiceFactory(final BundleContext context)
	{
		this.context = context;
	}

	public synchronized void stop()
	{
		final Set<String> pids = new HashSet<>(services.keySet());
		for (final String pid : pids)
		{
			deleted(pid);
		}
	}

	@Override
	public String getName()
	{
		return "Apache Felix Http Jetty";
	}

	@Override
	public synchronized void updated(final String pid, final Dictionary<String, ?> properties) throws ConfigurationException
	{
		JettyServiceStarter jetty = services.get(pid);

		try
		{
			if (jetty == null)
			{
				jetty = new JettyServiceStarter(context, properties);
				services.put(pid, jetty);
			}
			else
			{
				jetty.updated(properties);
			}
		}
		catch (final Exception e)
		{
            SystemLogger.error("Failed to start Http Jetty pid=" + pid, e);
		}
	}

	@Override
	public synchronized void deleted(String pid)
	{
		JettyServiceStarter jetty = services.remove(pid);

		if (jetty != null)
		{
			try
			{
				jetty.stop();
			}
			catch (Exception e)
			{
			    SystemLogger.error("Faiiled to stop Http Jetty pid=" + pid, e);
			}
		}

	}

}
