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

package org.apache.felix.jaas.internal;

import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator
{

    private BundleLoginModuleCreator loginModuleCreator;
    private JaasConfigFactory jaasConfigFactory;
    private ConfigSpiOsgi configSpi;
    private JaasWebConsolePlugin webConsolePlugin;
    private Logger logger;

    @Override
    public void start(BundleContext context) throws Exception
    {
        logger = new Logger(context);
        loginModuleCreator = new BundleLoginModuleCreator(context, logger);
        jaasConfigFactory = new JaasConfigFactory(context, loginModuleCreator, logger);
        configSpi = new ConfigSpiOsgi(context, logger);
        registerWebConsolePlugin(context);

        logger.open();
        loginModuleCreator.open();
        configSpi.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        if (loginModuleCreator != null)
        {
            loginModuleCreator.close();
        }

        if (configSpi != null)
        {
            configSpi.close();
        }

        if (logger != null)
        {
            logger.close();
        }
    }

    private void registerWebConsolePlugin(BundleContext context){
        Properties props = new Properties();
        props.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        props.put(Constants.SERVICE_DESCRIPTION, "JAAS Web Console Plugin");
        props.put("felix.webconsole.label", "jaas");
        props.put("felix.webconsole.title", "JAAS");
        props.put("felix.webconsole.configprinter.modes", "always");

        //Registering a ServiceFactory to avoid dependency on Servlet API
        //on startup
        context.registerService("javax.servlet.Servlet", new PluginServiceFactory(), props);
    }

    private class PluginServiceFactory implements ServiceFactory {

        @Override
        public Object getService(Bundle bundle, ServiceRegistration registration) {
            return new JaasWebConsolePlugin(configSpi,loginModuleCreator);
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        }
    }

}
