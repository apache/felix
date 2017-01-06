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


import java.util.HashMap;
import java.util.Map;


class ManagedServiceConfigurationMap extends ConfigurationMap<ManagedServiceConfigurationMap.Entry>
{

    protected ManagedServiceConfigurationMap( String[] configuredPids )
    {
        super( configuredPids );
    }


    @Override
    protected Map<String, Entry> createMap( int size )
    {
        return new HashMap<String, Entry>( size );
    }


    @Override
    protected boolean shallTake( TargetedPID configPid, TargetedPID factoryPid, long revision )
    {
        Entry entry = this.get( configPid );

        // no configuration assigned yet, take it
        if ( entry == null )
        {
            return true;
        }

        // compare revision numbers if raw PID is the same
        if ( configPid.equals( entry.targetedPid ) )
        {
            return revision > entry.revision;
        }

        // otherwise only take if targeted PID is more binding
        return configPid.bindsStronger( entry.targetedPid );
    }


    @Override
    protected boolean removeConfiguration( TargetedPID configPid, TargetedPID factoryPid )
    {
        Entry entry = this.get( configPid );

        // nothing to remove because the service does not know it anyway
        if ( entry == null )
        {
            return false;
        }

        // update if the used targeted PID matches
        if ( configPid.equals( entry.targetedPid ) )
        {
            return true;
        }

        // the config is not assigned and so there must not be a removal
        return false;
    }


    @Override
    protected void record( TargetedPID configPid, TargetedPID factoryPid, long revision )
    {
        final Entry entry = ( revision < 0 ) ? null : new Entry( configPid, revision );
        this.put( configPid, entry );
    }

    static class Entry
    {
        final TargetedPID targetedPid;
        final long revision;


        Entry( final TargetedPID targetedPid, final long revision )
        {
            this.targetedPid = targetedPid;
            this.revision = revision;
        }


        @Override
        public String toString()
        {
            return "Entry(pid=" + targetedPid + ",rev=" + revision + ")";
        }
    }
}
