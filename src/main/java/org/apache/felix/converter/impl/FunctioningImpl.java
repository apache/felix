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

import org.osgi.util.converter.Functioning;
import org.osgi.util.converter.TypeReference;
import org.osgi.util.function.Function;

class FunctioningImpl extends AbstractSpecifying<Functioning> implements Functioning {
    private InternalConverter converter;

    FunctioningImpl(InternalConverter converterImpl) {
        converter = converterImpl;
    }

    @Override
    public <T> Function<Object, T> to(Class<T> cls) {
        Type type = cls;
        return to(type);
    }

    @Override
    public <T> Function<Object, T> to(TypeReference<T> ref) {
        return to(ref.getType());
    }

    @Override
    public <T> Function<Object, T> to(final Type type) {
        return new Function<Object, T>() {
            @Override
            public T apply(Object t) {
                InternalConverting ic = converter.convert(t);
                return applyModifiers(ic).to(type);
            }
        };
    }

    InternalConverting applyModifiers(InternalConverting ic) {
        if (hasDefault) ic.defaultValue(defaultValue);
        if (forceCopy) ic.copy();
        if (keysIgnoreCase) ic.keysIgnoreCase();
        if (sourceAsClass != null) ic.sourceAs(sourceAsClass);
        if (sourceAsDTO) ic.sourceAsDTO();
        if (sourceAsJavaBean) ic.sourceAsBean();
        if (targetAsClass != null) ic.targetAs(targetAsClass);
        if (targetAsDTO) ic.targetAsBean();
        if (targetAsJavaBean) ic.targetAsBean();

        return ic;
    }
}
