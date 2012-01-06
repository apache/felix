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
package org.apache.felix.webconsole.internal.servlet;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.framework.BundleContext;

/**
 * A helper class to get configuration properties.
 */
public class ConfigurationUtil
{

    private ConfigurationUtil()
    {
        // prevent instantiation
    }


    /**
     * Returns the named property from the framework. If the property does
     * not exist, the default value <code>def</code> is returned.
     *
     * @param context The BundleContext providing framework properties
     * @param name The name of the property to return
     * @param def The default value if the named property does not exist
     * @return The value of the named property or <code>def</code>
     *         if the property does not exist
     */
    public static final String getProperty( BundleContext context, String name, String def )
    {
        String value = context.getProperty( name );
        return ( value == null ) ? def : value;
    }

    /**
     * Returns the named property from the framework. If the property does
     * not exist, the default value <code>def</code> is returned.
     *
     * @param context The BundleContext providing framework properties
     * @param name The name of the property to return
     * @param def The default value if the named property does not exist
     * @return The value of the named property as a string or <code>def</code>
     *         if the property does not exist
     */
    public static final int getProperty(BundleContext context, String name, int def)
    {
        String value = context.getProperty( name);
        if (value != null)
        {
            try
            {
                return Integer.parseInt(value.toString());
            }
            catch (NumberFormatException nfe)
            {
                // don't care
            }
        }

        // not a number, not convertible, not set, use default
        return def;
    }


    /**
     * Returns the named property from the configuration. If the property does
     * not exist, the default value <code>def</code> is returned.
     *
     * @param config The properties from which to returned the named one
     * @param name The name of the property to return
     * @param def The default value if the named property does not exist
     * @return The value of the named property as a string or <code>def</code>
     *         if the property does not exist
     */
    public static final String getProperty(Map config, String name, String def)
    {
        Object value = config.get(name);
        if (value instanceof String)
        {
            return (String) value;
        }

        if (value == null)
        {
            return def;
        }

        return String.valueOf(value);
    }

    /**
     * Returns the named property from the configuration. If the property does
     * not exist, the default value <code>def</code> is returned.
     *
     * @param config The properties from which to returned the named one
     * @param name The name of the property to return
     * @param def The default value if the named property does not exist
     * @return The value of the named property as a string or <code>def</code>
     *         if the property does not exist
     */
    public static final int getProperty(Map config, String name, int def)
    {
        Object value = config.get(name);
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }

        // try to convert the value to a number
        if (value != null)
        {
            try
            {
                return Integer.parseInt(value.toString());
            }
            catch (NumberFormatException nfe)
            {
                // don't care
            }
        }

        // not a number, not convertible, not set, use default
        return def;
    }

    /**
     * Gets a property as String[]
     *
     * @param config The properties from which to returned the named one
     * @param name The name of the property to return
     * @return the property value as string array - no matter if originally it was other kind of array, collection or comma-separated string. Returns <code>null</code> if the property is not set.
     */
    public static final String[] getStringArrayProperty(Map config, String name)
    {
        Object value = config.get(name);
        if (value == null)
        {
            return null;
        }

        String[] ret = null;
        if (value.getClass().isArray())
        {
            final Object[] names = (Object[]) value;
            ret = new String[names.length];
            for (int i = 0; i < names.length; i++)
            {
                ret[i] = String.valueOf(names[i]);
            }
        }
        else if (value instanceof Collection)
        {
            Collection collection = (Collection) value;
            ret = new String[collection.size()];
            int i = 0;
            for (Iterator iter = collection.iterator(); iter.hasNext();)
            {
                ret[i] = String.valueOf(iter.next());
                i++;
            }
        }
        else if (value instanceof String)
        {
            String pv = ((String) value).trim();
            if (pv.length() != 0)
            {
                StringTokenizer tok = new StringTokenizer(pv, ",;"); //$NON-NLS-1$
                ret = new String[tok.countTokens()];
                int i = 0;
                while (tok.hasMoreTokens())
                {
                    ret[i] = tok.nextToken();
                    i++;
                }
            }
        }
        return ret;
    }

}
