package org.apache.felix.dm.itest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.ResourceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@SuppressWarnings({"deprecation", "unchecked", "rawtypes", "unused"})
public class ResourceAdapterDependencyAddAndRemoveTest2 extends TestBase {
    public void testBasicResourceAdapter() throws Exception {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a resource provider
        ResourceProvider provider = new ResourceProvider(context, new URL("file://localhost/path/to/file1.txt"));
        // activate it
        Hashtable props = new Hashtable();
        props.put("id", "1");
        m.add(m.createComponent()
            .setInterface(ServiceInterface.class.getName(), props)
            .setImplementation(new ServiceProvider(e))
        );
        
        props = new Hashtable();
        props.put("id", "2");
        m.add(m.createComponent()
            .setInterface(ServiceInterface.class.getName(), props)
            .setImplementation(new ServiceProvider(e))
        );
        
        m.add(m.createComponent()
            .setImplementation(provider)
            .add(m.createServiceDependency()
                .setService(ResourceHandler.class)
                .setCallbacks("add", "remove")
            )
        );
        
        // create a resource adapter for our single resource
        // note that we can provide an actual implementation instance here because there will be only one
        // adapter, normally you'd want to specify a Class here
        Dependency d = m.createServiceDependency().setService(ServiceInterface.class, "(id=1)").setRequired(true);
        ResourceAdapter service = new ResourceAdapter(e, d);

        CallbackInstance callbackInstance = new CallbackInstance(e, d);
        Component component = m.createResourceAdapterService("(&(path=/path/to/*.txt)(host=localhost))", false, callbackInstance, "changed")
            .setImplementation(service)
            .setCallbacks(callbackInstance, "init", "start", "stop", "destroy");
        component.add(new ComponentStateListenerImpl(e));
        m.add(component);
        // wait until the single resource is available
        e.waitForStep(1, 5000);
        // trigger a 'change' in our resource
        provider.change();
        // wait until the changed callback is invoked
        e.waitForStep(2, 5000);
        
        System.out.println("Done!");
        m.clear();
     }
    
    static class ResourceAdapter {
        protected URL m_resource; // injected by reflection.
        private Ensure m_ensure;
        final Dependency m_dependency;
        
        ResourceAdapter(Ensure e, Dependency d) {
            m_ensure = e;
            m_dependency = d;
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
    
    static class CallbackInstance {
        
        private final Ensure m_ensure;
        private final Dependency m_dependency;
        
        public CallbackInstance(Ensure e, Dependency d) {
            m_ensure = e;
            m_dependency = d;
        }
        
        void init(Component c) {
            c.add(m_dependency);
            System.out.println("init");
            m_ensure.step(1);
        }
        
        void start() {
            System.out.println("start");
        }
        
        void stop() {
            System.out.println("stop");
        }
        
        void destroy() {
            System.out.println("destroy");
        }
        
        void changed(Component component) {
            m_ensure.step(2);
            System.out.println("Changing the dependencies");
            Dependency oldDependency = m_dependency;
            
            // and add a new dependency
            component.add(component.getDependencyManager().createServiceDependency().setService(ServiceInterface.class, "(id=2)").setRequired(true));
            // remove the old dependency
            component.remove(oldDependency);
            System.out.println("Changed the dependencies");
        }
    }
    
    static class ComponentStateListenerImpl implements ComponentStateListener {
        private final Ensure m_ensure;
        
        public ComponentStateListenerImpl(Ensure e) {
            this.m_ensure = e;
        }

        @Override
        public void changed(Component c, ComponentState state) {
            switch (state) {
            case INACTIVE:
                System.out.println("INACTIVE");
                break;
            case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
                System.out.println("INSTANTIATED_AND_WAITING_FOR_REQUIRED");
                break;
            case WAITING_FOR_REQUIRED:
                System.out.println("WAITING_FOR_REQUIRED");
                break;
            case TRACKING_OPTIONAL:
                System.out.println("TRACKING_OPTIONAL");
                break;

            }
        }
    }
}
