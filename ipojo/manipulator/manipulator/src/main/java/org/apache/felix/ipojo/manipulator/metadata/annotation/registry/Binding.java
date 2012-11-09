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

package org.apache.felix.ipojo.manipulator.metadata.annotation.registry;

import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.Predicate;

import java.lang.annotation.Annotation;

/**
 * Triple storing the source annotation, the associated factory and the predicate for conditional support.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Binding {
    private Class<? extends Annotation> annotationType;
    private AnnotationVisitorFactory factory;
    private Predicate predicate;

    public Class<? extends Annotation> getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    public AnnotationVisitorFactory getFactory() {
        return factory;
    }

    public void setFactory(AnnotationVisitorFactory factory) {
        this.factory = factory;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }
}
