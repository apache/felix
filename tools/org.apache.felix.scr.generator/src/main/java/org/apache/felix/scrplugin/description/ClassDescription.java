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

import java.util.ArrayList;
import java.util.List;

/**
 * A <code>ClassDescription</code> is describing the found annotations
 * of a class.
 * Its basically used as a wrapper to hold all the various annotations
 * found in a class. This description only contains the annotations
 * found in the current class, but not in any superclass.
 * The annotations are available as descriptions.
 * @see ComponentDescription
 * @see ServiceDescription
 * @see ReferenceDescription
 * @see PropertyDescription
 */
public class ClassDescription {

    /** All descriptions. */
    final List<AbstractDescription> descriptions = new ArrayList<AbstractDescription>();

    /** The corresponding class. */
    private final Class<?> describedClass;

    /** The source (file) of the class or any other location information. */
    private final String source;

    /**
     * Create a new class description
     */
    public ClassDescription(final Class<?> describedClass, final String source) {
        this.describedClass = describedClass;
        this.source = source;
    }

    /**
     * Get the associated class.
     * @return The associated class.
     */
    public Class<?> getDescribedClass() {
        return this.describedClass;
    }

    /**
     * Get the location information like the source file etc.
     * @return The location information.
     */
    public String getSource() {
        return this.source;
    }

    /**
     * Add a new description
     */
    public void add(final AbstractDescription desc) {
        this.descriptions.add(desc);
        desc.setSource(this.source);
    }

    /**
     * Get all descriptions of that type.
     * @param descType The description class.
     * @return A list of found descriptions or the empty array.
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractDescription> List<T> getDescriptions(final Class<T> descType) {
        final List<T> result = new ArrayList<T>();
        for(final AbstractDescription desc : descriptions) {
            if ( descType.isAssignableFrom(desc.getClass()) ) {
                result.add((T) desc);
            }
        }
        return result;
    }

    /**
     * Get the first description of that type
     * @param descType The description class
     * @return THe first description or <code>null</code>
     */
    public <T extends AbstractDescription> T getDescription(final Class<T> descType) {
        final List<T> result = this.getDescriptions(descType);
        if ( result.size() > 0 ) {
            return result.get(0);
        }
        return null;
    }

    @Override
    public String toString() {
        return "ClassDescription [descriptions=" + descriptions
                        + ", describedClass=" + describedClass + ", source=" + source
                        + "]";
    }

    public ClassDescription clone() {
        final ClassDescription cd = new ClassDescription(this.describedClass, this.source);
        for(final AbstractDescription ad : this.descriptions) {
            cd.add(ad.clone());
        }
        return cd;
    }
}