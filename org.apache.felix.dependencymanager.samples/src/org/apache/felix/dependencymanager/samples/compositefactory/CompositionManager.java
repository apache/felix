package org.apache.felix.dependencymanager.samples.compositefactory;

import java.util.Dictionary;

/**
 * Pojo used to create all the objects composition used to implements the "Provider" Service.
 * The manager is using a Configuration injected by Config Admin, in order to configure the 
 * various objects being part of the "Provider" service implementation.
 */
public class CompositionManager {
    private ProviderParticipant1 m_participant1;
    private ProviderParticipant2 m_participant2;
    private ProviderImpl m_providerImpl;
    private Dictionary<String, String> m_conf;

    public void updated(Dictionary<String, String> conf) throws Exception {
        // validate configuration and throw an exception if the properties are invalid
        m_conf = conf;
    }

    /**
     * Builds the composition of objects used to implement the "Provider" service.
     * The Configuration injected by Config Admin will be used to configure the components
     * @return The "main" object providing the "Provider" service.
     */
    Object create() {
        // Here, we can instantiate our object composition and configure them using the injected Configuration ...
        m_participant1 = new ProviderParticipant1(); // possibly configure this object using our configuration
        m_participant2 = new ProviderParticipant2(); // possibly configure this object using our configuration
        m_providerImpl = new ProviderImpl(m_participant1, m_participant2);
        return m_providerImpl; // Main object implementing the Provider service
    }

    Object[] getComposition() {
        return new Object[] { m_providerImpl, m_participant1, m_participant2 };
    }
}
