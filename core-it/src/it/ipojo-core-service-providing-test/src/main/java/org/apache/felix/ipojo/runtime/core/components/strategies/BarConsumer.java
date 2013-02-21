package org.apache.felix.ipojo.runtime.core.components.strategies;

import org.apache.felix.ipojo.runtime.core.services.BarService;
import org.apache.felix.ipojo.runtime.core.services.CheckService;

import java.util.Properties;

public class BarConsumer implements CheckService {
    
    private BarService bs;


    public boolean check() {
        return bs.bar();
    }

    public Properties getProps() {
        Properties props = bs.getProps();
        props.put("object", bs);
        return props;
    }

}
