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


public class ManagedServiceFactoryConfigurationMap extends ConfigurationMap<Map<TargetedPID, Long>>
{

    protected ManagedServiceFactoryConfigurationMap( String[] configuredPids )
    {
        super( configuredPids );
    }


    @Override
    protected Map<String, Map<TargetedPID, Long>> createMap( int size )
    {
        return new HashMap<String, Map<TargetedPID, Long>>( size );
    }


    @Override
    protected boolean shallTake( TargetedPID configPid, TargetedPID factoryPid, long revision )
    {
        Map<TargetedPID, Long> configs = this.get( factoryPid );

        // no configuration yet, yes we can
        if (configs == null) {
            return true;
        }

        Long rev = configs.get( configPid );

        // this config is missing, yes we can
        if (rev == null) {
            return true;
        }

        // finally take if newer
        return rev < revision;
    }


    @Override
    protected boolean removeConfiguration( TargetedPID configPid, TargetedPID factoryPid )
    {
        Map<TargetedPID, Long> configs = this.get( factoryPid );
        return configs != null && configs.containsKey( configPid );
    }


    @Override
    protected void record( TargetedPID configPid, TargetedPID factoryPid, long revision )
    {
        Map<TargetedPID, Long> configs = this.get( factoryPid );

        if (configs == null) {
            configs = new HashMap<TargetedPID, Long>( 4 );
        }

        if (revision < 0) {
            configs.remove( configPid );
        } else {
            configs.put(configPid, revision);
        }

        if (configs.size() == 0) {
            configs = null;
        }

        this.put( factoryPid, configs );
    }
}
