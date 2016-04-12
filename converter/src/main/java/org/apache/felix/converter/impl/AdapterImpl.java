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
import java.util.function.Function;

import org.osgi.service.converter.Adapter;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Converting;
import org.osgi.service.converter.Rule;
import org.osgi.service.converter.TypeReference;

public class AdapterImpl implements Adapter {
    private final Converter delegate;
    private final Map<ClassPair, Function<Object, Object>> classRules =
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
            Function<F, T> toFun, Function<T, F> fromFun) {
        if (fromCls.equals(toCls))
            throw new IllegalArgumentException();

        classRules.put(new ClassPair(fromCls, toCls), (Function<Object, Object>) toFun);
        classRules.put(new ClassPair(toCls, fromCls), (Function<Object, Object>) fromFun);
        return this;
    }


    @Override
    public <F, T> Adapter rule(Rule<F, T> rule) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <F, T> Adapter rule(Function<F, T> toFun, Function<T, F> fromFun) {
//        Type[] t = toFun.getClass().getGenericInterfaces();
//
//        TypeVariable<?>[] tp = toFun.getClass().getTypeParameters();
//        System.out.println("*** " + Arrays.toString(tp));
//
//        TypeReference<Map<String, Adapter>> tr = new TypeReference<Map<String,Adapter>>(){};
//        System.out.println("### " + tr);
//        Type type = tr.getType();
//        System.out.println("### " + type);

        // TODO Auto-generated method stub
        return this;
    }

    private class ConvertingWrapper implements Converting {
        private final Converting del;
        private final Object object;

        ConvertingWrapper(Object obj, Converting c) {
            object = obj;
            del = c;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T to(Class<T> cls) {
            Function<Object, Object> f = classRules.get(new ClassPair(object.getClass(), cls));
            if (f != null)
                return (T) f.apply(object);

            return del.to(cls);
        }

        @Override
        public <T> T to(TypeReference<T> ref) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object to(Type type) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    static class ClassPair {
        private final Class<?> from;
        private final Class<?> to;

        ClassPair(Class<?> from, Class<?> to) {
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
            if (!(obj instanceof ClassPair))
                return false;

            ClassPair o = (ClassPair) obj;
            return Objects.equals(from, o.from) &&
                    Objects.equals(to, o.to);
        }
    }
}
