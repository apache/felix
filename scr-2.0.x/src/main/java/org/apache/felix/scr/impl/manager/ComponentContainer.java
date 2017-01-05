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
package org.apache.felix.scr.impl.manager;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;

public interface ComponentContainer<S>
{

    /**
     * Returns the {@link ComponentActivator} owning this component
     * holder.  (overlaps ComponentHolder)
     */
    ComponentActivator getActivator();

    /**
     * Returns the {@link ComponentMetadata} describing and declaring this
     * component. (overlaps ComponentHolder)
     */
    ComponentMetadata getComponentMetadata();

    /**
     * Informs the holder that the component has been disposed as a result of
     * calling the dispose method.
     */
    void disposed(SingleComponentManager<S> component);

}