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
package org.apache.felix.scr.impl.helper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;

public class Annotations
{
    
    static public <T> T toObject(Class<T> clazz, Map<String, Object> props, Bundle b )
    {     
        Map<String, Object> m = new HashMap<String, Object>();
        
        Method[] methods = clazz.getDeclaredMethods();
        for ( Method method: methods )
        {
            String name = method.getName();
            name = fixup(name);
            Object raw = props.get(name);
            Object cooked = Coercions.coerce( method.getReturnType(), raw, b );
            m.put( name, cooked );
        }
        
        InvocationHandler h = new Handler(m);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, h);
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

    private static class Handler implements InvocationHandler 
    {
        
        private final Map<String, Object> values;
       
        public Handler(Map<String, Object> values)
        {
            this.values = values;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            return values.get(method.getName());
        }
        
    }

}
