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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A scanned class contains all scanned information
 * like the found annotations.
 */
public class ScannedClass {

    /** All found annotations .*/
    private final List<ScannedAnnotation> descriptions = new ArrayList<ScannedAnnotation>();

    /** The scanned class. */
    private final Class<?> scannedClass;

    /**
     * Constructor
     * @param desc List of found annotations.
     * @param scannedClass The scanned class.
     */
    public ScannedClass(final List<ScannedAnnotation> desc, final Class<?> scannedClass) {
        this.descriptions.addAll(desc);
        this.scannedClass = scannedClass;
    }

    /**
     * Get the scanned class.
     * @return The scanned class.
     */
    public Class<?> getScannedClass() {
        return this.scannedClass;
    }

    /**
     * Mark an annotation as processed.
     * A processed annotation will be removed from the list of annotations.
     * @param desc The annotation.
     */
    public void processed(final ScannedAnnotation desc) {
        this.descriptions.remove(desc);
    }

    /**
     * Mark several annotations as processed.
     * A processed annotation will be removed from the list of annotations.
     * @param desc The annotation.
     */
    public void processed(final Collection<? extends ScannedAnnotation> desc) {
        this.descriptions.removeAll(desc);
    }

    /**
     * Get all class annotations
     * @param name The name of the class annotation or <code>null</code>
     * for all class annotations.
     * @return A list of matching annotations or an empty list.
     */
    public List<ClassAnnotation> getClassAnnotations(final String name) {
        final List<ClassAnnotation> list = new ArrayList<ClassAnnotation>();
        for(final ScannedAnnotation desc : descriptions ) {
            if ( desc instanceof ClassAnnotation ) {
                if ( name == null || desc.getName().equals(name) ) {
                    list.add( (ClassAnnotation)desc);
                }
            }
        }
        return list;
    }

    /**
     * Get all field annotations
     * @param name The name of the field annotation or <code>null</code>
     * for all field annotations.
     * @return A list of matching annotations or an empty list.
     */
    public List<FieldAnnotation> getFieldAnnotations(final String name) {
        final List<FieldAnnotation> list = new ArrayList<FieldAnnotation>();
        for(final ScannedAnnotation desc : descriptions ) {
            if ( desc instanceof FieldAnnotation ) {
                if ( name == null || desc.getName().equals(name) ) {
                    list.add( (FieldAnnotation)desc);
                }
            }
        }
        return list;
    }

    /**
     * Get all method annotations
     * @param name The name of the method annotation or <code>null</code>
     * for all method annotations.
     * @return A list of matching annotations or an empty list.
     */
    public List<MethodAnnotation> getMethodAnnotations(final String name) {
        final List<MethodAnnotation> list = new ArrayList<MethodAnnotation>();
        for(final ScannedAnnotation desc : descriptions ) {
            if ( desc instanceof MethodAnnotation ) {
                if ( name == null || desc.getName().equals(name) ) {
                    list.add( (MethodAnnotation)desc);
                }
            }
        }
        return list;
    }
}
