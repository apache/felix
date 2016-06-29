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
package org.apache.felix.configurator.impl.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.felix.configurator.impl.TypeConverter;
import org.apache.felix.configurator.impl.model.ConfigurationFile;
import org.junit.Test;

public class YAMLUtilTest {

    /** Read the  model from that name */
    public static String readYAML(final String name) throws Exception {

        try ( final Reader reader = new InputStreamReader(YAMLUtilTest.class.getResourceAsStream("/" + name), "UTF-8");
              final Writer writer = new StringWriter()) {

            final char[] buf = new char[2048];
            int len = 0;
            while ((len = reader.read(buf)) > 0) {
                writer.write(buf, 0, len);
            }

            return writer.toString();
        }
    }

    @Test public void testReadYAML() throws Exception {
        final ConfigurationFile cg = YAMLUtil.readYAML(new TypeConverter(null),
                "a", new URL("http://a"), 1, readYAML("yaml/valid.yaml"));
        assertNotNull(cg);
        assertEquals(2, cg.getConfigurations().size());
    }

    @SuppressWarnings("unchecked")
    @Test public void testTypes() throws Exception {
        final Object obj = YAMLUtil.parseYAML("a", YAMLUtilTest.readYAML("yaml/simple-types.yaml"));
        final Map<String, Object> config = (Map<String, Object>)obj;
        final Map<String, Object> properties = (Map<String, Object>)config.get("config");

        assertTrue(properties.get("string") instanceof String);
        assertTrue(properties.get("boolean") instanceof Boolean);
        assertTrue(properties.get("number") instanceof Integer);
        assertTrue(properties.get("float") instanceof Double);

        // arrays
        assertTrue(properties.get("string.array") instanceof List<?>);
        assertTrue(((List<Object>)properties.get("string.array")).get(0) instanceof String);
        assertTrue(((List<Object>)properties.get("string.array")).get(1) instanceof String);

        assertTrue((List<Object>)properties.get("boolean.array") instanceof List<?>);
        assertTrue(((List<Object>)properties.get("boolean.array")).get(0) instanceof Boolean);
        assertTrue(((List<Object>)properties.get("boolean.array")).get(1) instanceof Boolean);

        assertTrue((List<Object>)properties.get("number.array") instanceof List<?>);
        assertTrue(((List<Object>)properties.get("number.array")).get(0) instanceof Integer);
        assertTrue(((List<Object>)properties.get("number.array")).get(1) instanceof Integer);

        assertTrue((List<Object>)properties.get("float.array") instanceof List<?>);
        assertTrue(((List<Object>)properties.get("float.array")).get(0) instanceof Double);
        assertTrue(((List<Object>)properties.get("float.array")).get(1) instanceof Double);
    }
}