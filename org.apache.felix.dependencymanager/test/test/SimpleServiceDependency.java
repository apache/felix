package test;

import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;

public class SimpleServiceDependency extends AbstractDependency<Dependency> {
    @Override
    public String getType() {
        return "SimpleServiceDependency";
    }

    @Override
    public String getSimpleName() {
        return "SimpleServiceDependency";
    }

    @Override
    public DependencyContext createCopy() {
        return new SimpleServiceDependency();
    }
    
    @Override
    public void invokeCallback(EventType type, Event ... e) {
        switch (type) {
        case ADDED:
            if (m_add != null) {
                invoke (m_add, e[0], getInstances());
            }
            break;
        case CHANGED:
            if (m_change != null) {
                invoke (m_change, e[0], getInstances());
            }
            break;
        case REMOVED:
            if (m_remove != null) {
                invoke (m_remove, e[0], getInstances());
            }
            break;
        default:
            break;
        }
    }

    public void invoke(String method, Event e, Object[] instances) {
        // specific for this type of dependency
        m_component.invokeCallbackMethod(instances, method, new Class[][] { {} }, new Object[][] { {} });
    }

    public void add(final Event e) {
        m_component.handleEvent(this, EventType.ADDED, e);
    }
    
    public void change(final Event e) {
        m_component.handleEvent(this, EventType.CHANGED, e);
    }

    public void remove(final Event e) {
        m_component.handleEvent(this, EventType.REMOVED, e);
    }
    
    public void swap(final Event event, final Event newEvent) {
        m_component.handleEvent(this, EventType.SWAPPED, event, newEvent);
    }

    @Override
    public Class<?> getAutoConfigType() {
        return null; // we don't support auto config mode.
    }
}
