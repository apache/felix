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
package org.apache.felix.dm.lambda.itest;

import java.io.IOException;
import java.util.Hashtable;

import org.junit.Assert;
import org.osgi.service.cm.ConfigurationAdmin;

public class FactoryConfigurationCreator {
    private volatile ConfigurationAdmin m_ca;
    private volatile org.osgi.service.cm.Configuration m_conf;
    private final String m_key;
    private final String m_value;
    private final String m_factoryPid;
    private final Ensure m_e;
    private final int m_firstStep;
    
    public FactoryConfigurationCreator(Ensure e, String factoryPid, int firstStep, String key, String value) {
        m_factoryPid = factoryPid;
        m_key = key;
        m_value = value;
        m_e = e;
        m_firstStep = firstStep;
    }

    public void start() {
        try {
            m_e.step(m_firstStep);
            m_conf = m_ca.createFactoryConfiguration(m_factoryPid, null);
            Hashtable<String, Object> props = new Hashtable<>();
            props.put(m_key, m_value);
            m_conf.update(props);
        }
        catch (IOException e) {
            Assert.fail("Could not create configuration: " + e.getMessage());
        }
    }
    
    public void update(String key, String val) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(key, val);
        try {
            m_conf.update(props);
        }
        catch (IOException e) {
            Assert.fail("Could not update configuration: " + e.getMessage());
        }
    }
    
    public void stop() {
        try
        {
            m_conf.delete();
        }
        catch (IOException e)
        {
            Assert.fail("Could not remove configuration: " + e.toString());
        }
    }
}

