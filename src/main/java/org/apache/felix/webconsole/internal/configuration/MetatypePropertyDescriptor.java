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


import org.osgi.service.metatype.AttributeDefinition;


/**
 * The <code>MetatypePropertyDescriptor</code> extends the
 * {@link PropertyDescriptor} describing a property based on a real
 * Metatype Service Attribute Definition.
 */
public class MetatypePropertyDescriptor extends PropertyDescriptor
{
    private final AttributeDefinition ad;
    private final boolean optional; 


    public MetatypePropertyDescriptor( AttributeDefinition ad, boolean optional )
    {
        super( ad.getID(), ad.getType(), ad.getCardinality() );
        this.ad = ad;
        this.optional = optional;
    }


    public String getName()
    {
        return ad.getName();
    }


    public String getDescription()
    {
        return ad.getDescription();
    }


    public int getType()
    {
        return ad.getType();
    }


    public String[] getOptionValues()
    {
        return ad.getOptionValues();
    }


    public String[] getOptionLabels()
    {
        return ad.getOptionLabels();
    }


    public String validate( String value )
    {
        return ad.validate( value );
    }


    public String[] getDefaultValue()
    {
        return ad.getDefaultValue();
    }

    public boolean isOptional()
    {
        return optional;
    }
}
