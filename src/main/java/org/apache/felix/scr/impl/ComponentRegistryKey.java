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
package org.apache.felix.scr.impl;


import org.osgi.framework.Bundle;


/**
 * The <code>ComponentRegistryKey</code> isused as the key in the
 * component register to register components by their names.
 * <p>
 * Two instances of this class are equal if they are the same or if there
 * component name and bundle ID is equal.
 */
final class ComponentRegistryKey
{

    private final long bundleId;
    private final String componentName;


    ComponentRegistryKey( final Bundle bundle, final String componentName )
    {
        this.bundleId = bundle.getBundleId();
        this.componentName = componentName;
    }


    public int hashCode()
    {
        int code = ( int ) this.bundleId;
        code += 31 * this.componentName.hashCode();
        return code;
    }


    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj instanceof ComponentRegistryKey )
        {
            ComponentRegistryKey other = ( ComponentRegistryKey ) obj;
            return this.bundleId == other.bundleId && this.componentName.equals( other.componentName );
        }

        return false;
    }


    public long getBundleId()
    {
        return bundleId;
    }


    public String getComponentName()
    {
        return componentName;
    }
    
    
}
