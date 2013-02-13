package org.apache.felix.ipojo.runtime.core.test.components;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;

@Component
public class NullableDependency {

    @Requires(nullable=true)
    public FooService fs;
    
    @Requires(nullable=false)
    public FooService fs2;
    
  
}
