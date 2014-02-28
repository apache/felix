package dm;

import org.osgi.framework.ServiceReference;

public interface ServiceDependency extends Dependency, ComponentDependencyDeclaration {
    public ServiceDependency setService(Class<?> serviceName);
    public ServiceDependency setService(Class<?> serviceName, String serviceFilter);
    public ServiceDependency setService(String serviceFilter);
    public ServiceDependency setService(Class<?> serviceName, ServiceReference serviceReference);
    public ServiceDependency setDefaultImplementation(Object implementation);
    public ServiceDependency setPropagate(boolean propagate);
    public ServiceDependency setPropagate(Object instance, String method);
    public ServiceDependency setDebug(String identifier);
}
