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
package org.apache.felix.scrplugin.annotations;

import java.util.Map;

/**
 * Base class for all annotation.
 */
public abstract class ScannedAnnotation {

    /** The fully qualified class name */
    protected final String name;

    /** The annotation values. */
    protected final Map<String, Object> values;

    /**
     * Create a new description
     * @param name   The fully qualified class name of the annotation
     * @param values The properties of the annotation (optional)
     */
    public ScannedAnnotation(final String name, final Map<String, Object> values) {
        this.name = name;
        this.values = values;
    }

    /**
     * Get the fully qualified class name of the annotation.
     * @return The fully qualified class name of the annotation.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the simple name of the annotation
     */
    public String getSimpleName() {
        final int pos = name.lastIndexOf('.');
        return name.substring(pos + 1);
    }

    /**
     * Check if a value exists for this annotation.
     * This method can be used to check whether a value exists,
     * even if the value is <code>null</code>.
     * @param paramName The property name
     * @return <code>true</code> If a value exists.
     */
    public boolean hasValue(final String paramName) {
        if ( values != null ) {
            return values.containsKey(paramName);
        }
        return false;
    }

    /**
     * Get a property value of the annotation.
     * @param paramName The property name.
     * @return The value of the property or <code>null</code>
     */
    public Object getValue(final String paramName) {
        if ( values != null ) {
            return values.get(paramName);
        }
        return null;
    }

    /**
     * Get a boolean value of the annotation
     * @param name The property name
     * @param defaultValue A default value if the property is not set
     * @return The property value or the default value.
     */
    public boolean getBooleanValue(final String name, final boolean defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null ) {
            return ((Boolean) val).booleanValue();
        }
        return defaultValue;
    }

    /**
     * Get an integer value of the annotation
     * @param name The property name
     * @param defaultValue A default value if the property is not set
     * @return The property value or the default value.
     */
    public int getIntegerValue(final String name, final int defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null ) {
            return ((Integer) val).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a long value of the annotation
     * @param name The property name
     * @param defaultValue A default value if the property is not set
     * @return The property value or the default value.
     */
    public long getLongValue(final String name, final long defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null ) {
            return ((Long) val).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a string value of the annotation
     * @param name The property name
     * @param defaultValue A default value if the property is not set
     * @return The property value or the default value.
     */
    public String getStringValue(final String name, final String defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null && val.toString().trim().length() > 0 ) {
            return val.toString().trim();
        }
        return defaultValue;
    }

    /**
     * Get an enumeration value of the annotation
     * @param name The property name
     * @param defaultValue A default value if the property is not set
     * @return The property value or the default value.
     */
    public String getEnumValue(final String name, final String defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null ) {
            if ( val instanceof String[] ) {
                return ((String[])val)[1];
            }
            if ( val instanceof String[][] ) {
                return ((String[][])val)[0][1];
            }
            return val.toString();
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return "AnnotationDescription [name=" + name + ", values=" + values
                + "]";
    }
}
