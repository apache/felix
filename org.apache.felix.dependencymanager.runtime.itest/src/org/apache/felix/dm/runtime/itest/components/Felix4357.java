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
package org.apache.felix.dm.runtime.itest.components;

import org.junit.Assert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Checks support of primitive types for @Property annotation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(properties={
        @Property(name="v1", value="s"),
        @Property(name="v2", value={"s1", "s2"}),
        @Property(name="v3", values={"s1", "s2"}),

        @Property(name="v4", value="1", type=Long.class),
        @Property(name="v5", longValue=1),
        @Property(name="v6", longValue={1, 2}),
        
        @Property(name="v7", value="1", type=Double.class),
        @Property(name="v8", doubleValue=1),
        @Property(name="v9", doubleValue={1, 2}),

        @Property(name="v10", value="1", type=Float.class),
        @Property(name="v11", floatValue=1),
        @Property(name="v12", floatValue={1, 2}),

        @Property(name="v13", value="1", type=Integer.class),
        @Property(name="v14", intValue=1),
        @Property(name="v15", intValue={1, 2}),

        @Property(name="v16", value="65", type=Byte.class),
        @Property(name="v17", byteValue=65),
        @Property(name="v18", byteValue={65, 66}),

        @Property(name="v19", value="A", type=Character.class),
        @Property(name="v20", charValue='A'),
        @Property(name="v21", charValue={'A', 'B'}),

        @Property(name="v22", value="true", type=Boolean.class),
        @Property(name="v23", booleanValue=true),
        @Property(name="v24", booleanValue={true, false}),

        @Property(name="v25", value="1", type=Short.class),
        @Property(name="v26", shortValue=1),
        @Property(name="v27", shortValue={1, 2}),
        
        @Property(name="v28", value="65", type=Character.class),
        @Property(name="v29", charValue=65),
        @Property(name="v30", charValue={65, 66}),
    },
    provides=Felix4357.class
)
public class Felix4357 {
    public final static String ENSURE = "Felix4357";
    
    @ServiceDependency(filter = "(name=" + ENSURE + ")")
    volatile Ensure m_ensure;
    
    @Registered
    void registered(ServiceRegistration sr) {
        ServiceReference ref = sr.getReference();
        assertEquals(ref, "v1", "s", 1);
        assertArrayEquals(ref, "v2", new String[] {"s1", "s2"}, 2);        
        assertArrayEquals(ref, "v3", new String[] {"s1", "s2"}, 3);                        
        assertEquals(ref, "v4", new Long(1), 4);
        assertEquals(ref, "v5", new Long(1), 5);
        assertArrayEquals(ref, "v6", new Long[] { 1L, 2L } , 6);
        assertEquals(ref, "v7", new Double(1), 7);
        assertEquals(ref, "v8", new Double(1), 8);
        assertArrayEquals(ref, "v9", new Double[] { 1.0, 2.0 } , 9);
        assertEquals(ref, "v10", new Float(1), 10);
        assertEquals(ref, "v11", new Float(1), 11);
        assertArrayEquals(ref, "v12", new Float[] { 1.f, 2.f } , 12);
        assertEquals(ref, "v13", new Integer(1), 13);
        assertEquals(ref, "v14", new Integer(1), 14);
        assertArrayEquals(ref, "v15", new Integer[] { 1, 2 } , 15);
        assertEquals(ref, "v16", Byte.valueOf("65"), 16);
        assertEquals(ref, "v17", Byte.valueOf("65"), 17);
        assertArrayEquals(ref, "v18", new Byte[] { Byte.valueOf("65"), Byte.valueOf("66") } , 18);
        assertEquals(ref, "v19", Character.valueOf('A'), 19);
        assertEquals(ref, "v20", Character.valueOf('A'), 20);
        assertArrayEquals(ref, "v21", new Character[] { 'A', 'B' } , 21);
        assertEquals(ref, "v22", Boolean.valueOf(true), 22);
        assertEquals(ref, "v23", Boolean.valueOf(true), 23);
        assertArrayEquals(ref, "v24", new Boolean[] { true, false } , 24);
        assertEquals(ref, "v25", Short.valueOf((short) 1), 25);
        assertEquals(ref, "v26", Short.valueOf((short) 1), 26);
        assertArrayEquals(ref, "v27", new Short[] { 1, 2 } , 27);
        assertEquals(ref, "v28", Character.valueOf('A'), 28);
        assertEquals(ref, "v29", Character.valueOf('A'), 29);
        assertArrayEquals(ref, "v30", new Character[] { 'A', 'B' } , 30);
    }
    
    void assertEquals(ServiceReference ref, String property, Object expected, int step) {
        Object value = ref.getProperty(property);
        Assert.assertNotNull(value);
        Assert.assertEquals(value.getClass(), expected.getClass());
        Assert.assertEquals(value, expected);        
        m_ensure.step(step);
    }
    
    void assertArrayEquals(ServiceReference ref, String property, Object[] expected, int step) {
        Object values = ref.getProperty(property);
        Assert.assertNotNull(values);
        Assert.assertTrue(values.getClass().isArray());
        Assert.assertEquals(values.getClass(), expected.getClass());
        Object[] array = (Object[]) values;
        Assert.assertEquals(array.length, expected.length);
        for (int i = 0; i < array.length; i ++) {
            Assert.assertEquals(array[i].getClass(), expected[i].getClass());
            Assert.assertEquals(array[i], expected[i]);
        }
        m_ensure.step(step);
    }
}
