package org.apache.felix.cm.integration;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

@RunWith(JUnit4TestRunner.class)
public class FELIX2813_ConfigurationAdminStartupTest implements ServiceListener, ConfigurationListener {
    private volatile BundleContext m_context;

    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.2.8").noStart()
            )
        );
    }    

    @Test
    public void testAddConfigurationWhenConfigurationAdminStarts(BundleContext context) throws InvalidSyntaxException, BundleException {
        m_context = context;
        m_context.registerService(ConfigurationListener.class.getName(), this, null);
        m_context.addServiceListener(this, "(" + Constants.OBJECTCLASS + "=" + ConfigurationAdmin.class.getName() + ")");
        Bundle[] bundles = m_context.getBundles();
        for (Bundle b : bundles) {
            if (b.getSymbolicName().equals("org.apache.felix.configadmin")) {
                b.start();
            }
        }
        
        /*
         * Look at the console output for the following exception:
         * 
         * *ERROR* Unexpected problem executing task
         * java.lang.NullPointerException: reference and pid must not be null
         *     at org.osgi.service.cm.ConfigurationEvent.<init>(ConfigurationEvent.java:120)
         *     at org.apache.felix.cm.impl.ConfigurationManager$FireConfigurationEvent.run(ConfigurationManager.java:1818)
         *     at org.apache.felix.cm.impl.UpdateThread.run(UpdateThread.java:104)
         *     at java.lang.Thread.run(Thread.java:680)
         *     
         * It is in fact the service reference that is still null, because the service registration
         * has not been 'set' yet.
         */
    }

    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {
            ServiceReference ref = event.getServiceReference();
            ConfigurationAdmin ca = (ConfigurationAdmin) m_context.getService(ref);
            try {
                org.osgi.service.cm.Configuration config = ca.getConfiguration("test");
                config.update(new Properties() {{ put("abc", "123"); }});
            }
            catch (IOException e) {
            }
        }
    }

    public void configurationEvent(ConfigurationEvent event) {
    }
}
