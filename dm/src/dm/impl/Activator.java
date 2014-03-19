package dm.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import dm.admin.ComponentAdmin;

/**
 * DependencyManager activator, used to register the DM Admin Service.
 */
public class Activator implements BundleActivator {
    // the service registration of the DependencyManager Admin Service.
    private volatile ServiceRegistration m_adminReg;

    public void start(BundleContext context) throws Exception {
        m_adminReg = context.registerService(ComponentAdmin.class.getName(), new ComponentAdminImpl(), null);
    }

    public void stop(BundleContext context) throws Exception {
        final ServiceRegistration adminReg = m_adminReg;
        m_adminReg = null;
        if (adminReg != null) {
            adminReg.unregister();
        }
    }
}
