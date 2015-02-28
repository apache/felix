package org.apache.felix.dependencymanager.samples.composite;

import java.util.Dictionary;

import org.osgi.service.log.LogService;

/**
 * This is the main implementation for our "Provider" service.
 * This service is using a composition of two participants, which are used to provide the service
 * (ProviderParticipant1, and ProviderParticipant2).
 */
public class ProviderImpl implements Provider {
    private final ProviderParticipant1 m_participant1 = new ProviderParticipant1();
    private final ProviderParticipant2 m_participant2 = new ProviderParticipant2();
    private volatile LogService m_log;
    private Dictionary<String, String> m_conf;

    public void updated(Dictionary<String, String> conf) throws Exception {
        // validate configuration and throw an exception if the properties are invalid
        m_conf = conf;
    }

    Object[] getComposition() {
        return new Object[] { this, m_participant1, m_participant2 };
    }

    void start() {
        m_log.log(LogService.LOG_INFO, "ProviderImpl.start(): participants=" + m_participant1 + "," + m_participant2
            + ", conf=" + m_conf);
    }
}
