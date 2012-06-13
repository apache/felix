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
package org.apache.felix.scrplugin.scanner;

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

    public boolean getBooleanValue(final String name, final boolean defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null ) {
            return ((Boolean) val).booleanValue();
        }
        return defaultValue;
    }

    public int getIntegerValue(final String name, final int defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null ) {
            return ((Integer) val).intValue();
        }
        return defaultValue;
    }

    public long getLongValue(final String name, final long defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null ) {
            return ((Long) val).intValue();
        }
        return defaultValue;
    }

    public String getStringValue(final String name, final String defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null && val.toString().trim().length() > 0 ) {
            return val.toString().trim();
        }
        return defaultValue;
    }

    public String getEnumValue(final String name, final String defaultValue) {
        final Object val = this.getValue(name);
        if ( val != null ) {
            return ((String[])val)[1];
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return "AnnotationDescription [name=" + name + ", values=" + values
                + "]";
    }
}
