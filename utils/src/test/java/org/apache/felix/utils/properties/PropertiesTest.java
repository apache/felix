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
package org.apache.felix.utils.properties;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import junit.framework.TestCase;

/**
 * <p>
 * Unit tests on <code>Properties</code>.
 * </p>
 *
 * @author jbonofre
 */
public class PropertiesTest extends TestCase {

    private final static String TEST_PROPERTIES_FILE = "test.properties";
    private final static String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String COMMENT = "# comment";
    private static final String KEY1 = "mvn:foo/bar";
    private static final String KEY1A = "mvn\\:foo/bar";
    private static final String KEY2 = "foo:bar:version:type:classifier";
    private static final String KEY2A = "foo\\:bar\\:version\\:type\\:classifier";
    private static final String VALUE1 = "value";

    private Properties properties;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream(TEST_PROPERTIES_FILE));
    }

    /**
     * <p>
     * Test getting property.
     * </p>
     *
     * @throws Exception
     */
    public void testGettingProperty() throws Exception {
        assertEquals("test", properties.get("test"));
    }

    public void testLoadSave() throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("# ");
        pw.println("# The Main  ");
        pw.println("# ");
        pw.println("# Comment ");
        pw.println("# ");
        pw.println("");
        pw.println("# Another comment");
        pw.println("");
        pw.println("# A value comment");
        pw.println("key1 = val1");
        pw.println("");
        pw.println("# Another value comment");
        pw.println("key2 = ${key1}/foo");
        pw.println("");
        pw.println("# A third comment");
        pw.println("key3 = val3");
        pw.println("");


        Properties props = new Properties();
        props.load(new StringReader(sw.toString()));
        props.save(System.err);
        System.err.println("=====");

        props.put("key2", props.get("key2"));
        props.put("key3", "foo");
        props.save(System.err);
        System.err.println("=====");
    }

    public void testJavaUtilPropertiesCompatibility() throws Exception {
        Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream(TEST_PROPERTIES_FILE));

        String test = properties.getProperty("test");
        assertEquals(test, "test");

        String defaultValue = properties.getProperty("notfound", "default");
        assertEquals(defaultValue, "default");

        properties.setProperty("another", "another");
        assertEquals(properties.getProperty("another"), "another");

        properties.store(System.err, null);
        System.err.println("====");
    }

    private static final String RESULT1 = COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1 + LINE_SEPARATOR;

    public void testSaveComment1() throws Exception {
        properties.put(KEY1, COMMENT, VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        assertTrue(sw.toString(), sw.toString().endsWith(RESULT1));
    }

    private static final String RESULT1A = COMMENT + LINE_SEPARATOR + KEY2A + " = " + VALUE1 + LINE_SEPARATOR;

    public void testSaveComment1a() throws Exception {
        properties.put(KEY2, COMMENT, VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        assertTrue(sw.toString(), sw.toString().endsWith(RESULT1A));
    }

    private static final String RESULT2 = COMMENT + LINE_SEPARATOR + COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1 + LINE_SEPARATOR;

    public void testSaveComment2() throws Exception {
        properties.put(KEY1, Arrays.asList(new String[] {COMMENT, COMMENT}), VALUE1);
        StringWriter sw = new StringWriter();
        properties.save(sw);
        assertTrue(sw.toString(), sw.toString().endsWith(RESULT2));
    }

    private static final String RESULT3 = COMMENT + LINE_SEPARATOR + COMMENT + LINE_SEPARATOR + KEY1A + " = " + VALUE1 + "\\" + LINE_SEPARATOR+ VALUE1 + LINE_SEPARATOR;

    public void testSaveComment3() throws Exception {
        properties.put(KEY1, Arrays.asList(new String[] {COMMENT, COMMENT}), Arrays.asList(new String[] {VALUE1, VALUE1}));
        StringWriter sw = new StringWriter();
        properties.save(sw);
        assertTrue(sw.toString(), sw.toString().endsWith(RESULT3));
        List<String> rawValue = properties.getRaw(KEY1);
        assertEquals(2, rawValue.size());
        assertEquals(KEY1A + " = " + VALUE1 + "\\", rawValue.get(0));
        assertEquals(VALUE1, rawValue.get(1));
    }
}
