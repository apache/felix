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

import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.StandardConverter;
import org.osgi.util.converter.TypeReference;
import org.osgi.util.function.Function;

public class SchematizingConverterBuilderImpl implements ConverterBuilder {

    private final ConverterBuilder builder = new StandardConverter().newConverterBuilder();

    @Override
    public Converter build()
    {
        Converter converter = builder.build(); 
        return new SchematizingConverterImpl(converter);
    }

    @Override
    public <F, T> ConverterBuilder rule( Rule<F, T> rule )
    {
        return builder.rule(rule);
    }

    @Override
    public <F, T> ConverterBuilder rule( Class<F> fromCls, Class<T> toCls, Function<F, T> toFun, Function<T, F> fromFun )
    {
        return builder.rule(fromCls, toCls, toFun, fromFun);
    }

    @Override
    public <F, T> ConverterBuilder rule( TypeReference<F> fromRef, TypeReference<T> toRef, Function<F, T> toFun, Function<T, F> fromFun )
    {
        return builder.rule(fromRef, toRef, toFun, fromFun);
    }

    @Override
    public <F, T> ConverterBuilder rule( Type fromType, Type toType, Function<F, T> toFun, Function<T, F> fromFun )
    {
        return builder.rule(fromType, toType, toFun, fromFun);
    }
}
