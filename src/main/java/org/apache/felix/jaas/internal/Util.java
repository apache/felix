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

package org.apache.felix.jaas.internal;

import java.util.*;


final class Util
{

    public static Set<String> parseHeader(String header)
    {
        //Could have used Sling commons ManifestHeader.parse
        //but our requirement are simple

        header = trimToNull(header);
        if (header == null)
        {
            return new HashSet<String>();
        }

        String[] splits = header.split(",");
        Set<String> values = new HashSet<String>();
        for (String s : splits)
        {
            s = trimToNull(s);
            if (s != null)
            {
                values.add(s);
            }
        }
        return values;
    }

    //Instead of adding dependency on commons StringUtil we copy the used method below

    public static String trimToNull(String str)
    {
        String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    private static String trim(String str)
    {
        return str == null ? null : str.trim();
    }

    public static boolean isEmpty(String str)
    {
        return str == null || str.length() == 0;
    }

    //----------------Methods taken from org.apache.sling.commons.osgi.PropertiesUtil

    //These are required to safely access properties from ConfigurationAdmin

    /**
     * Returns the parameter as a string or the
     * <code>defaultValue</code> if the parameter is <code>null</code>.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default string value
     */
    public static String toString(Object propValue, String defaultValue)
    {
        propValue = toObject(propValue);
        return (propValue != null) ? propValue.toString() : defaultValue;
    }

    /**
     * Returns the parameter as an integer or the
     * <code>defaultValue</code> if the parameter is <code>null</code> or if
     * the parameter is not an <code>Integer</code> and cannot be converted to
     * an <code>Integer</code> from the parameter's string value.
     * @param propValue the property value or <code>null</code>
     * @param defaultValue the default integer value
     */
    public static int toInteger(Object propValue, int defaultValue)
    {
        propValue = toObject(propValue);
        if (propValue instanceof Integer)
        {
            return (Integer) propValue;
        }
        else if (propValue != null)
        {
            try
            {
                return Integer.valueOf(String.valueOf(propValue));
            }
            catch (NumberFormatException nfe)
            {
                // don't care, fall through to default value
            }
        }

        return defaultValue;
    }

    /**
     * Returns the parameter as a single value. If the
     * parameter is neither an array nor a <code>java.util.Collection</code> the
     * parameter is returned unmodified. If the parameter is a non-empty array,
     * the first array element is returned. If the property is a non-empty
     * <code>java.util.Collection</code>, the first collection element is returned.
     * Otherwise <code>null</code> is returned.
     * @param propValue the parameter to convert.
     */
    private static Object toObject(Object propValue)
    {
        if (propValue == null)
        {
            return null;
        }
        else if (propValue.getClass().isArray())
        {
            Object[] prop = (Object[]) propValue;
            return prop.length > 0 ? prop[0] : null;
        }
        else if (propValue instanceof Collection<?>)
        {
            Collection<?> prop = (Collection<?>) propValue;
            return prop.isEmpty() ? null : prop.iterator().next();
        }
        else
        {
            return propValue;
        }
    }

    /**
     * Returns the parameter as an array of Strings. If
     * the parameter is a scalar value its string value is returned as a single
     * element array. If the parameter is an array, the elements are converted to
     * String objects and returned as an array. If the parameter is a collection, the
     * collection elements are converted to String objects and returned as an array.
     * Otherwise (if the property is <code>null</code>) a provided default value is
     * returned.
     * @param propValue The object to convert.
     * @param defaultArray The default array to return.
     */
    public static String[] toStringArray(Object propValue, String[] defaultArray)
    {
        if (propValue == null)
        {
            // no value at all
            return defaultArray;

        }
        else if (propValue instanceof String)
        {
            // single string
            return new String[] { (String) propValue };

        }
        else if (propValue instanceof String[])
        {
            // String[]
            return (String[]) propValue;

        }
        else if (propValue.getClass().isArray())
        {
            // other array
            Object[] valueArray = (Object[]) propValue;
            List<String> values = new ArrayList<String>(valueArray.length);
            for (Object value : valueArray)
            {
                if (value != null)
                {
                    values.add(value.toString());
                }
            }
            return values.toArray(new String[values.size()]);

        }
        else if (propValue instanceof Collection<?>)
        {
            // collection
            Collection<?> valueCollection = (Collection<?>) propValue;
            List<String> valueList = new ArrayList<String>(valueCollection.size());
            for (Object value : valueCollection)
            {
                if (value != null)
                {
                    valueList.add(value.toString());
                }
            }
            return valueList.toArray(new String[valueList.size()]);
        }

        return defaultArray;
    }
}
