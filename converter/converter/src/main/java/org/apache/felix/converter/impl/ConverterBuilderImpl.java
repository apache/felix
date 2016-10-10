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
package org.apache.felix.converter.impl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.converter.impl.AdapterImpl.ConvertFunctionImpl;
import org.osgi.converter.ConverterBuilder;
import org.osgi.converter.Function;
import org.osgi.converter.Rule;
import org.osgi.converter.TypeReference;

public class ConverterBuilderImpl implements ConverterBuilder {
    private final InternalConverter adapter;
    private final List<Rule<?,?>> rules = new ArrayList<>();

    public ConverterBuilderImpl(InternalConverter a) {
        this.adapter = a;
    }

    @Override
    public InternalConverter build() {
        return new AdapterImpl(adapter, rules);
    }

    @Override
    public <F, T> ConverterBuilder rule(Rule<F, T> rule) {
        rules.add(rule);
        return this;
    }

    @Override
    public <F, T> ConverterBuilder rule(Class<F> fromCls, Class<T> toCls, Function<F, T> toFun, Function<T, F> fromFun) {
        rules.add(new Rule<F, T>(fromCls, toCls, new ConvertFunctionImpl<>(toFun), new ConvertFunctionImpl<>(fromFun)));
        return this;
    }

    @Override
    public <F, T> ConverterBuilder rule(TypeReference<F> fromRef, TypeReference<T> toRef, Function<F, T> toFun,
            Function<T, F> fromFun) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <F, T> ConverterBuilder rule(Type fromType, Type toType, Function<F, T> toFun, Function<T, F> fromFun) {
        // TODO Auto-generated method stub
        return null;
    }
}
