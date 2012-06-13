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
package org.apache.felix.scrplugin.om;

import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.SpecVersion;
import org.apache.felix.scrplugin.scanner.ScannedAnnotation;

/**
 * <code>Property.java</code>...
 *
 */
public class Property extends AbstractObject {

    protected String name;
    protected String value;
    protected PropertyType type;
    protected String[] multiValue;

    /**
     * Constructor from java source.
     */
    public Property(final ScannedAnnotation annotation, final String sourceLocation) {
        super(annotation, sourceLocation);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
        this.multiValue = null;
    }

    public PropertyType getType() {
        return this.type;
    }

    public void setType(PropertyType type) {
        this.type = type;
    }

    public String[] getMultiValue() {
        return this.multiValue;
    }

    public void setMultiValue(String[] values) {
        this.multiValue = values;
        this.value = null;
    }

    /**
     * Validate the property.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    public void validate(final Context context) {
        if (name == null || name.trim().length() == 0) {
            this.logError(context.getIssueLog(), "Property name can not be empty.");
        }
        if (type != null) {
            // now check for old and new char
            if (context.getSpecVersion() == SpecVersion.VERSION_1_0 && type == PropertyType.Character) {
                type = PropertyType.Char;
            }
            if (context.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal()
                            && type == PropertyType.Char) {
                type = PropertyType.Character;
            }
        }
        // TODO might want to check value
    }
}
