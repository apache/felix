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


import java.util.Map;

import org.osgi.service.component.ComponentContext;


public class AnnoComponent
{

    public enum E1 {a, b, c}
    
    public @interface A1 {
        boolean bool();
        byte byt();
        Class<?> clas();
        E1 e1();
        double doubl();
        float floa();
        int integer();
        long lon();
        short shor();
        String string();
    }
    
    public @interface A1Arrays {
        boolean[] bool();
        byte[] byt();
        Class<?>[] clas();
        E1[] e1();
        double[] doubl();
        float[] floa();
        int[] integer();
        long[] lon();
        short[] shor();
        String[] string();
    }
    
    public A1 m_a1_activate;
    public A1Arrays m_a1Arrays_activate;
    public A1 m_a1_modified;
    public A1Arrays m_a1Arrays_modified;
    public A1 m_a1_deactivate;
    public A1Arrays m_a1Arrays_deactivate;
    

    @SuppressWarnings("unused")
    private void activate( ComponentContext activateContext, A1 a1, A1Arrays a1Arrays, Map<?, ?> config )
    {
        m_a1_activate = a1;
        m_a1Arrays_activate = a1Arrays;
    }



    @SuppressWarnings("unused")
    private void modified( ComponentContext context, A1 a1, A1Arrays a1Arrays)
    {
        m_a1_modified = a1;
        m_a1Arrays_modified = a1Arrays;
    }
    
    @SuppressWarnings("unused")
    private void deactivate( A1 a1, A1Arrays a1Arrays )
    {
        m_a1_deactivate = a1;
        m_a1Arrays_deactivate = a1Arrays;
    }


}
