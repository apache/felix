package org.apache.felix.ipojo.runtime.core.test.components.temporal;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handler.temporal.Requires;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;

@Component
public class TemporalWithNull {
    
    @Requires(onTimeout="null")
    private FooService fs;
    
}
