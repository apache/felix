/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.jetty.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.http.base.internal.AbstractHttpActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public final class JettyActivator extends AbstractHttpActivator
{
    private JettyService jetty;

    private ServiceRegistration<?> metatypeReg;

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Metatype provider for Jetty Http Service");
        properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        properties.put("metatype.pid", JettyService.PID);

        metatypeReg = this.getBundleContext().registerService("org.osgi.service.metatype.MetaTypeProvider",
                new ServiceFactory()
                {

                    @Override
                    public Object getService(final Bundle bundle, final ServiceRegistration registration)
                    {
                        return new ConfigMetaTypeProvider(getBundleContext().getBundle());
                    }

                    @Override
                    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
                    {
                        // nothing to do
                    }
                }, properties);
        this.jetty = new JettyService(getBundleContext(), getDispatcherServlet(), getEventDispatcher(), getHttpServiceController());
        this.jetty.start();
    }

    @Override
    protected void doStop() throws Exception
    {
        this.jetty.stop();
        if ( metatypeReg != null )
        {
            metatypeReg.unregister();
            metatypeReg = null;
        }
        super.doStop();
    }
}
