/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps the result of a method invocation
 */
public class InvocationResult<T> {

    private final Method method;
    private final Object target;
    private final T result;
    private final Throwable error;

    public InvocationResult(Method method, Object target, T result, Throwable error) {
        this.method = method;
        this.target = target;
        this.result = result;
        this.error = error;
    }

    public Method getMethod() {
        return method;
    }

    public Object getTarget() {
        return target;
    }

    public Throwable error() {
        return error;
    }

    public T get() {
        return result;
    }

    public T getOrElse(T def) {
        if (error == null) {
            return def;
        } else {
            return result;
        }
    }

    public static <T> InvocationResult<T> fromInvocation(Method method, Object target, Object[] args) {
        try {
            T result = (T) method.invoke(target, args);
            return new InvocationResult<T>(method, target, result, null);
        } catch (IllegalAccessException e) {
            return new InvocationResult<T>(method, target, null, e);
        } catch (InvocationTargetException e) {
            return new InvocationResult<T>(method, target, null, e);
        }
    }
}
