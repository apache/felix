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
package org.apache.felix.framework.util;

import java.util.Map;
import java.util.TreeMap;

/**
 * Simple utility class that creates a map for string-based keys.
 * This map can be set to use case-sensitive or case-insensitive
 * comparison when searching for the key.  Any keys put into this
 * map will be converted to a <tt>String</tt> using the
 * <tt>toString()</tt> method, since it is only intended to
 * compare strings.
 **/
public class StringMap extends TreeMap<String, Object>
{

    public StringMap()
    {
        super(StringComparator.COMPARATOR);
    }

    public StringMap(Map<?, ?> map)
    {
        this();
        for (Map.Entry<?, ?> e : map.entrySet())
        {
            put(e.getKey().toString(), e.getValue());
        }
    }

}
