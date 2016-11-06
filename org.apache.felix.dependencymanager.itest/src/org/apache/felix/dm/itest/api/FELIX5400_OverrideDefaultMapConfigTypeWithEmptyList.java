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

import java.util.Arrays;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * This test validates that we can register in the actual configuration an empty list for overriding a non empty default
 * list that the configuration type has defined by default.
 * 
 * Example: if the configuration type provides a default list value [a, b] for property "list", and if 
 * there is a "list=[]" entry in the actual configuration, then the empty list should be returned instead of default [a,b].
 */
public class FELIX5400_OverrideDefaultMapConfigTypeWithEmptyList extends TestBase {
    Ensure m_ensure = new Ensure();
    
    public void testDefaulValues() {
        DependencyManager m = getDM();
        
        ConfigurationCreator configurator = new ConfigurationCreator(m_ensure, MyConfig.class.getName(), 1, "list=[]");        		
        
        Component confCreator = m.createComponent()
        	.setImplementation(configurator)
            .add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        
        Component myComponent = m.createComponent()
            .setImplementation(new MyComponent())
            .add(m.createConfigurationDependency().setCallback("updated", MyConfig.class).setRequired(true));

        
        // register an empty configuration.
        m.add(confCreator);
        m_ensure.waitForStep(1, 5000);     

        // create the component: since there is no value for the map in the actual configuration, then default map 
        // provided by the configuration type default method should be returned.
        m.add(myComponent);
        m_ensure.waitForStep(2, 5000);        
    }
    
    public interface MyConfig {
        public default List<String> getList() { 
        	return Arrays.asList("a", "b");
        }        
    }
    
    public class MyComponent {
        void updated(MyConfig cnf) {
        	List<String> list = cnf.getList();
        	Assert.assertEquals(0, list.size()); // the actual configuration contains "list=[]" and default "list=[a,b]" must not be returned.
        	m_ensure.step(2);
        }
    }
}
