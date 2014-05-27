package dm.context;

import java.util.List;
import java.util.concurrent.Executor;

//TODO should this interface extend Component ?
public interface ComponentContext {
    public Executor getExecutor(); // shared between a component and its dependencies
    public void start();
    public void stop();
    public boolean isAvailable();
    public void handleAdded(DependencyContext dc, Event e);
    public void handleChanged(DependencyContext dc, Event e);
    public void handleRemoved(DependencyContext dc, Event e);
    public void handleSwapped(DependencyContext dc, Event event, Event newEvent);
    public List<DependencyContext> getDependencies(); // for testing only...
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters);
    public Object[] getInstances();
    public String getAutoConfigInstance(Class<?> clazz);
    public boolean getAutoConfig(Class<?> clazz);
    public Event getDependencyEvent(DependencyContext dc);
}
