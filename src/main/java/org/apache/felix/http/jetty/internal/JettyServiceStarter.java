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

import org.apache.felix.http.base.internal.HttpServiceController;
import org.osgi.framework.BundleContext;

public class JettyServiceStarter
{

	private final HttpServiceController controller;
    private final JettyService jetty;

    public JettyServiceStarter(final BundleContext context, final Dictionary<String, ?> properties)
    throws Exception
    {
    	this.controller = new HttpServiceController(context);
        this.jetty = new JettyService(context, this.controller, properties);
        this.jetty.start();
    }

    public void stop() throws Exception
    {
    	this.jetty.stop();
    	this.controller.stop();
    }

    public void updated(final Dictionary<String, ?> properties) throws Exception
    {
    	this.jetty.updated(properties);
    }
}
