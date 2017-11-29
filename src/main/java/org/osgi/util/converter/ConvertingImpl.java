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
package org.osgi.util.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author $Id: 0316415f23407ff80f745182a0422f3542488008 $
 */
class ConvertingImpl extends AbstractSpecifying<Converting> implements Converting, InternalConverting {
	private static final Map<Class< ? >,Class< ? >>	INTERFACE_IMPLS;
	// Interfaces with no methods are also not considered
	private static final Collection<Class< ? >>		NO_MAP_VIEW_TYPES;
    static {

		Map<Class< ? >,Class< ? >> cim = new HashMap<>();
		cim.put(Collection.class, ArrayList.class);
		// Lists
		cim.put(List.class, ArrayList.class);
		// Sets
		cim.put(Set.class, LinkedHashSet.class); // preserves insertion order
		cim.put(NavigableSet.class, TreeSet.class);
		cim.put(SortedSet.class, TreeSet.class);
		// Queues
		cim.put(Queue.class, LinkedList.class);
		cim.put(Deque.class, LinkedList.class);

		Map<Class< ? >,Class< ? >> iim = new HashMap<>(cim);
		// Maps
		iim.put(Map.class, LinkedHashMap.class); // preserves insertion order
		iim.put(ConcurrentMap.class, ConcurrentHashMap.class);
		iim.put(ConcurrentNavigableMap.class,
				ConcurrentSkipListMap.class);
		iim.put(NavigableMap.class, TreeMap.class);
		iim.put(SortedMap.class, TreeMap.class);

		Set<Class< ? >> nmv = new HashSet<>(cim.keySet());
		nmv.addAll(Arrays.<Class< ? >> asList(String.class, Class.class,
				Comparable.class, CharSequence.class, Map.Entry.class));

		INTERFACE_IMPLS = Collections.unmodifiableMap(iim);
		NO_MAP_VIEW_TYPES = Collections.unmodifiableSet(nmv);
    }


	volatile InternalConverter					converter;
    private volatile Object object;
	private volatile Class< ? >					sourceClass;
    private volatile Class<?> targetClass;
	private volatile Type[]						typeArguments;

    ConvertingImpl(InternalConverter c, Object obj) {
        converter = c;
        object = obj;
    }

    @Override
    public void setConverter(Converter c) {
        if (c instanceof InternalConverter)
            converter = (InternalConverter) c;
        else
            throw new IllegalStateException("Incorrect converter used. Should implement " +
                InternalConverter.class + " but was " + c);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T to(Class<T> cls)  {
        Type type = cls;
        return (T) to(type);
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T to(TypeReference<T> ref)  {
        return (T) to(ref.getType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object to(Type type) {
        Class<?> cls = null;
        if (type instanceof Class) {
            cls = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type rt = pt.getRawType();
            typeArguments = pt.getActualTypeArguments();
            if (rt instanceof Class)
                cls = (Class<?>) rt;
        }
        if (cls == null)
            return null;

        if (object == null)
            return handleNull(cls);

        targetClass = Util.primitiveToBoxed(cls);
        if (targetAsClass == null)
            targetAsClass = targetClass;

        sourceClass = sourceAsClass != null ? sourceAsClass : object.getClass();

        if (!isCopyRequiredType(targetAsClass) && targetAsClass.isAssignableFrom(sourceClass)) {
                return object;
        }

        Object res = trySpecialCases();
        if (res != null)
            return res;

        if (targetAsClass.isArray()) {
            return convertToArray();
        } else if (Collection.class.isAssignableFrom(targetAsClass)) {
            return convertToCollectionType();
		} else if (isMapType(targetAsClass, targetAsJavaBean, targetAsDTO)) {
            return convertToMapType();
        }

        // At this point we know that the target is a 'singular' type: not a map, collection or array
        if (Collection.class.isAssignableFrom(sourceClass)) {
            return convertCollectionToSingleValue(targetAsClass);
		} else if (isMapType(sourceClass, sourceAsJavaBean, sourceAsDTO)) {
			return convertMapToSingleValue(targetAsClass);
        } else if (object instanceof Map.Entry) {
            return convertMapEntryToSingleValue(targetAsClass);
        } else if ((object = asBoxedArray(object)) instanceof Object[]) {
            return convertArrayToSingleValue(targetAsClass);
        }

        Object res2 = tryStandardMethods();
        if (res2 != null) {
            return res2;
        } else {
            if (hasDefault)
                return converter.convert(defaultValue).sourceAs(sourceAsClass).targetAs(targetAsClass).to(targetClass);
            else
                throw new ConversionException("Cannot convert " + object + " to " + targetAsClass);
        }
    }

	private Object convertArrayToSingleValue(Class< ? > cls) {
        Object[] arr = (Object[]) object;
        if (arr.length == 0)
            return null;
        else
            return converter.convert(arr[0]).to(cls);
    }

    private Object convertCollectionToSingleValue(Class<?> cls) {
        Collection<?> coll = (Collection<?>) object;
        if (coll.size() == 0)
            return null;
        else
            return converter.convert(coll.iterator().next()).to(cls);
    }

	private Object convertMapToSingleValue(Class< ? > cls) {
		Map< ? , ? > m = mapView(object, sourceClass, converter);
		if (m.size() > 0) {
			return converter.convert(m.entrySet().iterator().next()).to(cls);
		} else {
			return null;
		}
	}

    @SuppressWarnings("rawtypes")
    private Object convertMapEntryToSingleValue(Class<?> cls) {
        Map.Entry entry = (Map.Entry) object;

        Class keyCls = entry.getKey() != null ? entry.getKey().getClass() : null;
        Class valueCls = entry.getValue() != null ? entry.getValue().getClass() : null;

        if (cls.equals(keyCls)) {
            return converter.convert(entry.getKey()).to(cls);
        } else if (cls.equals(valueCls)) {
            return converter.convert(entry.getValue()).to(cls);
        } else if (cls.isAssignableFrom(keyCls)) {
            return converter.convert(entry.getKey()).to(cls);
        } else if (cls.isAssignableFrom(valueCls)) {
            return converter.convert(entry.getValue()).to(cls);
        } else if (entry.getKey() instanceof String) {
            return converter.convert(entry.getKey()).to(cls);
        } else if (entry.getValue() instanceof String) {
            return converter.convert(entry.getValue()).to(cls);
        }

        return converter.convert(converter.convert(entry.getKey()).to(String.class)).to(cls);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToArray() {
		Collection< ? > collectionView = collectionView();
        Iterator<?> itertor = collectionView.iterator();
        try {
            Object array = Array.newInstance(targetAsClass.getComponentType(), collectionView.size());
            for (int i=0; i<collectionView.size() && itertor.hasNext(); i++) {
                Object next = itertor.next();
                Object converted = converter.convert(next).to(targetAsClass.getComponentType());
                Array.set(array, i, converted);
            }
            return (T) array;
        } catch (Exception e) {
            return null;
        }
    }

	@SuppressWarnings("unchecked")
    private <T> T convertToCollectionType() {
		Collection< ? > res = convertToCollectionDelegate();
		if (res != null)
			return (T) res;

		return convertToCollection();
	}

	private Collection< ? > convertToCollectionDelegate() {
		if (forceCopy)
			return null;
		
		if (List.class.equals(targetClass) || Collection.class.equals(targetClass)) {
			if (sourceClass.isArray()) {
				return ListDelegate.forArray(object, this);
			} else if (Collection.class.isAssignableFrom(sourceClass)) {
				return ListDelegate.forCollection((Collection) object, this);
			}
		} else if (Set.class.equals(targetClass)) {
			if (sourceClass.isArray()) {
				return SetDelegate
						.forCollection(ListDelegate.forArray(object, this),
								this);
			} else if (Collection.class.isAssignableFrom(sourceClass)) {
				return SetDelegate.forCollection((Collection) object, this);
			}
		}
		return null;
	}

	private <T> T convertToCollection() {
		Collection< ? > cv = collectionView();
        Class<?> targetElementType = null;
        if (typeArguments != null && typeArguments.length > 0 && typeArguments[0] instanceof Class) {
            targetElementType = (Class<?>) typeArguments[0];
        }

        Class<?> ctrCls = INTERFACE_IMPLS.get(targetAsClass);
        Class<?>targetCls;
        if (ctrCls != null)
            targetCls = ctrCls;
        else
            targetCls = targetAsClass;

        Collection instance = (Collection) createMapOrCollection(targetCls, cv.size());
        if (instance == null)
            return null;

        for (Object o : cv) {
            if (targetElementType != null) {
                try {
                    o = converter.convert(o).to(targetElementType);
                } catch (ConversionException ce) {
                    if (hasDefault) {
                        return (T) defaultValue;
                    }
                }
            }

            instance.add(o);
        }

        return (T) instance;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> T convertToDTO(Class<?> sourceCls, Class<?> targetAsCls) {
        Map m = mapView(object, sourceCls, converter);

        try {
            String prefix = Util.getPrefix(targetAsCls);

            T dto = (T) targetClass.newInstance();

            for (Map.Entry entry : (Set<Map.Entry>) m.entrySet()) {
                Object key = entry.getKey();
                if (key == null)
                    continue;

                String fieldName = Util.mangleName(prefix, key.toString());
                if (fieldName == null)
                    continue;

                Field f = null;
                try {
                    f = targetAsCls.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    try {
                        f = targetAsCls.getField(fieldName);
                    } catch (NoSuchFieldException | NullPointerException e1) {
                        // There is no field with this name
                        if (keysIgnoreCase) {
                            // If enabled, try again but now ignore case
                            for (Field fs : targetAsCls.getDeclaredFields()) {
                                if (fs.getName().equalsIgnoreCase(fieldName)) {
                                    f = fs;
                                    break;
                                }
                            }

                            if (f == null) {
                                for (Field fs : targetAsCls.getFields()) {
                                    if (fs.getName().equalsIgnoreCase(fieldName)) {
                                        f = fs;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (f != null) {
                    Object val = entry.getValue();
                    if (sourceAsDTO && DTOUtil.isDTOType(f.getType()))
                        val = converter.convert(val).sourceAsDTO().to(f.getType());
                    else
                        val = converter.convert(val).to(f.getType());
                    f.set(dto, val);
                }
            }

            return dto;
        } catch (Exception e) {
            throw new ConversionException("Cannot create DTO " + targetClass, e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map convertToMap() {
        Map m = mapView(object, sourceClass, converter);
        if (m == null)
            return null;

        Class<?> ctrCls = INTERFACE_IMPLS.get(targetClass);
        if (ctrCls == null)
            ctrCls = targetClass;

        Map instance = (Map) createMapOrCollection(ctrCls, m.size());
        if (instance == null)
            return null;

        for (Map.Entry entry : (Set<Entry>) m.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
			key = convertMapKey(key);
            value = convertMapValue(value);
            instance.put(key, value);
        }

        return instance;
    }

	Object convertCollectionValue(Object element) {
		Type targetType = null;
		if (typeArguments != null && typeArguments.length > 0) {
			targetType = typeArguments[0];
		}

		if (element != null) {
			if (targetType != null) {
				element = converter.convert(element).to(targetType);
			} else {
				Class< ? > cls = element.getClass();
				if (isCopyRequiredType(cls)) {
					cls = getConstructableType(cls);
				}

				if (sourceAsDTO || DTOUtil.isDTOType(cls))
					element = converter.convert(element).sourceAsDTO().to(cls);
				else
					element = converter.convert(element).to(cls);
			}
		}
		return element;
	}

	Object convertMapKey(Object key) {
		return convertMapElement(key, 0);
	}

    Object convertMapValue(Object value) {
		return convertMapElement(value, 1);
	}

	private Object convertMapElement(Object element, int typeIdx) {
		Type targetType = null;
		if (typeArguments != null && typeArguments.length > typeIdx) {
			targetType = typeArguments[typeIdx];
		}

		if (element != null) {
			if (targetType != null) {
				element = converter.convert(element).to(targetType);
			} else {
				Class< ? > cls = element.getClass();
				if (isCopyRequiredType(cls)) {
					cls = getConstructableType(cls);
				}

				if (sourceAsDTO || DTOUtil.isDTOType(cls))
					element = converter.convert(element).sourceAsDTO().to(cls);
				else
					element = converter.convert(element).to(cls);
			}
		}
		return element;
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
    private Map convertToMapDelegate() {
        if (Map.class.isAssignableFrom(sourceClass)) {
            return MapDelegate.forMap((Map) object, this);
        } else if (Dictionary.class.isAssignableFrom(sourceClass)) {
            return MapDelegate.forDictionary((Dictionary) object, this);
        } else if (DTOUtil.isDTOType(sourceClass) || sourceAsDTO) {
			return MapDelegate.forDTO(object, sourceClass, this);
        } else if (sourceAsJavaBean) {
			return MapDelegate.forBean(object, sourceClass, this);
        } else if (hasGetProperties(sourceClass)) {
            return null; // Handled in convertToMap()
        }

        // Assume it's an interface
		Set<Class< ? >> interfaces = getInterfaces(sourceClass);
		if (interfaces.size() > 0) {
			return MapDelegate.forInterface(object,
					interfaces.iterator().next(), this);
		}
		return null;
    }

    @SuppressWarnings("rawtypes")
    private Object convertToMapType() {
		if (!isMapType(sourceClass, sourceAsJavaBean, sourceAsDTO)) {
			throw new ConversionException(
					"Cannot convert " + object + " to " + targetAsClass);
		}

        if (Map.class.equals(targetClass) && !forceCopy) {
            Map res = convertToMapDelegate();
            if (res != null)
                return res;
        }

        if (Map.class.isAssignableFrom(targetAsClass))
            return convertToMap();
        else if (Dictionary.class.isAssignableFrom(targetAsClass))
            return convertToDictionary();
        else if (targetAsDTO || DTOUtil.isDTOType(targetAsClass))
            return convertToDTO(sourceClass, targetAsClass);
        else if (targetAsClass.isInterface())
            return convertToInterface(sourceClass, targetAsClass);
        else if (targetAsJavaBean)
            return convertToJavaBean(sourceClass, targetAsClass);
        throw new ConversionException("Cannot convert " + object + " to " + targetAsClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object convertToDictionary() {
        return new Hashtable((Map) converter.convert(object).to(new ParameterizedType() {
            @Override
            public Type getRawType() {
                return HashMap.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

					@SuppressWarnings("synthetic-access")
					@Override
            public Type[] getActualTypeArguments() {
                return typeArguments;
            }
        }));
    }

    private Object convertToJavaBean(Class<?> sourceCls, Class<?> targetCls) {
		String prefix = Util.getPrefix(targetCls);

        @SuppressWarnings("rawtypes")
        Map m = mapView(object, sourceCls, converter);
        try {
            Object res = targetClass.newInstance();
            for (Method setter : getSetters(targetCls)) {
                String setterName = setter.getName();
                StringBuilder propName = new StringBuilder(Character.valueOf(Character.toLowerCase(setterName.charAt(3))).toString());
                if (setterName.length() > 4)
                    propName.append(setterName.substring(4));

                Class<?> setterType = setter.getParameterTypes()[0];
				String key = propName.toString();
				Object val = m.get(Util.unMangleName(prefix, key));
				setter.invoke(res, converter.convert(val).to(setterType));
            }
            return res;
        } catch (Exception e) {
            throw new ConversionException("Cannot convert to class: " + targetCls.getName() +
                    ". Not a JavaBean with a Zero-arg Constructor.", e);
        }
    }

    @SuppressWarnings("rawtypes")
    private Object convertToInterface(Class<?> sourceCls, final Class<?> targetCls) {
		InternalConverting ic = converter.convert(object);
		ic.sourceAs(sourceAsClass);
		if (sourceAsDTO)
			ic.sourceAsDTO();
		if (sourceAsJavaBean)
			ic.sourceAsBean();
		final Map m = ic.to(Map.class);

        return Proxy.newProxyInstance(targetCls.getClassLoader(), new Class[] {targetCls},
            new InvocationHandler() {
					@SuppressWarnings("boxing")
					@Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Class<?> mdDecl = method.getDeclaringClass();
                    if (mdDecl.equals(Object.class))
                        switch (method.getName()) {
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "toString":
                            return "Proxy for " + targetCls;
                        default:
                            throw new UnsupportedOperationException("Method " + method + " not supported on proxy for " + targetCls);
                        }
                    if (mdDecl.equals(Annotation.class)) {
                        if ("annotationType".equals(method.getName()) && method.getParameterTypes().length == 0) {
                            return targetCls;
                        }
                    }

                    String propName = Util.getInterfacePropertyName(method, Util.getSingleElementAnnotationKey(targetCls, proxy), proxy);
                    if (propName == null)
                        return null;

                    Class<?> targetType = method.getReturnType();

                    Object val = m.get(propName);
                    if (val == null && keysIgnoreCase) {
                        // try in a case-insensitive way
                        for (Iterator it = m.keySet().iterator(); it.hasNext() && val == null; ) {
                            String k = it.next().toString();
                            if (propName.equalsIgnoreCase(k)) {
                                val = m.get(k);
                            }
                        }
                    }

                    // If no value is available take the default if specified
                    if (val == null) {
                        if (targetCls.isAnnotation()) {
                            val = method.getDefaultValue();
                        }

                        if (val == null) {
                            if (args != null && args.length == 1) {
									val = args[0];
								} else {
									throw new ConversionException(
											"No value for property: "
													+ propName);
								}
                        }
                    }

                    return converter.convert(val).to(targetType);
                }
            });
    }

	@SuppressWarnings("boxing")
	private Object handleNull(Class< ? > cls) {
        if (hasDefault)
            return converter.convert(defaultValue).to(cls);

        Class<?> boxed = Util.primitiveToBoxed(cls);
        if (boxed.equals(cls)) {
            if (cls.isArray()) {
                return new Object[] {};
            } else if (Collection.class.isAssignableFrom(cls)) {
                return converter.convert(Collections.emptyList()).to(cls);
            }
            // This is not a primitive, just return null
            return null;
        }

        return converter.convert(0).to(cls);
    }

	private static boolean isMapType(Class< ? > cls, boolean asJavaBean,
			boolean asDTO) {
		if (asDTO)
			return true;

        // All interface types that are not Collections are treated as maps
        if (Map.class.isAssignableFrom(cls))
            return true;
		else if (getInterfaces(cls).size() > 0)
            return true;
        else if (DTOUtil.isDTOType(cls))
            return true;
        else if (asJavaBean && isWriteableJavaBean(cls))
            return true;
        else
            return Dictionary.class.isAssignableFrom(cls);
    }

	@SuppressWarnings("boxing")
	private Object trySpecialCases() {
        if (Boolean.class.equals(targetAsClass)) {
            if (object instanceof Collection && ((Collection<?>) object).size() == 0) {
                return Boolean.FALSE;
            }
        } else if (Number.class.isAssignableFrom(targetAsClass)) {
            if (object instanceof Boolean) {
                return ((Boolean) object).booleanValue() ? 1 : 0;
            } else if (object instanceof Number) {
                if (Byte.class.isAssignableFrom(targetAsClass)) {
                    return ((Number) object).byteValue();
                } else if (Short.class.isAssignableFrom(targetAsClass)) {
                    return ((Number) object).shortValue();
                } else if (Integer.class.isAssignableFrom(targetAsClass)) {
                    return ((Number) object).intValue();
                } else if (Long.class.isAssignableFrom(targetAsClass)) {
                    return ((Number) object).longValue();
                } else if (Float.class.isAssignableFrom(targetAsClass)) {
                    return ((Number) object).floatValue();
                } else if (Double.class.isAssignableFrom(targetAsClass)) {
                    return ((Number) object).doubleValue();
                }
            }
        } else if (Enum.class.isAssignableFrom(targetAsClass)) {
            if (object instanceof Number) {
                try {
                    Method m = targetAsClass.getMethod("values");
                    Object[] values = (Object[]) m.invoke(null);
                    return values[((Number) object).intValue()];
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    Method m = targetAsClass.getMethod("valueOf", String.class);
                    return m.invoke(null, object.toString());
                } catch (Exception e) {
                    try {
                        // Case insensitive fallback
                        Method m = targetAsClass.getMethod("values");
                        for (Object v : (Object[]) m.invoke(null)) {
                            if (v.toString().equalsIgnoreCase(object.toString())) {
                                return v;
                            }
                        }
                    } catch (Exception e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T tryStandardMethods() {
        try {
            Method m = targetAsClass.getDeclaredMethod("valueOf", String.class);
            if (m != null) {
                return (T) m.invoke(null, object.toString());
            }
        } catch (Exception e) {
            try {
                Constructor<?> ctr = targetAsClass.getConstructor(String.class);
                return (T) ctr.newInstance(object.toString());
            } catch (Exception e2) {
				// Ignore
            }
        }
        return null;
    }

	private Collection< ? > collectionView() {
		if (object == null)
            return null;

		Collection< ? > c = asCollection();
        if (c == null)
			return Collections.singleton(object);
        else
            return c;
    }

	private Collection< ? > asCollection() {
		if (object instanceof Collection)
			return (Collection< ? >) object;
		else if ((object = asBoxedArray(object)) instanceof Object[])
			return Arrays.asList((Object[]) object);
		else if (isMapType(sourceClass, sourceAsJavaBean, sourceAsDTO))
			return mapView(object, sourceClass, converter).entrySet();
		else
            return null;
    }

    private static Object asBoxedArray(Object obj) {
        Class<?> objClass = obj.getClass();
        if (!objClass.isArray())
            return obj;

        int len = Array.getLength(obj);
        Object arr = Array.newInstance(Util.primitiveToBoxed(objClass.getComponentType()), len);
        for (int i=0; i<len; i++) {
            Object val = Array.get(obj, i);
            Array.set(arr, i, val);
        }
        return arr;
    }

    @SuppressWarnings("rawtypes")
    private static Map createMapFromBeanAccessors(Object obj, Class<?> sourceCls) {
        Set<String> invokedMethods = new HashSet<>();

        Map result = new HashMap();
        for (Method md : sourceCls.getDeclaredMethods()) {
            handleBeanMethod(obj, md, invokedMethods, result);
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    private Map createMapFromDTO(Object obj, InternalConverter ic) {
        Set<String> handledFields = new HashSet<>();

        Map result = new HashMap();
        // Do we need 'declaredfields'? We only need to look at the public ones...
        for (Field f : obj.getClass().getDeclaredFields()) {
            handleDTOField(obj, f, handledFields, result, ic);
        }
        for (Field f : obj.getClass().getFields()) {
            handleDTOField(obj, f, handledFields, result, ic);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
	private static Map createMapFromInterface(Object obj, Class< ? > srcCls) {
        Map result = new HashMap();

		for (Class i : getInterfaces(srcCls)) {
            for (Method md : i.getMethods()) {
                handleInterfaceMethod(obj, i, md, new HashSet<String>(), result);
            }
            if (result.size() > 0)
                return result;
        }
        throw new ConversionException("Cannot be converted to map: " + obj);
    }

	@SuppressWarnings("boxing")
	private static Object createMapOrCollection(Class< ? > cls,
			int initialSize) {
        try {
            Constructor<?> ctor = cls.getConstructor(int.class);
            return ctor.newInstance(initialSize);
        } catch (Exception e1) {
            try {
                Constructor<?> ctor2 = cls.getConstructor();
                return ctor2.newInstance();
            } catch (Exception e2) {
                // ignore
            }
        }
        return null;
    }

    private static Class<?> getConstructableType(Class<?> targetCls) {
        if (targetCls.isArray())
            return targetCls;

        Class<?> cls = targetCls;
        do {
            try {
                cls.getConstructor(int.class);
                return cls; // If no exception the constructor is there
            } catch (NoSuchMethodException e) {
                try {
                    cls.getConstructor();
                    return cls; // If no exception the constructor is there
                } catch (NoSuchMethodException e1) {
                    // There is no constructor with this name
                }
            }
            for (Class<?> intf : cls.getInterfaces()) {
                Class<?> impl = INTERFACE_IMPLS.get(intf);
                if (impl != null)
                    return impl;
            }

            cls = cls.getSuperclass();
        } while (!Object.class.equals(cls));

        return null;
    }

	// Returns an ordered set
	private static Set<Class< ? >> getInterfaces(Class< ? > cls) {
		if (NO_MAP_VIEW_TYPES.contains(cls))
			return Collections.emptySet();

		Set<Class< ? >> interfaces = getInterfaces0(cls);
		for (Iterator<Class< ? >> it = interfaces.iterator(); it.hasNext();) {
			Class< ? > intf = it.next();
			if (intf.getDeclaredMethods().length == 0)
				it.remove();
		}

		interfaces.removeAll(NO_MAP_VIEW_TYPES);

		return interfaces;
	}

	// Returns an ordered set
	private static Set<Class< ? >> getInterfaces0(Class< ? > cls) {
		if (cls == null)
			return Collections.emptySet();

		Set<Class< ? >> classes = new LinkedHashSet<>();
		if (cls.isInterface()) {
			classes.add(cls);
		} else {
			classes.addAll(Arrays.asList(cls.getInterfaces()));
		}

		classes.addAll(getInterfaces(cls.getSuperclass()));

		return classes;
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void handleDTOField(Object obj, Field field, Set<String> handledFields, Map result,
            InternalConverter ic) {
        String fn = Util.getDTOKey(field);
        if (fn == null)
            return;

        if (handledFields.contains(fn))
            return; // Field with this name was already handled

        try {
            Object fVal = field.get(obj);
            result.put(fn, fVal);
            handledFields.add(fn);
        } catch (Exception e) {
			// Ignore
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void handleBeanMethod(Object obj, Method md, Set<String> invokedMethods, Map res) {
        String bp = Util.getBeanKey(md);
        if (bp == null)
            return;

        if (invokedMethods.contains(bp))
            return; // method with this name already invoked

        try {
            res.put(bp, md.invoke(obj));
            invokedMethods.add(bp);
        } catch (Exception e) {
			// Ignore
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void handleInterfaceMethod(Object obj, Class<?> intf, Method md, Set<String> invokedMethods, Map res) {
        String mn = md.getName();
        if (invokedMethods.contains(mn))
            return; // method with this name already invoked

        String propName = Util.getInterfacePropertyName(md, Util.getSingleElementAnnotationKey(intf, obj), obj);
        if (propName == null)
            return;

        try {
            Object r = Util.getInterfaceProperty(obj, md);
            if (r == null)
                return;

            res.put(propName, r);
            invokedMethods.add(mn);
        } catch (Exception e) {
			// Ignore
        }
    }

    private Map<?,?> mapView(Object obj, Class<?> sourceCls, InternalConverter ic) {
        if (Map.class.isAssignableFrom(sourceCls) || (DTOUtil.isDTOType(sourceCls) && obj instanceof Map))
            return (Map<?,?>) obj;
        else if (Dictionary.class.isAssignableFrom(sourceCls))
            return MapDelegate.forDictionary((Dictionary<?,?>) object, this);
        else if (DTOUtil.isDTOType(sourceCls) || sourceAsDTO)
            return createMapFromDTO(obj, ic);
        else if (sourceAsJavaBean) {
            Map<?,?> m = createMapFromBeanAccessors(obj, sourceCls);
            if (m.size() > 0)
                return m;
        } else if (hasGetProperties(sourceCls)) {
            return getPropertiesDelegate(obj, sourceCls);
		}
		return createMapFromInterface(obj, sourceClass);
    }

    private boolean hasGetProperties(Class<?> cls) {
        try {
            Method m = cls.getDeclaredMethod("getProperties");
            if (m == null)
                m = cls.getMethod("getProperties");
            return m != null;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<?, ?> getPropertiesDelegate(Object obj, Class<?> cls) {
        try {
            Method m = cls.getDeclaredMethod("getProperties");
            if (m == null)
                m = cls.getMethod("getProperties");

            return converter.convert(m.invoke(obj)).to(Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static boolean isCopyRequiredType(Class<?> cls) {
        if (cls.isEnum())
            return false;
        return Map.class.isAssignableFrom(cls) ||
                Collection.class.isAssignableFrom(cls) ||
                DTOUtil.isDTOType(cls) ||
                cls.isArray();
    }

    private static boolean isWriteableJavaBean(Class<?> cls) {
        boolean hasNoArgCtor = false;
        for (Constructor<?> ctor : cls.getConstructors()) {
            if (ctor.getParameterTypes().length == 0)
                hasNoArgCtor = true;
        }
        if (!hasNoArgCtor)
            return false; // A JavaBean must have a public no-arg constructor

        return getSetters(cls).size() > 0;
    }

    private static Set<Method> getSetters(Class<?> cls) {
        Set<Method> setters = new HashSet<>();
        while (!Object.class.equals(cls)) {
            Set<Method> methods = new HashSet<>();
            methods.addAll(Arrays.asList(cls.getDeclaredMethods()));
            methods.addAll(Arrays.asList(cls.getMethods()));
            for (Method md : methods) {
                if (md.getParameterTypes().length != 1)
                    continue; // Only setters with a single argument
                String name = md.getName();
                if (name.length() < 4)
                    continue;
                if (name.startsWith("set") &&
                        Character.isUpperCase(name.charAt(3)))
                    setters.add(md);
            }
            cls = cls.getSuperclass();
        }
        return setters;
    }
}
