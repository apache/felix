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
package org.apache.felix.ipojo.handlers.providedservice;

import org.apache.felix.ipojo.ComponentInstance;

/**
 * Listener interface for services provided by iPOJO component instances.
 */
public interface ProvidedServiceListener {

    /**
     * Called when the service has been registered.
     *
     * @param instance the concerned component instance
     * @param providedService the registered service
     */
    void serviceRegistered(ComponentInstance instance, ProvidedService providedService);

    /**
     * Called when the registered service has been updated.
     *
     * @param instance the concerned component instance
     * @param providedService the updated service
     */
    void serviceModified(ComponentInstance instance, ProvidedService providedService);

    /**
     * Called when the service is unregistered.
     *
     * @param instance the concerned component instance
     * @param providedService the unregistered service
     */
    void serviceUnregistered(ComponentInstance instance, ProvidedService providedService);

}
