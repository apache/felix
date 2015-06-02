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
package org.apache.felix.dependencymanager.samples.compositefactory;

import java.util.Dictionary;

/**
 * Pojo used to create all the objects composition used to implements the "Provider" Service.
 * The manager is using a Configuration injected by Config Admin, in order to configure the 
 * various objects being part of the "Provider" service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositionManager {
    private ProviderParticipant1 m_participant1;
    private ProviderParticipant2 m_participant2;
    private ProviderImpl m_providerImpl;
    @SuppressWarnings("unused")
	private Dictionary<String, String> m_conf;

    public void updated(Dictionary<String, String> conf) throws Exception {
        // validate configuration and throw an exception if the properties are invalid
        m_conf = conf;
    }

    /**
     * Builds the composition of objects used to implement the "Provider" service.
     * The Configuration injected by Config Admin will be used to configure the components
     * @return The "main" object providing the "Provider" service.
     */
    Object create() {
        // Here, we can instantiate our objects composition and configure them using the injected Configuration ...
        // Notice that we can also instantiate some different implementation objects, based on the what we find 
        // from the configuration.
        m_participant1 = new ProviderParticipant1(); // possibly configure this object using our configuration
        m_participant2 = new ProviderParticipant2(); // possibly configure this object using our configuration
        m_providerImpl = new ProviderImpl(m_participant1, m_participant2);
        return m_providerImpl; // Main object implementing the Provider service
    }

    Object[] getComposition() {
        return new Object[] { m_providerImpl, m_participant1, m_participant2 };
    }
}
