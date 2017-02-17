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

import java.util.Map;
import java.util.Optional;

import org.apache.felix.schematizer.impl.SchematizerImpl;
import org.osgi.dto.DTO;
import org.osgi.util.converter.TypeReference;

public class StandardSchematizer implements Schematizer {
    private final Schematizer schematizer;

    public StandardSchematizer() {
        schematizer = new SchematizerImpl();
    }

    @Override
    public Optional<Schema> get(String name) {
        return schematizer.get(name);
    }

    @Override
    public Optional<Schema> from(String name, Map<String, Node.DTO> map) {
        return schematizer.from(name, map);
    }

    @Override
    public <T extends DTO> Schematizer rule(String name, TypeRule<T> rule) {
        return schematizer.rule(name, rule);
    }

    @Override
    public <T extends DTO> Schematizer rule( String name, String path, TypeReference<T> type ) {
        return schematizer.rule(name, path, type);
    }

    @Override
    public <T> Schematizer rule(String name, String path, Class<T> cls) {
        return schematizer.rule(name, path, cls);
    }

    @Override
    public <T extends DTO> Schematizer rule(String name, TypeReference<T> type) {
        return schematizer.rule(name, type);
    }

    @Override
    public Schematizer usingLookup(ClassLoader classLoader) {
        return schematizer.usingLookup(classLoader);
    }
}
