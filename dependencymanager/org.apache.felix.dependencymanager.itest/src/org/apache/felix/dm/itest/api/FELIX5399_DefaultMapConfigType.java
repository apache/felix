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

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * This test validates that a map specified by default from a configuration type is used when
 * the configuration is unavailable or when the configuration is available but the key is not present in the
 * configuration dictionary.
 */
public class FELIX5399_DefaultMapConfigType extends TestBase
{
    Ensure m_ensure = new Ensure();
    
    public void testDefaulValues() {
        DependencyManager m = getDM();
        
        Component myComponent = m.createComponent()
            .setImplementation(new MyComponent())
            .add(m.createConfigurationDependency().setCallback("updated", MyConfig.class).setRequired(false));

        
        m.add(myComponent);
        m_ensure.waitForStep(1, 5000);        
    }
    
    public interface MyConfig {
        public default SortedMap<String, String> getMap() { 
        	SortedMap<String, String> defaultMap = new TreeMap<>();
        	defaultMap.put("key1", "default1");
        	defaultMap.put("key2", "default2");
        	return defaultMap;
        }        
    }
    
    public class MyComponent {
        void updated(MyConfig cnf) {
        	Map<String, String> map = cnf.getMap();
        	Assert.assertEquals("default1", map.get("key1"));
        	Assert.assertEquals("default2", map.get("key2"));
        	m_ensure.step(1);
        }
    }

}
