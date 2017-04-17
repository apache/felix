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
package org.apache.felix.schematizer;

import org.apache.felix.schematizer.impl.SchematizerImpl;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.TypeReference;

public class StandardSchematizer implements Schematizer {
    private final Schematizer schematizer;

    public StandardSchematizer() {
        schematizer = new SchematizerImpl();
    }

    @Override
    public Schematizer schematize(String schemaName, Object type) {
        return schematizer.schematize(schemaName, type);
    }

    @Override
    public Schema get(String schemaName) {
        return schematizer.get(schemaName);
    }

    @Override
    public Schematizer type(String name, String path, TypeReference<?> type) {
        return schematizer.type(name, path, type);
    }

    @Override
    public Schematizer type(String name, String path, Class<?> type) {
        return schematizer.type(name, path, type);
    }

    @Override
    public Converter converterFor(String schemaName) {
        return schematizer.converterFor(schemaName);
    }
}
