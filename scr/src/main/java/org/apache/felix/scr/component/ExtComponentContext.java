/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.component;


import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;


/**
 * The <code>ExtComponentContext</code> is a custom extension of the
 * standard ComponentContext allowing to update the service registration
 * properties of a component registered as a service.
 */
public interface ExtComponentContext extends ComponentContext
{

    /**
     * Updates the service registration properties of the component
     * registered as a service. The effects are:
     * <ol>
     * <li>The properties read from the component descriptor are updated
     * with the values from the given dictionary</li>
     * <li>Configuration Admin properties are applied</li>
     * <li>The ServiceRegistration service properties are updated with the
     * result.</li>
     * </ol>
     * <p>
     * Calling this method is does not cause a component reconfiguration as
     * would be caused by a Configuration update. Particularly the
     * configured modified method (if any) is not called as a result of
     * calling this method.
     * <p>
     * Please note:
     * <ul>
     * <li>The provided properties may overwrite or add properties to
     * the properties read from the component descriptor. It is not
     * possible to remove such descriptor properties</li>
     * <li>The provided properties are only valid for the livecycle of the
     * component instance. After reactivation of a component (and thus
     * creation of a new component instance) the properties are removed.
     * </li>
     * <li>If the component can be dynamically updated with configuration
     * these properties will influence such configuration.</li>
     * <li>Configuration is not updated in the Configuration Admin Service
     * when calling service</li>
     * <li>Properties provided with this method may still be overwriiten
     * with configuration provided by the Configuration Admin Service.</lI>
     * </ul>
     * <p>
     * If the component to which this context belongs is not registered as
     * a service, this method
     *
     * @param properties properties to update the default component
     *      properties with.
     *
     * @throws IllegalStateException if this method is called for a
     *      Component Factory component
     */
    void updateProperties( Dictionary properties );

}
