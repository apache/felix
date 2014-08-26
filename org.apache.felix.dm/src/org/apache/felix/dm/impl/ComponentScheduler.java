package org.apache.felix.dm.impl;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.context.ComponentContext;

/**
 * Components addition/removal made through the DependencyManagerAPI are delegated to this
 * scheduler, which will use a threadpool optionally provided by any management bundle. 
 * The management bundle can register a threadpool (java.util.Executor) in the osgi registry
 * using the "target=org.apache.felix.dependencymanager" service property.
 * 
 * If the "org.apache.felix.dependencymanager.parallel" OSGi service property is set to true,
 * then the scheduler will wait for a threadpool before creating any DM components (that is:
 * added components will be cached until threadpool comes up from the OSGi service registry. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentScheduler {
    private final static ComponentScheduler m_instance = new ComponentScheduler();
    private volatile Executor m_threadPool;
    private final Executor m_serial = new SerialExecutor(null);
    private boolean m_started;
    private Set<Component> m_pending = new HashSet<>();
    public final static class NullExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
        }
    };

    public static ComponentScheduler instance() {
        return m_instance;
    }

    public void start() {
        m_serial.execute(new Runnable() {
            @Override
            public void run() {
                m_started = true;
                for (Component c : m_pending) {
                    doAdd(c);
                }
                m_pending.clear();
            }
        });
    }

    public void add(final Component c) {
        if (! isParallelComponent(c)) {
            ((ComponentContext) c).start();
        } else {
            m_serial.execute(new Runnable() {
                @Override
                public void run() {
                    if (!m_started) {
                        m_pending.add(c);
                    }
                    else {
                        doAdd(c);
                    }
                }
            });
        }
    }

    public void remove(final Component c) {
        if (! isParallelComponent(c)) {
            ((ComponentContext) c).stop();
        } else {
            m_serial.execute(new Runnable() {
                @Override
                public void run() {
                    if (!m_started) {
                        m_pending.remove(c);
                    }
                    else {
                        doRemove(c);
                    }
                }
            });
        }
    }

    private void doAdd(Component c) {
        if (! (m_threadPool instanceof NullExecutor)) {
            ((ComponentContext) c).setThreadPool(m_threadPool);
        }
        ((ComponentContext) c).start();
    }

    private void doRemove(Component c) {
        ((ComponentContext) c).stop();
    }
    
    private boolean isParallelComponent(Component c) {
        ComponentDeclaration decl = c.getComponentDeclaration();
        
        // The component declared from our DM Activator can not be parallel.
        if (ComponentScheduler.class.getName().equals(decl.getName())) {
            return false;
        }
        
        // A threadpool declared by a "management agent" using DM cannot be itself parallel.
        if (Executor.class.getName().equals(decl.getName())) {
            Dictionary<?, ?> props = decl.getServiceProperties();
            if (props != null) {
                Object property = props.get(DependencyManager.THREADPOOL);
                if (property != null && "true".equalsIgnoreCase(property.toString())) {
                    return false;
                }
            }
        }
        return true;
    }
}
