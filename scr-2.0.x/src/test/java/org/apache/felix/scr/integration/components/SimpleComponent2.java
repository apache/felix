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


import java.util.ArrayList;
import java.util.List;


public class SimpleComponent2
{

    public static SimpleComponent2 INSTANCE;

    private List<String> bindings = new ArrayList<String>();


    public List<String> getBindings()
    {
        return bindings;
    }


    @SuppressWarnings("unused")
    private void activate()
    {
        INSTANCE = this;
    }


    @SuppressWarnings("unused")
    private void deactivate()
    {
        INSTANCE = null;
    }


    public void bindSimpleService( @SuppressWarnings("unused") SimpleService simpleService )
    {
        bindings.add( "bindSimpleService" );
    }


    public void unbindSimpleService( @SuppressWarnings("unused") SimpleService simpleService )
    {
        bindings.add( "unbindSimpleService" );
    }


    public void bindSimpleService2( @SuppressWarnings("unused") SimpleService2 simpleService2 )
    {
        bindings.add( "bindSimpleService2" );
    }


    public void unbindSimpleService2( @SuppressWarnings("unused") SimpleService2 simpleService2 )
    {
        bindings.add( "unbindSimpleService2" );
    }
}
