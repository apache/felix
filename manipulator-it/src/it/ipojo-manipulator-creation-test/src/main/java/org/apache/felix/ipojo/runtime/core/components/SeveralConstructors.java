package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.CheckService;

import java.util.Properties;

public class SeveralConstructors implements CheckService {
    
   
        private String name;

        public SeveralConstructors(){
            this("hello world");
        }

        public SeveralConstructors(final String n) {
            name = n;
        }

        public boolean check() {
            return name != null;
        }

        public Properties getProps() {
            Properties props = new Properties();
            props.put("name", name);
            return props;
        }


}
