/*
 * Copyright (c) OSGi Alliance (2017). All Rights Reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author $Id$
 */
class Util {
	private static final Map<Class< ? >,Class< ? >> boxedClasses;
	static {
		Map<Class< ? >,Class< ? >> m = new HashMap<>();
		m.put(int.class, Integer.class);
		m.put(long.class, Long.class);
		m.put(double.class, Double.class);
		m.put(float.class, Float.class);
		m.put(boolean.class, Boolean.class);
		m.put(char.class, Character.class);
		m.put(byte.class, Byte.class);
		m.put(void.class, Void.class);
		m.put(short.class, Short.class);
		boxedClasses = Collections.unmodifiableMap(m);
	}

	private Util() {} // prevent instantiation

	static Type primitiveToBoxed(Type type) {
		if (type instanceof Class)
			return primitiveToBoxed((Class< ? >) type);
		else
			return null;
	}

	static Type baseType(Type type) {
		if (type instanceof Class)
			return primitiveToBoxed((Class< ? >) type);
		else if (type instanceof ParameterizedType)
			return type;
		else
			return null;
	}

	static Class< ? > primitiveToBoxed(Class< ? > cls) {
		Class< ? > boxed = boxedClasses.get(cls);
		if (boxed != null)
			return boxed;
		else
			return cls;
	}

	static Map<String,Method> getBeanKeys(Class< ? > beanClass) {
		Map<String,Method> keys = new LinkedHashMap<>();
		// Bean methods must be public and can be on parent classes
		for (Method md : beanClass.getMethods()) {
			String key = getBeanKey(md);
			if (key != null && !keys.containsKey(key)) {
				keys.put(key, md);
			}
		}
		return keys;
	}

	static String getBeanKey(Method md) {
		if (Modifier.isStatic(md.getModifiers()))
			return null;

		if (!Modifier.isPublic(md.getModifiers()))
			return null;

		return getBeanAccessorPropertyName(md);
	}

	private static String getBeanAccessorPropertyName(Method md) {
		if (md.getReturnType().equals(Void.class))
			return null; // not an accessor

		if (md.getParameterTypes().length > 0)
			return null; // not an accessor

		if (Object.class.equals(md.getDeclaringClass()))
			return null; // do not use any methods on the Object class as a
							// accessor

		String mn = md.getName();
		int prefix;
		if (mn.startsWith("get"))
			prefix = 3;
		else if (mn.startsWith("is"))
			prefix = 2;
		else
			return null; // not an accessor prefix

		if (mn.length() <= prefix)
			return null; // just 'get' or 'is': not an accessor
		String propStr = mn.substring(prefix);
		StringBuilder propName = new StringBuilder(propStr.length());
		char firstChar = propStr.charAt(0);
		if (!Character.isUpperCase(firstChar))
			return null; // no acccessor as no camel casing
		propName.append(Character.toLowerCase(firstChar));
		if (propStr.length() > 1)
			propName.append(propStr.substring(1));

		return unMangleName(getPrefix(md.getDeclaringClass()),
				propName.toString());
	}

	static Map<String,Field> getDTOKeys(Class< ? > dto) {
		Map<String,Field> keys = new LinkedHashMap<>();

		for (Field f : dto.getFields()) {
			String key = getDTOKey(f);
			if (key != null && !keys.containsKey(key))
				keys.put(key, f);
		}
		return keys;
	}

	static String getDTOKey(Field f) {
		if (Modifier.isStatic(f.getModifiers()))
			return null;

		if (!Modifier.isPublic(f.getModifiers()))
			return null;

		return unMangleName(getPrefix(f.getDeclaringClass()), f.getName());
	}

	static Map<String,Set<Method>> getInterfaceKeys(Class< ? > intf,
			Object object) {
		Map<String,Set<Method>> keys = new LinkedHashMap<>();

		String seank = getSingleElementAnnotationKey(intf, object);
		for (Method md : intf.getMethods()) {
			String name = getInterfacePropertyName(md, seank, object);
			if (name != null) {
				Set<Method> set = keys.get(name);
				if (set == null) {
					set = new LinkedHashSet<>();
					keys.put(name, set);
				}
				md.setAccessible(true);
				set.add(md);
			}
		}

		for (Iterator<Entry<String,Set<Method>>> it = keys.entrySet()
				.iterator(); it.hasNext();) {
			Entry<String,Set<Method>> entry = it.next();
			boolean zeroArgFound = false;
			for (Method md : entry.getValue()) {
				if (md.getParameterTypes().length == 0) {
					// OK found the zero-arg param
					zeroArgFound = true;
					break;
				}
			}
			if (!zeroArgFound)
				it.remove();
		}
		return keys;
	}

	static String getMarkerAnnotationKey(Class< ? > intf, Object obj) {
		Class< ? > ann = getAnnotationType(intf, obj);
		return getPrefix(intf) + toSingleElementAnnotationKey(ann.getSimpleName());
	}

	static String getSingleElementAnnotationKey(Class< ? > intf, Object obj) {
		Class< ? > ann = getAnnotationType(intf, obj);
		if (ann == null)
			return null;

		boolean valueFound = false;
		// All annotation methods must be public
		for (Method md : ann.getMethods()) {
			if(md.getDeclaringClass() != ann) {
				// Ignore Object methods and Annotation methods
				continue;
			}
			
			if ("value".equals(md.getName())) {
				valueFound = true;
				continue;
			}

			if (md.getDefaultValue() == null) {
				// All elements bar value must have a default
				return null;
			}
		}

		if (!valueFound) {
			// Single Element Annotation must have a value element.
			return null;
		}

		return getPrefix(ann) + toSingleElementAnnotationKey(ann.getSimpleName());
	}

	static Class< ? > getAnnotationType(Class< ? > intf, Object obj) {
		try {
			Method md = intf.getMethod("annotationType");
			Object res = md.invoke(obj);
			if (res instanceof Class)
				return (Class< ? >) res;
		} catch (Exception e) {
			// Ignore exception
		}
		return null;
	}

	static String toSingleElementAnnotationKey(String simpleName) {
		StringBuilder sb = new StringBuilder();

		boolean capitalSeen = true;
		for (char c : simpleName.toCharArray()) {
			if (!capitalSeen) {
				if (Character.isUpperCase(c)) {
					capitalSeen = true;
					sb.append('.');
				}
			} else {
				if (Character.isLowerCase(c)) {
					capitalSeen = false;
				}
			}
			sb.append(Character.toLowerCase(c));
		}

		return sb.toString();
	}

	static String getInterfacePropertyName(Method md,
			String singleElementAnnotationKey, Object object) {
		if (md.getReturnType().equals(Void.class))
			return null; // not an accessor

		if (md.getParameterTypes().length > 1)
			return null; // not an accessor

		if ("value".equals(md.getName()) && md.getParameterTypes().length == 0
				&& singleElementAnnotationKey != null)
			return singleElementAnnotationKey;

		if (Object.class.equals(md.getDeclaringClass())
				|| Annotation.class.equals(md.getDeclaringClass()))
			return null; // do not use any methods on the Object or Annotation
							// class as a accessor

		if ("annotationType".equals(md.getName())
				&& md.getParameterTypes().length == 0) {
			try {
				Object cls = md.invoke(object);
				if (cls instanceof Class && ((Class< ? >) cls).isAnnotation())
					return null;
			} catch (Exception e) {
				// Ignore exception
			}
		}

		if (md.getDeclaringClass().getSimpleName().startsWith("$Proxy")) {
			// TODO is there a better way to do this?
			if (isInheritedMethodInProxy(md, Object.class)
					|| isInheritedMethodInProxy(md, Annotation.class))
				return null;
		}

		return unMangleName(getPrefix(md.getDeclaringClass()), md.getName());
	}

	private static boolean isInheritedMethodInProxy(Method md, Class< ? > cls) {
		for (Method om : cls.getMethods()) {
			if (om.getName().equals(md.getName()) && Arrays
					.equals(om.getParameterTypes(), md.getParameterTypes())) {
				return true;
			}
		}
		return false;
	}

	static Object getInterfaceProperty(Object obj, Method md)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		if (Modifier.isStatic(md.getModifiers()))
			return null;

		if (md.getParameterTypes().length > 0)
			return null;

		return md.invoke(obj);
	}

	static String getPrefix(Class< ? > cls) {
		try {
			// We can use getField as the PREFIX must be public (see spec erratum)
			Field prefixField = cls.getField("PREFIX_");
			if (prefixField.getDeclaringClass() == cls &&
					prefixField.getType().equals(String.class)) {
				int modifiers = prefixField.getModifiers();
				// We need to be final *and* static
				if (Modifier.isFinal(modifiers) &&
					Modifier.isStatic(modifiers)) {
					
					if(!prefixField.isAccessible()) {
						// Should we log that we have to do this?
						prefixField.setAccessible(true);
					}
					
					return (String) prefixField.get(null);
				}
			}
		} catch (Exception ex) {
			// LOG no prefix field
		}

		if (!cls.isInterface()) {
			for (Class< ? > intf : cls.getInterfaces()) {
				String prefix = getPrefix(intf);
				if (prefix.length() > 0)
					return prefix;
			}
		}

		return "";
	}

	static String mangleName(String prefix, String key, List<String> names) {
		if (!key.startsWith(prefix))
			return null;

		key = key.substring(prefix.length());

		// Do a reverse search because some characters get removed as part of
		// the mangling
		for (String name : names) {
			if (key.equals(unMangleName(name)))
				return name;
		}

		// Fallback if not found in the list - TODO maybe this can be removed.
		String res = key.replace("_", "__");
		res = res.replace("$", "$$");
		res = res.replace("-", "$_$");
		res = res.replaceAll("[.]([._])", "_\\$$1");
		res = res.replace('.', '_');
		return res;
	}

	static String unMangleName(String prefix, String key) {
		return prefix + unMangleName(key);
	}

	static String unMangleName(String id) {
		char[] array = id.toCharArray();
		int out = 0;

		boolean changed = false;
		for (int i = 0; i < array.length; i++) {
			if (match("$$", array, i) || match("__", array, i)) {
				array[out++] = array[i++];
				changed = true;
			} else if (match("$_$", array, i)) {
				array[out++] = '-';
				i += 2;
			} else {
				char c = array[i];
				if (c == '_') {
					array[out++] = '.';
					changed = true;
				} else if (c == '$') {
					changed = true;
				} else {
					array[out++] = c;
				}
			}
		}
		if (id.length() != out || changed)
			return new String(array, 0, out);

		return id;
	}

	private static boolean match(String pattern, char[] array, int i) {
		for (int j = 0; j < pattern.length(); j++, i++) {
			if (i >= array.length)
				return false;

			if (pattern.charAt(j) != array[i])
				return false;
		}
		return true;
	}
}
