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
package org.apache.felix.metatype;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class OptionalAttributes
{
    public static final String DEFAULT_NAMESPACE = "";

    private Map<String, Map<String, String>> namespacedAttributes = new HashMap<String, Map<String,String>>();


    public void addOptionalAttribute( String name, String value )
    {
        addOptionalAttribute(name, value, DEFAULT_NAMESPACE);
    }


    public void addOptionalAttribute( String name, String value, String namespace )
    {
        if (namespace == null) {
            namespace = DEFAULT_NAMESPACE;
        }
        Map<String, String> optionalAttributes = namespacedAttributes.get(namespace);
        if ( optionalAttributes == null )
        {
            optionalAttributes = new HashMap<String, String>();
            namespacedAttributes.put(namespace, optionalAttributes);
        }
        optionalAttributes.put( name, value );
    }


    public Map<String, String> getOptionalAttributes()
    {
        return getOptionalAttributes(DEFAULT_NAMESPACE);
    }


    public Map<String, String> getOptionalAttributes( String namespace )
    {
        return Collections.unmodifiableMap(namespacedAttributes.get(namespace));
    }
}
