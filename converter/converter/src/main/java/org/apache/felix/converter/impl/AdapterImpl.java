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

import org.osgi.service.converter.Adapter;
import org.osgi.service.converter.ConversionException;
import org.osgi.service.converter.ConvertFunction;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Converting;
import org.osgi.service.converter.Rule;
import org.osgi.service.converter.SimpleConvertFunction;
import org.osgi.service.converter.TypeReference;

public class AdapterImpl implements Adapter, InternalConverter {
    private final InternalConverter delegate;
    private final Map<TypePair, ConvertFunction<Object, Object>> classRules =
            new ConcurrentHashMap<>();

    AdapterImpl(InternalConverter converter) {
        this.delegate = converter;
    }

    @Override
    public InternalConverting convert(Object obj) {
        InternalConverting converting = delegate.convert(obj);
        converting.setConverter(this);
        return new ConvertingWrapper(obj, converting);
    }

    @Override
    public Adapter getAdapter() {
        return new AdapterImpl(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F, T> Adapter rule(Class<F> fromCls, Class<T> toCls,
            SimpleConvertFunction<F, T> toFun, SimpleConvertFunction<T, F> fromFun) {
        if (fromCls.equals(toCls))
            throw new IllegalArgumentException();

        classRules.put(new TypePair(fromCls, toCls), (ConvertFunction<Object, Object>) toFun);
        classRules.put(new TypePair(toCls, fromCls), (ConvertFunction<Object, Object>) fromFun);
        return this;
    }

    @Override
    public <F, T> Adapter rule(TypeReference<F> fromRef, TypeReference<T> toRef,
            SimpleConvertFunction<F, T> toFun, SimpleConvertFunction<T, F> fromFun) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <F, T> Adapter rule(Type fromType, Type toType,
            SimpleConvertFunction<F, T> toFun, SimpleConvertFunction<T, F> fromFun) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F, T> Adapter rule(Rule<F, T> rule) {
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
        private volatile boolean hasDefault;

        ConvertingWrapper(Object obj, InternalConverting c) {
            object = obj;
            del = c;
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
                Set<Type> fromTypes = assignableTypes(object.getClass());
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
                        if (res != ConvertFunction.CANNOT_CONVERT)
                            return res;
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
}
