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
package org.apache.felix.dm.lambda.samples.compositefactory;

/**
 * Pojo used to create all the objects composition used to implements the "Provider" Service.
 * The manager is using a Configuration injected by Config Admin, in order to configure the 
 * various objects being part of the "Provider" service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProviderFactory {
    private ProviderComposite1 m_composite1;
    private ProviderComposite2 m_composite2;
    private ProviderImpl m_providerImpl;
    @SuppressWarnings("unused")
	private MyConfig m_conf;

    public void updated(MyConfig conf) {
        m_conf = conf; // conf.getFoo() returns "bar"
    }

    /**
     * Builds the composition of objects used to implement the "Provider" service.
     * The Configuration injected by Config Admin will be used to configure the components
     * @return The "main" object providing the "Provider" service.
     */
    ProviderImpl create() {
        // Here, we can instantiate our object composition and configure them using the injected Configuration ...
        m_composite1 = new ProviderComposite1(); // possibly configure this object using our configuration
        m_composite2 = new ProviderComposite2(); // possibly configure this object using our configuration
        m_providerImpl = new ProviderImpl(m_composite1, m_composite2);
        return m_providerImpl; // Main object implementing the Provider service
    }

    /**
     * Returns the 
     * @return
     */
    Object[] getComposition() {
        return new Object[] { m_providerImpl, m_composite1, m_composite2 };
    }
}
