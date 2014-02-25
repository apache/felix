package dm.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;

import dm.Dependency;
import dm.context.ComponentContext;
import dm.context.DependencyContext;
import dm.context.Event;

public class DependencyImpl implements Dependency, DependencyContext {
	protected ComponentContext m_component;
	protected boolean m_available;
	protected boolean m_instanceBound;
	protected boolean m_required;
	protected String m_add;
	protected String m_change;
	protected String m_remove;
	protected boolean m_autoConfig;
	protected String m_autoConfigInstance;
	protected boolean m_autoConfigInvoked;

	// TODO when we start injecting the "highest" one, this needs to be sorted at
	// some point in time (note that we could choose to only do that if the dependency is
	// actually injected (auto config is on for it)
	protected final ConcurrentSkipListSet<Event> m_dependencies = new ConcurrentSkipListSet();
	
	public synchronized void add(final Event e) {
		// since this method can be invoked by anyone from any thread, we need to
		// pass on the event to a runnable that we execute using the component's
		// executor
		m_component.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				addDependency(e);
			}
		});
	}
	public synchronized void change(final Event e) {
		// since this method can be invoked by anyone from any thread, we need to
		// pass on the event to a runnable that we execute using the component's
		// executor
		m_component.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				changeDependency(e);
			}
		});
	}
	public synchronized void remove(final Event e) {
		// since this method can be invoked by anyone from any thread, we need to
		// pass on the event to a runnable that we execute using the component's
		// executor
		m_component.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				removeDependency(e);
			}
		});
	}

	protected void addDependency(Event e) {
		m_dependencies.add(e);
		m_available = true;
		// if this is an optional dependency and the component is in an instantiated
		// state, we can invoke the callback here
		if (m_component.isAvailable()) {
			invoke(m_add, e);
		}
		m_component.handleChange();
	}

	protected void changeDependency(Event e) {
		m_dependencies.remove(e);
		m_dependencies.add(e);
		if (m_component.isAvailable()) {
			invoke(m_change, e);
		}
		m_component.handleChange();
	}

	protected void removeDependency(Event e) {
		m_dependencies.remove(e);
		m_available = (!m_dependencies.isEmpty());
		if (m_component.isAvailable()) {
			invoke(m_remove, e);
		}
		m_component.handleChange();
	}

	@Override
	public synchronized void add(ComponentContext component) {
		m_component = component;
	}
	
	@Override
	public void remove(ComponentContext component) {
		m_component = null;
	}

	@Override
	public void start() {
		// you would normally start tracking this dependency here, so for example
		// for a service dependency you might start a service tracker here
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isAvailable() {
		return m_available;
	}
	
	@Override
	public boolean isRequired() {
		return m_required;
	}
	public boolean isInstanceBound() {
		return m_instanceBound;
	}
	public void setInstanceBound(boolean instanceBound) {
		m_instanceBound = instanceBound;
	}
	
	public Dependency setCallbacks(String add, String remove) {
	    return setCallbacks(add, null, remove);
	}
	
	public Dependency setCallbacks(String add, String change, String remove) {
        if ((add != null || change != null || remove != null) && !m_autoConfigInvoked) {
            setAutoConfig(false);
        }
		m_add = add;
		m_change = change;
		m_remove = remove;
		return this;
	}
	
	public void invokeAdd() {
		if (m_add != null) {
			for (Event e : m_dependencies) {
				invoke(m_add, e);
			}
		}
	}
	
	public void invokeChange() {
		if (m_change != null) {
			for (Event e : m_dependencies) {
				invoke(m_change, e);
			}
		}
	}
	
	public void invokeRemove() {
		if (m_remove != null) {
			for (Event e : m_dependencies) {
				invoke(m_remove, e);
			}
		}
	}
	
	public Object[] getInstances() {
		// TODO what instance(s) should be called should be configurable
		return m_component.getInstances();
	}

	public void invoke(String method, Event e) {
		// specific for this type of dependency
		m_component.invokeCallbackMethod(getInstances(), method, new Class[][] {{}}, new Object[][] {{}});
	}
	
	@Override
	public Dependency setRequired(boolean required) {
		m_required = required;
		return this;
	}
	
    @Override
    public boolean needsInstance() {
        return false;
    }
    
    @Override
    public Class getAutoConfigType() {
        return null; // must be implemented by subclasses
    }
    
    @Override
    public Object getAutoConfigInstance() {
        return getService();
    }
    
    @Override
    public boolean isAutoConfig() {
        return m_autoConfig;
    }
    
    @Override
    public String getAutoConfigName() {
        return m_autoConfigInstance;
    }
    
    @Override
    public Dependency setAutoConfig(boolean autoConfig) {
        m_autoConfig = autoConfig;
        m_autoConfigInvoked = true;
        return this;
    }
    
    @Override
    public Dependency setAutoConfig(String instanceName) {
        m_autoConfig = (instanceName != null);
        m_autoConfigInstance = instanceName;
        m_autoConfigInvoked = true;
        return this;
    }
    
    protected Object getService() {
        // only real dependencies can return actual service.
        return null;
    }
}
