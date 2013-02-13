package org.apache.felix.ipojo.runtime.core.test.components.temporal;

import java.util.Collection;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handler.temporal.Requires;

@Component
public class TemporalCollection {
    
    @Requires(specification="org.apache.felix.ipojo.runtime.core.test.services.FooService")
    private Collection fs1;
    
    @Requires(specification="org.apache.felix.ipojo.runtime.core.test.services.FooService", timeout=300)
    private Collection fs2;
    
    @Requires(onTimeout="empty", specification="org.apache.felix.ipojo.runtime.core.test.services.FooService")
    private Collection fs3;
    
    @Requires(proxy=true, specification="org.apache.felix.ipojo.runtime.core.test.services.FooService")
    private Collection fs4;
    
    
    
}
