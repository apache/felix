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
package org.apache.felix.dependencymanager.samples.conf;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

/**
 * Configurator class used to inject configuration into Configuration Admin Service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Configurator {
    private volatile ConfigurationAdmin m_ca;
    volatile Configuration m_serviceConsumerConf;
    volatile Configuration m_serviceConsumerAnnotConf;
    volatile LogService m_log;
    
    public void start() {
        try {
            System.out.println("Configuring sample components ... please consult log messages to see example output, like this:");
            System.out.println("\"log warn\"");
            // Provide configuration to the hello.ServiceConsumer component
            m_serviceConsumerConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.hello.api.ServiceConsumerConf", null);
            Hashtable<String, String> props = new Hashtable<>();
            props.put("key", "value");
            m_serviceConsumerConf.update(props);
            
            // Provide configuration to the hello.annot.ServiceConsumer component
            m_serviceConsumerAnnotConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.hello.annot.ServiceConsumerConf", null);
            props = new Hashtable<>();
            props.put("key", "value");
            m_serviceConsumerAnnotConf.update(props);
            
            // Provide configuration to the composite component
            m_serviceConsumerAnnotConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.composite.ProviderImpl", null);
            props = new Hashtable<>();
            props.put("key", "value");
            m_serviceConsumerAnnotConf.update(props);

            // Provide configuration to the compositefactory component
            m_serviceConsumerAnnotConf = m_ca.getConfiguration("org.apache.felix.dependencymanager.samples.compositefactory.CompositionManager", null);
            props = new Hashtable<>();
            props.put("key", "value");
            m_serviceConsumerAnnotConf.update(props);            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void destroy() throws IOException {
    	m_serviceConsumerConf.delete();
        m_serviceConsumerAnnotConf.delete();
    }
}
