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
package org.apache.felix.schematizer.impl;

import org.apache.felix.schematizer.Schema;
import org.apache.felix.schematizer.Schematizing;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Converting;

public class SchematizingConverterImpl implements Schematizing, Converter {

    private final Converter converter;
    private Schema schema;
    private boolean asDTO = false;

    public SchematizingConverterImpl(Converter c)
    {
        converter = c;
    }

    @Override
    public Converting convert(Object obj) {
        return converter.convert(obj);
    }

    @Override
    public ConverterBuilder newConverterBuilder() {
        return new SchematizingConverterBuilderImpl();
    }

    @Override
    public Converter asDTO() {
        asDTO = true;
        return converter;
    }

    @Override
    public boolean isDTOType() {
        return asDTO;
    }

    public Converter withSchema(Schema s) {
        schema = s;
        return this;
    }

    public Schema getSchema() {
        return schema;
    }
}
