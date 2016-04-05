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
package org.apache.felix.converter.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Converting;
import org.osgi.service.converter.TypeReference;

public class ConvertingImpl implements Converting {
    private Converter converter;
    private final Object object;

    ConvertingImpl(Converter c, Object obj) {
        converter = c;
        object = obj;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T to(Class<T> cls) {
        if (String.class.equals(cls)) {
            if (object instanceof Object[])
                return (T) ((Object[])object)[0];
            return (T) object.toString();
        } else if (String[].class.equals(cls)) {
            String[] res = new String[1];
            res[0] = object.toString();
            return (T) res;
        }

        T res = tryStandardMethods(cls);

        if (res != null) {
            return res;
        } else {
            return null;
        }
    }

    @Override
    public <T> T to(TypeReference<T> ref) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object to(Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T tryStandardMethods(Class<T> cls) {
        try {
            Method m = cls.getDeclaredMethod("valueOf", String.class);
            if (m != null) {
                return (T) m.invoke(null, object);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
