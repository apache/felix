package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.CheckService;

import java.util.Properties;

public class NoEmptyConstructor implements CheckService {
    
   
        private String name;

        public NoEmptyConstructor(final String n) {
            name = n;
        }

        public boolean check() {
            return name != null;
        }

        public Properties getProps() {
            Properties props = new Properties();
            if (name == null) {
                props.put("name", "NULL");
            } else {
                props.put("name", name);
            }
            return props;
        }


}
