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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Unit tests on <code>TypedProperties</code>.
 * </p>
 *
 * @author gnodet
 */
public class TypedPropertiesTest extends TestCase {

    private final static String TEST_PROPERTIES_FILE = "test.properties";
    private final static String TEST_TYPED_PROPERTIES_FILE = "typed.properties";

    public void testConfigInterpolation() throws IOException
    {
        TypedProperties properties = new TypedProperties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream(TEST_TYPED_PROPERTIES_FILE));

        assertEquals(8101, properties.get("port"));
        assertEquals("127.0.0.1:8101", properties.get("url"));
    }

    public void testSetProp() throws IOException
    {
        TypedProperties properties = new TypedProperties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream(TEST_TYPED_PROPERTIES_FILE));

        properties.put("port", 8101);
        properties.put("port2", 8103);
        properties.put("url", "127.0.0.1:8101");

        properties.save(System.out);
    }

    public void testLoadNonTypedProps() throws IOException
    {
        TypedProperties properties = new TypedProperties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream(TEST_PROPERTIES_FILE));
        properties.put("port", 8101);

        StringWriter sw = new StringWriter();
        properties.save(sw);

        TypedProperties p2 = new TypedProperties();
        p2.load(new StringReader(sw.toString()));
        assertEquals(8101, p2.get("port"));
        assertEquals("test", p2.get("test"));
    }

}
