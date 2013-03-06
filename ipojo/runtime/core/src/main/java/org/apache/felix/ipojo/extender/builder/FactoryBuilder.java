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

package org.apache.felix.ipojo.extender.builder;

import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * Interface defining the method used to build {@link IPojoFactory} instances.
 * As each type of iPOJO factories can be different, factory builder are the common facade of all those types.
 */
public interface FactoryBuilder {

    /**
     * Creates an iPOJO Factory.
     *
     * @param bundleContext the bundle context of the bundle declaring the component type
     * @param metadata      the metadata of the component type (<code>component</code> element).
     * @return the iPOJO Factory instance.
     * @throws FactoryBuilderException if the factory cannot be created.
     */
    IPojoFactory build(BundleContext bundleContext, Element metadata) throws FactoryBuilderException;
}
