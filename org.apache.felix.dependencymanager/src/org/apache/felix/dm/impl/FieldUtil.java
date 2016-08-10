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
package org.apache.felix.dm.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.felix.dm.Logger;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;

/**
 * Reflection Helper methods, used to inject autoconfig fields in component instances.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FieldUtil {
    /**
     * Callbacks for fields to be injected
     */
    private interface FieldFunction {
        // Inject an updated service in the given field for the the given target.
        void injectField(Field f, Object target);

        // Inject an Iterable Field in the given target 
        void injectIterableField(Field f, Object target);

        // Inject a Map field in the given target (key = dependency service, value = Dictionary with dependency service properties).
        void injectMapField(Field f, Object target);
    }

    /**
     * Injects some component instances (on a given field, if provided), with an object of a given class.
     * @param targets the component instances to fill in
     * @param fieldName the fieldname, or null. If null, the field must exaclty match the injected service classname.
     * @param clazz the injected service class
     * @param service the injected service
     * @param logger the component logger.
     */
    public static boolean injectField(Object[] targets, String fieldName, Class<?> clazz, final Object service,
        final Logger logger)
    {
        if (service == null) {
            return true; // TODO why service can be null ?
        }
        return mapField(true, clazz, targets, fieldName, logger, new FieldFunction() {
            public void injectField(Field f, Object target) {
                try {
                    f.setAccessible(true);
                    f.set(target, service);
                } catch (Throwable e) {
                    logger.log(Logger.LOG_ERROR, "Could not set field " + f + " in class "
                        + target.getClass().getName(), e);
                }
            }

            public void injectIterableField(Field f, Object target) { // never called
            }

            public void injectMapField(Field f, Object target) { // never called
            }
        });
    }

    /**
     * Injects a dependency service in some component instances.
     * Here, we'll inject the dependency services in the component if the field is of the same type of the injected services,
     * or if the field is a Collection of the injected service, or if the field is a Map<Injected Service class, Dictionary).
     * @param targets the component instances to fill in
     * @param fieldName the fieldname, or null. If null, the field must exaclty match the injected service classname.
     * @param clazz the injected service class
     * @param service the injected service
     * @param logger the component logger.
     */
    public static boolean injectDependencyField(Object[] targets, String fieldName, Class<?> clazz,
        final DependencyContext dc, final Logger logger)
    {
        final Event event = dc.getService();
        if (event == null) {
            return true; // TODO check why event can be null
        }
        return mapField(false, clazz, targets, fieldName, logger, new FieldFunction() {
            public void injectField(Field f, Object target) {
                try {
                    f.setAccessible(true);
                    f.set(target, event.getEvent());
                } catch (Throwable e) {
                    logger.log(Logger.LOG_ERROR, "Could not set field " + f + " in class "
                        + target.getClass().getName(), e);
                }
            }

            @SuppressWarnings("unchecked")
            public void injectIterableField(Field f, Object target) {
                f.setAccessible(true);

                try {
                    Iterable<Object> iter = (Iterable<Object>) f.get(target);
                    if (iter == null) {
                        iter = new ConcurrentLinkedQueue<Object>();
                        f.set(target, iter);
                    }
                    dc.copyToCollection((Collection<Object>) iter);
                } catch (Throwable e) {
                    logger.log(Logger.LOG_ERROR, "Could not set field " + f + " in class "
                        + target.getClass().getName(), e);
                }
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public void injectMapField(Field f, Object target) {
                f.setAccessible(true);
                try {
                    Map<Object, Dictionary<?, ?>> map = (Map) f.get(target);
                    if (map == null) {
                        map = new ConcurrentHashMap<>();
                        f.set(target, map);
                    }
                    dc.copyToMap(map);
                } catch (Throwable e) {
                    logger.log(Logger.LOG_ERROR, "Could not set field " + f + " in class "
                        + target.getClass().getName(), e);
                }
            }
        });
    }

    /**
     * Adds, or removes, or update some component instances with an updated dependency service
     * @param targets the component instances to fill in with the updated service
     * @param fieldName the component instance fieldname 
     * @param update true if it's a dependency service update, false if the dependency service is added or removed
     * @param add true if the dependency service has been added, false the dependency service has been removed. 
     * This flag is ignored if the "update" parameter is "true". 
     * @param clazz the clazz of the dependency service
     * @param event the event holding the dependency service
     * @param dc the dependency service context
     * @param logger the logger used when problems occure.
     */
    public static void updateDependencyField(Object[] targets, String fieldName, final boolean update,
        final boolean add, Class<?> clazz, final Event event, final DependencyContext dc, final Logger logger)
    {
        mapField(false, clazz, targets, fieldName, logger, new FieldFunction() {
            public void injectField(Field f, Object target) {
                try {
                    f.setAccessible(true);
                    f.set(target, dc.getService().getEvent());
                } catch (Throwable e) {
                    logger.log(Logger.LOG_ERROR, "Could not set field " + f + " in class "
                        + target.getClass().getName(), e);
                }
            }

            @SuppressWarnings("unchecked")
            public void injectIterableField(Field f, Object target) {
                if (update) {
                    return;
                }

                f.setAccessible(true);

                try {
                    Collection<Object> coll = (Collection<Object>) f.get(target);
                    if (add) {
                        coll.add(event.getEvent());
                    } else {
                        coll.remove(event.getEvent());
                    }
                } catch (Throwable e) {
                    logger.log(Logger.LOG_ERROR, "Could not set field " + f + " in class "
                        + target.getClass().getName(), e);
                }
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public void injectMapField(Field f, Object target) {
                f.setAccessible(true);

                try {
                    Map<Object, Dictionary<?, ?>> map = (Map) f.get(target);
                    if (add || update) {
                        map.put(event.getEvent(), event.getProperties());
                    } else {
                        map.remove(event.getEvent());
                    }
                } catch (Throwable e) {
                    logger.log(Logger.LOG_ERROR, "Could not set field " + f + " in class "
                        + target.getClass().getName(), e);
                }
            }
        });
    }

    /**
     * Scans component instances for fields having either the same dependency service type, or being a 
     * Collection of the dependency service, or being a Map<Dependency Service class, Dictionary> (the Dictionary
     * corresponds to the dependency service properties).
     * @param strict true if we are only looking for fields having exactly the same type as the dependency service. 
     * In other words, if strict = true, we don't lookup for fields of "Collection" or "Map" types.
     * @param clazz the dependency service class
     * @param targets the component instances
     * @param fieldName the component instances field name or null
     * @param logger a logger used when exceptions are occuring
     * @param func the callback used to notify when we find either a field with the same dependency service type, or
     * with a "Collection" type, or with a "Map" type.
     */
    private static boolean mapField(boolean strict, Class<?> clazz, Object[] targets, String fieldName, Logger logger,
        FieldFunction func)
    {
        boolean injected = false;
        if (targets != null && clazz != null) {
            for (int i = 0; i < targets.length; i++) {
                Object target = targets[i];
                Class<?> targetClass = target.getClass();
                if (Proxy.isProxyClass(targetClass)) {
                    target = Proxy.getInvocationHandler(target);
                    targetClass = target.getClass();
                }
                while (targetClass != null) {
                    Field[] fields = targetClass.getDeclaredFields();
                    for (int j = 0; j < fields.length; j++) {
                        Field field = fields[j];
                        Class<?> fieldType = field.getType();

                        if (fieldName == null) {
                            // Field type class must match injected service type
                            if (fieldType.equals(clazz)) {
                                injected = true;
                                func.injectField(field, target);
                            } else if (!strict && mayInjectToIterable(clazz, field, true)) {
                                injected = true;
                                func.injectIterableField(field, target);
                            } else if (!strict && mayInjectToMap(clazz, field, true)) {
                                injected = true;
                                func.injectMapField(field, target);
                            }
                        } else if (field.getName().equals(fieldName)) {
                            // Field type may be a superclass of the service type
                            if (fieldType.isAssignableFrom(clazz)) {
                                injected = true;
                                func.injectField(field, target);
                            } else if (!strict && mayInjectToIterable(clazz, field, false)) {
                                injected = true;
                                func.injectIterableField(field, target);
                            } else if (!strict && mayInjectToMap(clazz, field, false)) {
                                injected = true;
                                func.injectMapField(field, target);
                            } else {
                                logger.log(
                                    Logger.LOG_ERROR,
                                    "Could not set field " + field + " in class " + target.getClass().getName()
                                        + ": the type of the field type should be either assignable from "
                                        + clazz.getName() + " or Collection, or Map");
                            }
                        }
                    }
                    targetClass = targetClass.getSuperclass();
                }
            }
        }
        return injected;
    }

    private static boolean mayInjectToIterable(Class<?> clazz, Field field, boolean strictClassEquality) {
        Class<?> fieldType = field.getType();
        if (Iterable.class.isAssignableFrom(fieldType)) {
            Type type = field.getGenericType();
            
            // The field must be a parameterized map (generics).
            if (! (type instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType parameterType = (ParameterizedType) type;
            Type[] types = parameterType.getActualTypeArguments();
            if (types == null || types.length != 1) {
            	return false;
            }
            if (types[0] instanceof Class<?>) {
            	Class<?> parameterizedTypeClass = (Class<?>) types[0];
            	return strictClassEquality ? parameterizedTypeClass.equals(clazz)
            			: parameterizedTypeClass.isAssignableFrom(clazz);
            }	
        }
        return false;
    }

    private static boolean mayInjectToMap(Class<?> clazz, Field field, boolean strictClassEquality) {
        Class<?> fieldType = field.getType();
        if (Map.class.isAssignableFrom(fieldType)) {
            Type type = field.getGenericType();
            
            // The field must be a parameterized map (generics).
            if (! (type instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType parameterType = (ParameterizedType) type;
            Type[] types = parameterType.getActualTypeArguments();
            if (types == null || types.length != 2) {
            	return false;
            }
                   
            // The map field generic key parameter must be "Class".
            if (! (types[0] instanceof Class<?>)) {
                return false;
            }
            
            // The map generic value parameter must be Dictionary, or Dictionary<String, ...>
            if (types[1] instanceof Class<?>) {
                // The map field is in the form "Map m_field<Class, Dictionary>"
                Class<?> mapValueGenericType = (Class<?>) types[1];
                if (! mapValueGenericType.equals(Dictionary.class)) {
                    return false;
                }
            } else if (types[1] instanceof ParameterizedType) {
                // The map field is in the form "Map m_field<Class, Dictionary<String, ...>"
                ParameterizedType mapValueGenericType = (ParameterizedType) types[1];
                if (! mapValueGenericType.getRawType().equals(Dictionary.class)) {
                    return false;
                }
            }
            
            Class<?> K = (Class<?>) types[0];
            return strictClassEquality ? K.equals(clazz) : K.isAssignableFrom(clazz);
        }
        return false;
    }
}
