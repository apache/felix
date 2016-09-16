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

import org.apache.felix.http.base.internal.AbstractHttpActivator;
import org.osgi.framework.BundleContext;

public class JettyServiceStarter extends AbstractHttpActivator
{

	private final BundleContext context;
	private final Dictionary<String, ?> props;
    private JettyService jetty;

    JettyServiceStarter(BundleContext context, Dictionary<String, ?> properties)
    {
    	this.context = context;
		this.props = properties;
    }
    
    public void start() throws Exception
    {
		super.setBundleContext(context);
		super.doStart();
		jetty = new JettyService(context, getDispatcherServlet(), getEventDispatcher(),
				getHttpServiceController(), props);
		jetty.start();
    }

    public void stop() throws Exception
    {
    	jetty.stop();
    	super.doStop();
    }

    public void updated(Dictionary<String, ?> properties) throws Exception
    {
    	jetty.updated(properties);
    }

}
