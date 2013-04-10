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

package org.apache.felix.ipojo.manipulator.spi;

import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.Binding;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.alwaysTrue;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.on;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.onlySupportedElements;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.or;

/**
 * All provided {@link Module}s have to inherit from this class.
 * It provides a simple to use DSL to express annotation bindings.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbsBindingModule implements Module {

    /**
     * Build bindings.
     */
    private List<Binding> bindings = new ArrayList<Binding>();

    public Iterator<Binding> iterator() {
        return bindings.iterator();
    }

    /**
     * Initiate an annotation binding.
     * Examples:
     * <pre>
     *     AnnotationVisitorFactory factory = new CompositeVisitorFactory();
     *     bind(Composite.class).to(factory);
     *     bind(Composite.class).when(.. some condition ..)
     *                          .to(factory);
     * </pre>
     * @param annotationType the annotation that will be bound to the {@link AnnotationVisitorFactory}
     */
    protected AnnotationBindingBuilder bind(Class<? extends Annotation> annotationType) {
        return new AnnotationBindingBuilder(bindings, annotationType);
    }

    /**
     * DSL helper class.
     */
    public class AnnotationBindingBuilder {
        private Class<? extends Annotation> annotationType;
        private AnnotationVisitorFactory factory;
        private List<Binding> registry;

        public AnnotationBindingBuilder(List<Binding> registry,
                                        Class<? extends Annotation> annotationType) {
            this.registry = registry;
            this.annotationType = annotationType;
        }

        /**
         * Declares a {@link Predicate} that will add a condition to the annotation binding.
         * @see org.apache.felix.ipojo.manipulator.spi.helper.Predicates
         * @param predicate the predicate to use
         */
        public ConditionalBindingBuilder when(Predicate predicate) {
            return new ConditionalBindingBuilder(this, predicate);
        }

        /**
         * Complete the annotation binding with the {@link AnnotationVisitorFactory} to be executed
         * when the annotation is found.
         * @param factory to be executed when the annotation is found.
         */
        public void to(AnnotationVisitorFactory factory) {
            this.factory = factory;
            registry.add(build());
        }

        /**
         * Creates the Binding.
         */
        private Binding build() {
            Binding binding = new Binding();
            binding.setAnnotationType(annotationType);
            binding.setPredicate(onlySupportedElements(annotationType));
            binding.setFactory(factory);
            return binding;
        }

    }

    public class ConditionalBindingBuilder {
        private AnnotationBindingBuilder parent;
        private Predicate predicate;
        private AnnotationVisitorFactory factory;

        public ConditionalBindingBuilder(AnnotationBindingBuilder parent, Predicate predicate) {
            this.parent = parent;
            this.predicate = predicate;
        }

        /**
         * Complete the annotation binding with the {@link AnnotationVisitorFactory} to be executed
         * when the annotation is found.
         * @param factory to be executed when the annotation is found.
         */
        public AnnotationBindingBuilder to(AnnotationVisitorFactory factory) {
            this.factory = factory;
            bindings.add(build());

            return parent;
        }

        /**
         * Creates the Binding.
         */
        private Binding build() {
            Binding binding = parent.build();
            binding.setPredicate(predicate);
            binding.setFactory(factory);
            return binding;
        }
    }

}
