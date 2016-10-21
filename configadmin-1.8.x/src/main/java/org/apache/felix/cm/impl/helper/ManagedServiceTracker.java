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
package org.apache.felix.cm.impl.helper;


import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.cm.impl.ConfigurationManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


public class ManagedServiceTracker extends BaseTracker<ManagedService>
{

    private static final Dictionary<String, ?> INITIAL_MARKER = new Hashtable<String, Object>( 0 );


    public ManagedServiceTracker( ConfigurationManager cm )
    {
        super( cm, false );
    }


    @Override
    protected ConfigurationMap<?> createConfigurationMap( String[] pids )
    {
        return new ManagedServiceConfigurationMap( pids );
    }


    @Override
    public String getServicePid( ServiceReference<ManagedService> service, TargetedPID pid )
    {
        final ConfigurationMap configs = this.getService( service );
        if ( configs != null )
        {
            return configs.getKeyPid( pid );
        }

        // this service is not handled...
        return null;
    }


    /**
     * Provides the given configuration to the managed service.
     * <p>
     * Depending on targeted PIDs this configuration may not actually be
     * provided if the service already has more strictly binding
     * configuration from a targeted configuration bound.
     * <p>
     * If the revision is a negative value, the provided configuration
     * is assigned to the ManagedService in any case without further
     * checks. This allows a replacement configuration for a deleted
     * or invisible configuration to be assigned without first removing
     * the deleted or invisible configuration.
     */
    @Override
    public void provideConfiguration( ServiceReference<ManagedService> service, TargetedPID configPid,
        TargetedPID factoryPid, Dictionary<String, ?> properties, long revision, ConfigurationMap<?> configs )
    {
        Dictionary<String, ?> supplied = ( properties == null ) ? INITIAL_MARKER : properties;
        updateService( service, configPid, supplied, revision, configs );
    }


    @Override
    public void removeConfiguration( ServiceReference<ManagedService> service, TargetedPID configPid,
        TargetedPID factoryPid )
    {
        updateService( service, configPid, null, -1, null );
    }


    private void updateService( ServiceReference<ManagedService> service, final TargetedPID configPid,
        Dictionary<String, ?> properties, long revision, ConfigurationMap<?> configs)
    {
        // Get the ManagedService and terminate here if already
        // unregistered from the framework concurrently
        final ManagedService srv = this.getRealService( service );
        if (srv == null) {
            return;
        }

        // Get the Configuration-to-PID map from the parameter or from
        // the service tracker. If not available, the service tracker
        // already unregistered this service concurrently
        if ( configs == null )
        {
            configs = this.getService( service );
            if ( configs == null )
            {
                return;
            }
        }

        // Both the ManagedService to update and the Configuration-to-PID
        // are available, so the service can be updated with the
        // configuration (which may be null)

        boolean doUpdate = false;
        if ( properties == null )
        {
            doUpdate = configs.removeConfiguration( configPid, null );
        }
        else if ( properties == INITIAL_MARKER )
        {
            // initial call to ManagedService may supply null properties
            properties = null;
            revision = -1;
            doUpdate = true;
        }
        else if ( revision < 0 || configs.shallTake( configPid, null, revision ) )
        {
            // run the plugins and cause the update
            properties = getProperties( properties, service, configPid.toString(), null );
            doUpdate = true;
            revision = Math.abs( revision );
        }
        else
        {
            // new configuration is not a better match, don't update
            doUpdate = false;
        }

        if ( doUpdate )
        {
            try
            {
                updated( srv, properties );
                configs.record( configPid, null, revision );
            }
            catch ( Throwable t )
            {
                this.handleCallBackError( t, service, configPid );
            }
            finally
            {
                this.ungetRealService( service );
            }
        }
    }


    private void updated( final ManagedService service, final Dictionary properties ) throws ConfigurationException
    {
        if ( System.getSecurityManager() != null )
        {
            try
            {
                AccessController.doPrivileged( new PrivilegedExceptionAction()
                {
                    public Object run() throws ConfigurationException
                    {
                        service.updated( properties );
                        return null;
                    }
                }, getAccessControlContext( service ) );
            }
            catch ( PrivilegedActionException e )
            {
                throw ( ConfigurationException ) e.getException();
            }
        }
        else
        {
            service.updated( properties );
        }
    }
}