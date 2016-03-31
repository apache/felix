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
package org.apache.felix.scr.integration.components;


import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.component.ExtComponentContext;
import org.osgi.service.component.ComponentContext;


public class MutatingServiceImpl implements MutatingService
{
    private ComponentContext activateContext;

    private void activate( ComponentContext activateContext )
    {
        this.activateContext = activateContext;
    }

    private void modified( ComponentContext activateContext )
    {

    }

    private Map activateMutate( ComponentContext activateContext )
    {
        this.activateContext = activateContext;
        Map result = new Hashtable( (Map )activateContext.getProperties() );
        if (activateContext.getServiceReference() != null) 
        {
            result.put( "theValue", "anotherValue1" );
        }
        if (result.containsKey( ".p2" ))
        {
            result.put( ".theValue", "privateValue" );
        }
        return result;
    }

    private Map modifiedMutate( ComponentContext activateContext )
    {
        Map result = new Hashtable( (Map )activateContext.getProperties() );
        result.put( "theValue", "anotherValue2" );
        return result;
    }

    private Map deactivateMutate( ComponentContext activateContext )
    {
        Map result = new Hashtable( (Map )activateContext.getProperties() );
        result.put( "theValue", "anotherValue3" );
        return result;
    }

    public void updateProperties( Dictionary changes )
    {
        ( ( ExtComponentContext ) activateContext ).setServiceProperties( changes );
    }

    private Map bindSimpleService( SimpleService ss )
    {
        return Collections.singletonMap( "SimpleService", "bound" );
    }

    private Map unbindSimpleService( SimpleService ss )
    {
        return Collections.singletonMap( "SimpleService", "unbound" );
    }

    private Map updateSimpleService( SimpleService ss )
    {
        return Collections.singletonMap( "SimpleService", "updated" );
    }


}
