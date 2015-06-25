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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * <p>
 * Unit tests on <code>Properties</code>.
 * </p>
 *
 * @author jbonofre
 */
public class Utf8PropertiesTest extends TestCase {

    private final static String TEST_PROPERTIES_FILE = "test-utf8.properties";

    private final static String KEY1 = "test";
    private final static String VALUE1 = "testé";
    private Properties properties;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        System.setProperty("felix.fileinstall.configEncoding", "UTF-8");

        properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream(TEST_PROPERTIES_FILE));
    }

    public void testEncoding() throws Exception {
        assertEquals(VALUE1, properties.get(KEY1));
    }
}
