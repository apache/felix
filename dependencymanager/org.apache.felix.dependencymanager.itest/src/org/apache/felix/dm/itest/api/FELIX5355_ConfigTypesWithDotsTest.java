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
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Validates the case when a config type property contains some dot "." characters.
 * In this case, the configuration type interface method must contain a "_" characeter which is mapped to ".".
 */
public class FELIX5355_ConfigTypesWithDotsTest extends TestBase
{
    Ensure m_ensure = new Ensure();

    public void testPropertiesWithDots() {
        DependencyManager m = getDM();
        
        ConfigurationCreator configurator = new ConfigurationCreator(m_ensure, MyConfig.class.getName(), 2, 
            "foo.bar.param1=bar", 
            "foo_param2=bar2", 
            "param3=bar3", 
            "foo_BaR.Param4=bar4",
            "foo.bar5.0=1",
        	"foo.bar5.1=2",
            "foo.bar6.0=1",
        	"foo.bar6.1=2",
        	"foo.bar7.key=value",
        	"foo.bar8={key.value}");
        
        Component confCreator = m.createComponent().setImplementation(configurator)
            .add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        
        Component myComponent = m.createComponent()
            .setImplementation(new MyComponent())
            .add(m.createConfigurationDependency().setCallback("updated", MyConfig.class).setRequired(false));
        
        m.add(myComponent);
        m_ensure.waitForStep(1, 5000);        
      
        m.add(confCreator);
        m_ensure.waitForStep(3, 5000);     
    }
    
    public interface MyConfig {
        public default String getFooBarParam1() { return "default value"; } // getFooBarParam1() -> foo.bar.param1
        public default String foo__param2() { return "default value2"; } // foo__param2() -> foo_param2
        public default String getParam3() { return "default value3"; } // getParam3() -> param3
        public default String foo__BaR_Param4() { return "default value4"; } // foo__BaR_Param4 -> foo_BaR.Param4
        public default String[] getFooBar5() { return new String[] {"default value1", "default value2"}; }
        public default List<String> getFooBar6() { return Arrays.asList("default value1", "default value2"); }
        public default SortedMap<String, String> getFooBar7() { TreeMap<String, String> defMap = new TreeMap<>(); defMap.put("key", "default"); return defMap; }
        public default SortedMap<String, String> getFooBar8() { TreeMap<String, String> defMap = new TreeMap<>(); defMap.put("key", "default"); return defMap; }
    }
    
    public class MyComponent {
        int step = 0;
        
        void updated(MyConfig cnf) {
            step ++;
            if (step == 1) {
            	TreeMap<String, String> defMap = new TreeMap<>(); defMap.put("key", "default");
                Assert.assertEquals("default value", cnf.getFooBarParam1());
                Assert.assertEquals("default value2", cnf.foo__param2());
                Assert.assertEquals("default value3", cnf.getParam3());
                Assert.assertEquals("default value4", cnf.foo__BaR_Param4());
                Assert.assertArrayEquals(new String[] { "default value1", "default value2"}, cnf.getFooBar5());
                Assert.assertEquals(Arrays.asList("default value1", "default value2"), cnf.getFooBar6());
                Assert.assertEquals(defMap, cnf.getFooBar7());
                Assert.assertEquals(defMap, cnf.getFooBar8());
                m_ensure.step(1);
            } else if (step == 2) {
            	TreeMap<String, String> map = new TreeMap<>(); map.put("key", "value");
            	Assert.assertEquals("bar", cnf.getFooBarParam1());
                Assert.assertEquals("bar2", cnf.foo__param2());
                Assert.assertEquals("bar3", cnf.getParam3());
                Assert.assertEquals("bar4", cnf.foo__BaR_Param4());
                Assert.assertArrayEquals(new String[] { "1", "2"}, cnf.getFooBar5());
                Assert.assertEquals(map, cnf.getFooBar7());
                Assert.assertEquals(map, cnf.getFooBar8());
                m_ensure.step(3);
            }
        }
    }

}
