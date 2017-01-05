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


import junit.framework.TestCase;


public class ComponentRegistryKeyTest extends TestCase
{

    private final ComponentRegistryKey b1_a_0 = key( 1, "a" );
    private final ComponentRegistryKey b2_a_0 = key( 2, "a" );

    private final ComponentRegistryKey b1_a_1 = key( 1, "a" );
    private final ComponentRegistryKey b2_a_1 = key( 2, "a" );

    private final ComponentRegistryKey b1_b = key( 1, "b" );
    private final ComponentRegistryKey b2_b = key( 2, "b" );


    public void test_globally_unique_key()
    {
        // same
        TestCase.assertEquals( b1_a_0, b1_a_0 );

        // equals both ways
        TestCase.assertEquals( b1_a_0, b1_a_1 );
        TestCase.assertEquals( b1_a_1, b1_a_0 );

        // not equals both ways
        TestCase.assertFalse( b1_a_0.equals( b2_a_0 ) );
        TestCase.assertFalse( b2_a_0.equals( b1_a_0 ) );

        // not equals both ways
        TestCase.assertFalse( b1_a_0.equals( b1_b ) );
        TestCase.assertFalse( b1_b.equals( b1_a_0 ) );

        // not equals both ways
        TestCase.assertFalse( b1_a_0.equals( b2_b ) );
        TestCase.assertFalse( b2_b.equals( b1_a_0 ) );
    }


    private static ComponentRegistryKey key( final long bundleId, final String name )
    {
        return new ComponentRegistryKey( new MockBundle()
        {
//            @Override
            public long getBundleId()
            {
                return bundleId;
            }
        }, name );
    }
}
