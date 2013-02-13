package org.apache.felix.ipojo.runtime.core.test.components;

import java.util.Properties;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.runtime.core.test.services.BarService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.apache.felix.ipojo.runtime.core.test.services.BarService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;

@Component
@Provides(specifications= {FooService.class, BarService.class})
public class PSServiceController implements FooService, BarService {

    @ServiceController(value=false)
    public boolean controller;
    
    public boolean foo() {
        return false;
    }

    public Properties fooProps() {
        return null;
    }

    public boolean getBoolean() {
        return false;
    }

    public double getDouble() {
        return 0;
    }

    public int getInt() {
        return 0;
    }

    public long getLong() {
        return 0;
    }

    public Boolean getObject() {
        return null;
    }

    public boolean bar() {
        return false;
    }

    public Properties getProps() {
        return null;
    }

}
