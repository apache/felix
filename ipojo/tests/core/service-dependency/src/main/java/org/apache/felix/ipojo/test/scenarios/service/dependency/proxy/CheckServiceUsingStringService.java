package org.apache.felix.ipojo.test.scenarios.service.dependency.proxy;

import java.util.AbstractMap;
import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;

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
