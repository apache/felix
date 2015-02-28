package org.apache.felix.dependencymanager.samples.hello.annot;

import java.util.Dictionary;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.log.LogService;

/**
 * Our service consumer. We depend on a ServiceProvider, and on a configuration.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class ServiceConsumer {
    @ServiceDependency
    volatile ServiceProvider service;

    @ServiceDependency
    volatile LogService log;

    Dictionary<?, ?> conf;

    @ConfigurationDependency
    protected void update(Dictionary<?, ?> conf) {
        this.conf = conf;
    }

    @Start
    public void start() {
        log.log(LogService.LOG_INFO, "ServiceConsumer.start: calling service.hello() ...");
        this.service.hello();
    }
}
