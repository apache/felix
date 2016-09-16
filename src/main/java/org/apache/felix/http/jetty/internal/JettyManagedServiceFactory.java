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
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class JettyManagedServiceFactory implements ManagedServiceFactory
{
	private final Map<String, JettyServiceStarter> services = new HashMap<>();
	private final BundleContext context;

	JettyManagedServiceFactory(BundleContext context)
	{
		this.context = context;
	}

	@Override
	public String getName()
	{
		return "Apache Felix Http Jetty";
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException
	{
		JettyServiceStarter jetty = services.get(pid);

		try
		{
			if (jetty == null)
			{
				jetty = new JettyServiceStarter(context, properties);
				services.put(pid, jetty);
				jetty.start();
			}
			else
			{
				jetty.updated(properties);
			}
		}
		catch (Exception e)
		{
			throw new ConfigurationException(null, "Failed to start Http Jetty pid=" + pid, e);
		}
	}

	@Override
	public void deleted(String pid)
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
				throw new RuntimeException("Faiiled to stop Http Jetty pid=" + pid, e);
			}
		}

	}

}
