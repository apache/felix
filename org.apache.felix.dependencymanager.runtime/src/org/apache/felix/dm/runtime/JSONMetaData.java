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
package org.apache.felix.dm.runtime;

import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * This class represents the parsed data found from meta-inf dependencymanager descriptors.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class JSONMetaData implements MetaData, Cloneable
{
    /**
     * The parsed Dependency or Service metadata. The map value is either a String, a String[],
     * or a Dictionary, whose values are String or String[]. 
     */
    private HashMap<String, Object> m_metadata = new HashMap<String, Object>();

    /**
     * Decodes Json metadata for either a Service or a Dependency descriptor entry.
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
     * @param input the JSON string that corresponds to a dependency manager descriptor entry line.
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
    public JSONMetaData(String input) throws Exception
    {
    	JsonReader jreader = new JsonReader(input);
        Map<String, Object> m = jreader.getParsed();
        
        for (Map.Entry<String, Object> e : m.entrySet())
        {
        	String key = e.getKey();
        	Object value = e.getValue();
        	if (value instanceof String)
        	{
                m_metadata.put(key, value);
        	} else if (value instanceof List)
        	{
        		// String array
                m_metadata.put(key, decodeStringArray((List<?>) value));
        	} else if (value instanceof Map)
        	{
        		// properties
                m_metadata.put(key, parseProperties((Map<String, Object>) value));
        	}
        }
    }

    @SuppressWarnings("unchecked")
	private Hashtable<String, Object> parseProperties(Map<String, Object> properties) throws Exception {
        Hashtable<String, Object> parsedProps = new Hashtable<String, Object>();
        
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();            
            Object value = entry.getValue();
            
            if (value instanceof String)
            {
                // This property type is a simple string
            	parsedProps.put(key, value);
            }
            else if (value instanceof List)
            {
                // This property type is a simple string array
                parsedProps.put(key, decodeStringArray((List<?>) value));
            }
            else if (value instanceof Map)
            {
                // This property type is a typed value, encoded as a JSONObject with two keys: "type"/"value"
                Map<String, Object> json = ((Map<String, Object>) value);
                String type = json.get("type").toString();
                Object typeValue = json.get("value");

                if (type == null)
                {
                    throw new Exception("missing type attribute in json metadata for key " + key);
                }
                if (typeValue == null)
                {
                    throw new Exception("missing type value attribute in json metadata for key " + key);
                }

                Class<?> typeClass;
                try
                {
                    typeClass = Class.forName(type);
                }
                catch (ClassNotFoundException e)
                {
                    throw new Exception("invalid type attribute (" + type + ") in json metadata for key "
                        + key);
                }

                if (typeValue instanceof List)
                {
                    parsedProps.put(key, toPrimitiveTypeArray(typeClass, (List<?>) typeValue));
                }
                else 
                {
                    parsedProps.put(key, toPrimitiveType(typeClass, typeValue.toString()));
                }
            }
        }
        
        return parsedProps;
    }

    private Object toPrimitiveType(Class<?> type, String value) throws Exception {
        if (type.equals(String.class))
        {
            return value;
        }
        else if (type.equals(Long.class))
        {
            return Long.parseLong(value);
        }
        else if (type.equals(Double.class))
        {
            return Double.valueOf(value);
        }
        else if (type.equals(Float.class))
        {
            return Float.valueOf(value);
        }
        else if (type.equals(Integer.class))
        {
            return Integer.valueOf(value);
        }
        else if (type.equals(Byte.class))
        {
            return Byte.valueOf(value);
        }
        else if (type.equals(Character.class))
        {
            return Character.valueOf((char) Integer.parseInt(value));
        }
        else if (type.equals(Boolean.class))
        {
            return Boolean.valueOf(value);
        }
        else if (type.equals(Short.class))
        {
            return Short.valueOf(value);
        }
        else
        {
            throw new Exception("invalid type (" + type + ") attribute in json metadata");
        }
    }

    private Object toPrimitiveTypeArray(Class<?> type, List<?> array) throws Exception {
        int len = array.size();
        Object result = Array.newInstance(type, len);

        if (type.equals(String.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i, array.get(i).toString());
            }
        } 
        else if (type.equals(Long.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i, Long.valueOf(array.get(i).toString()));
            }
        }
        else if (type.equals(Double.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i, Double.valueOf(array.get(i).toString()));
            }
        } 
        else if (type.equals(Float.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i, Float.valueOf(array.get(i).toString()));
            }
        }
        else if (type.equals(Integer.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i, Integer.valueOf(array.get(i).toString()));
            }
        }
        else if (type.equals(Byte.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i, Byte.valueOf(array.get(i).toString()));
            }
        }
        else if (type.equals(Character.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i,  Character.valueOf((char) Integer.parseInt(array.get(i).toString())));
            }
        }
        else if (type.equals(Boolean.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i, Boolean.valueOf(array.get(i).toString()));
            }
        } 
        else if (type.equals(Short.class))
        {
            for (int i = 0; i < len; i ++) {
                Array.set(result, i, Short.valueOf(array.get(i).toString()));
            }
        }
        else 
        {
            throw new Exception("invalid type (" + type + ") attribute in json metadata");
        }   
        return result;
    }

    /**
     * Close this class instance to another one.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        JSONMetaData clone = (JSONMetaData) super.clone();
        clone.m_metadata = (HashMap<String, Object>) m_metadata.clone();
        return clone;
    }
    
    public String getString(Params key)
    {
        Object value = m_metadata.get(key.toString());
        if (value == null)
        {
            throw new IllegalArgumentException("Parameter " + key + " not found");
        }
        return value.toString();
    }

    public String getString(Params key, String def)
    {
        try
        {
            return getString(key);
        }
        catch (IllegalArgumentException e)
        {
            return def;
        }
    }

    public int getInt(Params key)
    {
        Object value = m_metadata.get(key.toString());
        if (value != null)
        {
            try
            {
                if (value instanceof Integer) {
                    return ((Integer) value).intValue();
                }
                return Integer.parseInt(value.toString());
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("parameter " + key
                    + " is not an int value: "
                    + value);
            }
        }
        else
        {
            throw new IllegalArgumentException("missing " + key
                + " parameter from annotation");
        }
    }

    public int getInt(Params key, int def)
    {
        Object value = m_metadata.get(key.toString());
        if (value != null)
        {
            try
            {
                if (value instanceof Integer) {
                    return ((Integer) value).intValue();
                }
                return Integer.parseInt(value.toString());
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("parameter " + key
                    + " is not an int value: "
                    + value);
            }
        }
        else
        {
            return def;
        }
    }

    public long getLong(Params key)
    {
        Object value = m_metadata.get(key.toString());
        if (value != null)
        {
            try
            {
                if (value instanceof Long) {
                    return ((Long) value).longValue();
                }
                return Long.parseLong(value.toString());
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("parameter " + key
                    + " is not a long value: "
                    + value);
            }
        }
        else
        {
            throw new IllegalArgumentException("missing " + key
                + " parameter from annotation");
        }
    }

    public long getLong(Params key, long def)
    {
        Object value = m_metadata.get(key.toString());
        if (value != null)
        {
            try
            {
                if (value instanceof Long) {
                    return (Long) value;
                }
                return Long.parseLong(value.toString());
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("parameter " + key
                    + " is not a long value: "
                    + value);
            }
        }
        else
        {
            return def;
        }
    }

    public String[] getStrings(Params key)
    {
        Object array = m_metadata.get(key.toString());
        if (array == null)
        {
            throw new IllegalArgumentException("Parameter " + key + " not found");
        }

        if (!(array instanceof String[]))
        {
            throw new IllegalArgumentException("Parameter " + key + " is not a String[] (" + array.getClass()
                + ")");
        }
        return (String[]) array;
    }

    public String[] getStrings(Params key, String[] def)
    {
        try
        {
            return getStrings(key);
        }
        catch (IllegalArgumentException t)
        {
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    public Dictionary<String, Object> getDictionary(Params key,
        Dictionary<String, Object> def)
    {
        Object dictionary = m_metadata.get(key.toString());
        if (dictionary == null)
        {
            return def;
        }

        if (!(dictionary instanceof Dictionary<?, ?>))
        {
            throw new IllegalArgumentException("Parameter " + key + " is not a Dictionary ("
                + dictionary.getClass() + ")");
        }

        return (Dictionary<String, Object>) dictionary;
    }

    @Override
    public String toString()
    {
        return m_metadata.toString();
    }

    public void setDictionary(Params key, Dictionary<String, Object> dictionary)
    {
        m_metadata.put(key.toString(), dictionary);
    }

    public void setString(Params key, String value)
    {
        m_metadata.put(key.toString(), value);
    }

    public void setStrings(Params key, String[] values)
    {
        m_metadata.put(key.toString(), values);
    }
    
    /**
     * Decodes a JSONArray into a String array (all JSON array values are supposed to be strings).
     */
    private String[] decodeStringArray(List<?> array)
    {
    	return array.stream().map(x -> x.toString()).toArray(String[]::new);
    }
}
