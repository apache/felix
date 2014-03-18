package dm.it;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import dm.Component;
import dm.ComponentState;
import dm.ComponentStateListener;
import dm.Dependency;
import dm.DependencyManager;
import dm.ResourceHandler;
import dm.ResourceUtil;

public class ResourceAdapterDependencyAddAndRemoveTest extends TestBase {
    public void testBasicResourceAdapter() throws Exception {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();

        // create and add a service provider
        m.add(m.createComponent()
                .setInterface(ServiceInterface.class.getName(), null)
                .setImplementation(new ServiceProvider(e)));
        
        // create and add a resource provider
        ResourceProvider provider = new ResourceProvider(e);
        m.add(m.createComponent()
                .setImplementation(provider)
                .add(m.createServiceDependency()
                    .setService(ResourceHandler.class)
                    .setCallbacks("add", "remove"))
                );
        
        // create a resource adapter for our single resource
        // note that we can provide an actual implementation instance here because there will be only one
        // adapter, normally you'd want to specify a Class here
        // also, create a callback instance which will be used for both callbacks on resource changes and
        // life cycle callbacks on the adapters themselves
        
        Dependency d = m.createServiceDependency()
            .setService(ServiceInterface.class)
            .setRequired(true);
        CallbackInstance callbackInstance = new CallbackInstance(e, d);
        Component component = m.createResourceAdapterService("(&(path=/path/to/*.txt)(host=localhost))", false, callbackInstance, "changed")
            .setImplementation(new ResourceAdapter(e, d))
            .setCallbacks(callbackInstance, "init", "start", "stop", "destroy");
        
        // add the resource adapter
        m.add(component);
        
        // wait until the single resource is available (the adapter has been started)
        e.waitForStep(1, 5000);
        // trigger a 'change' in our resource
        provider.change();
        // wait until the changed callback is invoked
        e.waitForStep(2, 5000);
        // and has completed (ensuring no "extra" steps are invoked in the mean time)
        e.waitForStep(3, 5000);
                
        // remove the resource adapter again
        // add a component state listener, in order to track resource adapter destruction
        component.add(new ComponentStateListenerImpl(e));
        m.remove(component);
        
        // wait for the stopped callback in the state listener
        e.waitForStep(4, 5000);
        m.clear();
     }
    
    static class ResourceAdapter {
        protected URL m_resource; // injected by reflection.
        private Ensure m_ensure;
        private final Dependency m_dependency;
        
        ResourceAdapter(Ensure e, Dependency d) {
            m_ensure = e;
            m_dependency = d;
        }
        
        void init(Component c) {
            c.add(m_dependency);
        }        
    }
    
    class ResourceProvider {
        private volatile BundleContext m_context;
        private final Ensure m_ensure;
        private final Map m_handlers = new HashMap();
        private URL[] m_resources;

        public ResourceProvider(Ensure ensure) throws MalformedURLException {
            m_ensure = ensure;
            m_resources = new URL[] {
                new URL("file://localhost/path/to/file1.txt")
            };
        }
        
        public void change() {
            ResourceHandler[] handlers;
            synchronized (m_handlers) {
                handlers = (ResourceHandler[]) m_handlers.keySet().toArray(new ResourceHandler[m_handlers.size()]);
            }
            for (int i = 0; i < m_resources.length; i++) {
                for (int j = 0; j < handlers.length; j++) {
                    ResourceHandler handler = handlers[j];
                    handler.changed(m_resources[i]);
                }
            }
        }

        public void add(ServiceReference ref, ResourceHandler handler) {
            debug("ResourceProvider.add(ref=%s, handler=%s", ref, handler);
            String filterString = (String) ref.getProperty("filter");
            Filter filter = null;
            if (filterString != null) {
                try {
                    filter = m_context.createFilter(filterString);
                }
                catch (InvalidSyntaxException e) {
                    Assert.fail("Could not create filter for resource handler: " + e);
                    return;
                }
            }
            synchronized (m_handlers) {
                m_handlers.put(handler, filter);
            }
            for (int i = 0; i < m_resources.length; i++) {
                if (filter == null || filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                    handler.added(m_resources[i]);
                }
            }
        }

        public void remove(ServiceReference ref, ResourceHandler handler) {
            Filter filter;
            synchronized (m_handlers) {
                debug("ResourceProvider.remove: ref=%s, handler=%s, handlers=%s", ref, handler, m_handlers);
                filter = (Filter) m_handlers.remove(handler);
            }
            removeResources(handler, filter);
        }

        private void removeResources(ResourceHandler handler, Filter filter) {
                for (int i = 0; i < m_resources.length; i++) {
                    if (filter == null || filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                        handler.removed(m_resources[i]);
                    }
                }
            }

        public void destroy() {
            debug("ResourceProvider:%s", m_handlers);
            Entry[] handlers;
            synchronized (m_handlers) {
                debug("ResourceProvider.destroy: handlers=%s", m_handlers);
                handlers = (Entry[]) m_handlers.entrySet().toArray(new Entry[m_handlers.size()]);
            }
            for (int i = 0; i < handlers.length; i++) {
                removeResources((ResourceHandler) handlers[i].getKey(), (Filter) handlers[i].getValue());
            }
            
            debug("DESTROY...%d", m_handlers.size());
        }
    }
    
    static interface ServiceInterface {
        public void invoke();
    }

    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
        }
    }    
    
    class CallbackInstance {
        private final Dependency m_dependency;
        private final Ensure m_ensure;
        
        
        public CallbackInstance(Ensure e, Dependency d) {
            m_ensure = e;
            m_dependency = d;
        }
        
        void init() {
            debug("CallbackInstance.init");
        }
        
        void start() {
            debug("CallbackInstance.start");
            m_ensure.step(1);
        }
        
        void stop() {
            debug("CallbackInstance.stop");
        }
        
        void destroy() {
            debug("CallbackInstance.destroy");
        }
        
        void changed(Component component) {
            m_ensure.step(2);
            Dependency oldDependency = m_dependency;
            // and add a new dependency
            component.add(component.getDependencyManager().createServiceDependency().setService(ServiceInterface.class).setRequired(true));
            // remove the old dependency
            component.remove(oldDependency);
            debug("CallbackInstance.changed the dependencies");
            m_ensure.step(3);
        }
    }
    
    class ComponentStateListenerImpl implements ComponentStateListener {
        
        private final Ensure m_ensure;
        
        public ComponentStateListenerImpl(Ensure e) {
            this.m_ensure = e;
        }
        
        public void changed(ComponentState state) {
            debug("ComponentStateListenerImpl.changed: state=%s", state);
            switch (state) {
            case INACTIVE:
                System.out.println("stopped");
                m_ensure.step(4);
            }
        }
    }
}
