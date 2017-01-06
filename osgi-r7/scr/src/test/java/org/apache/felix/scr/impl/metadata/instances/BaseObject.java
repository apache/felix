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
package org.apache.felix.scr.impl.metadata.instances;


import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


/**
 * The <code>BaseObject</code> is a base class providing a number of methods
 * to check. All methods take various combinations of arguments and return
 * a single helper string to indicate what method has been called.
 */
public class BaseObject
{

    private String m_calledMethod;


    public String getCalledMethod()
    {
        String cm = m_calledMethod;
        m_calledMethod = null;
        return cm;
    }


    protected void setCalledMethod(String calledMethod) {
        m_calledMethod = calledMethod;
    }

    private void activate_no_arg()
    {
        setCalledMethod( "activate_no_arg" );
    }


    protected void activate_comp( ComponentContext ctx )
    {
        setCalledMethod( "activate_comp" );
    }


    void activate_comp_bundle( ComponentContext ctx, BundleContext bundle )
    {
        setCalledMethod( "activate_comp_bundle" );
    }


    protected void activate_suitable( ComponentContext ctx )
    {
        setCalledMethod( "activate_suitable" );
    }

    //precedence rules

    private void activate_precedence_1( ComponentContext ctx )
    {
        setCalledMethod("activate_precedence_1_comp");
    }

    void activate_precedence_1( BundleContext bundleContext )
    {
        setCalledMethod("activate_precedence_1_bundleContext");
    }

    protected void activate_precedence_1( Map map)
    {
        setCalledMethod("activate_precedence_1_map");
    }

    private void activate_precedence_2( Map map )
    {
        setCalledMethod("activate_precedence_2_map");
    }

    void activate_precedence_2( ComponentContext ctx, BundleContext bundle )
    {
        setCalledMethod("activate_precedence_2_comp_bundleContext");
    }

    protected void activate_precedence_2()
    {
        setCalledMethod("activate_precedence_2_empty");
    }


    public @interface Ann1 { }
    public @interface Ann2 { }
   
    void activate_13_2_annotations(ComponentContext cc, Ann1 a1, BundleContext c, Ann2 a2, Map<String, Object> map, ComponentContext cc2) {
        setCalledMethod("activate_13_2_annotations");
    }
}
