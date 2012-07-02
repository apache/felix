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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigurationMap
{
    private Map<String, Object> configurations;


    public ConfigurationMap( final String[] configuredPids )
    {
        this.configurations = Collections.emptyMap();
        setConfiguredPids( configuredPids );
    }

    public boolean accepts(final String servicePid) {
        return configurations.containsKey( servicePid );
    }

    public void setConfiguredPids( String[] configuredPids )
    {
        HashMap<String, Object> newConfigs = new HashMap<String, Object>();
        if ( configuredPids != null )
        {
            for ( String pid : configuredPids )
            {
                newConfigs.put( pid, this.configurations.get( pid ) );
            }
        }
        this.configurations = newConfigs;
    }


    public boolean isDifferentPids( final String[] pids )
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