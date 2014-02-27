package dm;

import java.util.Dictionary;


/**
 * A component. Has dependencies. 
 */
public interface Component {
	public Component setImplementation(Object implementation);
	public Component add(Dependency ... dependencies);
	public Component remove(Dependency d);
	public Component add(ComponentStateListener l);
	public Component remove(ComponentStateListener l);
    public Component setInterface(String serviceName, Dictionary properties);
    public Component setInterface(String[] serviceNames, Dictionary properties);
    public Component setAutoConfig(Class clazz, boolean autoConfig);
    public Component setAutoConfig(Class clazz, String instanceName);
}
