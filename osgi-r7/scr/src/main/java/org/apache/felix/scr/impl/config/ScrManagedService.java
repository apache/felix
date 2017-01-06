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
package org.apache.felix.scr.impl.config;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * The <code>ScrManagedService</code> receives configuration for the Declarative
 * Services Runtime itself.
 * <p>
 * This class is instantiated in a ServiceFactory manner by the
 * {@link ScrManagedServiceServiceFactory} when the Configuration Admin service
 * implementation and API is available.
 * <p>
 * Requires OSGi Configuration Admin Service API available
 *
 * @see ScrManagedServiceServiceFactory
 */
public class ScrManagedService implements ManagedService
{

    private final ScrConfigurationImpl scrConfiguration;

    protected final ScrConfigurationImpl getScrConfiguration()
    {
        return scrConfiguration;
    }

    public ScrManagedService(final ScrConfigurationImpl scrConfiguration)
    {
        this.scrConfiguration = scrConfiguration;
    }

    public void updated(Dictionary<String, ?> properties) throws ConfigurationException
    {
        this.scrConfiguration.configure(properties, true);
    }
}
