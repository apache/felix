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

package org.apache.felix.ipojo.manipulator.spi.helper;

import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.apache.felix.ipojo.manipulator.spi.Predicate;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Ready-to-use {@link Predicate} implementations.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Predicates {
    public static Node node() {
        return new Node();
    }

    public static Reference reference(String refId) {
        return new Reference(refId);
    }

    public static Matcher pattern(String regex) {
        return new Matcher(regex);
    }

    /**
     * Restrict to the given {@link ElementType}.
     * @param type expected {@link ElementType}
     */
    public static Predicate on(final ElementType type) {
        return new Predicate() {
            public boolean matches(BindingContext context) {
                return context.getElementType().equals(type);
            }
        };
    }

    /**
     * Always return {@literal true}.
     */
    public static Predicate alwaysTrue() {
        return new Predicate() {
            public boolean matches(BindingContext context) {
                return true;
            }
        };
    }

    /**
     * Successful if all given predicates are satisfied.
     * @param predicates predicates to be satisfied
     */
    public static Predicate and(final Predicate... predicates) {

        // Optimization
        if (predicates.length == 1) {
            return predicates[0];
        }

        return new Predicate() {
            public boolean matches(BindingContext context) {

                for (Predicate predicate : predicates) {
                    // Quit with first failure
                    if (!predicate.matches(context)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Successful if at least one of the given predicates is satisfied.
     * @param predicates predicates to be satisfied (at least one)
     */
    public static Predicate or(final Collection<Predicate> predicates) {

        // Optimization
        if (predicates.size() == 1) {
            return predicates.iterator().next();
        }

        return new Predicate() {
            public boolean matches(BindingContext context) {

                for (Predicate predicate : predicates) {
                    // Quit with first success
                    if (predicate.matches(context)) {
                        return true;
                    }
                }
                // No predicate were matching
                return false;
            }
        };
    }

    /**
     * Successful if at least one of the given predicates is satisfied.
     * @param predicates predicates to be satisfied (at least one)
     */
    public static Predicate or(final Predicate... predicates) {
        return or(Arrays.asList(predicates));
    }

    /**
     * Restrict to the supported {@link ElementType}(s) of the annotation (use the @Target, if provided).
     * @param annotationType annotation to explore
     */
    public static Predicate onlySupportedElements(final Class<? extends Annotation> annotationType) {
        Target target = annotationType.getAnnotation(Target.class);
        if (target == null) {
            return alwaysTrue();
        }

        Collection<Predicate> supportedTypes = new HashSet<Predicate>();
        for (ElementType type : target.value()) {
            supportedTypes.add(on(type));
        }

        return or(supportedTypes);
    }

    public static class Reference {

        private String refId;

        public Reference(String refId) {
            this.refId = refId;
        }

        /**
         * Restrict execution if the {@link org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench}
         * contains the given reference's name.
         */
        public Predicate exists() {
            return new Predicate() {
                public boolean matches(BindingContext context) {
                    return context.getWorkbench().getIds().containsKey(refId);
                }
            };
        }
    }

    public static class Matcher {

        private Pattern pattern;

        public Matcher(String regex) {
            pattern = Pattern.compile(regex);
        }

        /**
         * Restrict execution if the annotation's classname matches the given pattern.
         */
        public Predicate matches() {
            return new Predicate() {
                public boolean matches(BindingContext context) {
                    return pattern.matcher(context.getAnnotationType().getClassName()).matches();
                }
            };
        }
    }

    public static class Node {
        /**
         * Restrict execution if the supported {@literal Node} has the given name.
         */
        public Predicate named(final String expected) {
            return new Predicate() {
                public boolean matches(BindingContext context) {
                    if (context.getFieldNode() != null) {
                        FieldNode field = context.getFieldNode();
                        return field.name.equals(expected);
                    }

                    if (context.getMethodNode() != null) {
                        MethodNode method = context.getMethodNode();
                        return method.name.equals(expected);
                    }

                    if (context.getClassNode() != null) {
                        ClassNode clazz = context.getClassNode();
                        return clazz.name.equals(expected);
                    }

                    // Parameters have no name in bytecode

                    return false;
                }
            };
        }
    }

}
