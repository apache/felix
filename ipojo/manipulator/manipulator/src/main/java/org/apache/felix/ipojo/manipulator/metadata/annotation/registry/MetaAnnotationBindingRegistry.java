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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.alwaysTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.annotations.HandlerBinding;
import org.apache.felix.ipojo.annotations.Ignore;
import org.apache.felix.ipojo.annotations.Stereotype;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationType;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.discovery.ChainedAnnotationDiscovery;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.discovery.HandlerBindingDiscovery;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.discovery.IgnoredDiscovery;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.discovery.StereotypeDiscovery;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.parser.AnnotationParser;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.GenericVisitorFactory;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.ignore.NullBinding;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.stereotype.StereotypeVisitorFactory;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Elements;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.Type;

/**
 * The {@link MetaAnnotationBindingRegistry} is a registry that tries to complete its list
 * of bindings when an unknown one is detected.
 * It uses the given {@link ResourceStore} to parse the annotation's type and find if
 * it's annotated with @{@link org.apache.felix.ipojo.annotations.Stereotype},
 * @{@link org.apache.felix.ipojo.annotations.HandlerBinding} or @{@link org.apache.felix.ipojo.annotations.Ignore}
 */
public class MetaAnnotationBindingRegistry extends CompletableBindingRegistry {

    private ResourceStore m_store;
    private Reporter m_reporter;

    public MetaAnnotationBindingRegistry(final BindingRegistry delegate, final Reporter reporter, final ResourceStore store) {
        super(delegate, reporter);
        this.m_reporter = reporter;
        this.m_store = store;
        addBindings(nullBindingsForMetaAnnotations());
    }

    protected Iterable<Binding> nullBindingsForMetaAnnotations() {
        // Do not re-apply meta-annotations
        ArrayList<Binding> bindings = new ArrayList<Binding>();
        bindings.add(new NullBinding(Type.getType(Stereotype.class)));
        bindings.add(new NullBinding(Type.getType(HandlerBinding.class)));
        bindings.add(new NullBinding(Type.getType(Ignore.class)));
        return bindings;
    }

    @Override
    protected List<Binding> createBindings(final Type type) {

        // Parse the annotation
        byte[] bytes;
        try {
            bytes = m_store.read(type.getInternalName().concat(".class"));
        } catch (IOException e) {
            // Annotation type cannot be read
            m_reporter.trace("Could not read bytecode for @%s.", type.getClassName());
            return emptyList();
        } catch (IllegalStateException e) {
            m_reporter.trace("Could not read bytecode for @%s because the bundle is not in a state allowing read " +
                            "operations.",
                    type.getClassName());
            return emptyList();
        }
        AnnotationParser parser = new AnnotationParser();
        AnnotationType annotationType = parser.read(bytes);

        // Search meta-annotations
        ChainedAnnotationDiscovery chain = new ChainedAnnotationDiscovery();
        StereotypeDiscovery stereotypeDiscovery = new StereotypeDiscovery();
        HandlerBindingDiscovery handlerBindingDiscovery = new HandlerBindingDiscovery();
        IgnoredDiscovery ignoredDiscovery = new IgnoredDiscovery();
        chain.getDiscoveries().add(stereotypeDiscovery);
        chain.getDiscoveries().add(handlerBindingDiscovery);
        chain.getDiscoveries().add(ignoredDiscovery);

        annotationType.traverse(chain);

        // Produced Bindings
        List<Binding> bindings = new ArrayList<Binding>();

        // @Stereotype support
        if (stereotypeDiscovery.isStereotype()) {
            m_reporter.trace("@Stereotype detected: @%s", type.getClassName());
            Binding binding = new Binding();
            binding.setAnnotationType(type);
            binding.setPredicate(alwaysTrue());
            binding.setFactory(new StereotypeVisitorFactory(annotationType));

            bindings.add(binding);
        }

        // @HandlerBinding support
        if (handlerBindingDiscovery.isHandlerBinding()) {

            m_reporter.trace("@HandlerBinding detected: @%s", type.getClassName());
            Binding binding = new Binding();
            binding.setAnnotationType(type);
            binding.setPredicate(alwaysTrue());
            final Element element = buildElement(handlerBindingDiscovery, type);
            binding.setFactory(new GenericVisitorFactory(element.getName(), element.getNameSpace()));

            bindings.add(binding);
        }

        // Its IMPORTANT that the @Ignore is processed last since it removes existing bindings
        if (ignoredDiscovery.isIgnore()) {
            m_reporter.trace("@Ignore detected: @%s", type.getClassName());
            Binding binding = new NullBinding(type);

            bindings.clear();
            bindings.add(binding);
            bindings = unmodifiableList(bindings); // just in case of ...
        }

        return bindings;

    }

    private Element buildElement(final HandlerBindingDiscovery handler, final Type type) {
        Element element;
        if ((handler.getNamespace() == null) &&
                (handler.getValue() == null)) {
            // No attributes specified, use annotation type as element's source
            element = Elements.buildElement(type);
        } else if ((handler.getNamespace() == null)) {
            // Namespace attribute is omitted
            element = Elements.buildElement(handler.getValue());
        } else {
            element = Elements.buildElement(handler.getNamespace(),
                                            handler.getValue());
        }
        return element;
    }

}
