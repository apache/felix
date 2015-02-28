package org.apache.felix.dependencymanager.samples.hello.api;

import java.util.Dictionary;

import org.osgi.service.log.LogService;

/**
 * Our service consumer. We depend on a ServiceProvider, and on a configuration.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceConsumer {
    volatile ServiceProvider service;
    volatile LogService log;
    Dictionary<?, ?> conf;

    protected void update(Dictionary<?, ?> conf) {
        this.conf = conf;
    }

    public void start() {
        log.log(LogService.LOG_INFO, "ServiceConsumer.start: calling service.hello()");
        this.service.hello();
    }
}
