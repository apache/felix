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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.converter.Adapter;
import org.osgi.service.converter.ConversionException;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Converting;
import org.osgi.service.converter.FunctionThrowsException;
import org.osgi.service.converter.Rule;
import org.osgi.service.converter.TypeReference;

public class AdapterImpl implements Adapter {
    private final Converter delegate;
    private final Map<TypePair, FunctionThrowsException<Object, Object>> classRules =
            new ConcurrentHashMap<>();

    public AdapterImpl(Converter converter) {
        this.delegate = converter;
    }

    @Override
    public Converting convert(Object obj) {
        Converting converting = delegate.convert(obj);
        return new ConvertingWrapper(obj, converting);
    }

    @Override
    public Adapter getAdapter() {
        return new AdapterImpl(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F, T> Adapter rule(Class<F> fromCls, Class<T> toCls,
            FunctionThrowsException<F, T> toFun, FunctionThrowsException<T, F> fromFun) {
        if (fromCls.equals(toCls))
            throw new IllegalArgumentException();

        classRules.put(new TypePair(fromCls, toCls), (FunctionThrowsException<Object, Object>) toFun);
        classRules.put(new TypePair(toCls, fromCls), (FunctionThrowsException<Object, Object>) fromFun);
        return this;
    }

    @Override
    public <F, T> Adapter rule(TypeReference<F> fromRef, TypeReference<T> toRef,
            FunctionThrowsException<F, T> toFun, FunctionThrowsException<T, F> fromFun) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <F, T> Adapter rule(Type fromType, Type toType,
            FunctionThrowsException<F, T> toFun, FunctionThrowsException<T, F> fromFun) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <F, T> Adapter rule(Rule<F, T> rule) {
        // TODO Auto-generated method stub
        return null;
    }

    private class ConvertingWrapper implements Converting {
        private final Converting del;
        private final Object object;
        private volatile Object defaultValue;
        private volatile boolean hasDefault;

        ConvertingWrapper(Object obj, Converting c) {
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
                FunctionThrowsException<Object, Object> f = classRules.get(
                    new TypePair(object.getClass(), Util.primitiveToBoxed(type)));
                if (f != null) {
                    try {
                        return f.apply(object);
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
