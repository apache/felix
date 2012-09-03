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


import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;


/**
 * The <code>ScrConfiguration</code> class conveys configuration for the
 * Felix DS implementation bundle.
 * <p>
 * <b>Configuration Source</b>
 * <p>
 * <ol>
 * <li>Framework properties: These are read when the Declarative Services
 * implementation is first started.</li>
 * <li>Configuration Admin Service: Properties are provided by means of a
 * <code>ManagedService</code> with Service PID
 * <code>org.apache.felix.scr.ScrService</code>. This class uses an OSGi
 * Service Factory ({@link ScrManagedServiceServiceFactory}) to register the
 * managed service without requiring the Configuration Admin Service API to be
 * required upfront.
 * </li>
 * </ol>
 * <p>
 * See the <i>Configuration</i> section of the
 * <a href="http://felix.apache.org/site/apache-felix-service-component-runtime.html">Apache Felix Service Component Runtime</a>
 * documentation page for detailed information.
 */
public class ScrConfiguration
{

    private static final String VALUE_TRUE = Boolean.TRUE.toString();

    public static final String PID = "org.apache.felix.scr.ScrService";

    public static final String PROP_FACTORY_ENABLED = "ds.factory.enabled";

    public static final String PROP_DELAYED_KEEP_INSTANCES = "ds.delayed.keepInstances";

    public static final String PROP_LOGLEVEL = "ds.loglevel";

    private static final String LOG_LEVEL_DEBUG = "debug";

    private static final String LOG_LEVEL_INFO = "info";

    private static final String LOG_LEVEL_WARN = "warn";

    private static final String LOG_LEVEL_ERROR = "error";

    private static final String PROP_SHOWTRACE = "ds.showtrace";

    private static final String PROP_SHOWERRORS = "ds.showerrors";

    private int logLevel;

    private boolean factoryEnabled;

    private boolean keepInstances;

    private BundleContext bundleContext;

    private ServiceRegistration managedService;

    public ScrConfiguration( )
    {
        // default configuration
        configure( null );
    }

    public void start(final BundleContext bundleContext){
        this.bundleContext = bundleContext;

        // reconfigure from bundle context properties
        configure( null );

        // listen for Configuration Admin configuration
        Dictionary props = new Hashtable();
        props.put(Constants.SERVICE_PID, PID);
        props.put(Constants.SERVICE_DESCRIPTION, "SCR Configurator");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        bundleContext.registerService("org.osgi.service.cm.ManagedService", new ScrManagedServiceServiceFactory(this),
            props);
    }

    public void stop() {
        if (this.managedService != null) {
            this.managedService.unregister();
            this.managedService = null;
        }

        this.bundleContext = null;
    }

    // Called from the ScrManagedService.updated method to reconfigure
    void configure( Dictionary config )
    {
        if ( config == null )
        {
            if ( this.bundleContext == null )
            {
                logLevel = LogService.LOG_ERROR;
                factoryEnabled = false;
                keepInstances = false;
            }
            else
            {
                logLevel = getDefaultLogLevel();
                factoryEnabled = getDefaultFactoryEnabled();
                keepInstances = getDefaultKeepInstances();
            }
        }
        else
        {
            logLevel = getLogLevel( config.get( PROP_LOGLEVEL ) );
            factoryEnabled = VALUE_TRUE.equalsIgnoreCase( String.valueOf( config.get( PROP_FACTORY_ENABLED ) ) );
            keepInstances = VALUE_TRUE.equalsIgnoreCase( String.valueOf( config.get( PROP_DELAYED_KEEP_INSTANCES ) ) );
        }
    }

    /**
     * Returns the current log level.
     * @return
     */
    public int getLogLevel()
    {
        return logLevel;
    }


    public boolean isFactoryEnabled()
    {
        return factoryEnabled;
    }


    public boolean keepInstances()
    {
        return keepInstances;
    }


    private boolean getDefaultFactoryEnabled()
    {
        return VALUE_TRUE.equals( bundleContext.getProperty( PROP_FACTORY_ENABLED ) );
    }


    private boolean getDefaultKeepInstances()
    {
        return VALUE_TRUE.equals( bundleContext.getProperty( PROP_DELAYED_KEEP_INSTANCES ) );
    }


    private int getDefaultLogLevel()
    {
        return getLogLevel( bundleContext.getProperty( PROP_LOGLEVEL ) );
    }


    private int getLogLevel( final Object levelObject )
    {
        if ( levelObject != null )
        {
            if ( levelObject instanceof Number )
            {
                return ( ( Number ) levelObject ).intValue();
            }

            String levelString = levelObject.toString();
            try
            {
                return Integer.parseInt( levelString );
            }
            catch ( NumberFormatException nfe )
            {
                // might be a descriptive name
            }

            if ( LOG_LEVEL_DEBUG.equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_DEBUG;
            }
            else if ( LOG_LEVEL_INFO.equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_INFO;
            }
            else if ( LOG_LEVEL_WARN.equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_WARNING;
            }
            else if ( LOG_LEVEL_ERROR.equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_ERROR;
            }
        }

        // check ds.showtrace property
        if ( VALUE_TRUE.equalsIgnoreCase( bundleContext.getProperty( PROP_SHOWTRACE ) ) )
        {
            return LogService.LOG_DEBUG;
        }

        // next check ds.showerrors property
        if ( "false".equalsIgnoreCase( bundleContext.getProperty( PROP_SHOWERRORS ) ) )
        {
            return -1; // no logging at all !!
        }

        // default log level (errors only)
        return LogService.LOG_ERROR;
    }
}
