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
package org.apache.felix.scr.impl.helper;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;

import junit.framework.TestCase;

public class AnnotationTest extends TestCase
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
    
    private Bundle mockBundle() throws ClassNotFoundException
    {
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.loadClass(String.class.getName())).andReturn((Class) String.class).anyTimes();
        EasyMock.replay(b);
        return b;
    }
    
    public void testA1() throws Exception
    {
        Map<String, Object> values = allValues();
        
        Object o = Annotations.toObject( A1.class, values, mockBundle());
        assertTrue("expected an A1", o instanceof A1);
        
        A1 a = (A1) o;
        assertEquals(true, a.bool());
        assertEquals((byte)12, a.byt());
        assertEquals(String.class, a.clas());
        assertEquals(E1.a, a.e1());
        assertEquals(3.14d, a.doubl());
        assertEquals(500f, a.floa());
        assertEquals(3, a.integer());
        assertEquals(12345678l,  a.lon());
        assertEquals((short)3, a.shor());
        assertEquals("3", a.string());
    }

    private Map<String, Object> allValues()
    {
        Map<String, Object> values = new HashMap();
        values.put("bool", "true");
        values.put("byt", 12l);
        values.put("clas", String.class.getName());
        values.put("e1", E1.a.toString());
        values.put("doubl", "3.14");
        values.put("floa", 500l);
        values.put("integer", 3.0d);
        values.put("lon", "12345678");
        values.put("shor", 3l);
        values.put("string", 3);
        return values;
    }

    public @interface A2 {
        boolean bool() default true;
        byte byt() default 5;
        Class<?> clas() default Integer.class;
        E1 e1() default E1.b;
        double doubl() default -2;
        float floa() default -4;
        int integer() default -5;
        long lon() default Long.MIN_VALUE;
        short shor() default -8;
        String string() default "default";
    }
    
    public void testA2AllValues() throws Exception
    {
        Map<String, Object> values = allValues();
        
        Object o = Annotations.toObject( A2.class, values, mockBundle());
        assertTrue("expected an A2", o instanceof A2);
        
        A2 a = (A2) o;
        assertEquals(true, a.bool());
        assertEquals((byte)12, a.byt());
        assertEquals(String.class, a.clas());
        assertEquals(E1.a, a.e1());
        assertEquals(3.14d, a.doubl());
        assertEquals(500f, a.floa());
        assertEquals(3, a.integer());
        assertEquals(12345678l,  a.lon());
        assertEquals((short)3, a.shor());
        assertEquals("3", a.string());
    }

}
