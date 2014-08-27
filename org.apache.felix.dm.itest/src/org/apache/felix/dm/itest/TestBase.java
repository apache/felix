package org.apache.felix.dm.itest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * Base class for all integration tests.
 */
public abstract class TestBase extends TestCase implements LogService, FrameworkListener {
    // Default OSGI log service level.
    protected final static int LOG_LEVEL = LogService.LOG_WARNING;
    
    // optional thread pool used by parallel dependency managers
    private final static ExecutorService m_threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    // flag used to check if the threadpool must be used for a given test.
    private boolean m_parallel;
        
    // Flag used to check if some errors have been logged during the execution of a given test.
    private volatile boolean m_errorsLogged;

    // We implement OSGI log service.
    protected ServiceRegistration logService;
    
    // Our bundle context
    protected BundleContext context;

    // Our dependency manager used to create test components.
    protected volatile DependencyManager m_dm;

    public TestBase() {
    }
       
    protected void setParallel() {
        m_parallel = true;
    }
    
    public void setUp() throws Exception {
    	warn("Setting up test " + getClass().getName());
    	context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        logService = context.registerService(LogService.class.getName(), this, null);
        context.addFrameworkListener(this);
        m_dm = new DependencyManager(context);
        if (m_parallel) {
            warn("Using threadpool ...");
            m_dm.setThreadPool(m_threadPool);
        }
    }
    
    public void tearDown() throws Exception {
    	warn("Tearing down test " + getClass().getName());
    	logService.unregister();
    	context.removeFrameworkListener(this);
    	clearComponents();
    }
        
    protected DependencyManager getDM() {
        return m_dm;
    }
                
    protected void clearComponents() throws InterruptedException {
        List<Component> list = m_dm.getComponents();
        int count = list.size();
        m_dm.clear(); // can be asynchronous if test is running in parallel.
        warn("All component cleared (" + count + ")");
    }

    /**
     * Creates and provides an Ensure object with a name service property into the OSGi service registry.
     */
    protected ServiceRegistration register(Ensure e, String name) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("name", name);
        return context.registerService(Ensure.class.getName(), e, props);
    }

    /**
     * Helper method used to stop a given bundle.
     * 
     * @param symbolicName
     *            the symbolic name of the bundle to be stopped.
     */
    protected void stopBundle(String symbolicName) {
        // Stop the test.annotation bundle
        boolean found = false;
        for (Bundle b : context.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                try {
                    found = true;
                    b.stop();
                } catch (BundleException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!found) {
            throw new IllegalStateException("bundle " + symbolicName + " not found");
        }
    }

    /**
     * Helper method used to start a given bundle.
     * 
     * @param symbolicName
     *            the symbolic name of the bundle to be started.
     */
    protected void startBundle(String symbolicName) {
        // Stop the test.annotation bundle
        boolean found = false;
        for (Bundle b : context.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                try {
                    found = true;
                    b.start();
                } catch (BundleException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!found) {
            throw new IllegalStateException("bundle " + symbolicName + " not found");
        }
    }

    /**
     * Helper method used to get a given bundle.
     * 
     * @param symbolicName
     *            the symbolic name of the bundle to get.
     */
    protected Bundle getBundle(String symbolicName) {
        for (Bundle b : context.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        throw new IllegalStateException("bundle " + symbolicName + " not found");
    }
    
    /**
     * Suspend the current thread for a while.
     * 
     * @param n
     *            the number of milliseconds to wait for.
     */
    protected void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    public void log(int level, String message) {
        checkError(level, null);
        if (LOG_LEVEL >= level) {
            System.out.println(getLevel(level) + " - " + Thread.currentThread().getName() + " : " + message);
        }
    }

    public void log(int level, String message, Throwable exception) {
        checkError(level, exception);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            parse(sb, exception);
            System.out.println(sb.toString());
        }
    }

    public void log(ServiceReference sr, int level, String message) {
        checkError(level, null);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            System.out.println(sb.toString());
        }
    }

    public void log(ServiceReference sr, int level, String message, Throwable exception) {
        checkError(level, exception);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            parse(sb, exception);
            System.out.println(sb.toString());
        }
    }

    protected boolean errorsLogged() {
        return m_errorsLogged;
    }

    private void parse(StringBuilder sb, Throwable t) {
        if (t != null) {
            sb.append(" - ");
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            t.printStackTrace(pw);
            sb.append(buffer.toString());
            m_errorsLogged = true;
        }
    }

    private String getLevel(int level) {
        switch (level) {
            case LogService.LOG_DEBUG :
                return "DEBUG";
            case LogService.LOG_ERROR :
                return "ERROR";
            case LogService.LOG_INFO :
                return "INFO";
            case LogService.LOG_WARNING :
                return "WARN";
            default :
                return "";
        }
    }

    private void checkError(int level, Throwable exception) {
        if (level <= LOG_ERROR) {
            m_errorsLogged = true;
        }
        if (exception != null) {
            m_errorsLogged = true;
        }
    }

    public void frameworkEvent(FrameworkEvent event) {
        int eventType = event.getType();
        String msg = getFrameworkEventMessage(eventType);
        int level = (eventType == FrameworkEvent.ERROR) ? LOG_ERROR : LOG_WARNING;
        if (msg != null) {
            log(level, msg, event.getThrowable());
        } else {
            log(level, "Unknown fwk event: " + event);
        }
    }

    private String getFrameworkEventMessage(int event) {
        switch (event) {
            case FrameworkEvent.ERROR :
                return "FrameworkEvent: ERROR";
            case FrameworkEvent.INFO :
                return "FrameworkEvent INFO";
            case FrameworkEvent.PACKAGES_REFRESHED :
                return "FrameworkEvent: PACKAGE REFRESHED";
            case FrameworkEvent.STARTED :
                return "FrameworkEvent: STARTED";
            case FrameworkEvent.STARTLEVEL_CHANGED :
                return "FrameworkEvent: STARTLEVEL CHANGED";
            case FrameworkEvent.WARNING :
                return "FrameworkEvent: WARNING";
            default :
                return null;
        }
    }

    protected void warn(String msg, Object ... params) {
	if (LOG_LEVEL >= LogService.LOG_WARNING) {
	    log(LogService.LOG_WARNING, params.length > 0 ? String.format(msg, params) : msg);
	}
    }

    protected void info(String msg, Object ... params) {
	if (LOG_LEVEL >= LogService.LOG_INFO) {
	    log(LogService.LOG_INFO, params.length > 0 ? String.format(msg, params) : msg);
	}
    }

    protected void debug(String msg, Object ... params) {
	if (LOG_LEVEL >= LogService.LOG_DEBUG) {
	    log(LogService.LOG_DEBUG, params.length > 0 ? String.format(msg, params) : msg);
	}
    }

    protected void error(String msg, Object ... params) {
        log(LogService.LOG_ERROR, params.length > 0 ? String.format(msg, params) : msg);
    }

    protected void error(String msg, Throwable err, Object ... params) {
        log(LogService.LOG_ERROR, params.length > 0 ? String.format(msg, params) : msg, err);
    }

    protected void error(Throwable err) {
        log(LogService.LOG_ERROR, "error", err);
    }
}
