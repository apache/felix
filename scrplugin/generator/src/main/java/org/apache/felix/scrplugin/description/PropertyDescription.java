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
package org.apache.felix.scrplugin.description;

import java.util.Arrays;

import org.apache.felix.scrplugin.annotations.ScannedAnnotation;


/**
 * A <code>PropertyDescription</code> describes a property
 * of a component.
 *
 * In general all fields should be set by an annocation scanner,
 * no default values should be assumed for these fields:
 * <ul>
 * <li>name</li>
 * <li>value</li>
 * <li>multiValue</li>
 * <li>type</li>
 * <li>unbounded</li>
 * <li>cardinality</li>
 * </ul>
 *
 * These values have the following default values:
 * <ul>
 * <li>label : null - will be handled by the scr generator</li>
 * <li>description : null - will be handled by the scr generator</li>
 * <li>isPrivate : null - will be be handled by the scr generator</li>
 * <li>options : null</li>
 * </ul>
 *
 */
public class PropertyDescription extends AbstractDescription {

    private String name;
    private String value;
    private PropertyType type;
    private String[] multiValue;
    private PropertyUnbounded unbounded;
    private int cardinality;

    private Boolean isPrivate;
    private String label;
    private String description;
    private String[] options;

    public PropertyDescription(final ScannedAnnotation annotation) {
        super(annotation);
    }

    public PropertyUnbounded getUnbounded() {
        return unbounded;
    }

    public void setUnbounded(PropertyUnbounded unbounded) {
        this.unbounded = unbounded;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] values) {
        this.options = values;
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
    public Boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCardinality() {
        return cardinality;
    }

    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    @Override
    public String toString() {
        return "PropertyDescription [name=" + name + ", value=" + value
                + ", type=" + type + ", multiValue="
                + Arrays.toString(multiValue) + ", isPrivate=" + isPrivate
                + ", label=" + label + ", description=" + description
                + ", cardinality=" + cardinality + ", unbounded=" + unbounded
                + ", options=" + Arrays.toString(options) + ", annotation=" + annotation + "]";
    }

    @Override
    public AbstractDescription clone() {
        final PropertyDescription cd = new PropertyDescription(this.annotation);
        cd.setName(this.getName());
        cd.setLabel(this.getLabel());
        cd.setDescription(this.getDescription());
        if ( this.getValue() != null ) {
            cd.setValue(this.getValue());
        } else {
            cd.setMultiValue(this.getMultiValue());
        }
        cd.setType(this.getType());
        cd.setUnbounded(this.getUnbounded());
        cd.setCardinality(this.getCardinality());
        cd.setPrivate(this.isPrivate());
        cd.setOptions(this.getOptions());
        return cd;
    }
}
