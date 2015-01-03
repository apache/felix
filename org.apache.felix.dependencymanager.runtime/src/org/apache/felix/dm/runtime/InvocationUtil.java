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
package org.apache.felix.dm.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Java reflexion utility methods.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InvocationUtil
{
    public static Object invokeCallbackMethod(Object instance,
                                              String methodName,
                                              Class<?>[][] signatures,
                                              Object[][] parameters)
        throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException
    {
        Class<?> currentClazz = instance.getClass();
        while (currentClazz != null)
        {
            try
            {
                return invokeMethod(instance, currentClazz, methodName, signatures, parameters, false);
            }
            catch (NoSuchMethodException nsme)
            {
                // ignore
            }
            currentClazz = currentClazz.getSuperclass();
        }
        throw new NoSuchMethodException(methodName);
    }

    public static Object invokeMethod(Object object,
                                      Class<?> clazz,
                                      String name,
                                      Class<?>[][] signatures,
                                      Object[][] parameters,
                                      boolean isSuper)
        throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException,
        IllegalAccessException
    {
        if (object == null)
        {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        if (clazz == null)
        {
            throw new IllegalArgumentException("Class cannot be null");
        }

        // If we're talking to a proxy here, dig one level deeper to expose the
        // underlying invocation handler ...

        if (Proxy.isProxyClass(clazz))
        {
            object = Proxy.getInvocationHandler(object);
            clazz = object.getClass();
        }

        for (int i = 0; i < signatures.length; i++)
        {
            Class<?>[] signature = signatures[i];
            try
            {
                final Method m = clazz.getDeclaredMethod(name, signature);
                if (!(isSuper && Modifier.isPrivate(m.getModifiers())))
                {
                    AccessController.doPrivileged(new PrivilegedAction<Object>()
                    {
                        public Object run()
                        {
                            m.setAccessible(true);
                            return null;
                        }
                    });
                    return m.invoke(object, parameters[i]);
                }
            }
            catch (NoSuchMethodException e)
            {
                // ignore this and keep looking
            }
        }
        throw new NoSuchMethodException(name);
    }
}
