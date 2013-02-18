package org.apache.felix.ipojo.core.tests.components;

import org.apache.felix.ipojo.core.tests.services.MyService;

public class MyCons {

    private MyService[] services;

    public MyCons() {
        System.out.println("Bound to " + services.length);
    }

}
