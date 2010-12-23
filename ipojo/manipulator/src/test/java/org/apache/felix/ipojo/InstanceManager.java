package org.apache.felix.ipojo;

import java.util.Set;

/**
 * Instance Manager Fake.
 * We're using a fake to avoid the cyclic build dependency:
 * manipulator -> ipojo -> maven-ipojo-plugin -> manipulator
 */
public class InstanceManager {

    public Set getRegistredFields() {
        return null;
    }

    public Set getRegistredMethods() {
        return null;
    }

}
