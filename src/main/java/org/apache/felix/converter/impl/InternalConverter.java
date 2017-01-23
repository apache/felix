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

import java.util.Map;

import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.TypeReference;

public interface InternalConverter extends Converter {
    public InternalConverting convert(Object obj);

    @Override
    default public boolean equals(Object o1, Object o2) {
        try {
            Map<String, Object> m1 = convert(o1).to(new TypeReference<Map<String,Object>>(){});
            Map<String, Object> m2 = convert(o2).to(new TypeReference<Map<String,Object>>(){});

            if (m1.size() != m2.size())
                return false;

            for (Map.Entry<String, Object> entry : m1.entrySet()) {
                Object val = m2.get(entry.getKey());
                if (!equals(entry.getValue(), val))
                    return false;
            }
            return true;
        } catch (ConversionException e) {
            // do lists as well

            // It's a scalar - compare via strings
            String s1 = convert(o1).to(String.class);
            String s2 = convert(o2).to(String.class);
            return s1.equals(s2);
        }
    }
}
