/*
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
package org.apache.felix.bundlerepository.impl;

import java.util.List;
import java.util.Map;

import org.apache.felix.bundlerepository.Property;
import org.osgi.framework.Version;

class FelixPropertyAdapter implements Property
{
    private final String name;
    private final Object value;

    public FelixPropertyAdapter(String name, Object value)
    {
        if (name == null)
            throw new NullPointerException("Missing required parameter: name");
        if (value == null)
            throw new NullPointerException("Missing required parameter: value");
        this.name = name;
        this.value = value;
    }

    public FelixPropertyAdapter(Map.Entry<String, Object> entry)
    {
        this(entry.getKey(), entry.getValue());
    }

    public Object getConvertedValue()
    {
        return value;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        if (value instanceof Version)
            return Property.VERSION;
        if (value instanceof Long)
            return Property.LONG;
        if (value instanceof Double)
            return Property.DOUBLE;
        if (value instanceof List<?>)
            return Property.SET;
        return null;
    }

    public String getValue()
    {
        return String.valueOf(value);
    }
}
