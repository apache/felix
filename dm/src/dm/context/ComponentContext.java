package dm.context;

import java.util.List;

import dm.ComponentState;
import dm.impl.SerialExecutor;

public interface ComponentContext {
    public SerialExecutor getExecutor(); // shared between a component and its dependencies
    public void start();
    public void stop();
    public boolean isAvailable();
    public ComponentState getComponentState();
    public void handleChange();
    public List<DependencyContext> getDependencies(); // for testing only...
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters);
    public Object[] getInstances();
    public void updateInstance(DependencyContext dependency);
    public String getAutoConfigInstance(Class<?> clazz);
    public boolean getAutoConfig(Class<?> clazz);
}
