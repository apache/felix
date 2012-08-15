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
package org.apache.felix.webconsole.bundleinfo;

/**
 * This entity defines additional bundle information entry, that is provided by
 * the {@link BundleInfoProvider}. Each information entry is featured by name,
 * value, type and description.
 * 
 * @author Valentin Valchev
 */
public class BundleInfo 
{

    private final String name;
    private final String description;
    private final Object value;
    private final BundleInfoType type;

    /**
     * Creates a new bundle information entry.
     * 
     * @param name
     *            the name of the entry
     * @param value
     *            the value associated with that entry
     * @param type
     *            the type of the value
     * @param description
     *            additional, user-friendly description for that value.
     */
    public BundleInfo(String name, Object value, BundleInfoType type,
	    String description) 
    {
	this.name = name;
	this.value = value;
	this.type = type;
	this.description = description;
	type.validate(value);
    }

    /**
     * Gets the name of the information entry. The name should be localized
     * according the requested locale.
     * 
     * @return the name of that information key.
     */
    public String getName() 
    {
	return name;
    }

    /**
     * Gets user-friendly description of the key pair. The description should be
     * localized according the requested locale.
     * 
     * @return the description for that information key.
     */
    public String getDescription() 
    {
	return description;
    }

    /**
     * Gets the information value.
     * 
     * @return the value.
     */
    public Object getValue() 
    {
	return value;
    }

    /**
     * Gets the type of the information value.
     * 
     * @return the information type.
     */
    public BundleInfoType getType() 
    {
	return type;
    }

}
