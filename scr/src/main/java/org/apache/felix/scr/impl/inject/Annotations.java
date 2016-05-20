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
package org.apache.felix.scr.impl.inject;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.impl.helper.Coercions;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentException;

public class Annotations
{
    
    static public <T> T toObject(Class<T> clazz, Map<String, Object> props, Bundle b, boolean supportsInterfaces )
    {     
        Map<String, Object> m = new HashMap<String, Object>();
        
        Method[] methods = clazz.getMethods();
        Map<String, Method> complexFields = new HashMap<String, Method>();
        for ( Method method: methods )
        {
            String name = method.getName();
            String key = fixup(name);
            Object raw = props.get(key);
            Class<?> returnType = method.getReturnType();
            Object cooked;
            if ( returnType.isInterface() || returnType.isAnnotation())
            {
                complexFields.put(key, method);
                continue;
            }
            try
            {
                if (returnType.isArray())
                {
                    Class<?> componentType = returnType.getComponentType();
                    if (componentType.isInterface() || componentType.isAnnotation())
                    {
                        complexFields.put(key, method);
                        continue;
                    }
                    cooked = coerceToArray(componentType, raw, b);
                }
                else
                {
                    cooked = Coercions.coerce(returnType, raw, b);
                }
            }
            catch (ComponentException e)
            {
                cooked = new Invalid(e);
            }
            m.put( name, cooked );
        }
        if (!complexFields.isEmpty())
        { 
            if (supportsInterfaces )
            {
                Map<String, List<Map<String, Object>>> nested = extractSubMaps(complexFields.keySet(), props);
                for (Map.Entry<String, Method> entry: complexFields.entrySet())
                {
                    List<Map<String, Object>> proplist = nested.get(entry.getKey());
                    if (proplist == null)
                    {
                    	proplist = Collections.emptyList();
                    }
                    Method method = entry.getValue();
                    Class<?> returnType  = method.getReturnType();
                    if (returnType.isArray())
                    {
                        Class<?> componentType = returnType.getComponentType();
                        Object result = Array.newInstance(componentType, proplist.size());
                        for (int i = 0; i < proplist.size(); i++)
                        {
                            Map<String, Object> rawElement = proplist.get(i);
                            Object cooked = toObject(componentType, rawElement, b, supportsInterfaces);
                            Array.set(result, i, cooked);
                        }
                        m.put(method.getName(), result);
                    }
                    else
                    {
                        if (!proplist.isEmpty())
                        {
                            Object cooked = toObject(returnType, proplist.get(0), b, supportsInterfaces);
                            m.put(method.getName(), cooked);
                        }
                    }
                }
            }
            else
            {
                for (Method method: complexFields.values())
                {
                    m.put(method.getName(), new Invalid("Invalid annotation member type" + method.getReturnType().getName() + " for member: " + method.getName()));
                }
            }
        }
        
        InvocationHandler h = new Handler(m);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, h);
    }
    
    private static Map<String, List<Map<String, Object>>> extractSubMaps(Collection<String> keys, Map<String, Object> map) 
    {
        Map<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>();
        //Form a regexp to recognize all the keys as prefixes in the map keys.
        StringBuilder b = new StringBuilder("(");
        for (String key: keys)
        {
            b.append(key).append("|");
        }
        b.deleteCharAt(b.length() -1);
        b.append(")\\.([0-9]*)\\.(.*)");
        Pattern p = Pattern.compile(b.toString());
        for (Map.Entry<String, Object> entry: map.entrySet())
        {
            String longKey = entry.getKey();
            Matcher m = p.matcher(longKey);
            if (m.matches())
            {
                String key = m.group(1);
                int index = Integer.parseInt(m.group(2));
                String subkey = m.group(3);
                List<Map<String, Object>> subMapsForKey = result.get(key);
                if (subMapsForKey == null)
                {
                    subMapsForKey = new ArrayList<Map<String, Object>>();
                    result.put(key, subMapsForKey);
                }
                //make sure there is room for the possible new submap
                for (int i = subMapsForKey.size(); i <= index; i++)
                {
                    subMapsForKey.add(new HashMap<String, Object>());                    
                }
                Map<String, Object> subMap = subMapsForKey.get(index);
                subMap.put(subkey, entry.getValue());
            }
        }
        return result;
    }

    private static Object coerceToArray(Class<?> componentType, Object raw, Bundle bundle)
    {
        if (raw == null)
        {
            return null;
        }
        if (raw.getClass().isArray())
        {
            int size = Array.getLength(raw);
            Object result = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++)
            {
                Object rawElement = Array.get(raw, i);
                Object cooked = Coercions.coerce(componentType, rawElement, bundle);
                Array.set(result, i, cooked);
            }
            return result;
        }
        if (raw instanceof Collection)
        {
            Collection raws = (Collection) raw;
            int size = raws.size();
            Object result = Array.newInstance(componentType, size);
            int i = 0;
            for (Object rawElement: raws)
            {
                Object cooked = Coercions.coerce(componentType, rawElement, bundle);
                Array.set(result, i++, cooked);
            }
            return result;

        }
        Object cooked = Coercions.coerce(componentType, raw, bundle);
        Object result = Array.newInstance(componentType, 1);
        Array.set(result, 0, cooked);
        return result;

    }
    
    private static final Pattern p = Pattern.compile("(\\$\\$)|(\\$)|(__)|(_)");
    
    static String fixup(String name)
    {
        Matcher m = p.matcher(name);
        StringBuffer b = new StringBuffer();
        while (m.find())
        {
            String replacement = "";//null;
            if (m.group(1) != null) replacement = "\\$";
            if (m.group(2) != null) replacement = "";
            if (m.group(3) != null) replacement = "_";
            if (m.group(4) != null) replacement = ".";
            
            m.appendReplacement(b, replacement);
        }
        m.appendTail(b);
        return b.toString();
    }

    private final static class Handler implements InvocationHandler 
    {
        private final Map<String, Object> values;
       
        public Handler(Map<String, Object> values)
        {
            this.values = values;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            Object value = values.get(method.getName());
            if (value instanceof Invalid)
            {
                throw new ComponentException(((Invalid)value).getMessage());
            }
            return value;
        }
        
    }
    
    private final static class Invalid 
    {
        private final String message;
        
        public Invalid(ComponentException e)
        {
            this.message = e.getMessage();
        }
        
        public Invalid(String message)
        {
            this.message = message;
        }

        public String getMessage()
        {
            return message;
        }
    }

}
