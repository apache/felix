package dm;

import org.osgi.framework.ServiceReference;

public interface ServiceDependency extends Dependency, ComponentDependencyDeclaration {
	public ServiceDependency setCallbacks(String add, String remove);
	public ServiceDependency setCallbacks(String add, String change, String remove);
    public ServiceDependency setCallbacks(String added, String changed, String removed, String swapped);
	public ServiceDependency setCallbacks(Object instance, String add, String remove);
	public ServiceDependency setCallbacks(Object instance, String add, String change, String remove);
    public ServiceDependency setCallbacks(Object instance, String added, String changed, String removed, String swapped);
	public ServiceDependency setRequired(boolean required);
	public ServiceDependency setAutoConfig(boolean autoConfig);
    public ServiceDependency setAutoConfig(String instanceName);

    public ServiceDependency setService(Class<?> serviceName);
    public ServiceDependency setService(Class<?> serviceName, String serviceFilter);
    public ServiceDependency setService(String serviceFilter);
    public ServiceDependency setService(Class<?> serviceName, ServiceReference serviceReference);
    public ServiceDependency setDefaultImplementation(Object implementation);
    public ServiceDependency setPropagate(boolean propagate);
    public ServiceDependency setPropagate(Object instance, String method);
}
