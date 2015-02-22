package org.apache.felix.dm.runtime.itest.components;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;

public class ConfigurationDependencyAnnotation {
    @Component
    public static class ConfigurableComponent {
        public final static String ENSURE = "ConfigurableComponent";
        
        volatile Ensure m_ensure;
        volatile Dictionary<String, String> m_conf;

        @ConfigurationDependency
        void updated(Dictionary<String, String> conf) {
            m_conf = conf;
        }
        
        @ServiceDependency(filter="(name=" + ENSURE + ")")
        void bind(Ensure ensure) {
            m_ensure = ensure;
            Assert.assertNotNull(m_conf);
            Assert.assertEquals("bar", m_conf.get("foo"));
            m_ensure.step(1);
        }
        
        @Start
        void start() {
            m_ensure.step(2);
        }
        
        @Stop
        void stop() {
            m_ensure.step(3);
        }
    }
    
    @Component
    public static class ConfigurableComponentWithDynamicExtraConfiguration {
        public final static String ENSURE = "ConfigurableComponentWithDynamicExtraConfiguration";
        
        volatile Ensure m_ensure;
        volatile Dictionary<String, String> m_conf;

        @ConfigurationDependency
        void updated(Dictionary<String, String> conf) {
            m_conf = conf;
        }
        
        @ServiceDependency(filter="(name=" + ENSURE + ")")
        void bind(Ensure ensure) {
            m_ensure = ensure;
            Assert.assertNotNull(m_conf);
            Assert.assertEquals("bar", m_conf.get("foo"));
            m_ensure.step(1);
        }
        
        @Init
        Map<String, String> init() {
            Assert.assertNotNull(m_conf);
            String dynamicPid = m_conf.get("dynamicPid");
            Assert.assertNotNull(dynamicPid);
            Map<String, String> map = new HashMap<>();
            map.put("dynamicConfig.pid", m_conf.get("dynamicPid"));
            m_ensure.step(2);
            return map;
        }
        
        @ConfigurationDependency(name="dynamicConfig")
        void extraConfiguration(Dictionary<String, String> dynamicConf) {
            if (dynamicConf != null) {
                Assert.assertEquals("bar2", dynamicConf.get("foo2"));
                m_ensure.step(3);
            }
        }        
        
        @Start
        void start() {
            m_ensure.step(4);
        }
        
        @Stop
        void stop() {
            m_ensure.step(5);
        }
    }
}
