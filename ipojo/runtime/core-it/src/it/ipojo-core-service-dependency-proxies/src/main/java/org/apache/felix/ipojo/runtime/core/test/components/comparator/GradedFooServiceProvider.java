package org.apache.felix.ipojo.runtime.core.test.components.comparator;

import org.apache.felix.ipojo.runtime.core.test.services.FooService;

import java.util.Properties;

public class GradedFooServiceProvider implements FooService {
    
    
    private int grade;

    public boolean foo() {
        return grade > 0;
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
        return grade;
    }

    public long getLong() {
        return 0;
    }

    public Boolean getObject() {
        return null;
    }

}
