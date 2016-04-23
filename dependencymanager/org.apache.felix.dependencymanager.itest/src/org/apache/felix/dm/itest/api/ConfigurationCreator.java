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
package org.apache.felix.dm.itest.api;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Helper component used to create configurations.
 */
public class ConfigurationCreator {
    private volatile ConfigurationAdmin m_ca;
    private final Ensure m_ensure;
    Configuration m_conf;
    final String m_pid;
    final int m_initStep;
    
    public ConfigurationCreator(Ensure e, String pid, int initStep) {
        m_ensure = e;
        m_pid = pid;
        m_initStep = initStep;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public void init() {
        try {
        	Assert.assertNotNull(m_ca);
            m_ensure.step(m_initStep);
            m_conf = m_ca.getConfiguration(m_pid, null);
            Hashtable props = new Properties();
            props.put("testkey", "testvalue");
            m_conf.update(props);
        }
        catch (IOException e) {
            Assert.fail("Could not create configuration: " + e.getMessage());
        }
    }
    
    public void destroy() throws IOException {
    	m_conf.delete();  
    }
}
