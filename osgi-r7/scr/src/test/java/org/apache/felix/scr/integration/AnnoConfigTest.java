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
package org.apache.felix.scr.integration;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.annoconfig.AnnoComponent;
import org.apache.felix.scr.integration.components.annoconfig.AnnoComponent.A1;
import org.apache.felix.scr.integration.components.annoconfig.AnnoComponent.A1Arrays;
import org.apache.felix.scr.integration.components.annoconfig.AnnoComponent.E1;
import org.apache.felix.scr.integration.components.annoconfig.NestedAnnoComponent;
import org.apache.felix.scr.integration.components.annoconfig.NestedAnnoComponent.A2;
import org.apache.felix.scr.integration.components.annoconfig.NestedAnnoComponent.B2;
import org.apache.felix.scr.integration.components.annoconfig.NestedAnnoComponent.E2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

@RunWith(JUnit4TestRunner.class)
public class AnnoConfigTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
//        paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_annoconfig.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".annoconfig";
   }
    
    @Test
    public void testAnnoConfig() throws Exception
    {
        String name = "org.apache.felix.scr.integration.components.annoconfig";
        ComponentConfigurationDTO dto = findComponentConfigurationByName(name, ComponentConfigurationDTO.SATISFIED);
        AnnoComponent ac = getServiceFromConfiguration(dto, AnnoComponent.class);
        checkA1NoValues(ac.m_a1_activate);
        checkA1ArraysNoValues(ac.m_a1Arrays_activate);
        
        Configuration c = configure(name, null, allValues());
        delay();
        
        checkA1(ac.m_a1_modified);
        checkA1Array(ac.m_a1Arrays_modified);

        ungetServiceFromConfiguration(dto, AnnoComponent.class);
        checkA1(ac.m_a1_deactivate);
        checkA1Array(ac.m_a1Arrays_deactivate);
        ac = getServiceFromConfiguration(dto, AnnoComponent.class);
        checkA1(ac.m_a1_activate);
        checkA1Array(ac.m_a1Arrays_activate);
        
        c.delete();
        delay();
        
        checkA1NoValues(ac.m_a1_modified);
        checkA1ArraysNoValues(ac.m_a1Arrays_modified);
        
        c = configure(name, null, arrayValues());
        delay();
        
        checkA1FromArray(ac.m_a1_modified);
        checkA1ArrayFromArray(ac.m_a1Arrays_modified, false);
        
        c.delete();
        delay();
        
        checkA1NoValues(ac.m_a1_modified);
        checkA1ArraysNoValues(ac.m_a1Arrays_modified);
        
        c = configure(name, null, collectionValues());
        delay();
        
        checkA1FromArray(ac.m_a1_modified);
        checkA1ArrayFromArray(ac.m_a1Arrays_modified, true);

    }
    
    private Hashtable<String, Object> allValues()
    {
        Hashtable<String, Object> values = new Hashtable<String, Object>();
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

    private Hashtable<String, Object> arrayValues()
    {
        Hashtable<String, Object> values = new Hashtable<String, Object>();
        values.put("bool", new boolean[] {true, false});
        values.put("byt", new byte[] {12, 3});
        values.put("clas", new String[] {String.class.getName(), Integer.class.getName()});
        values.put("e1", new String[] {E1.a.name(), E1.b.name()});
        values.put("doubl", new double[] {3.14, 2.78, 9});
        values.put("floa", new float[] {500, 37.44f});
        values.put("integer", new int[] {3, 6, 9});
        values.put("lon", new long[] {12345678l, -1});
        values.put("shor", new short[] {3, 88});
        values.put("string", new String[] {});
        return values;
    }
    
    private Hashtable<String, Object> collectionValues()
    {
        Hashtable<String, Object> values = arrayValues();
        Hashtable<String, Object> collectionValues = new Hashtable<String, Object>();
        for (Map.Entry<String, Object> entry: values.entrySet())
        {
            collectionValues.put(entry.getKey(), toList(entry.getValue()));
        }
        //yuck
        collectionValues.remove("string");
        return collectionValues;
    }
    
    private List<?> toList(Object value)
    {
        List result = new ArrayList();
        for (int i = 0; i < Array.getLength(value); i++)
        {
            result.add(Array.get(value, i));
        }
        return result;
    }

    private void checkA1(A1 a)
    {
        TestCase.assertEquals(true, a.bool());
        TestCase.assertEquals((byte)12, a.byt());
        TestCase.assertEquals(String.class, a.clas());
        TestCase.assertEquals(E1.a, a.e1());
        TestCase.assertEquals(3.14d, a.doubl());
        TestCase.assertEquals(500f, a.floa());
        TestCase.assertEquals(3, a.integer());
        TestCase.assertEquals(12345678l,  a.lon());
        TestCase.assertEquals((short)3, a.shor());
        TestCase.assertEquals("3", a.string());
    }


    private void checkA1FromArray(A1 a)
    {        
        TestCase.assertEquals(true, a.bool());
        TestCase.assertEquals((byte)12, a.byt());
        TestCase.assertEquals(String.class, a.clas());
        TestCase.assertEquals(E1.a, a.e1());
        TestCase.assertEquals(3.14d, a.doubl());
        TestCase.assertEquals(500f, a.floa());
        TestCase.assertEquals(3, a.integer());
        TestCase.assertEquals(12345678l,  a.lon());
        TestCase.assertEquals((short)3, a.shor());
        TestCase.assertEquals(null, a.string());
    }
    
    private void checkA1Array(A1Arrays a)
    {
        assertArrayEquals(new boolean[] {true}, a.bool());
        assertArrayEquals(new byte[] {(byte)12}, a.byt());
        assertArrayEquals(new Class<?>[] {String.class}, a.clas());
        assertArrayEquals(new E1[] {E1.a}, a.e1());
        assertArrayEquals(new double[] {3.14d}, a.doubl());
        assertArrayEquals(new float[] {500f}, a.floa());
        assertArrayEquals(new int[] {3}, a.integer());
        assertArrayEquals(new long[] {12345678l},  a.lon());
        assertArrayEquals(new short[] {(short)3}, a.shor());
        assertArrayEquals(new String[] {"3"}, a.string());
    }
    
    private void checkA1ArrayFromArray(A1Arrays a, boolean caBug)
    {
        assertArrayEquals(new boolean[] {true, false}, a.bool());
        assertArrayEquals(new byte[] {12, 3}, a.byt());
        assertArrayEquals(new Class<?>[] {String.class, Integer.class}, a.clas());
        assertArrayEquals(new E1[] {E1.a, E1.b}, a.e1());
        assertArrayEquals(new double[] {3.14, 2.78, 9}, a.doubl());
        assertArrayEquals(new float[] {500f, 37.44f}, a.floa());
        assertArrayEquals(new int[] {3, 6, 9}, a.integer());
        assertArrayEquals(new long[] {12345678l, -1},  a.lon());
        assertArrayEquals(new short[] {(short)3, 88}, a.shor());
        if (!caBug)
        {
            assertArrayEquals(new String[] {}, a.string());
        }
    }
    
    private void assertArrayEquals(Object a, Object b)
    {
        TestCase.assertTrue(a.getClass().isArray());
        TestCase.assertTrue(b.getClass().isArray());
        TestCase.assertEquals("wrong length", Array.getLength(a), Array.getLength(b));
        TestCase.assertEquals("wrong type", a.getClass().getComponentType(), b.getClass().getComponentType());
        for (int i = 0; i < Array.getLength(a); i++)
        {
            TestCase.assertEquals("different value at " + i, Array.get(a, i), Array.get(b, i));
        }
        
    }
    
    private void checkA1NoValues(A1 a)
    {
        TestCase.assertEquals(false, a.bool());
        TestCase.assertEquals((byte)0, a.byt());
        TestCase.assertEquals(null, a.clas());
        TestCase.assertEquals(null, a.e1());
        TestCase.assertEquals(0d, a.doubl());
        TestCase.assertEquals(0f, a.floa());
        TestCase.assertEquals(0, a.integer());
        TestCase.assertEquals(0l,  a.lon());
        TestCase.assertEquals((short)0, a.shor());
        TestCase.assertEquals(null, a.string());
    }

    private void checkA1ArraysNoValues(A1Arrays a)
    {
        TestCase.assertEquals(null, a.bool());
        TestCase.assertEquals(null, a.byt());
        TestCase.assertEquals(null, a.clas());
        TestCase.assertEquals(null, a.e1());
        TestCase.assertEquals(null, a.doubl());
        TestCase.assertEquals(null, a.floa());
        TestCase.assertEquals(null, a.integer());
        TestCase.assertEquals(null,  a.lon());
        TestCase.assertEquals(null, a.shor());
        TestCase.assertEquals(null, a.string());
    }
    
    @Test
    public void testNestedAnnoConfig() throws Exception
    {
        String name = "org.apache.felix.scr.integration.components.nestedannoconfig";
        ComponentConfigurationDTO dto = findComponentConfigurationByName(name, ComponentConfigurationDTO.SATISFIED);
        NestedAnnoComponent ac = getServiceFromConfiguration(dto, NestedAnnoComponent.class);
        checkA2NoValues(ac.m_a2_activate);
        
        Configuration c = configure(name, null, allNestedValues());
        delay();
        
        checkA2(ac.m_a2_modified);

        ungetServiceFromConfiguration(dto, NestedAnnoComponent.class);
        checkA2(ac.m_a2_deactivate);
        ac = getServiceFromConfiguration(dto, NestedAnnoComponent.class);
        checkA2(ac.m_a2_activate);
        

    }
    private Hashtable<String, Object> allNestedValues()
    {
        Hashtable<String, Object> values = new Hashtable<String, Object>();
        values.put("b2.0.bool", "true");
        values.put("b2.0.e2", E2.a.toString());
        values.put("b2s.0.bool", "true");
        values.put("b2s.0.e2", E2.a.toString());
        values.put("b2s.1.bool", "true");
        values.put("b2s.1.e2", E2.b.toString());
        values.put("b2s.2.bool", "true");
        values.put("b2s.2.e2", E2.c.toString());
        return values;
    }

    private void checkA2NoValues(A2 a)
    {
        TestCase.assertEquals(0, a.b2s().length);
    }
    private void checkA2(A2 a)
    {
        checkB2(a.b2(), E2.a);
        TestCase.assertNull(a.b2null());

        TestCase.assertEquals(3, a.b2s().length);
        checkB2(a.b2s()[0], E2.a);
        checkB2(a.b2s()[1], E2.b);
        checkB2(a.b2s()[2], E2.c);
    }

	private void checkB2(B2 b, E2 e2) {
		TestCase.assertEquals(true, b.bool());
		TestCase.assertEquals(e2, b.e2());
	}

}
