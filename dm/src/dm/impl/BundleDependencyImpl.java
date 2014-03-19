package dm.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.log.LogService;

import tracker.BundleTracker;
import tracker.BundleTrackerCustomizer;
import dm.BundleDependency;
import dm.admin.ComponentDependencyDeclaration;
import dm.context.DependencyContext;
import dm.context.Event;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleDependencyImpl extends DependencyImpl<BundleDependency> implements BundleDependency, BundleTrackerCustomizer, ComponentDependencyDeclaration {
    private BundleTracker m_tracker;
    private int m_stateMask = Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE;
    private Bundle m_bundleInstance;
    private Filter m_filter;
    private long m_bundleId = -1;
    private Object m_nullObject;
    private boolean m_propagate;
    private Object m_propagateCallbackInstance;
    private String m_propagateCallbackMethod;
    private final Logger m_logger;

    public BundleDependencyImpl(BundleContext context, Logger logger) {
        super(true /* autoconfig */, context);
        m_logger = logger;
    }
    
    public BundleDependencyImpl(BundleDependencyImpl prototype) {
        super(prototype);
        m_logger = prototype.m_logger;
        m_stateMask = prototype.m_stateMask;
        m_nullObject = prototype.m_nullObject;
        m_bundleInstance = prototype.m_bundleInstance;
        m_filter = prototype.m_filter;
        m_bundleId = prototype.m_bundleId;
        m_propagate = prototype.m_propagate;
        m_propagateCallbackInstance = prototype.m_propagateCallbackInstance;
        m_propagateCallbackMethod = prototype.m_propagateCallbackMethod;       
    }
    
    @Override
    public DependencyContext createCopy() {
        return new BundleDependencyImpl(this);
    }
    
    @Override
    public void start() {
        boolean wasStarted = isStarted();
        super.start();
        if (!wasStarted) {
            m_tracker = new BundleTracker(m_context, m_stateMask, this);
            m_tracker.open();
        }
    }

    @Override
    public void stop() {
        boolean wasStarted = isStarted();
        super.stop();
        if (wasStarted) {
            m_tracker.close();
            m_tracker = null;
        }            
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        if ((m_stateMask & Bundle.ACTIVE) != 0) {
            sb.append("active ");
        }
        if ((m_stateMask & Bundle.INSTALLED) != 0) {
            sb.append("installed ");
        }
        if ((m_stateMask & Bundle.RESOLVED) != 0) {
            sb.append("resolved ");
        }
        if (m_filter != null) {
            sb.append(m_filter.toString());
        }
        if (m_bundleId != -1) {
            sb.append("bundle.id=" + m_bundleId);
        }
        return sb.toString();
    }

    @Override
    public String getType() {
        return "bundle";
    }

    public Object addingBundle(Bundle bundle, BundleEvent event) {
        // if we don't like a bundle, we could reject it here by returning null
        long bundleId = bundle.getBundleId();
        if (m_bundleId >= 0 && m_bundleId != bundleId) {
            return null;
        }
        Filter filter = m_filter;
        if (filter != null) {
            Dictionary<?,?> headers = bundle.getHeaders();
            if (!m_filter.match(headers)) {
                return null;
            }
        }
        return bundle;
    }
    
    public void addedBundle(Bundle bundle, BundleEvent event, Object object) {
        add(new BundleEventImpl(bundle, event));
    }
        
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        change(new BundleEventImpl(bundle, event));
    }

    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        remove(new BundleEventImpl(bundle, event));
    }
    
    @Override
    public void invoke(String method, Event e) {
        BundleEventImpl be = (BundleEventImpl) e;
        m_component.invokeCallbackMethod(getInstances(), method,
            new Class[][] {{Bundle.class}, {Object.class}, {}},             
            new Object[][] {{be.getBundle()}, {be.getBundle()}, {}}
        );
    }  
        
    public BundleDependency setBundle(Bundle bundle) {
        m_bundleId = bundle.getBundleId();
        return this;
    }

    public BundleDependency setFilter(String filter) throws IllegalArgumentException {
        if (filter != null) {
            try {
                m_filter = m_context.createFilter(filter);
            } 
            catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        return this;
    }
    
    public BundleDependency setStateMask(int mask) {
        m_stateMask = mask;
        return this;
    }
    
    @Override
    public Object getAutoConfigInstance() {
        return getService();
    }

    @Override
    public Class<?> getAutoConfigType() {
        return Bundle.class;
    }
    
    @Override
    public Dictionary<?,?> getProperties() {
        Bundle bundle = (Bundle) getService();
        if (bundle != null) {
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                try {
                    return (Dictionary<?,?>) InvocationUtil.invokeCallbackMethod(m_propagateCallbackInstance, m_propagateCallbackMethod, new Class[][] {{ Bundle.class }}, new Object[][] {{ bundle }});
                }
                catch (InvocationTargetException e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while invoking callback method", e.getCause());
                }
                catch (Exception e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while trying to invoke callback method", e);
                }
                throw new IllegalStateException("Could not invoke callback");
            }
            else {
                return bundle.getHeaders();
            }
        }
        else {
            throw new IllegalStateException("cannot find bundle");
        }
    }
    
    @Override
    protected Object getService() {
        Bundle service = null;
        if (isStarted()) {
            BundleEventImpl be = (BundleEventImpl) m_component.getDependencyEvent(this);
            return be != null ? be.getBundle() : null;
        }
        else {
            Bundle[] bundles = m_context.getBundles();
            for (int i = 0; i < bundles.length; i++) {
                if ((bundles[i].getState() & m_stateMask) > 0) {
                    Filter filter = m_filter;
                    if (filter == null) {
                        service = bundles[i];
                        break;
                    }
                    else if (filter.match(bundles[i].getHeaders())) {
                        service = bundles[i];
                        break;
                    }
                }
            }
        }
        if (service == null && isAutoConfig()) {
            // TODO does it make sense to add support for custom bundle impls?
//            service = getDefaultImplementation();
            if (service == null) {
                service = getNullObject();
            }
        }
        return service;
    }
    
    private Bundle getNullObject() {
        if (m_nullObject == null) {
            try {
                m_nullObject = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Bundle.class }, new DefaultNullObject()); 
            }
            catch (Exception e) {
                m_logger.log(Logger.LOG_ERROR, "Could not create null object for Bundle.", e);
            }
        }
        return (Bundle) m_nullObject;
    }
}

