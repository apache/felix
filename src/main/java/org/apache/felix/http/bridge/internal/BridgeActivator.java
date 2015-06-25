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
package org.apache.felix.http.bridge.internal;

import java.util.EventListener;
import java.util.Hashtable;

import javax.servlet.http.HttpServlet;

import org.apache.felix.http.base.internal.AbstractHttpActivator;
import org.apache.felix.http.base.internal.EventDispatcher;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.framework.Constants;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

public final class BridgeActivator extends AbstractHttpActivator
{
    /** Framework property containing the endpoint registration information (optional). */
    private static final String FELIX_HTTP_SERVICE_ENDPOINTS = "org.apache.felix.http.service.endpoints";

    private static final String VENDOR = "The Apache Software Foundation";

    private static final String MARKER_PROP = "http.felix.dispatcher";

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        // dispatcher servlet
        final Object servlet = getDispatcherServlet();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(MARKER_PROP, servlet.getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Felix Http Dispatcher for bridged request handling");
        props.put(Constants.SERVICE_VENDOR, VENDOR);
        getBundleContext().registerService(HttpServlet.class.getName(), servlet, props);

        // Http Session event dispatcher
        final EventDispatcher dispatcher = getEventDispatcher();
        dispatcher.setActive(true);
        props = new Hashtable<String, Object>();
        props.put(MARKER_PROP, dispatcher.getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Felix Http Dispatcher for bridged event handling");
        props.put(Constants.SERVICE_VENDOR, VENDOR);
        getBundleContext().registerService(EventListener.class.getName(), dispatcher, props);

        // check for endpoint registration property
        if ( getBundleContext().getProperty(FELIX_HTTP_SERVICE_ENDPOINTS) != null )
        {
            final Hashtable<String, Object> serviceRegProps = new Hashtable<String, Object>();
            serviceRegProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT,
                    getBundleContext().getProperty(FELIX_HTTP_SERVICE_ENDPOINTS));
            this.getHttpServiceController().setProperties(serviceRegProps);
        }

        SystemLogger.info("Started bridged http services");
    }
}
