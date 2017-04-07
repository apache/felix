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

import org.osgi.util.converter.ConvertFunction;

public class Helper {

    @FunctionalInterface
    public interface ConvertFunctionConverter<T> {
        T convert(Object obj, Type targetType) throws Exception;
    }

    @FunctionalInterface
    public interface ConvertFunctionErrorHandler<T> {
        T handleError(Object obj, Type targetType);
    }

    public static <T> ConvertFunction<T> convert(ConvertFunctionConverter<T> converter) {
        return convert(converter, null);
    }

    public static <T> ConvertFunction<T> convert(ConvertFunctionConverter<T> converter, ConvertFunctionErrorHandler<T> errorHandler) {
        return new ConvertFunction<T>() {
            @Override
            public T convert(Object obj, Type targetType) throws Exception {
                return converter != null ? converter.convert(obj, targetType) : null;
            }
        };
    }

}
