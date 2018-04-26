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
package org.apache.felix.scr.integration.components.annoconfig;




public class NestedAnnoComponent
{

    public enum E2 {a, b, c}
    
    public @interface A2 {
        B2 b2();
        B2 b2null();
        B2[] b2s();
    }
    
    public @interface B2 {
        boolean bool();
        E2 e2();
    }
    
    
    public A2 m_a2_activate;
    public A2 m_a2_modified;
    public A2 m_a2_deactivate;
    

    @SuppressWarnings("unused")
    private void activate( A2 a1 )
    {
        m_a2_activate = a1;
    }



    @SuppressWarnings("unused")
    private void modified( A2 a1)
    {
        m_a2_modified = a1;
    }
    
    @SuppressWarnings("unused")
    private void deactivate( A2 a1 )
    {
        m_a2_deactivate = a1;
    }


}
