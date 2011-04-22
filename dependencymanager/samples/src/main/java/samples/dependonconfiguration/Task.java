package samples.dependonconfiguration;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class Task implements ManagedService {
    private String m_interval;

    public void updated(Dictionary properties) throws ConfigurationException {
        if (properties != null) {
            m_interval = (String) properties.get("interval");
            if (m_interval == null) {
                throw new ConfigurationException("interval", "must be specified");
            }
            System.out.println("Task.updated: properties=" + properties);
        }
    }

    public void start() {
        System.out.println("Task.start");
    }

    public void execute() {
        System.out.println("Scheduling task with interval " + m_interval);
    }
}
