package dm;

import java.util.Dictionary;
import java.util.concurrent.Executor;

import org.osgi.framework.ServiceRegistration;


/**
 * A component. Has dependencies. 
 */
public interface Component {
	public Component setImplementation(Object implementation);
	public Component add(Dependency ... dependencies);
	public Component remove(Dependency d);
	public Component add(ComponentStateListener l);
	public Component remove(ComponentStateListener l);
    public Component setInterface(String serviceName, Dictionary<?,?> properties);
    public Component setInterface(String[] serviceNames, Dictionary<?,?> properties);
    public Component setAutoConfig(Class<?> clazz, boolean autoConfig);
    public Component setAutoConfig(Class<?> clazz, String instanceName);
    public ServiceRegistration getServiceRegistration();
    public Object getService(); // TODO do we really need this method (getInstances[0] returns the main component instance) ?
    public Object[] getInstances();
    public Dictionary getServiceProperties();
    public Component setServiceProperties(Dictionary<?,?> serviceProperties);
    public Component setCallbacks(String init, String start, String stop, String destroy);
    public Component setCallbacks(Object instance, String init, String start, String stop, String destroy);
    public Component setFactory(Object factory, String createMethod);
	public Component setFactory(String createMethod);
	public Component setComposition(Object instance, String getMethod);
	public Component setComposition(String getMethod);
	public DependencyManager getDependencyManager();
	public ComponentDeclaration getComponentDeclaration();
	public Component setExecutor(Executor executor);
}
