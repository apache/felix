/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.converter.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.TypeReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConverterMapTest {
    private Converter converter;

    @Before
    public void setUp() {
        converter = Converters.standardConverter();
    }

    @After
    public void tearDown() {
        converter = null;
    }

    @Test
    public void testGenericMapConversion() {
        Map<Integer, String> m1 = Collections.singletonMap(42, "987654321");
        Map<String, Long> m2 = converter.convert(m1).to(new TypeReference<Map<String, Long>>(){});
        assertEquals(1, m2.size());
        assertEquals(987654321L, (long) m2.get("42"));
    }

    @Test
    public void testConvertMapToDictionary() throws Exception {
        Map<BigInteger, URL> m = new HashMap<>();
        BigInteger bi = new BigInteger("123");
        URL url = new URL("http://0.0.0.0:123");
        m.put(bi, url);

        @SuppressWarnings("unchecked")
        Dictionary<BigInteger, URL> d = converter.convert(m).to(Dictionary.class);
        assertEquals(1, d.size());
        assertSame(bi, d.keys().nextElement());
        assertSame(url, d.get(bi));
    }

    @Test
    public void testJavaBeanToMap() {
        MyBean mb = new MyBean();
        mb.setMe("You");
        mb.setF(true);
        mb.setNumbers(new int[] {3,2,1});

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(mb).sourceAsBean().to(Map.class);
        assertEquals(5, m.size());
        assertEquals("You", m.get("me"));
        assertTrue((boolean) m.get("f"));
        assertFalse((boolean) m.get("enabled"));
        assertArrayEquals(new int [] {3,2,1}, (int[]) m.get("numbers"));
    }

    @Test
    public void testJavaBeanToMapCustom() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmssZ");
        Date d = new Date();
        String expectedDate = sdf.format(d);

        MyBean mb = new MyBean();
        mb.setStartDate(d);
        mb.setEnabled(true);

        ConverterBuilder cb = Converters.newConverterBuilder();
        cb.rule(new Rule<Date,String>(v -> sdf.format(v)) {});
        cb.rule(new Rule<String,Date>(v -> {
            try {
                return sdf.parse(v);
            } catch (Exception ex) {
                return null;
            }
        }) {});
        Converter ca = cb.build();
        Map<String, String> m = ca.convert(mb).sourceAsBean().to(new TypeReference<Map<String, String>>(){});
        assertEquals("true", m.get("enabled"));
        assertEquals(expectedDate, m.get("startDate"));
    }

    @Test
    public void testMapToJavaBean() {
        Map<String, String> m = new HashMap<>();

        m.put("me", "Joe");
        m.put("enabled", "true");
        m.put("numbers", "42");
        m.put("s", "will disappear");
        MyBean mb = converter.convert(m).targetAsBean().to(MyBean.class);
        assertEquals("Joe", mb.getMe());
        assertTrue(mb.isEnabled());
        assertNull(mb.getF());
        assertArrayEquals(new int[] {42}, mb.getNumbers());
    }

    public void testMapToJavaBean2() {
        Map<String, String> m = new HashMap<>();

        m.put("blah", "blahblah");
        m.put("f", "true");
        MyBean mb = converter.convert(m).to(MyBean.class);
        assertNull(mb.getMe());
        assertTrue(mb.getF());
        assertFalse(mb.isEnabled());
        assertNull(mb.getNumbers());
    }

    @Test
    public void testInterfaceToMap() {
        TestInterface impl = new TestInterface() {
            @Override
            public String foo() {
                return "Chocolate!";
            }

            @Override
            public int bar() {
                return 76543;
            }

            @Override
            public int bar(String def) {
                return 0;
            }

            @Override
            public Boolean za_za() {
                return true;
            }
        };

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(impl).to(Map.class);
        assertEquals(3, m.size());
        assertEquals("Chocolate!", m.get("foo"));
        assertEquals(76543, (int) m.get("bar"));
        assertEquals(true, (boolean) m.get("za.za"));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testMapToInterface1() {
        Map m = new HashMap<>();
        m.put("foo", 12345);
        m.put("bar", "999");
        m.put("alt", "someval");
        m.put("za.za", true);

        TestInterface ti = converter.convert(m).to(TestInterface.class);
        assertEquals("12345", ti.foo());
        assertEquals(999, ti.bar());
        assertEquals(Boolean.TRUE, ti.za_za());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testMapToInterface2() {
        Map m = new HashMap<>();

        TestInterface ti = converter.convert(m).to(TestInterface.class);
        try {
            ti.foo();
            fail("Should have thrown a conversion exception");
        } catch (ConversionException ce) {
        	// good
        }
        assertEquals(999, ti.bar("999"));
        try {
            assertNull(ti.za_za());
            fail("Should have thrown a conversion exception");
        } catch (ConversionException ce) {
            // good
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testMapToAnnotation1() {
        Map m = new HashMap<>();
        m.put("foo", 12345);
        m.put("bar", "999");
        m.put("alt", "someval");
        m.put("za.za", true);

        TestAnnotation ta = converter.convert(m).to(TestAnnotation.class);
        assertEquals("12345", ta.foo());
        assertEquals(999, ta.bar());
        assertTrue(ta.za_za());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testMapToAnnotationDefaults() {
        Map m = new HashMap<>();
        m.put("alt", "someval");

        TestAnnotation ta = converter.convert(m).to(TestAnnotation.class);
        assertEquals("fooo!", ta.foo());
        assertEquals(42, ta.bar());
    }

    @Test
    public void testAnnotationMethods() {
        TestAnnotation ta = converter.convert(new HashMap<>()).to(TestAnnotation.class);
        Map<String, Object> m = converter.convert(ta).to(new TypeReference<Map<String, Object>>(){});
        assertEquals(3, m.size());
        assertEquals("fooo!", m.get("foo"));
        assertEquals(42, m.get("bar"));
        try {
            assertEquals(false, m.get("za.za"));
            fail("Should have thrown a conversion exception as there is no default for 'za.za'");
        } catch (ConversionException ce) {
        	// good
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testSingleElementAnnotation() {
    	class MySingleElementAnnotation implements SingleElementAnnotation {
            @Override
            public Class<? extends Annotation> annotationType() {
                return SingleElementAnnotation.class;
            }

            @Override
            public String[] value() {
                return new String[] {"hi", "there"};
            }

            @Override
            public long somethingElse() {
                return 42;
            }
    	};
    	MySingleElementAnnotation sea = new MySingleElementAnnotation();
    	Map m = converter.convert(sea).to(Map.class);
    	assertEquals(2, m.size());
    	assertArrayEquals(new String[] {"hi", "there"}, (String []) m.get("single.element.annotation"));
    	assertEquals(42L, m.get("somethingElse"));

    	m.put("somethingElse", 51.0);
    	SingleElementAnnotation sea2 = converter.convert(m).to(SingleElementAnnotation.class);
    	assertArrayEquals(new String[] {"hi", "there"}, sea2.value());
    	assertEquals(51L, sea2.somethingElse());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCopyMap() {
        Object obj = new Object();
        Map m = new HashMap<>();
        m.put("key", obj);
        Map cm = converter.convert(m).to(Map.class);
        assertNotSame(m, cm);
        assertSame(m.get("key"), cm.get("key"));
    }

    @Test
    public void testProxyObjectMethodsInterface() {
        Map<String, String> m = new HashMap<>();
        TestInterface ti = converter.convert(m).to(TestInterface.class);
        assertTrue(ti.equals(ti));
        assertFalse(ti.equals(new Object()));
        assertFalse(ti.equals(null));

        assertNotNull(ti.toString());
        assertTrue(ti.hashCode() != 0);
    }

    @Test
    public void testProxyObjectMethodsAnnotation() {
        Map<String, String> m = new HashMap<>();
        TestAnnotation ta = converter.convert(m).to(TestAnnotation.class);
        assertTrue(ta.equals(ta));
    }

    @Test
    public void testCaseInsensitiveKeysAnnotation() {
        Map<String, Object> m = new HashMap<>();
        m.put("FOO", "Bleh");
        m.put("baR", 21);
        m.put("za.za", true);

        TestInterface ti = converter.convert(m).keysIgnoreCase().to(TestInterface.class);
        assertEquals("Bleh", ti.foo());
        assertEquals(21, ti.bar("42"));
        assertTrue(ti.za_za());
    }

    @Test
    public void testCaseSensitiveKeysAnnotation() {
        Map<String, Object> m = new HashMap<>();
        m.put("FOO", "Bleh");
        m.put("baR", 21);
        m.put("za.za", true);

        TestInterface ti = converter.convert(m).to(TestInterface.class);
        try {
            ti.foo();
            fail("Should have thrown a conversion exception as 'foo' was not set");
        } catch (ConversionException ce) {
            // good
        }
        assertEquals(42, ti.bar("42"));
        assertTrue(ti.za_za());
    }

    @Test
    public void testCaseInsensitiveDTO() {
        Dictionary<String, String> d = new Hashtable<>();
        d.put("COUNT", "one");
        d.put("PinG", "Piiiiiiing!");
        d.put("pong", "999");

        MyDTO dto = converter.convert(d).keysIgnoreCase().to(MyDTO.class);
        assertEquals(MyDTO.Count.ONE, dto.count);
        assertEquals("Piiiiiiing!", dto.ping);
        assertEquals(999L, dto.pong);
    }

    @Test
    public void testCaseSensitiveDTO() {
        Dictionary<String, String> d = new Hashtable<>();
        d.put("COUNT", "one");
        d.put("PinG", "Piiiiiiing!");
        d.put("pong", "999");

        MyDTO dto = converter.convert(d).to(MyDTO.class);
        assertNull(dto.count);
        assertNull(dto.ping);
        assertEquals(999L, dto.pong);
    }

    @Test
    public void testRemovePasswords() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("foo", "bar");
        m.put("password", "secret");

        Converter c = converter.newConverterBuilder().
            rule(new Rule<Map<String,Object>,String>(v -> {
                Map<String, Object> cm = new LinkedHashMap<>(v);

                for (Map.Entry<String, Object> entry : cm.entrySet()) {
                    if (entry.getKey().contains("password"))
                        entry.setValue("xxx");
                }
                return converter.convert(cm).to(String.class);
            }) {}).
            build();
        assertEquals("{foo=bar, password=xxx}", c.convert(m).to(String.class));
        assertEquals("Original should not be modified",
                "{foo=bar, password=secret}", m.toString());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testAnnotationDefaultMaterializer() throws Exception {
        Map<String, Object> vals = new HashMap<>();
        vals.put("bar", 99L);
        vals.put("tar", true);
        vals.put("za.za", false);

        Class<?> ta1cls = getClass().getClassLoader().loadClass(getClass().getPackage().getName() + ".sub1.TestAnn1");
        Object ta = converter.convert(vals).to(ta1cls);
        Map vals2 = converter.convert(ta).to(Map.class);
        vals2.putAll(vals);
        Class<?> ta2cls = getClass().getClassLoader().loadClass(getClass().getPackage().getName() + ".sub2.TestAnn2");
        Object ta2 = converter.convert(vals2).to(ta2cls);

        Method m1 = ta2cls.getDeclaredMethod("foo");
        m1.setAccessible(true);
        assertEquals("fooo!", m1.invoke(ta2));

        Method m2 = ta2cls.getDeclaredMethod("bar");
        m2.setAccessible(true);
        assertEquals(99, m2.invoke(ta2));

        Method m3 = ta2cls.getDeclaredMethod("tar");
        m3.setAccessible(true);
        assertEquals(true, m3.invoke(ta2));
    }

    @Test
    public void testMapEntry() {
        Map<String, Boolean> m1 = Collections.singletonMap("Hi", Boolean.TRUE);
        Map.Entry<String, Boolean> e1 = getMapEntry(m1);

        assertTrue(converter.convert(e1).to(Boolean.class));
        assertTrue(converter.convert(e1).to(boolean.class));
        assertEquals("Hi", converter.convert(e1).to(String.class));

    }

    @Test
    public void testMapEntry1() {
        Map<Long, String> m1 = Collections.singletonMap(17L, "18");
        Map.Entry<Long, String> e1 = getMapEntry(m1);

        assertEquals(17L, converter.convert(e1).to(Number.class));
        assertEquals("18", converter.convert(e1).to(String.class));
        assertEquals("18", converter.convert(e1).to(Bar.class).value);
    }

    @Test
    public void testMapEntry2() {
        Map<String, Short> m1 = Collections.singletonMap("123", Short.valueOf((short) 567));
        Map.Entry<String, Short> e1 = getMapEntry(m1);

        assertEquals(Integer.valueOf(123), converter.convert(e1).to(Integer.class));
    }

    @Test
    public void testMapEntry3() {
        Map<Long,Long> l1 = Collections.singletonMap(9L, 10L);
        Map.Entry<Long, Long> e1 = getMapEntry(l1);

        assertEquals("Should take the key if key and value are equally suitable",
                9L, (long) converter.convert(e1).to(long.class));
    }

    @Test
    public void testMapEntry4() {
        Map<Foo, Foo> m1 = Collections.singletonMap(new Foo(111), new Foo(999));
        Map.Entry<Foo, Foo> e1 = getMapEntry(m1);

        assertEquals("111", converter.convert(e1).to(Bar.class).value);
    }

    @Test
    public void testDictionaryToAnnotation() {
        Dictionary<String, Object> dict = new TestDictionary<>();
        dict.put("foo", "hello");
        TestAnnotation ta = converter.convert(dict).to(TestAnnotation.class);
        assertEquals("hello", ta.foo());
    }

    @Test
    public void testDictionaryToMap() {
        Dictionary<String, Object> dict = new TestDictionary<>();
        dict.put("foo", "hello");
        @SuppressWarnings("rawtypes")
        Map m = converter.convert(dict).copy().to(Map.class);
        assertEquals("hello", m.get("foo"));
    }

    @Test
    public void testInterfaceWithGetProperties() {
        TestInterfaceWithGetProperties tiwgp = new TestInterfaceWithGetProperties() {
            @Override
            public int blah() {
                return 99;
            }

            @Override
            public Dictionary<String, Object> getProperties() {
                Dictionary<String, Object> d = new TestDictionary<>();
                d.put("hi", "ha");
                d.put("ho", "ho");
                return d;
            }
        };

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(tiwgp).to(Map.class);
        assertEquals(2, m.size());
        assertEquals("ha", m.get("hi"));
        assertEquals("ho", m.get("ho"));
    }

    @Test
    public void testInterfaceWithGetPropertiesCopied() {
        TestInterfaceWithGetProperties tiwgp = new TestInterfaceWithGetProperties() {
            @Override
            public int blah() {
                return 99;
            }

            @Override
            public Dictionary<String, Object> getProperties() {
                Dictionary<String, Object> d = new TestDictionary<>();
                d.put("hi", "ha");
                d.put("ho", "ho");
                return d;
            }
        };

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(tiwgp).copy().to(Map.class);
        assertEquals(2, m.size());
        assertEquals("ha", m.get("hi"));
        assertEquals("ho", m.get("ho"));
    }

    private <K,V> Map.Entry<K,V> getMapEntry(Map<K,V> map) {
        assertEquals("This method assumes a map of size 1", 1, map.size());
        return map.entrySet().iterator().next();
    }

    interface TestInterface {
        String foo();
        int bar();
        int bar(String def);
        Boolean za_za();
    }

    interface TestInterfaceWithGetProperties {
        int blah();
        Dictionary<String, Object> getProperties();
    }

    @interface TestAnnotation {
        String foo() default "fooo!";
        int bar() default 42;
        boolean za_za();
    }

    @interface SingleElementAnnotation {
    	String[] value();
    	long somethingElse() default -87;
    }

    private static class Foo {
        private final int value;

        Foo(int v) {
            value = v;
        }

        @Override
        public String toString() {
            return "" + value;
        }
    }

    public static class Bar {
        final String value;
        public Bar(String v) {
            value = v;
        }
    }
}
