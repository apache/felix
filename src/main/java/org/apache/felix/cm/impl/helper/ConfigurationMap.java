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


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class ConfigurationMap<T>
{
    private Map<String, T> configurations;


    protected ConfigurationMap( final String[] configuredPids )
    {
        this.configurations = Collections.emptyMap();
        setConfiguredPids( configuredPids );
    }


    protected abstract Map<String, T> createMap( int size );


    protected abstract boolean shallTake( TargetedPID configPid, TargetedPID factoryPid, long revision );


    protected abstract void record( TargetedPID configPid, TargetedPID factoryPid, long revision );


    protected abstract boolean removeConfiguration( TargetedPID configPid, TargetedPID factoryPid );


    protected T get( final TargetedPID key )
    {
        final String servicePid = getKeyPid( key );
        if ( servicePid != null )
        {
            return this.configurations.get( servicePid );
        }

        // the targeted PID does not match here
        return null;
    }


    protected void put( final TargetedPID key, final T value )
    {
        final String servicePid = getKeyPid( key );
        if ( servicePid != null )
        {
            this.configurations.put( servicePid, value );
        }
    }


    protected String getKeyPid( final TargetedPID targetedPid )
    {
        // regular use case: service PID is the key
        if ( this.accepts( targetedPid.getServicePid() ) )
        {
            return targetedPid.getServicePid();
        }

        // the raw PID is the key (if the service PID contains pipes)
        if ( this.accepts( targetedPid.getRawPid() ) )
        {
            return targetedPid.getRawPid();
        }

        // this is not really expected here
        return null;
    }


    /**
     * Returns <code>true</code> if this map is foreseen to take a
     * configuration with the given service PID.
     *
     * @param servicePid The service PID of the configuration which is
     *      the part of the targeted PID without the bundle's symbolic
     *      name, version, and location; i.e. {@link TargetedPID#getServicePid()}
     *
     * @return <code>true</code> if this map is configured to take
     *      configurations for the service PID.
     */
    public boolean accepts( final String servicePid )
    {
        return configurations.containsKey( servicePid );
    }


    public void setConfiguredPids( String[] configuredPids )
    {
        final Map<String, T> newConfigs;
        if ( configuredPids != null )
        {
            newConfigs = this.createMap( configuredPids.length );
            for ( String pid : configuredPids )
            {
                newConfigs.put( pid, this.configurations.get( pid ) );
            }
        }
        else
        {
            newConfigs = Collections.emptyMap();
        }
        this.configurations = newConfigs;
    }


    /**
     * Returns <code>true</code> if the set of service PIDs given is
     * different from the current set of service PIDs.
     * <p>
     * For comparison a <code>null</code> argument is considered to
     * be an empty set of service PIDs.
     *
     * @param pids The new set of service PIDs to be compared to the
     *      current set of service PIDs.
     * @return <code>true</code> if the set is different
     */
    boolean isDifferentPids( final String[] pids )
    {
        if ( this.configurations.isEmpty() && pids == null )
        {
            return false;
        }
        else if ( this.configurations.isEmpty() )
        {
            return true;
        }
        else if ( pids == null )
        {
            return true;
        }
        else if ( this.configurations.size() != pids.length )
        {
            return true;
        }
        else
        {
            Set<String> thisPids = this.configurations.keySet();
            HashSet<String> otherPids = new HashSet<String>( Arrays.asList( pids ) );
            return !thisPids.equals( otherPids );
        }
    }
}