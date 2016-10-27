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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.ConvertFunction;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Converting;
import org.osgi.util.converter.Function;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.TypeReference;

public class AdapterImpl implements InternalConverter {
    private final InternalConverter delegate;
    private final Map<TypePair, ConvertFunction<Object, Object>> classRules =
            new ConcurrentHashMap<>();

    AdapterImpl(InternalConverter converter, List<Rule<?,?>> rules) {
        this.delegate = converter;
        for (Rule<?,?> r : rules) {
            rule(r);
        }
    }

    @Override
    public InternalConverting convert(Object obj) {
        InternalConverting converting = delegate.convert(obj);
        converting.setConverter(this);
        return new ConvertingWrapper(obj, converting);
    }

    @Override
    public ConverterBuilder newConverterBuilder() {
        return new ConverterBuilderImpl(this);
    }

    @SuppressWarnings("unchecked")
    private <F, T> AdapterImpl rule(Rule<F, T> rule) {
        ConvertFunction<F, T> toFun = rule.getToFunction();
        if (toFun != null)
            classRules.put(new TypePair(rule.getFromClass(), rule.getToClass()),
                (ConvertFunction<Object, Object>) toFun);


        ConvertFunction<T, F> fromFun = rule.getFromFunction();
        if (fromFun != null)
            classRules.put(new TypePair(rule.getToClass(), rule.getFromClass()),
                (ConvertFunction<Object, Object>) fromFun);
        return this;
    }

    private class ConvertingWrapper implements InternalConverting {
        private final InternalConverting del;
        private final Object object;
        private volatile Object defaultValue;
        private volatile Class<?> treatAsClass;
        private volatile boolean hasDefault;

        ConvertingWrapper(Object obj, InternalConverting c) {
            object = obj;
            del = c;
        }

        @Override
        public Converting as(Class<?> cls) {
            treatAsClass = cls;
            del.as(cls);
            return this;
        }

        @Override
        public Converting copy() {
            del.copy();
            return this;
        }

        @Override
        public Converting defaultValue(Object defVal) {
            del.defaultValue(defVal);
            defaultValue = defVal;
            hasDefault = true;
            return this;
        }

        @Override
        public void setConverter(Converter c) {
            del.setConverter(c);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T to(Class<T> cls)  {
            Type type = cls;
            return (T) to(type);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T to(TypeReference<T> ref)  {
            return (T) to(ref.getType());
        }

        @Override
        public Object to(Type type) {
            if (object != null) {
                Set<Type> fromTypes = assignableTypes(treatAsClass != null ? treatAsClass : object.getClass());
                Set<Type> toTypes = assignableTypes(type);

                List<ConvertFunction<Object, Object>> converters = new ArrayList<>();
                for (Type fromType : fromTypes) {
                    for (Type toType : toTypes) {
                        // TODO what exactly do we use as order here?
                        converters.add(classRules.get(new TypePair(fromType, Util.primitiveToBoxed(toType))));
                    }
                }
                for (Type fromType : fromTypes) {
                    converters.add(classRules.get(new TypePair(fromType, Object.class)));
                }
                for (Type toType : toTypes) {
                    converters.add(classRules.get(new TypePair(Object.class, Util.primitiveToBoxed(toType))));
                }

                for (Iterator<ConvertFunction<Object, Object>> it = converters.iterator(); it.hasNext(); ) {
                    ConvertFunction<Object, Object> func = it.next();
                    it.remove();
                    if (func == null)
                        continue;

                    try {
                        Object res = func.convert(object, type);
                        if (res != null) {
                            return res;
                        }
                    } catch (Exception ex) {
                        if (hasDefault)
                            return defaultValue;
                        else
                            throw new ConversionException("Cannot convert " + object + " to " + type, ex);
                    }
                }
            }

            return del.to(type);
        }

        @Override
        public String toString() {
            return to(String.class);
        }

        @Override
        public Converting target(Class<?> cls) {
            del.target(cls);
            return this;
        }
    }

    private static Set<Type> assignableTypes(Type mostSpecialized) {
        if (!(mostSpecialized instanceof Class))
            return Collections.singleton(mostSpecialized);

        Class<?> curClass = (Class<?>) mostSpecialized;
        Set<Type> lookupTypes = new LinkedHashSet<>(); // Iteration order matters!
        while((curClass != null) && (!(Object.class.equals(curClass)))) {
            lookupTypes.add(curClass);
            lookupTypes.addAll(Arrays.asList(curClass.getInterfaces()));
            curClass = curClass.getSuperclass();
        }
        lookupTypes.add(Object.class); // Object is the superclass of any type
        return lookupTypes;
    }

    static class TypePair {
        private final Type from;
        private final Type to;

        TypePair(Type from, Type to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof TypePair))
                return false;

            TypePair o = (TypePair) obj;
            return Objects.equals(from, o.from) &&
                    Objects.equals(to, o.to);
        }
    }

    static class ConvertFunctionImpl<F, T> implements ConvertFunction<F, T> {
        private final Function<F, T> function;

        public ConvertFunctionImpl(Function<F, T> function) {
            this.function = function;
        }

        @Override
        public T convert(F obj, Type targetType) throws Exception {
            return function.convert(obj);
        }
    }
}
