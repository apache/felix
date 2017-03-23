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
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.StandardConverter;
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
        converter = new StandardConverter();
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

        ConverterBuilder cb = new StandardConverter().newConverterBuilder();
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
    @Ignore("This test is broken :-(")
    public void testMapToJavaBean() {
        Map<String, String> m = new HashMap<>();

        m.put("me", "Joe");
        m.put("enabled", "true");
        m.put("numbers", "42");
        m.put("s", "will disappear");
        MyBean mb = converter.convert(m).to(MyBean.class);
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

    interface TestInterface {
        String foo();
        int bar();
        int bar(String def);
        Boolean za_za();
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
}
