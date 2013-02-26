package org.apache.felix.ipojo.runtime.core.test.components.proxy;

import org.apache.felix.ipojo.runtime.core.test.services.CheckService;

import java.util.AbstractMap;
import java.util.Properties;

public class CheckServiceUsingStringService implements CheckService {

    private String string;
    private AbstractMap map;


    public CheckServiceUsingStringService() {
        System.out.println("Service : " + string);
        System.out.println("Map : " + map);
    }

    public boolean check() {
        return string != null
                && map != null;
    }

    public Properties getProps() {
        return null;
    }

}
