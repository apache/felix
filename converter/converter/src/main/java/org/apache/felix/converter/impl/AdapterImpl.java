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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.ConvertFunction;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Converting;
import org.osgi.util.converter.TypeReference;

public class AdapterImpl implements InternalConverter {
    private final InternalConverter delegate;
    private final Map<Type, List<ConvertFunction<?>>> typeRules;
    private final List<ConvertFunction<?>> allRules;

    AdapterImpl(InternalConverter converter, Map<Type, List<ConvertFunction<?>>> rules, List<ConvertFunction<?>> catchAllRules) {
        delegate = converter;
        typeRules = rules;
        allRules = catchAllRules;
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
        public Converting keysIgnoreCase() {
            del.keysIgnoreCase();
            return this;
        }

        @Override
        public void setConverter(Converter c) {
            del.setConverter(c);
        }

        @Override
        public Converting sourceAs(Class<?> type) {
            del.sourceAs(type);
            return this;
        }

        @Override
        public Converting sourceAsBean() {
            del.sourceAsBean();
            return this;
        }

        @Override
        public Converting sourceAsDTO() {
            del.sourceAsDTO();
            return this;
        }

        @Override
        public Converting targetAs(Class<?> cls) {
            del.targetAs(cls);
            return this;
        }

        @Override
        public Converting targetAsBean() {
            del.targetAsBean();
            return this;
        }

        @Override
        public Converting targetAsDTO() {
            del.targetAsDTO();
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

        @SuppressWarnings("unchecked")
        @Override
        public Object to(Type type) {
            List<ConvertFunction<?>> tr = typeRules.get(Util.primitiveToBoxed(type));
            if (tr == null)
                tr = Collections.emptyList();
            List<ConvertFunction<?>> converters = new ArrayList<>(tr.size() + allRules.size());
            converters.addAll(tr);
            converters.addAll(allRules);

            try {
                if (object != null) {
                    for (ConvertFunction<?> cf : converters) {
                        try {
                            Object res = cf.convert(object, type);
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
            } catch (Exception ex) {
                // do custom error handling
                for (ConvertFunction<?> cf : converters) {
                    Object eh = cf.handleError(object, type);
                    if (eh != null)
                        return eh;
                }
                // No error handler, throw the original exception
                throw ex;
            }
        }

        @Override
        public String toString() {
            return to(String.class);
        }
    }
}
