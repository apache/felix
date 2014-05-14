package dm.impl;

import java.util.Dictionary;

import org.osgi.framework.BundleContext;

import dm.ComponentDependencyDeclaration;
import dm.Dependency;
import dm.context.ComponentContext;
import dm.context.DependencyContext;
import dm.context.Event;

public class DependencyImpl<T extends Dependency> implements Dependency, DependencyContext {
	protected volatile ComponentContext m_component;
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
    protected volatile boolean m_propagate;
    protected volatile Object m_propagateCallbackInstance;
    protected volatile String m_propagateCallbackMethod;
    protected final BundleContext m_context;

	public DependencyImpl() {	
        this(true, null);
	}

	public DependencyImpl(boolean autoConfig, BundleContext bc) {	
        m_autoConfig = autoConfig;
        m_context = bc;
	}
	
	public DependencyImpl(DependencyImpl<T> prototype) {
		m_component = prototype.m_component;
		m_instanceBound = prototype.m_instanceBound;
		m_required = prototype.m_required;
		m_add = prototype.m_add;
		m_change = prototype.m_change;
		m_remove = prototype.m_remove;
		m_autoConfig = prototype.m_autoConfig;
		m_autoConfigInstance = prototype.m_autoConfigInstance;
		m_autoConfigInvoked = prototype.m_autoConfigInvoked;
		m_callbackInstance = prototype.m_callbackInstance;
        m_propagate = prototype.m_propagate;
        m_propagateCallbackInstance = prototype.m_propagateCallbackInstance;
        m_propagateCallbackMethod = prototype.m_propagateCallbackMethod;
        m_context = prototype.m_context;
	}
	
	public void add(final Event e) {
		// since this method can be invoked by anyone from any thread, we need to
		// pass on the event to a runnable that we execute using the component's
		// executor
		m_component.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
			    m_component.handleAdded(DependencyImpl.this, e);
			}
		});
	}
	public void change(final Event e) {
		// since this method can be invoked by anyone from any thread, we need to
		// pass on the event to a runnable that we execute using the component's
		// executor
		m_component.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
                m_component.handleChanged(DependencyImpl.this, e);
			}
		});
	}
	
	public void remove(final Event e) {
		// since this method can be invoked by anyone from any thread, we need to
		// pass on the event to a runnable that we execute using the component's
		// executor
		m_component.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
			    try {
			        m_component.handleRemoved(DependencyImpl.this, e);	
			    } finally {
			        e.close(m_context);
			    }
			}
		});
	}

	@Override
	public void add(ComponentContext component) {
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
	
    public T setPropagate(boolean propagate) {
        ensureNotActive();
        m_propagate = propagate;
        return (T) this;
    }

    public T setPropagate(Object instance, String method) {
        setPropagate(instance != null && method != null);
        m_propagateCallbackInstance = instance;
        m_propagateCallbackMethod = method;
        return (T) this;
    }
    
	@Override
	public void setAvailable(boolean available) {
	    m_available = available;
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
	
	public T setCallbacks(String add, String remove) {
	    return setCallbacks(add, null, remove);
	}
	
	public T setCallbacks(String add, String change, String remove) {
		return setCallbacks(null, add, change, remove);		
	}
	
	public T setCallbacks(Object instance, String add, String remove) {
		return setCallbacks(instance, add, null, remove);
	}
	
	public T setCallbacks(Object instance, String add, String change, String remove) {
        if ((add != null || change != null || remove != null) && !m_autoConfigInvoked) {
            setAutoConfig(false);
        }
        m_callbackInstance = instance;
		m_add = add;
		m_change = change;
		m_remove = remove;
		return (T) this;
	}
	
	@Override
	public void invokeAdd(Event e) {
		if (m_add != null) {
		    invoke(m_add, e);
		}
	}
	
    @Override	
	public void invokeChange(Event e) {
		if (m_change != null) {
		    invoke(m_change, e);
		}
	}
	
    @Override   
    public void invokeRemove(Event e) {
		if (m_remove != null) {
		    invoke(m_remove, e);
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
	
	public T setRequired(boolean required) {
		m_required = required;
		return (T) this;
	}
	
    @Override
    public boolean needsInstance() {
        return false;
    }
    
    @Override
    public Class<?> getAutoConfigType() {
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
    
    public T setAutoConfig(boolean autoConfig) {
        m_autoConfig = autoConfig;
        m_autoConfigInvoked = true;
        return (T) this;
    }
    
    public T setAutoConfig(String instanceName) {
        m_autoConfig = (instanceName != null);
        m_autoConfigInstance = instanceName;
        m_autoConfigInvoked = true;
        return (T) this;
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
    
    public boolean isStarted() {
    	return m_isStarted;
    }

	@Override
	public boolean isPropagated() {
		return m_propagate;
	}

	@Override
	public Dictionary getProperties() {
		// specific for this type of dependency
		return null;
	}
	
    protected void ensureNotActive() {
        if (isStarted()) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
}
