package org.apache.felix.ipojo.handler.eventadmin.test;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;


public class SimpleTest extends Common {

    @Inject
    BundleContext bc;

    @Test
    public void testSomethingStupid() {
        System.out.println("This is fucking weird");
        for (Bundle bundle : bc.getBundles()) {
            System.out.println(bundle.getSymbolicName() + " " + bundle.getState());
        }
    }
}
