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
package org.apache.felix.webconsole.internal.servlet;


import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


class ConfigurationSupport implements ManagedService
{

    protected final OsgiManager osgiManager;


    ConfigurationSupport( final OsgiManager osgiManager )
    {
        this.osgiManager = osgiManager;
    }


    //---------- ManagedService
    public void updated( final Dictionary config ) throws ConfigurationException
    {
        if (null != System.getSecurityManager())
        {
            try
            {
                AccessController.doPrivileged(new PrivilegedExceptionAction()
                {
                    public Object run() throws Exception
                    {
                        updated0(config);
                        return null;
                    }
                });
            }
            catch (PrivilegedActionException e)
            {
                final Exception x = e.getException();
                if (x instanceof ConfigurationException)
                {
                    throw (ConfigurationException) x;
                }
                else
                {
                    throw new ConfigurationException("?", "Update failed", x);
                }
            }
        }
        else
        {
            updated0(config);
        }
    }

    void updated0( Dictionary config ) throws ConfigurationException
    {
        // validate hashed password
        if ( isPasswordHashed( config ) )
        {
            osgiManager.updateConfiguration( config );
        }
        else
        {
            // hash the password, update config and wait for the
            // updated configuration to be supplied later
            final BundleContext bc = this.osgiManager.getBundleContext();
            final ServiceReference ref = bc.getServiceReference( ConfigurationAdmin.class.getName() );
            if ( ref != null )
            {
                final ConfigurationAdmin ca = ( ConfigurationAdmin ) bc.getService( ref );
                if ( ca != null )
                {
                    try
                    {
                        Configuration cfg = ca.getConfiguration( this.osgiManager.getConfigurationPid() );
                        Dictionary newConfig = cfg.getProperties();
                        if ( newConfig != null )
                        {
                            String pwd = ( String ) config.get( OsgiManager.PROP_PASSWORD );
                            // password can be null, see FELIX-4995
                            final String hashedPassword = null == pwd 
                                ? OsgiManager.DEFAULT_PASSWORD
                                : Password.hashPassword( pwd );
                            newConfig.put( OsgiManager.PROP_PASSWORD, hashedPassword );
                            cfg.update( newConfig );
                        }
                    }
                    catch ( Exception e )
                    {
                        // IOException from getting/updated config
                        // IllegalStateException from hashing password
                        throw new ConfigurationException( OsgiManager.PROP_PASSWORD, "Cannot update password property",
                            e );
                    }
                    finally
                    {
                        bc.ungetService( ref );
                    }
                }
            }
        }
    }


    private boolean isPasswordHashed( final Dictionary config )
    {
        // assume hashed (default) password if no config
        if ( config == null )
        {
            return true;
        }

        // assume hashed (default) password if no password property
        final Object pwd = config.get( OsgiManager.PROP_PASSWORD );
        return (pwd instanceof String) && Password.isPasswordHashed((String) pwd);
    }

}