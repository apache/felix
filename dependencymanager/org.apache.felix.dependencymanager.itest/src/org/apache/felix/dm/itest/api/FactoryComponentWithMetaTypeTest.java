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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.function.Consumer;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.FactoryComponent;
import org.apache.felix.dm.PropertyMetaData;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Tests an Adapter which adapts A To B interface.
 * And the Adapter also depends on a Configuration Dependency with MetaType support.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FactoryComponentWithMetaTypeTest extends TestBase {
    final static String PID = "FactoryComponentWithMetaType";
    final static String PID_HEADING = "English Dictionary";
    final static String PID_DESC = "Configuration for the EnglishDictionary Service";
    final static String WORDS_HEADING = "English words";
    final static String WORDS_DESC = "Declare here some valid English words";
    final static String WORDS_PROPERTY = "words";
    
    public void testFactoryComponentWithMetaType() {
        DependencyManager m = getDM();
        Ensure e = new Ensure();
                
        FactoryComponent factoryComponent = m.createFactoryComponent()
            .setFactoryPid(PID)
            .setHeading(PID_HEADING)
            .setDesc(PID_DESC)
            .setImplementation(new AImpl(e))
            .setInterface(A.class.getName(), null)
            .add(m.createPropertyMetaData()
                .setCardinality(Integer.MAX_VALUE)
                .setType(String.class)
                .setHeading(WORDS_HEADING)
                .setDescription(WORDS_DESC)
                .setDefaults(new String[] {"hello", "world"})
                .setId(WORDS_PROPERTY));

        m.add(factoryComponent);
              
        Component configurator = m.createComponent()
            .setImplementation(new Configurator(e))
            .add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true))
            .add(m.createServiceDependency().setService(MetaTypeService.class).setRequired(true));
        m.add(configurator);
        
        // Ensures that all components are started
        e.waitForStep(4, 5000);
        
        // now stop configurator, and ensure that all components have been stopped
        m.remove(configurator);
        e.waitForStep(7, 5000);
        m.clear();
    }
    
    public interface A {
    }

    public static class AImpl implements A {
        Dictionary<String, String> m_conf;
        final Ensure m_ensure;
        
        public AImpl(Ensure e) {
            m_ensure = e;
        }
        
        public void updated(Dictionary<String, String> conf) {
            if (conf != null) {
                m_ensure.step(3);
                m_conf = conf;
            }
        }

        public void start() {
            Assert.assertNotNull(m_conf);
            Assert.assertEquals("bar", m_conf.get("foo"));
            m_ensure.step(4);
        }
        
        public void stop() {
            m_ensure.step(7);
        }
    }
    
    public class Configurator {
        volatile MetaTypeService m_metaType;
        volatile ConfigurationAdmin m_cm;
        volatile BundleContext m_ctx;
        final Ensure m_ensure;
        Configuration m_conf;

        Configurator(Ensure ensure) {
            m_ensure = ensure;
        }

        void start() {
            m_ensure.step(1);
            checkMetaTypeAndConfigure();
        }        

        void stop() {
            m_ensure.step(5);
            if (m_conf != null) {
                try {
                    m_ensure.step(6);
                    m_conf.delete();
                }
                catch (IOException e) {
                    m_ensure.throwable(e);
                }
            }
        }
        
        void checkMetaTypeAndConfigure() {
            MetaTypeInformation info = m_metaType.getMetaTypeInformation(m_ctx.getBundle());
            Assert.assertNotNull(info);
            Assert.assertEquals(PID, info.getFactoryPids()[0]);
            ObjectClassDefinition ocd = info.getObjectClassDefinition(PID, null);
            Assert.assertNotNull(ocd);
            Assert.assertEquals(PID_HEADING, ocd.getName());
            Assert.assertEquals(PID_DESC, ocd.getDescription());
            AttributeDefinition[] defs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
            Assert.assertNotNull(defs);
            Assert.assertEquals(1, defs.length);
            Assert.assertEquals(WORDS_HEADING, defs[0].getName());
            Assert.assertEquals(WORDS_DESC, defs[0].getDescription());
            Assert.assertEquals(WORDS_PROPERTY, defs[0].getID());
            m_ensure.step(2);
            
            try {
                m_conf = m_cm.createFactoryConfiguration(PID, null);
                Hashtable<String, String> props = new Hashtable<>();
                props.put("foo", "bar");
                m_conf.update(props);                
            } catch (Throwable t) {
                m_ensure.throwable(t);
            }
        }
    }

}
