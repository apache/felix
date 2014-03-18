package dm.context;

import java.util.Dictionary;


public interface DependencyContext {
	public void invokeAdd(Event e);
	public void invokeChange(Event e);
	public void invokeRemove(Event e);
	/** Whenever the dependency changes state, this method is invoked with the Event containing the new state information. */
	public void add(final Event e);
	public void change(final Event e);
	public void remove(final Event e);
	public void add(ComponentContext component);
	public void remove(ComponentContext component);
	/** Invoked by the component when the dependency should start working. */
	public void start();
	/** Invoked by the component when the dependency should stop working. */
	public void stop();
	
	public boolean isAvailable();
	public void setAvailable(boolean available);
	
	public boolean isRequired();
	
	public boolean isInstanceBound();
	public void setInstanceBound(boolean instanceBound);
	
	/** Does this dependency need the component instances to determine if the dependency is available or not */
	public boolean needsInstance();
	
    public Class getAutoConfigType();
    public Object getAutoConfigInstance();
    public boolean isAutoConfig();
    public String getAutoConfigName();
    public DependencyContext createCopy();
    public boolean isPropagated();
    public Dictionary getProperties();
}
