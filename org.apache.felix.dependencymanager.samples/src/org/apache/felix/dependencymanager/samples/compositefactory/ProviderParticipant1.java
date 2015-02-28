package org.apache.felix.dependencymanager.samples.compositefactory;

import org.osgi.service.log.LogService;

public class ProviderParticipant1 {
    private volatile LogService m_log; // Injected

    void start() {
        m_log.log(LogService.LOG_INFO, "ProviderParticipant1.start()");
    }
}
