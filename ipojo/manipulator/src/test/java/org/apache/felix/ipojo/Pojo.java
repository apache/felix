package org.apache.felix.ipojo;

/**
 * POJO Interface fake.
 * We're using a fake to avoid the cyclic build dependency:
 * manipulator -> ipojo -> maven-ipojo-plugin -> manipulator
 */
public interface Pojo {

}
