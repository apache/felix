/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.jline;

import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class BaseConverters implements Converter {

    public Object convert(Class<?> desiredType, final Object in) throws Exception {
        if (desiredType == Class.class) {
            try {
                return Class.forName(in.toString());
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        if (desiredType.isAssignableFrom(String.class) && in instanceof InputStream) {
            return read(((InputStream) in));
        }

        if (in instanceof Function && isFunctional(desiredType)) {
            return Proxy.newProxyInstance(desiredType.getClassLoader(),
                    new Class[]{desiredType}, new InvocationHandler() {
                        Function command = ((Function) in);

                        public Object invoke(Object proxy, Method method, Object[] args)
                                throws Throwable {
                            if (isObjectMethod(method)) {
                                return method.invoke(command, args);
                            } else if (method.isDefault()) {
                                final Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                                field.setAccessible(true);
                                final MethodHandles.Lookup lookup = (MethodHandles.Lookup) field.get(null);
                                return lookup
                                        .unreflectSpecial(method, method.getDeclaringClass())
                                        .bindTo(proxy)
                                        .invokeWithArguments(args);
                            } else {
                                return command.execute(null, Arrays.asList(args));
                            }
                        }
                    });
        }

        return null;
    }

    public CharSequence format(Object target, int level, Converter converter)
            throws IOException {
        if (level == INSPECT && target instanceof InputStream) {
            return read(((InputStream) target));
        }
        return null;
    }

    private CharSequence read(InputStream in) throws IOException {
        int c;
        StringBuffer sb = new StringBuffer();
        while ((c = in.read()) > 0) {
            if (c >= 32 && c <= 0x7F || c == '\n' || c == '\r') {
                sb.append((char) c);
            } else {
                String s = Integer.toHexString(c).toUpperCase();
                sb.append("\\");
                if (s.length() < 1) {
                    sb.append(0);
                }
                sb.append(s);
            }
        }
        return sb;
    }

    public static boolean isFunctional(Class<?> clazz) {
        if (!clazz.isInterface()) {
            return false;
        }
        int nb = 0;
        for (Method method : clazz.getMethods()) {
            if (method.isDefault() || isObjectMethod(method)) {
                continue;
            }
            nb++;
        }
        return nb == 1;
    }

    public static boolean isObjectMethod(Method method) {
        switch (method.getName()) {
            case "toString":
                if (method.getParameterCount() == 0 && method.getReturnType() == String.class) {
                    return true;
                }
                break;
            case "equals":
                if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == Object.class
                        && method.getReturnType() == boolean.class) {
                    return true;
                }
                break;
            case "hashCode":
                if (method.getParameterCount() == 0 && method.getReturnType() == int.class) {
                    return true;
                }
                break;
        }
        return false;
    }

}
