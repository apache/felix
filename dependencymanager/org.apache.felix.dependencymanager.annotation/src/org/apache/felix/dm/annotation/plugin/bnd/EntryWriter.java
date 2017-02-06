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
package org.apache.felix.dm.annotation.plugin.bnd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.osgi.Annotation;

/**
 * This class encodes a component descriptor entry line, using json.
 * We are using a slightly adapted version of the nice JsonSerializingImpl class from the Apache Felix Converter project.
 * 
 * Internally, we store parameters in a map. The format of key/values stored in the map is the following:
 * 
 * The JSON object has the following form:
 * 
 * entry            ::= String | String[] | dictionary
 * dictionary       ::= key-value-pair*
 * key-value-pair   ::= key value
 * value            ::= String | String[] | value-type
 * value-type       ::= jsonObject with value-type-info
 * value-type-info  ::= "type"=primitive java type
 *                      "value"=String|String[]
 *                      
 * Exemple:
 * 
 * {"string-param" : "string-value",
 *  "string-array-param" : ["string1", "string2"],
 *  "properties" : {
 *      "string-param" : "string-value",
 *      "string-array-param" : ["str1", "str2],
 *      "long-param" : {"type":"java.lang.Long", "value":"1"}}
 *      "long-array-param" : {"type":"java.lang.Long", "value":["1"]}}
 *  }
 * }
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EntryWriter
{
    /**
     * Every descriptor entries contains a type parameter for identifying the kind of entry
     */
    private final static String TYPE = "type";

    /** 
     * All parameters as stored in a map object 
     */
    private final HashMap<String, Object> m_params = new HashMap<>();

    /** The entry type */
    private final EntryType m_type;
    
    /**
     * Makes a new component descriptor entry.
     */
    public EntryWriter(EntryType type)
    {
        m_type = type;
        m_params.put("type", type.toString());
    }

    /**
     * Returns this entry type.
     */
    EntryType getEntryType()
    {
        return m_type;
    }

    /**
     * Returns a string representation for the given component descriptor entry.
     * @param m_logger 
     */
    public String toString()
    {
    	return new JsonWriter(m_params).toString();
    }

    /**
     * Adds a String parameter in this descritor entry.
     */
    public void put(EntryParam param, String value)
    {
        checkType(param.toString());
        m_params.put(param.toString(), value);
    }    

    /**
     * Adds a String[] parameter in this descriptor entry.
     */
    public void put(EntryParam param, String[] values)
    {
        checkType(param.toString());
        m_params.put(param.toString(), Arrays.asList(values));
    }

    /**
     * Adds a property in this descriptor entry.
     */
    @SuppressWarnings("unchecked")
    public void addProperty(String name, Object value, Class<?> type)
    {
		Map<String, Object> properties = (Map<String, Object>) m_params.get(EntryParam.properties.toString());
		if (properties == null) {
			properties = new HashMap<>();
			m_params.put(EntryParam.properties.toString(), properties);
		}
        if (value.getClass().isArray())
        {
            Object[] array = (Object[]) value;
            if (array.length == 1)
            {
                value = array[0];
            }
        }

        if (type.equals(String.class))
        {
        	properties.put(name, value.getClass().isArray() ? Arrays.asList((Object[]) value) : value);
        }
        else
        {
           Map<String, Object> val = new HashMap<>();
           val.put("type", type.getName());
           val.put("value", value.getClass().isArray() ? Arrays.asList((Object[]) value) : value);
           properties.put(name, val);
        }
    }
    
    /**
     * Get a String attribute value from an annotation and write it into this descriptor entry.
     */
    public String putString(Annotation annotation, EntryParam param, String def)
    {
        checkType(param.toString());
        Object value = annotation.get(param.toString());
        if (value == null && def != null)
        {
            value = def;
        }
        if (value != null)
        {
            put(param, value.toString());
        }
        return value == null ? null : value.toString();
    }

    /**
     * Get a class attribute value from an annotation and write it into this descriptor entry.
     */
    public void putClass(Annotation annotation, EntryParam param)
    {
        checkType(param.toString());
        String value = AnnotationCollector.parseClassAttrValue(annotation.get(param.toString()));
        if (value != null)
        {
            put(param, value);
        }
    }

    /**
     * Get a class array attribute value from an annotation and write it into this descriptor entry.
     *
     * @param annotation the annotation containing an array of classes
     * @param param the attribute name corresponding to an array of classes
     * @param def the default array of classes (String[]), if the attribute is not defined in the annotation
     * @return the class array size.
     */
    public int putClassArray(Annotation annotation, EntryParam param, Object def, Set<String> collect)
    {
        checkType(param.toString());

        boolean usingDefault = false;
        Object value = annotation.get(param.toString());
        if (value == null && def != null)
        {
            value = def;
            usingDefault = true;
        }
        if (value != null)
        {
            if (!(value instanceof Object[]))
            {
                throw new IllegalArgumentException("annotation parameter " + param
                    + " has not a class array type");
            }

            List<String> classes = new ArrayList<>();
            for (Object v: ((Object[]) value))
            {
                if (! usingDefault)
                {
                	// Parse the annotation attribute value.
                    v = AnnotationCollector.parseClassAttrValue(v);
                }
                classes.add(v.toString());
                collect.add(v.toString());
            }
            
            m_params.put(param.toString(), classes);
            return ((Object[]) value).length;
        }
        return 0;
    }

    /**
     * Check if the written key is not equals to "type" ("type" is an internal attribute we are using
     * in order to identify a kind of descriptor entry (Service, ServiceDependency, etc ...).
     */
    private void checkType(String key)
    {
        if (TYPE.equals(key))
        {
            throw new IllegalArgumentException("\"" + TYPE + "\" parameter can't be overriden");
        }
    }
}
