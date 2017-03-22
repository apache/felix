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

import java.lang.reflect.Type;

import org.osgi.util.converter.ConvertFunction;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.StandardConverter;
import org.osgi.util.converter.TargetRule;

public class SchematizingConverterBuilderImpl implements ConverterBuilder {

    private final ConverterBuilder builder = new StandardConverter().newConverterBuilder();

    @Override
    public Converter build()
    {
        Converter converter = builder.build();
        return new SchematizingConverterImpl(converter);
    }

    public <T> ConverterBuilder rule(Type type, ConvertFunction<T> function) {
        builder.rule(type, function);
        return this;
    }

    public <T> ConverterBuilder rule(TargetRule<T> rule) {
        builder.rule(rule);
        return this;
    }

    public <T> ConverterBuilder rule(ConvertFunction<T> func) {
        builder.rule(func);
        return this;
    }
}
