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

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

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

}