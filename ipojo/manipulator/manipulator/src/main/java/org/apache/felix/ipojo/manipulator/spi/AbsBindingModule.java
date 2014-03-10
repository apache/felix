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

import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.onlySupportedElements;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationType;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.AnnotationPlayback;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.Binding;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.GenericVisitorFactory;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.ignore.NullBinding;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.stereotype.StereotypeVisitorFactory;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Elements;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.Type;

/**
 * All provided {@link Module}s have to inherit from this class.
 * It provides a simple to use DSL to express annotation bindings.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbsBindingModule implements Module {

    /**
     * Build bindings.
     */
    private final List<Binding> bindings = new ArrayList<Binding>();

    private boolean loaded = false;

    public synchronized void load() {
        if (!loaded) {
            configure();
            loaded = true;
        }
    }

    /**
     * Configure the bindings provided by this module.
     */
    protected abstract void configure();


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

    protected StereotypeBindingBuilder bindStereotype(Class<? extends Annotation> annotationType) {
        return new StereotypeBindingBuilder(bindings, annotationType);
    }

    protected HandlerBindingBuilder bindHandlerBinding(Class<? extends Annotation> annotationType) {
        return new HandlerBindingBuilder(bindings, annotationType);
    }

    protected void bindIgnore(Class<? extends Annotation> annotationType) {
        bindings.add(new NullBinding(Type.getType(annotationType)));
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
            binding.setAnnotationType(Type.getType(annotationType));
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

    public class StereotypeBindingBuilder {
        private final AnnotationType m_annotationType;

        public StereotypeBindingBuilder(final List<Binding> bindings, final Class<? extends Annotation> type) {
            m_annotationType = new AnnotationType(Type.getType(type));
            Binding binding = new Binding();
            binding.setAnnotationType(m_annotationType.getType());
            binding.setPredicate(onlySupportedElements(type));
            binding.setFactory(new StereotypeVisitorFactory(m_annotationType));
            bindings.add(binding);
        }

        public StereotypeBindingBuilder with(AnnotationLiteral<?> literal) {
            m_annotationType.getPlaybacks().add(new AnnotationPlayback(literal));
            return this;
        }
    }

    public class HandlerBindingBuilder {

        private final Binding m_binding;

        public HandlerBindingBuilder(final List<Binding> bindings, final Class<? extends Annotation> annotationType) {
            m_binding = new Binding();
            Type type = Type.getType(annotationType);
            m_binding.setAnnotationType(type);
            m_binding.setPredicate(onlySupportedElements(annotationType));
            Element e = Elements.buildElement(type);
            m_binding.setFactory(new GenericVisitorFactory(e.getName(), e.getNameSpace()));
            bindings.add(m_binding);
        }

        public void to(String namespace, String name) {
            m_binding.setFactory(new GenericVisitorFactory(name, namespace));
        }
    }
}
