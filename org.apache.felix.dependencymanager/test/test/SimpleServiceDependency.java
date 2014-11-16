package test;

import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;

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
    
    @Override
    public void invokeAdd(Event e) {
        if (m_add != null) {
            invoke (m_add, e, getInstances());
        }
    }
    
    @Override
    public void invokeChange(Event e) {
        if (m_change != null) {
            invoke (m_change, e, getInstances());
        }
    }

    @Override
    public void invokeRemove(Event e) {
        if (m_remove != null) {
            invoke (m_remove, e, getInstances());
        }
    }

    public void invoke(String method, Event e, Object[] instances) {
        // specific for this type of dependency
        m_component.invokeCallbackMethod(instances, method, new Class[][] { {} }, new Object[][] { {} });
    }

    public void add(final Event e) {
        m_component.handleAdded(this, e);
    }
    
    public void change(final Event e) {
        m_component.handleChanged(this, e);
    }

    public void remove(final Event e) {
        m_component.handleRemoved(this, e);
    }
    
    public void swap(final Event event, final Event newEvent) {
        m_component.handleSwapped(this, event, newEvent);
    }
}
