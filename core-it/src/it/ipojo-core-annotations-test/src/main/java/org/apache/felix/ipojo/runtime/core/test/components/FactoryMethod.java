package org.apache.felix.ipojo.runtime.core.test.components;

import org.apache.felix.ipojo.annotations.Component;

@Component(factoryMethod="create")
public class FactoryMethod {

    public static FactoryMethod create() {
        return new FactoryMethod();
    }
}
