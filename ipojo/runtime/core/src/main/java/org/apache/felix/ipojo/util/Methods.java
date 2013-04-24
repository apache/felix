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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Fluent API to retrieve methods.
 */
public class Methods<T> {

    private Class<? extends T> returnType;
    private List<Class<?>> argumentTypes = new ArrayList<Class<?>>();
    private Class<?> clazz;
    private Object object;
    private List<Method> methods;

    public Map<Method, InvocationResult<T>> map(Object... args) {
        Collection<Method> set = retrieve();
        Map<Method, InvocationResult<T>> results = new LinkedHashMap<Method, InvocationResult<T>>();
        for (Method method : set) {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }

            results.put(method, InvocationResult.<T>fromInvocation(method, object, args));

        }
        return results;
    }

    public Collection<InvocationResult<T>> invoke(Object... args) {
        return map(args).values();
    }

    public Methods ofReturnType(Class<? extends T> clazz) {
        this.returnType = clazz;
        return this;
    }

    public Methods withParameter(Class<?>... type) {
        argumentTypes.addAll(Arrays.asList(type));
        return this;
    }

    public Methods in(Object o) {
        this.object = o;
        this.clazz = o.getClass();
        return this;
    }

    public Methods in(Class<?> c) {
        this.clazz = c;
        this.object = null;
        return this;
    }

    private Collection<Method> retrieve() {
        if (methods != null) {
            return methods;
        }

        if (clazz == null) {
            throw new NullPointerException("Cannot retrieve method, class not set");
        }


        methods = new ArrayList<Method>();

        // First the class itself
        Method[] list = clazz.getDeclaredMethods();
        for (Method method : list) {
            // Two criteria : the return type and the argument type
            if (matchReturnType(method)
                    && matchArgumentTypes(method)) {
                // The method matches
                methods.add(method);
            }
        }

        // Traverse class hierarchy
        if (clazz.getSuperclass() != null) {
            traverse(methods, clazz.getSuperclass());
        }

        return methods;
    }

    private boolean matchReturnType(Method method) {
        if (returnType == null) { // Void.
            return method.getReturnType() == null;
        }

        return !(method.getReturnType() == null || !returnType.isAssignableFrom(method.getReturnType()));
    }

    private boolean matchArgumentTypes(Method method) {
        // Fast check, the size must be the same.
        if (argumentTypes.size() != method.getParameterTypes().length) {
            return false;
        }

        // We have the same size.
        for (int i = 0; i < argumentTypes.size(); i++) {
            Class<?> argType = method.getParameterTypes()[i];
            if (!argumentTypes.get(i).isAssignableFrom(argType)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchInheritanceVisibility(Method method) {
        return Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers());
    }

    private boolean matchNotOverridden(Method method, List<Method> methods) {
        for (Method meth : methods) {
            if (methodEquality(meth, method)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares this <code>Method</code> against the specified object.  Returns
     * true if the objects are the same.  Two <code>Methods</code> are the same if
     * they were declared by the same class and have the same name
     * and formal parameter types and return type.
     */
    private boolean methodEquality(Method method1, Method method2) {
        if (method1.getName().equals(method2.getName())) {
            if (!method1.getReturnType().equals(method2.getReturnType())) {
                return false;
            }

            Class[] params1 = method1.getParameterTypes();
            Class[] params2 = method2.getParameterTypes();
            if (params1.length == params2.length) {
                for (int i = 0; i < params1.length; i++) {
                    if (params1[i] != params2[i])
                        return false;
                }
                return true;
            }

        }

        return false;
    }

    private void traverse(List<Method> methods, Class<?> clazz) {
        // First the given class
        Method[] list = clazz.getDeclaredMethods();
        for (Method method : list) {
            if (matchReturnType(method) && matchArgumentTypes(method) && matchInheritanceVisibility(method)
                    && matchNotOverridden(method, methods)) {
                methods.add(method);
            }
        }

        // If we have a parent class, traverse it
        if (clazz.getSuperclass() != null) {
            traverse(methods, clazz.getSuperclass());
        }
    }

}
