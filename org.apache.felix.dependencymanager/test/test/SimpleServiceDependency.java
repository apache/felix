package test;

import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;

public class SimpleServiceDependency extends AbstractDependency<Dependency> {
    @Override
    public String getType() {
        return "SimpleServiceDependency";
    }

    @Override
    public String getName() {
        return "SimpleServiceDependency";
    }

    @Override
    public DependencyContext createCopy() {
        return new SimpleServiceDependency();
    }
}
