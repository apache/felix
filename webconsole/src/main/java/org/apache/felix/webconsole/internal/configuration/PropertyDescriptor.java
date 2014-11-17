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
package org.apache.felix.webconsole.internal.configuration;

/**
 * The <code>PropertyDescriptor</code> class describes a property to
 * generate the Configuration Management UI. It is built after the
 * AttributeDescriptor but to prevent a hard dependency on the Metatype
 * Service API, this does not implement it.
 */
class PropertyDescriptor
{

    final String id;
    final int type;
    final int cardinality;


    public PropertyDescriptor( final String id, int type, int cardinality )
    {
        this.id = id;
        this.type = type;
        this.cardinality = cardinality;
    }


    public String getName()
    {
        return id;
    }


    public String getID()
    {
        return id;
    }


    public String getDescription()
    {
        // no description
        return null;
    }


    public int getCardinality()
    {
        return cardinality;
    }


    public int getType()
    {
        return type;
    }


    public String[] getOptionValues()
    {
        return null;
    }


    public String[] getOptionLabels()
    {
        return null;
    }


    public String validate( String value )
    {
        // no validation
        return null;
    }


    public String[] getDefaultValue()
    {
        return null;
    }

    public boolean isOptional()
    {
        return false;
    }
    
}