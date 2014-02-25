package dm;

import org.osgi.framework.ServiceReference;

public interface ServiceDependency extends Dependency {
    public ServiceDependency setService(Class serviceName);
    public ServiceDependency setService(Class serviceName, String serviceFilter);
    public ServiceDependency setService(String serviceFilter);
    public ServiceDependency setService(Class serviceName, ServiceReference serviceReference);
    public ServiceDependency setDefaultImplementation(Object implementation);
}
