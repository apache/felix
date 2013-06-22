/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.runtime.core.services.FooService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * A complex component
 */
@Component
@Provides
public class MyComplexComponent implements FooService {

    @Property
    private File file;
    @Property
    private Bean bean;
    @Property
    private Map<String, String> map;

    @Override
    public boolean foo() {
        return file != null;
    }

    @Override
    public Properties fooProps() {
        String content;
        try {
            content = read();
        } catch (IOException e) {
            throw new IllegalStateException("unexpected error", e);
        }
        Properties props = new Properties();
        props.put("map", map);
        props.put("content", content);
        props.put("bean", bean);
        return props;
    }

    private String read() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        return stringBuilder.toString();
    }

    @Override
    public Boolean getObject() {
        return false;
    }

    @Override
    public boolean getBoolean() {
        return false;
    }

    @Override
    public int getInt() {
        return 0;
    }

    @Override
    public long getLong() {
        return 1;
    }

    @Override
    public double getDouble() {
        return 1;
    }
}
