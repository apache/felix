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

import org.apache.felix.scrplugin.annotations.ScannedAnnotation;


/**
 * <code>AbstractDescription</code> is the base class for all descriptions.
 *
 * @see ComponentDescription
 * @see ServiceDescription
 * @see ReferenceDescription
 * @see PropertyDescription
 */
public abstract class AbstractDescription {

    /** The corresponding annotation from the class file. */
    protected final ScannedAnnotation annotation;

    private final String annotationPrefix;

    private String sourceLocation;

    /**
     * Create a new abstract description
     * @param annotation The corresponding annotation.
     */
    public AbstractDescription(final ScannedAnnotation annotation) {
        this.annotation = annotation;
        if ( annotation == null ) {
            this.annotationPrefix = "";
        } else {
            this.annotationPrefix = "@" + annotation.getSimpleName();
        }
        this.sourceLocation = "<unknown>";
    }

    /**
     * Get the annotation.
     * @return The annotation or <code>null</code>
     */
    public ScannedAnnotation getAnnotation() {
        return this.annotation;
    }

    public void setSource(final String location) {
        this.sourceLocation = location;
    }

    public String getSource() {
        return this.sourceLocation;
    }

    public String getIdentifier() {
        return this.annotationPrefix;
    }

    public abstract AbstractDescription clone();
}