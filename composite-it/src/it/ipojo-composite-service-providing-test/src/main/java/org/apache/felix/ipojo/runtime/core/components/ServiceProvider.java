package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.Service;


public class ServiceProvider implements Service {

    private int i = 0;

    public int count() {
        i++;
        return i;
    }

}
