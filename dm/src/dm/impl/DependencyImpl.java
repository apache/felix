package dm.impl;

import java.util.Dictionary;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;

import dm.Dependency;
import dm.admin.ComponentDependencyDeclaration;
import dm.context.ComponentContext;
import dm.context.DependencyContext;
import dm.context.Event;

public class DependencyImpl implements Dependency, DependencyContext {
	protected ComponentContext m_component;
	protected volatile boolean m_available; // volatile because accessed by getState method
	protected boolean m_instanceBound;
	protected volatile boolean m_required; // volatile because accessed by getState method
	protected String m_add;
	protected String m_change;
	protected String m_remove;
	protected boolean m_autoConfig;
	protected String m_autoConfigInstance;
	protected boolean m_autoConfigInvoked;
    private volatile boolean m_isStarted; // volatile because accessed by getState method
    private Object m_callbackInstance;
    
	// TODO when we start injecting the "highest" one, this needs to be sorted at
	// some point in time (note that we could choose to only do that if the dependency is
	// actually injected (auto config is on for it)
	protected final ConcurrentSkipListSet<Event> m_dependencies = new ConcurrentSkipListSet();
	
	public DependencyImpl() {	
        this(true);
	}

	public DependencyImpl(boolean autoConfig) {	
        m_autoConfig = autoConfig;
	}
	
	public DependencyImpl(DependencyImpl prototype) {
		m_component = prototype.m_component;
		m_available = prototype.m_available;
		m_instanceBound = prototype.m_instanceBound;
		m_required = prototype.m_required;
		m_add = prototype.m_add;
		m_change = prototype.m_change;
		m_remove = prototype.m_remove;
		m_autoConfig = prototype.m_autoConfig;
		m_autoConfigInstance = prototype.m_autoConfigInstance;
		m_autoConfigInvoked = prototype.m_autoConfigInvoked;
		m_callbackInstance = prototype.m_callbackInstance;
	}
	
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
			if (m_add != null) {
				invoke(m_add, e);
			}
			m_component.updateInstance(this);
		}
		if (isRequired()) {
			// Only required dependencies may change state. 
			m_component.handleChange();
		}
	}

	protected void changeDependency(Event e) {	    
		m_dependencies.remove(e);
		m_dependencies.add(e);
        if (m_change != null && m_component.isInstantiated()) {
            // invoke change only if state is in instantiated_waiting_for_required or tracking_optional
            invoke(m_change, e);
        } 
		if (m_component.isAvailable()) {
			m_component.updateInstance(this);
		}
		m_component.handleChange();
	}
		
	protected void removeDependency(Event e) {
		m_available = !(m_dependencies.contains(e) && m_dependencies.size() == 1);
		m_component.handleChange();
		m_dependencies.remove(e);
		if (m_component.isAvailable()) {
			if (m_remove != null) {
				invoke(m_remove, e);
			}
			m_component.updateInstance(this);
		}
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
        m_isStarted = true;
		// you would normally start tracking this dependency here, so for example
		// for a service dependency you might start a service tracker here
	}

	@Override
	public void stop() {
		m_isStarted = false;
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
		return setCallbacks(null, add, change, remove);		
	}
	
	public Dependency setCallbacks(Object instance, String add, String remove) {
		return setCallbacks(instance, add, null, remove);
	}
	
	public Dependency setCallbacks(Object instance, String add, String change, String remove) {
        if ((add != null || change != null || remove != null) && !m_autoConfigInvoked) {
            setAutoConfig(false);
        }
        m_callbackInstance = instance;
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
        if (m_callbackInstance == null) {
        	return m_component.getInstances();
        } else {
            return new Object[]{m_callbackInstance};
        }
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
    
    @Override
    public DependencyContext createCopy() {
        return new DependencyImpl(this);
    }
    
    public int getState() { // Can be called from any threads, but our class attributes are volatile
    	if (m_isStarted) {
    		return (isAvailable() ? 1 : 0) + (isRequired() ? 2 : 0);
    	}
    	else {
    		return isRequired() ? ComponentDependencyDeclaration.STATE_REQUIRED : ComponentDependencyDeclaration.STATE_OPTIONAL;
    	}
    }
   
    protected Object getService() {
        // only real dependencies can return actual service.
        return null;
    }
    
    protected boolean isStarted() {
    	return m_isStarted;
    }

	@Override
	public boolean isPropagated() {
		// specific for this type of dependency
		return false;
	}

	@Override
	public Dictionary getProperties() {
		// specific for this type of dependency
		return null;
	}
}
