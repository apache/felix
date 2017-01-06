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
     * Sets the service registration properties of the component
     * registered as a service. If the component is not registered as
     * a service, this method has no effect.
     * <p>
     * The <code>component.id</code> and <code>component.name</code>
     * property are set by the Service Component Runtime and cannot be
     * removed or replaced.
     *
     * @param properties properties to update the default component
     *      properties with. If this is <code>null</code> or empty the
     *      default set of properties as defined in Section 112.6,
     *      Component Properties, are used as the service registration
     *      properties.
     *
     * @throws IllegalStateException if this method is called for a
     *      Component Factory component
     */
    void setServiceProperties( Dictionary<String, ?> properties );

}
