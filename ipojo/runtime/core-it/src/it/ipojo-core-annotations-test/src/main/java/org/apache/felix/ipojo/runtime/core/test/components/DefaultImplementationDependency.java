package org.apache.felix.ipojo.runtime.core.test.components;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;

@Component
public class DefaultImplementationDependency {

    @Requires(defaultimplementation=ProvidesSimple.class, optional=true)
    public FooService fs;
}
