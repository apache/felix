package org.apache.felix.dependencymanager.samples.compositefactory;

import org.osgi.service.log.LogService;

/**
 * This is the main implementation for our "Provider" service.
 * This service is using a composition of two participants, which are used to provide the service
 * (ProviderParticipant1, and ProviderParticipant2).
 * 
 * This class is instantiated by the CompositionManager class.
 */
public class ProviderImpl implements Provider {
    private final ProviderParticipant1 m_participant1;
    private final ProviderParticipant2 m_participant2;
    
    private volatile LogService m_log; // Injected

    ProviderImpl(ProviderParticipant1 participant1, ProviderParticipant2 participant2) {
        m_participant1 = participant1;
        m_participant2 = participant2;
    }
    
    void start() {
        m_log.log(LogService.LOG_INFO, "ProviderImpl.start(): participants=" + m_participant1 + "," + m_participant2);
    }
}
