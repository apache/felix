package org.apache.felix.ipojo.runtime.core.components.strategies;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;

import java.util.Properties;

public class Consumer implements CheckService {
    
    private FooService fs;


    public boolean check() {
        return fs.foo();
    }

    public Properties getProps() {
        Properties props = fs.fooProps();
        props.put("object", fs);
        return props;
    }

}
