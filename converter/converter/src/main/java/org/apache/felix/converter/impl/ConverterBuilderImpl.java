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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.ConverterFunction;
import org.osgi.util.converter.TargetRule;

class ConverterBuilderImpl implements ConverterBuilder {
    private final InternalConverter converter;
    private final Map<Type, List<ConverterFunction>> rules = new HashMap<>();
    private final List<ConverterFunction> catchAllRules = new ArrayList<>();
    private final List<ConverterFunction> errorHandlers = new ArrayList<>();

    public ConverterBuilderImpl(InternalConverter c) {
        this.converter = c;
    }

    @Override
    public InternalConverter build() {
        return new CustomConverterImpl(converter, rules, catchAllRules, errorHandlers);
    }

    @Override
    public ConverterBuilder errorHandler(ConverterFunction func) {
        errorHandlers.add(func);
        return this;
    }

    @Override
    public ConverterBuilder rule(ConverterFunction func) {
    	catchAllRules.add(func);
        return this;
    }

    @Override
    public ConverterBuilder rule(Type t, ConverterFunction func) {
    	getRulesList(t).add(func);
    	return this;
    }

    @Override
    public ConverterBuilder rule(TargetRule rule) {
    	Type type = rule.getTargetType();
    	getRulesList(type).add(rule.getFunction());

    	if (type instanceof ParameterizedType) {
    	    ParameterizedType pt = (ParameterizedType) type;

    	    boolean containsWildCard = false;
    	    for (Type t : pt.getActualTypeArguments()) {
    	        if (t instanceof WildcardType) {
    	            containsWildCard = true;
    	            break;
    	        }
    	    }

    	    // If the parameterized type is a wildcard (e.g. '?') then register also the raw
    	    // type for the rule. I.e Class<?> will also be registered under bare Class.
    	    if (containsWildCard)
    	        getRulesList(pt.getRawType()).add(rule.getFunction());
    	}

        return this;
    }

    private List<ConverterFunction> getRulesList(Type type) {
        List<ConverterFunction> l = rules.get(type);
    	if (l == null) {
    		l = new ArrayList<>();
    		rules.put(type, l);
    	}
        return l;
    }
}
