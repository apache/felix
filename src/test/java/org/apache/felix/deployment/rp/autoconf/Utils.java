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
package org.apache.felix.deployment.rp.autoconf;

import java.io.File;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Utility class that injects dependencies. Can be used to unit test service implementations.
 */
public class Utils
{
    /**
     * Configures an object to use a null object for the specified service interface.
     *
     * @param object the object
     * @param iface the service interface
     */
    public static void configureObject(Object object, Class iface)
    {
        configureObject(object, iface, createNullObject(iface));
    }

    /**
     * Creates a null object for a service interface.
     *
     * @param iface the service interface
     * @return a null object
     */
    public static <T> T createNullObject(Class<T> iface)
    {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[] { iface }, new DefaultNullObject());
    }

    /**
     * Wraps the given handler in an adapter that will try to pass on received invocations to the hander if that has
     * an applicable methods else it defaults to a NullObject.
     *
     * @param iface the service interface
     * @param handler the handler to pass invocations to.
     * @return an adapter that will try to pass on received invocations to the given handler
     */
    public static <T> T createMockObjectAdapter(Class<T> iface, final Object handler)
    {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[] { iface }, new DefaultNullObject()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                try
                {
                    Method bridge = handler.getClass().getMethod(method.getName(), method.getParameterTypes());
                    bridge.setAccessible(true);
                    return bridge.invoke(handler, args);
                }
                catch (NoSuchMethodException ex)
                {
                    return super.invoke(proxy, method, args);
                }
                catch (InvocationTargetException ex)
                {
                    throw ex.getCause();
                }
            }
        });
    }

    /**
     * Configures an object to use a specific implementation for the specified service interface.
     *
     * @param object the object
     * @param iface the service interface
     * @param instance the implementation
     */
    public static void configureObject(Object object, Class iface, Object instance)
    {
        Class serviceClazz = object.getClass();

        while (serviceClazz != null)
        {
            Field[] fields = serviceClazz.getDeclaredFields();
            AccessibleObject.setAccessible(fields, true);
            for (int j = 0; j < fields.length; j++)
            {
                if (fields[j].getType().equals(iface))
                {
                    try
                    {
                        // synchronized makes sure the field is actually written to immediately
                        synchronized (new Object())
                        {
                            fields[j].set(object, instance);
                        }
                    }
                    catch (Exception e)
                    {
                        throw new IllegalStateException("Could not set field " + fields[j].getName() + " on " + object);
                    }
                }
            }
            serviceClazz = serviceClazz.getSuperclass();
        }
    }

    /**
     * Remove the given directory and all it's files and subdirectories
     * 
     * @param directory the name of the directory to remove
     */
    public static void removeDirectoryWithContent(File directory)
    {
        if ((directory == null) || !directory.exists())
        {
            return;
        }
        File[] filesAndSubDirs = directory.listFiles();
        for (int i = 0; i < filesAndSubDirs.length; i++)
        {
            File file = filesAndSubDirs[i];
            if (file.isDirectory())
            {
                removeDirectoryWithContent(file);
            }
            // else just remove the file
            file.delete();
        }
        directory.delete();
    }
}
