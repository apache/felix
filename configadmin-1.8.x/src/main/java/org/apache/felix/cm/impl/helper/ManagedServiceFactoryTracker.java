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
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;

import org.apache.felix.cm.impl.ConfigurationManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class ManagedServiceFactoryTracker extends BaseTracker<ManagedServiceFactory>
{

    public ManagedServiceFactoryTracker( ConfigurationManager cm )
    {
        super( cm, true );
    }


    @Override
    protected ConfigurationMap<?> createConfigurationMap( String[] pids )
    {
        return new ManagedServiceFactoryConfigurationMap( pids );
    }


    /**
     * Always returns the raw PID because for a ManagedServiceFactory
     * the configuration's PID is automatically generated and is not a
     * real targeted PID.
     */
    @Override
    public String getServicePid( ServiceReference<ManagedServiceFactory> service, TargetedPID pid )
    {
        return pid.getRawPid();
    }


    @Override
    public void provideConfiguration( ServiceReference<ManagedServiceFactory> reference, TargetedPID configPid,
        TargetedPID factoryPid, Dictionary<String, ?> properties, long revision, ConfigurationMap<?> configs )
    {
        // Get the ManagedServiceFactory and terminate here if already
        // unregistered from the framework concurrently
        ManagedServiceFactory service = getRealService( reference );
        if (service == null) {
            return;
        }

        // Get the Configuration-to-PID map from the parameter or from
        // the service tracker. If not available, the service tracker
        // already unregistered this service concurrently
        if ( configs == null )
        {
            configs =  this.getService( reference );
            if ( configs == null )
            {
                return;
            }
        }

        // Both the ManagedService to update and the Configuration-to-PID
        // are available, so the service can be updated with the
        // configuration (which may be null)

        if ( configs.shallTake( configPid, factoryPid, revision ) )
        {
            try
            {
                Dictionary props = getProperties( properties, reference, configPid.toString(),
                    factoryPid.toString() );
                updated( service, configPid.toString(), props );
                configs.record( configPid, factoryPid, revision );
            }
            catch ( Throwable t )
            {
                this.handleCallBackError( t, reference, configPid );
            }
            finally
            {
                this.ungetRealService( reference );
            }
        }
    }


    @Override
    public void removeConfiguration( ServiceReference<ManagedServiceFactory> reference, TargetedPID configPid,
        TargetedPID factoryPid )
    {
        final ManagedServiceFactory service = this.getRealService( reference );
        final ConfigurationMap configs = this.getService( reference );
        if ( service != null && configs != null)
        {
            if ( configs.removeConfiguration( configPid, factoryPid ) )
            {
                try
                {
                    deleted( service, configPid.toString() );
                    configs.record( configPid, factoryPid, -1 );
                }
                catch ( Throwable t )
                {
                    this.handleCallBackError( t, reference, configPid );
                }
                finally
                {
                    this.ungetRealService( reference );
                }
            }
        }
    }


    private void updated( final ManagedServiceFactory service, final String pid, final Dictionary properties )
        throws ConfigurationException
    {
        if ( System.getSecurityManager() != null )
        {
            try
            {
                AccessController.doPrivileged( new PrivilegedExceptionAction()
                {
                    public Object run() throws ConfigurationException
                    {
                        service.updated( pid, properties );
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
            service.updated( pid, properties );
        }
    }


    private void deleted( final ManagedServiceFactory service, final String pid )
    {
        if ( System.getSecurityManager() != null )
        {
            AccessController.doPrivileged( new PrivilegedAction()
            {
                public Object run()
                {
                    service.deleted( pid );
                    return null;
                }
            }, getAccessControlContext( service ) );
        }
        else
        {
            service.deleted( pid );
        }
    }
}