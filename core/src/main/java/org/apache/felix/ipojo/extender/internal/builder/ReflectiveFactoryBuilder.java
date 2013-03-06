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

package org.apache.felix.ipojo.extender.internal.builder;

import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.extender.builder.FactoryBuilder;
import org.apache.felix.ipojo.extender.builder.FactoryBuilderException;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * An Factory builder using a reflective call to build the factory.
 * <p/>
 * This builder is compatible with the original iPOJO method consisting in calling a constructor receiving the bundle
 * context and the metadata as parameters.
 * <p/>
 * This factory builder need the constructor to be called.
 */
public class ReflectiveFactoryBuilder implements FactoryBuilder {

    /**
     * The constructor to call.
     */
    private final Constructor<? extends IPojoFactory> m_constructor;

    /**
     * Creates the factory builder.
     *
     * @param constructor the constructor that will be called when a new component factory will be created.
     */
    public ReflectiveFactoryBuilder(Constructor<? extends IPojoFactory> constructor) {
        m_constructor = constructor;
    }

    /**
     * Calls the wrapped constructor to create an iPOJO factory.
     *
     * @param bundleContext the bundle context of the bundle declaring the component type
     * @param metadata      the metadata of the component type (<code>component</code> element).
     * @return the created iPOJO factory
     * @throws FactoryBuilderException if the constructor cannot be called or throws an error.
     */
    public IPojoFactory build(BundleContext bundleContext, Element metadata) throws FactoryBuilderException {
        try {
            return m_constructor.newInstance(bundleContext, metadata);
        } catch (InstantiationException e) {
            throw new FactoryBuilderException("Cannot create instance of " + m_constructor.getDeclaringClass(), e);
        } catch (IllegalAccessException e) {
            throw new FactoryBuilderException(m_constructor.getDeclaringClass() + " constructor is not " +
                    "accessible (not public)", e);
        } catch (InvocationTargetException e) {
            throw new FactoryBuilderException("Cannot create instance of " + m_constructor.getDeclaringClass(), e);
        }
    }
}
