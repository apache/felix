/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo;

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.osgi.framework.Bundle;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the description of primitive (non-composite) component
 * types. An instance of this class will be returned when invoking the
 * {@link org.apache.felix.ipojo.ComponentFactory#getComponentDescription()} method.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
final class PrimitiveTypeDescription extends ComponentTypeDescription {

    /**
     * Set to keep component's all super-class class-names.
     */
    private Set<String> m_superClasses = new HashSet<String>();

    /**
     * Set to keep component's all interface class-names.
     */
    private Set<String> m_interfaces = new HashSet<String>();

    /**
     * The described component factory.
     */
    private ComponentFactory m_factory;

    /**
     * Creates a PrimitiveTypeDescription object.
     *
     * @param factory the m_factory attached to this component type description.
     */
    public PrimitiveTypeDescription(ComponentFactory factory) {
        super(factory);
        this.m_factory = factory;

        try {
            // The inspection can be done only for primitive components
            if (factory.getClassName() != null) {
                // Read inherited classes and interfaces into given Sets.
                new InheritanceInspector(factory.getPojoMetadata(), getBundleContext().getBundle()).
                        computeInterfacesAndSuperClasses(m_interfaces, m_superClasses);
            }
        } catch (ClassNotFoundException e) {
            m_interfaces.clear();
            m_superClasses.clear();
        }

    }

    /**
     * Computes the properties to publish.
     * The <code>component.class</code> property contains the implementation class name.
     *
     * @return the dictionary of properties to publish
     * @see org.apache.felix.ipojo.architecture.ComponentTypeDescription#getPropertiesToPublish()
     */
    public Dictionary<String, Object> getPropertiesToPublish() {
        Dictionary<String, Object> dict = super.getPropertiesToPublish();
        if (m_factory.getClassName() != null) {
            dict.put("component.class", m_factory.getClassName());
        }
        return dict;
    }

    /**
     * Adds the "implementation-class" attribute to the type description.
     *
     * @return the component type description.
     * @see org.apache.felix.ipojo.architecture.ComponentTypeDescription#getDescription()
     */
    public Element getDescription() {
        Element elem = super.getDescription();
        elem.addAttribute(new Attribute("Implementation-Class", m_factory.getClassName()));

        /* Adding interfaces and super-classes of component into description */
        Element inheritance = new Element("Inherited", "");

        inheritance.addAttribute(new Attribute("Interfaces", m_interfaces.toString()));
        inheritance.addAttribute(new Attribute("SuperClasses", m_superClasses.toString()));

        elem.addElement(inheritance);

        return elem;
    }

    /**
     * This class is used to collect interfaces and super-classes of given component in specified Sets.
     *
     * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
     */
    private final class InheritanceInspector {
        /*
            * PojoMetadata of target Component.
            */
        private PojoMetadata m_pojoMetadata;
        /*
            * Bundle exposing target component.
            */
        private Bundle m_bundle;


        /**
         * Creates a TypeCollector object
         *
         * @param pojoMetadata PojoMetadata describing Component.
         * @param bundle       Bundle which has been exposed the intended Component.
         */
        public InheritanceInspector(PojoMetadata pojoMetadata, Bundle bundle) {
            m_pojoMetadata = pojoMetadata;
            m_bundle = bundle;
        }

        /**
         * Collect interfaces implemented by the POJO into given Sets.
         *
         * @param interfaces : the set of implemented interfaces
         * @param classes    : the set of extended classes
         * @throws ClassNotFoundException : occurs when an interface cannot be loaded.
         */
        public void computeInterfacesAndSuperClasses(Set<String> interfaces, Set<String> classes) throws ClassNotFoundException {
            String[] immediateInterfaces = m_pojoMetadata.getInterfaces();
            String parentClass = m_pojoMetadata.getSuperClass();

            // First iterate on found specification in manipulation metadata
            for (String immediateInterface : immediateInterfaces) {
                interfaces.add(immediateInterface);
                // Iterate on interfaces implemented by the current interface
                Class<?> clazz = m_bundle.loadClass(immediateInterface);
                collectInterfaces(clazz, interfaces, m_bundle);
            }

            // Look for parent class.
            if (parentClass != null) {
                Class clazz = m_bundle.loadClass(parentClass);
                collectInterfacesFromClass(clazz, interfaces, m_bundle);
                classes.add(parentClass);
                collectParentClassesFromClass(clazz, classes, m_bundle);
            }

            // Removing Object Class from the inherited classes list.
            classes.remove(Object.class.getName());
        }

        /**
         * Look for inherited interfaces.
         *
         * @param clazz  : interface name to explore (class object)
         * @param acc    : set (accumulator)
         * @param bundle : bundle
         * @throws ClassNotFoundException : occurs when an interface cannot be loaded.
         */
        private void collectInterfaces(Class<?> clazz, Set<String> acc, Bundle bundle) throws ClassNotFoundException {
            Class[] clazzes = clazz.getInterfaces();
            for (Class clazze : clazzes) {
                acc.add(clazze.getName());
                collectInterfaces(clazze, acc, bundle);
            }
        }

        /**
         * Collect interfaces for the given class.
         * This method explores super class to.
         *
         * @param clazz  : class object.
         * @param acc    : set of implemented interface (accumulator)
         * @param bundle : bundle.
         * @throws ClassNotFoundException : occurs if an interface cannot be load.
         */
        private void collectInterfacesFromClass(Class<?> clazz, Set<String> acc,
                                                Bundle bundle) throws ClassNotFoundException {
            Class[] clazzes = clazz.getInterfaces();
            for (Class clazze : clazzes) {
                acc.add(clazze.getName());
                collectInterfaces(clazze, acc, bundle);
            }
            // Iterate on parent classes
            Class sup = clazz.getSuperclass();
            if (sup != null) {
                collectInterfacesFromClass(sup, acc, bundle);
            }
        }

        /**
         * Collect parent classes for the given class.
         *
         * @param clazz  : class object.
         * @param acc    : set of extended classes (accumulator)
         * @param bundle : bundle.
         * @throws ClassNotFoundException : occurs if an interface cannot be load.
         */
        private void collectParentClassesFromClass(Class<?> clazz, Set<String> acc, Bundle bundle) throws ClassNotFoundException {
            Class<?> parent = clazz.getSuperclass();
            if (parent != null) {
                acc.add(parent.getName());
                collectParentClassesFromClass(parent, acc, bundle);
            }
        }
    }
}
