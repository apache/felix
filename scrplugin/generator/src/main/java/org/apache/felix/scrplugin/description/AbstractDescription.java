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

    /**
     * Create a new abstract description
     * @param annotation The corresponding annotation.
     */
    public AbstractDescription(final ScannedAnnotation annotation) {
        this.annotation = annotation;
    }

    /**
     * Get the annotation.
     * @return The annotation or <code>null</code>
     */
    public ScannedAnnotation getAnnotation() {
        return this.annotation;
    }
}