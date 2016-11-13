/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.util.converter;

import java.lang.reflect.Type;

/**
 * A rule is a data entity can hold all the information needed to specify a
 * custom conversion for use by the {@link ConverterBuilder}.
 *
 * @param <F> The type to convert from.
 * @param <T> The type to convert to.
 * @author $Id: 7f624253be48fc23d8a793b38673305dbfd5ff9a $
 * @Immutable
 */
public class Rule<F, T> {
    private final Type              sourceType;
    private final Type              targetType;

    @SuppressWarnings("rawtypes")
    private final ConvertFunction  toFun;
    @SuppressWarnings("rawtypes")
    private final ConvertFunction  backFun;

    /**
     * Create a bidirectional rule.
     *
     * @param fromCls The class from which to convert. If {@link Object} is
     *            specified then this functions as a wildcard for generic
     *            conversions.
     * @param toCls The class to which to convert. If {@link Object} is
     *            specified then this functions as a wildcard for generic
     *            conversions.
     * @param to The conversion function for this rule.
     * @param back The reverse conversion for this rule.
     */
    public Rule(Class<F> fromCls, Class<T> toCls, ConvertFunction<F,T> to,
            ConvertFunction<T,F> back) {
        this((Type) fromCls, (Type) toCls, to, back);
    }

    /**
     * Create a single-direction rule.
     *
     * @param fromCls The class from which to convert. If {@link Object} is
     *            specified then this functions as a wildcard for generic
     *            conversions.
     * @param toCls The class to which to convert. If {@link Object} is
     *            specified then this functions as a wildcard for generic
     *            conversions.
     * @param to The conversion function for this rule.
     */
    public Rule(Class<F> fromCls, Class<T> toCls, ConvertFunction<F,T> to) {
        this(fromCls, toCls, to, null);
    }

    public Rule(TypeReference<F> fromType, TypeReference<T> toType, ConvertFunction<F,T> to, ConvertFunction<F,T> back) {
        this(fromType.getType(), toType.getType(), to, back);
    }

    public Rule(TypeReference<F> fromType, TypeReference<T> toType, ConvertFunction<F,T> to) {
        this(fromType.getType(), toType.getType(), to, null);
    }

    public Rule(Type fromType, Type toType,
            @SuppressWarnings("rawtypes") ConvertFunction to, @SuppressWarnings("rawtypes") ConvertFunction back) {
        if (fromType.equals(toType)) {
            if (fromType.equals(Object.class)) {
                if (back != null) {
                    throw new IllegalStateException(
                            "Can only register one catchall converter");
                }
            } else {
                throw new IllegalStateException(
                        "Cannot register a converter to itself");
            }
        }

        sourceType = fromType;
        targetType = toType;
        toFun = to;
        backFun = back;
    }

    /**
     * Accessor for the class to convert from.
     *
     * @return The class to convert from.
     */
    public Type getSourceType() {
        return sourceType;
    }

    /**
     * Accessor for the class to convert to.
     *
     * @return The class to convert to.
     */
    public Type getTargetType() {
        return targetType;
    }

    /**
     * Obtain the conversion function.
     *
     * @return The conversion function.
     */
    @SuppressWarnings("unchecked")
    public ConvertFunction<F,T> getToFunction() {
        return toFun;
    }

    /**
     * Obtain the reverse conversion function.
     *
     * @return The reverse conversion function.
     */
    @SuppressWarnings("unchecked")
    public ConvertFunction<T,F> getBackFunction() {
        return backFun;
    }
}
