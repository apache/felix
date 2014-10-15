package org.apache.felix.dm.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    public static void injectField(Object[] targets, String fieldName, Class<?> clazz, final Object service,
        final Logger logger)
    {
        if (service == null) {
            return;
        }
        mapField(true, clazz, targets, fieldName, logger, new FieldFunction() {
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
    public static void injectDependencyField(Object[] targets, String fieldName, Class<?> clazz,
        final DependencyContext dc, final Logger logger)
    {
        final Event event = dc.getService();
        if (event == null) {
            return;
        }
        mapField(false, clazz, targets, fieldName, logger, new FieldFunction() {
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
                    Map<Object, Dictionary<String, ?>> map = (Map) f.get(target);
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
                    Map<Object, Dictionary<String, ?>> map = (Map) f.get(target);
                    if (add) {
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
    private static void mapField(boolean strict, Class<?> clazz, Object[] targets, String fieldName, Logger logger,
        FieldFunction func)
    {
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
                                func.injectField(field, target);
                            } else if (!strict && mayInjectToIterable(clazz, field, true)) {
                                func.injectIterableField(field, target);
                            } else if (!strict && mayInjectToMap(clazz, field, true)) {
                                func.injectMapField(field, target);
                            }
                        } else if (field.getName().equals(fieldName)) {
                            // Field type may be a superclass of the service type
                            if (fieldType.isAssignableFrom(clazz)) {
                                func.injectField(field, target);
                            } else if (!strict && mayInjectToIterable(clazz, field, false)) {
                                func.injectIterableField(field, target);
                            } else if (!strict && mayInjectToMap(clazz, field, false)) {
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
    }

    private static boolean mayInjectToIterable(Class<?> clazz, Field field, boolean strictClassEquality) {
        Class<?> fieldType = field.getType();
        if (Iterable.class.isAssignableFrom(fieldType)) {
            ParameterizedType parameterType = (ParameterizedType) field.getGenericType();
            if (parameterType == null) {
                return false;
            }
            Class<?> parameterizedTypeClass = (Class<?>) parameterType.getActualTypeArguments()[0];
            return strictClassEquality ? parameterizedTypeClass.equals(clazz)
                : parameterizedTypeClass.isAssignableFrom(clazz);
        }
        return false;
    }

    private static boolean mayInjectToMap(Class<?> clazz, Field field, boolean strictClassEquality) {
        Class<?> fieldType = field.getType();
        if (Map.class.isAssignableFrom(fieldType)) {
            ParameterizedType parameterType = (ParameterizedType) field.getGenericType();
            if (parameterType == null) {
                return false;
            }
            Class<?> K = (Class<?>) parameterType.getActualTypeArguments()[0];
            Class<?> V = (Class<?>) parameterType.getActualTypeArguments()[1];

            if (!V.equals(Dictionary.class)) {
                return false;
            }
            return strictClassEquality ? K.equals(clazz) : K.isAssignableFrom(clazz);
        }
        return false;
    }
}
