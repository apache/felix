package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Component;

@Component(factory_method="create")
public class FactoryMethodDeprecated {
    
    public static FactoryMethodDeprecated create() {
        return new FactoryMethodDeprecated();
    }
}
