package org.apache.felix.dependencymanager.samples.hello.annot;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.osgi.service.log.LogService;

/**
 * The implementation for our service provider.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class ServiceProviderImpl implements ServiceProvider {
    @ServiceDependency
    volatile LogService log;

    @Override
    public void hello() {
        log.log(LogService.LOG_INFO, "ServiceProviderImpl.hello");
    }
}
