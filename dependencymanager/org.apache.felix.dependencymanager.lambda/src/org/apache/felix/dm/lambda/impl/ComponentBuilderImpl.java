package org.apache.felix.dm.lambda.impl;

import static org.apache.felix.dm.lambda.impl.ComponentBuilderImpl.ComponentCallback.DESTROY;
import static org.apache.felix.dm.lambda.impl.ComponentBuilderImpl.ComponentCallback.INIT;
import static org.apache.felix.dm.lambda.impl.ComponentBuilderImpl.ComponentCallback.START;
import static org.apache.felix.dm.lambda.impl.ComponentBuilderImpl.ComponentCallback.STOP;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.BundleDependencyBuilder;
import org.apache.felix.dm.lambda.ComponentBuilder;
import org.apache.felix.dm.lambda.ConfigurationDependencyBuilder;
import org.apache.felix.dm.lambda.DependencyBuilder;
import org.apache.felix.dm.lambda.FluentProperty;
import org.apache.felix.dm.lambda.FutureDependencyBuilder;
import org.apache.felix.dm.lambda.ServiceDependencyBuilder;
import org.apache.felix.dm.lambda.callbacks.Cb;
import org.apache.felix.dm.lambda.callbacks.CbComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCb;
import org.apache.felix.dm.lambda.callbacks.InstanceCbComponent;

public class ComponentBuilderImpl implements ComponentBuilder<ComponentBuilderImpl> {
    private final List<DependencyBuilder<?>> m_dependencyBuilders = new ArrayList<>();
    private final Component m_component;
    private final boolean m_componentUpdated;
    private String[] m_serviceNames;
    private Dictionary<Object, Object> m_properties;
    private Object m_impl;
    private Object m_factory;
    private boolean m_factoryHasComposite;	
    private boolean m_autoAdd = true;
    protected final Map<ComponentCallback, List<MethodRef<Object>>> m_refs = new HashMap<>();
    private Object m_compositionInstance;
    private String m_compositionMethod;
    private String m_init;
    private String m_stop;
    private String m_start;
    private String m_destroy;
    private Object m_callbackInstance;
    private String m_factoryCreateMethod;
    private boolean m_hasFactoryRef;   
    private boolean m_hasFactory;   
    
    enum ComponentCallback { INIT, START, STOP, DESTROY };
    
    @FunctionalInterface
    interface MethodRef<I> {
        public void accept(I instance, Component c);
    }

    public ComponentBuilderImpl(DependencyManager dm) {
        m_component = dm.createComponent();
        m_componentUpdated = false;
    }
    
    public ComponentBuilderImpl(Component component, boolean update) {
        m_component = component;
        m_componentUpdated = update;
    }
        
    @Override
    public ComponentBuilderImpl autoConfig(Class<?> clazz, boolean autoConfig) {
        m_component.setAutoConfig(clazz, autoConfig);
        return this;
    }

    @Override
    public ComponentBuilderImpl autoConfig(Class<?> clazz, String instanceName) {
        m_component.setAutoConfig(clazz, instanceName);
        return this;
    }

    @Override
    public ComponentBuilderImpl provides(Class<?> iface) {
        m_serviceNames = new String[] {iface.getName()};
        return this;
    }

    @Override
    public ComponentBuilderImpl provides(Class<?>  iface, String name, Object value, Object ... rest) {
        provides(iface);
        properties(name, value, rest);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(Class<?>  iface, FluentProperty ... properties) {
        provides(iface);
        properties(properties);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(Class<?>  iface, Dictionary<?,?> properties) {
        provides(iface);
        properties(properties);
        return this;
    }

    @Override
    public ComponentBuilderImpl  provides(Class<?>[] ifaces) {
        m_serviceNames = Stream.of(ifaces).map(c -> c.getName()).toArray(String[]::new);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(Class<?>[] ifaces, String name, Object value, Object ... rest) {
        provides(ifaces);
        properties(name, value, rest);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(Class<?>[] ifaces, FluentProperty ... properties) {
        provides(ifaces);
        properties(properties);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(Class<?>[] ifaces, Dictionary<?,?> properties) {
        provides(ifaces);
        properties(properties);
        return this;
    }

    @Override
    public ComponentBuilderImpl provides(String iface) {
        m_serviceNames = new String[] {iface};
        return this;
    }

    @Override
    public ComponentBuilderImpl provides(String iface, String name, Object value, Object ... rest) {
        provides(iface);
        properties(name, value, rest);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(String iface, FluentProperty ... properties) {
        provides(iface);
        properties(properties);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(String  iface, Dictionary<?,?> properties) {
        provides(iface);
        properties(properties);
        return this;
    }

    @Override
    public ComponentBuilderImpl  provides(String[] ifaces) {
        m_serviceNames = ifaces;
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(String[] ifaces, String name, Object value, Object ... rest) {
        provides(ifaces);
        properties(name, value, rest);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(String[] ifaces, FluentProperty ... properties) {
        provides(ifaces);
        properties(properties);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl provides(String[] ifaces, Dictionary<?,?> properties) {
        provides(ifaces);
        properties(properties);
        return this;
    }    
    
    @SuppressWarnings("unchecked")
    @Override
    public ComponentBuilderImpl properties(Dictionary<?, ?> properties) {
        m_properties = (Dictionary<Object, Object>) properties;
        return this;
    }

    @Override
    public ComponentBuilderImpl properties(String name, Object value, Object ... rest) {
    	Objects.nonNull(name);
    	Objects.nonNull(value);
        Properties props = new Properties();
        props.put(name,  value);
        if ((rest.length & 1) != 0) {
        	throw new IllegalArgumentException("Invalid number of specified properties (number of arguments must be even).");
        }
        for (int i = 0; i < rest.length - 1; i += 2) {
            String k = rest[i].toString().trim();
            Object v = rest[i+1];
            props.put(k, v);
        }
        m_properties = props;
        return this;
    }
    
    @Override
    public ComponentBuilderImpl properties(FluentProperty ... properties) {
    	Dictionary<Object, Object> props = new Hashtable<>();
    	Stream.of(properties).forEach(property -> {
    		String name = Helpers.getLambdaParameterName(property, 0);
    		if (name.equals("arg0")) {
    			throw new IllegalArgumentException("arg0 property name not supported"); 
    		}
    		Object value = property.apply(name);
    		props.put(name, value);
    	});
        m_properties = props;
        return this;
    }

    @Override
    public ComponentBuilderImpl debug(String label) {
        m_component.setDebug(label);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl autoAdd(boolean autoAdd) {
        m_autoAdd = autoAdd;
        return this;
    }
    
    public ComponentBuilderImpl autoAdd() {
        m_autoAdd = true;
    	return this;
    }

    public boolean isAutoAdd() {
        return m_autoAdd;
    }

    @Override
    public ComponentBuilderImpl impl(Object instance) {
        m_impl = instance;
        return this;
    }
        
    @Override
    public ComponentBuilderImpl factory(Object factory, String createMethod) {
        m_factory = factory;
        m_factoryCreateMethod = createMethod;
        ensureHasNoFactoryRef();
        m_hasFactory = true;
        return this;
    }

    @Override
    public ComponentBuilderImpl factory(Supplier<?> create) {
        Objects.nonNull(create);
        ensureHasNoFactory();
        m_hasFactoryRef = true;
        m_factory = new Object() {
            @SuppressWarnings("unused")
            public Object create() {
                return create.get();
            }
        };
        return this;
    }
    
    @Override
    public <U, V> ComponentBuilderImpl factory(Supplier<U> supplier, Function<U, V> create) {
        Objects.nonNull(supplier);
        Objects.nonNull(create);
        ensureHasNoFactory();
        m_hasFactoryRef = true;

        m_factory = new Object() {
            @SuppressWarnings("unused")
            public Object create() {
                U factoryImpl = supplier.get();
                return create.apply(factoryImpl);
            }
        }; 
        return this;
    }

    @Override
    public ComponentBuilderImpl factory(Supplier<?> create, Supplier<Object[]> getComposite) {
        Objects.nonNull(create);
        Objects.nonNull(getComposite);
        ensureHasNoFactory();
        m_hasFactoryRef = true;

        m_factory = new Object() {
            @SuppressWarnings("unused")
            public Object create() { // Create Factory instance
                return create.get();
            }
            
            @SuppressWarnings("unused")
            public Object[] getComposite() { // Create Factory instance
                return getComposite.get();
            }
        };
        m_factoryHasComposite = true;
        return this;
    }
    
    @Override
    public <U> ComponentBuilderImpl factory(Supplier<U> factorySupplier, Function<U, ?> factoryCreate, Function<U, Object[]> factoryGetComposite) {
        Objects.nonNull(factorySupplier);
        Objects.nonNull(factoryCreate);
        Objects.nonNull(factoryGetComposite);
        ensureHasNoFactory();
        m_hasFactoryRef = true;

        m_factory = new Object() {
            U m_factoryInstance;
            
            @SuppressWarnings("unused")
            public Object create() { 
                m_factoryInstance = factorySupplier.get();
                return factoryCreate.apply(m_factoryInstance);
            }
            
            @SuppressWarnings("unused")
            public Object[] getComposite() { 
                return factoryGetComposite.apply(m_factoryInstance);
            }
        }; 
        m_factoryHasComposite = true;
        return this;
    }
        
    public ComponentBuilderImpl composition(String getCompositionMethod) {
        return composition(null, getCompositionMethod);
    }
    
    public ComponentBuilderImpl composition(Object instance, String getCompositionMethod) {
        m_compositionInstance = instance;
        m_compositionMethod = getCompositionMethod;
        return this;
    }
    
    public ComponentBuilderImpl composition(Supplier<Object[]> getCompositionMethod) {
        m_compositionInstance = new Object() {
            @SuppressWarnings("unused")
            public Object[] getComposition() {
                return getCompositionMethod.get();
            }
        };
        m_compositionMethod = "getComposition";
        return this;
    }

    @Override
    public ComponentBuilderImpl withSvc(Class<?> service, String filter) {
        return withSvc(service, srv->srv.filter(filter));
    }   

    @Override
    public ComponentBuilderImpl withSvc(Class<?> ... services) {
        for (Class<?> s : services) {
            doWithService(s);
        }
        return this;
    }   

    private <U> void doWithService(Class<U> service) {
        ServiceDependencyBuilder<U> dep = new ServiceDependencyBuilderImpl<>(m_component, service);
        m_dependencyBuilders.add(dep);
    }

    @Override
    public <U> ComponentBuilderImpl withSvc(Class<U> service, Consumer<ServiceDependencyBuilder<U>> consumer) {
        ServiceDependencyBuilder<U> dep = new ServiceDependencyBuilderImpl<>(m_component, service);
        consumer.accept(dep);
        m_dependencyBuilders.add(dep);
        return this;
    }   
    
    @Override
    public ComponentBuilderImpl withCnf(Consumer<ConfigurationDependencyBuilder> consumer) {
        ConfigurationDependencyBuilder dep = new ConfigurationDependencyBuilderImpl(m_component);
        consumer.accept(dep);
        m_dependencyBuilders.add(dep);
        return this;
    }
    
    @Override
    public ComponentBuilderImpl withBundle(Consumer<BundleDependencyBuilder> consumer) {
    	BundleDependencyBuilder dep = new BundleDependencyBuilderImpl(m_component);
        consumer.accept(dep);
        m_dependencyBuilders.add(dep);
        return this;
    }
           
    @Override
    public <V> ComponentBuilderImpl withFuture(CompletableFuture<V> future, Consumer<FutureDependencyBuilder<V>> consumer) {
        FutureDependencyBuilder<V> dep = new CompletableFutureDependencyImpl<>(m_component, future);
        consumer.accept(dep);
        m_dependencyBuilders.add(dep);
        return this;
    }
    
    public ComponentBuilderImpl lifecycleCallbackInstance(Object callbackInstance) {
        m_callbackInstance = callbackInstance;
        return this;
    }    

    public ComponentBuilderImpl init(String callback) {
        ensureHasNoLifecycleMethodRefs();
        m_init = callback;
        return this;
    }
    
    public ComponentBuilderImpl start(String callback) {
        ensureHasNoLifecycleMethodRefs();
        m_start = callback;
        return this;
    }

    public ComponentBuilderImpl stop(String callback) {
        ensureHasNoLifecycleMethodRefs();
        m_stop = callback;
        return this;
    }
    
    public ComponentBuilderImpl destroy(String callback) {
        ensureHasNoLifecycleMethodRefs();
        m_destroy = callback;
        return this;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> ComponentBuilderImpl init(Cb<T> callback) {
        if (callback != null) {
            setComponentCallbackRef(INIT, Helpers.getLambdaArgType(callback, 0), (inst, component) -> callback.accept((T) inst));
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ComponentBuilderImpl start(Cb<T> callback) {
        if (callback != null) {
            setComponentCallbackRef(START, Helpers.getLambdaArgType(callback, 0), (inst, component) -> callback.accept((T) inst));
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ComponentBuilderImpl stop(Cb<T> callback) {
        if (callback != null) {
            setComponentCallbackRef(STOP, Helpers.getLambdaArgType(callback, 0), (inst, component) -> callback.accept((T) inst));
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ComponentBuilderImpl destroy(Cb<T> callback) {
        if (callback != null) {
            setComponentCallbackRef(DESTROY, Helpers.getLambdaArgType(callback, 0), (inst, component) -> callback.accept((T) inst));
        }
        return this;
    }

   @SuppressWarnings("unchecked")
   @Override
    public <T> ComponentBuilderImpl init(CbComponent<T> callback) {
        if (callback != null) {
            setComponentCallbackRef(INIT, Helpers.getLambdaArgType(callback, 0), (inst, component) -> callback.accept((T) inst, component));
        }
        return this;
    }
            
   @SuppressWarnings("unchecked")
   @Override
    public <T> ComponentBuilderImpl start(CbComponent<T> callback) {
        if (callback != null) {
            setComponentCallbackRef(START, Helpers.getLambdaArgType(callback, 0), (inst, component) -> callback.accept((T) inst, component));
        }
        return this;
    }
            
   @SuppressWarnings("unchecked")
   @Override
    public <T> ComponentBuilderImpl stop(CbComponent<T> callback) {
        if (callback != null) {
            setComponentCallbackRef(STOP, Helpers.getLambdaArgType(callback, 0), (inst, component) -> callback.accept((T) inst, component));
        }
        return this;
    }
            
   @SuppressWarnings("unchecked")
   @Override
    public <T> ComponentBuilderImpl destroy(CbComponent<T> callback) {
        if (callback != null) {
            setComponentCallbackRef(DESTROY, Helpers.getLambdaArgType(callback, 0), (inst, component) -> callback.accept((T) inst, component));
        }
        return this;
    }
            
    @Override
    public ComponentBuilderImpl initInstance(InstanceCb callback) {
        if (callback != null) {
            setInstanceCallbackRef(INIT, (inst, component) -> callback.cb());
        }
        return this;
    }
    
    @Override
    public ComponentBuilderImpl startInstance(InstanceCb callback) {
        if (callback != null) {
            setInstanceCallbackRef(START, (inst, component) -> callback.cb());
        }
        return this;
    }

    @Override
    public ComponentBuilderImpl stopInstance(InstanceCb callback) {
        if (callback != null) {
            setInstanceCallbackRef(STOP, (inst, component) -> callback.cb());
        }
        return this;
    }

    @Override
    public ComponentBuilderImpl destroyInstance(InstanceCb callback) {
        if (callback != null) {
            setInstanceCallbackRef(DESTROY, (inst, component) -> callback.cb());
        }
        return this;
    }
        
    @Override
    public ComponentBuilderImpl initInstance(InstanceCbComponent callback) {
        if (callback != null) {
            setInstanceCallbackRef(INIT, (inst, component) -> callback.accept(component));
        }
        return this;
    }
  
    @Override
    public ComponentBuilderImpl startInstance(InstanceCbComponent callback) {
        if (callback != null) {
            setInstanceCallbackRef(START, (inst, component) -> callback.accept(component));
        }
        return this;
    }

    @Override
    public ComponentBuilderImpl stopInstance(InstanceCbComponent callback) {
        if (callback != null) {
            setInstanceCallbackRef(STOP, (inst, component) -> callback.accept(component));
        }
        return this;
    }

    @Override
    public ComponentBuilderImpl destroyInstance(InstanceCbComponent callback) {
        if (callback != null) {
            setInstanceCallbackRef(DESTROY, (inst, component) -> callback.accept(component));
        }
        return this;
    }
    
    public Component build() {
        if (m_serviceNames != null) {
            m_component.setInterface(m_serviceNames, m_properties);
        } 
        
        if (m_properties != null) {
            m_component.setServiceProperties(m_properties);
        }
                
        if (! m_componentUpdated) { // Don't override impl or set callbacks if component is being updated
           if (m_impl != null) {               
               m_component.setImplementation(m_impl);
               m_component.setComposition(m_compositionInstance, m_compositionMethod);
           } else {
               Objects.nonNull(m_factory);
               if (m_hasFactoryRef) {
                   m_component.setFactory(m_factory, "create");
                   if (m_factoryHasComposite) {
                       m_component.setComposition(m_factory, "getComposite");
                   }
               } else {
                   m_component.setFactory(m_factory, m_factoryCreateMethod);
               }
           }
            
           if (m_refs.size() > 0) {
        	   setLifecycleMethodRefs();
           } else if (hasLifecleMethods()) {
               m_component.setCallbacks(m_callbackInstance, m_init, m_start, m_stop, m_destroy);
           }
        }
        
        if (m_dependencyBuilders.size() > 0) {
            // add atomically in case we are building some component dependencies from a component init method.
            // We first transform the list of builders into a stream of built Dependencies, then we collect the result 
            // to an array of Dependency[].
            m_component.add(m_dependencyBuilders.stream().map(builder -> builder.build()).toArray(Dependency[]::new));
        }
        return m_component;
    }

    private boolean hasLifecleMethods() {
        return m_init != null || m_start != null || m_stop != null || m_destroy != null;
    }
    
    private boolean hasLifecleMethodRefs() {
        return m_refs.size() > 0;
    }
    
    private void ensureHasNoLifecycleMethods() {
        if (hasLifecleMethods()) {
            throw new IllegalStateException("Can't mix method references and name names for lifecycle callbacks");
        }
    }
    
    private void ensureHasNoLifecycleMethodRefs() {
        if (hasLifecleMethodRefs()) {
            throw new IllegalStateException("Can't mix method references and name names for lifecycle callbacks");
        }
    }

    protected <U> ComponentBuilderImpl setInstanceCallbackRef(ComponentCallback cbType, MethodRef<U> ref) {
        ensureHasNoLifecycleMethods();
        List<MethodRef<Object>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
        list.add((instance, component) -> {
            ref.accept(null, component);
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private <U> ComponentBuilderImpl setComponentCallbackRef(ComponentCallback cbType, Class<U> type, MethodRef<U> callback) {
        ensureHasNoLifecycleMethods();
        if (type.equals(Object.class)) {
            throw new IllegalStateException("callback does not seam to be one from the possible component implementation classes");
        }
        List<MethodRef<Object>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
        list.add((instance, component) -> {
            Object componentImpl = Stream.of(component.getInstances())
                .filter(impl -> Helpers.getClass(impl).equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The method reference " + callback + " does not match any available component impl classes."));   
            callback.accept((U) componentImpl, component);
        });
        return this;
    }
        
    @SuppressWarnings("unused")
    private void setLifecycleMethodRefs() {
        Object cb = new Object() {
            void init(Component comp) {
            	invokeLfcleCallbacks(ComponentCallback.INIT, comp);
            }

            void start(Component comp) {
            	invokeLfcleCallbacks(ComponentCallback.START, comp);
            }

            void stop(Component comp) {
            	invokeLfcleCallbacks(ComponentCallback.STOP, comp);
            }

            void destroy(Component comp) {
            	invokeLfcleCallbacks(ComponentCallback.DESTROY, comp);
            }
        };
        m_component.setCallbacks(cb, "init", "start", "stop", "destroy");
    }
    
    private void invokeLfcleCallbacks(ComponentCallback cbType, Component component) {
        m_refs.computeIfPresent(cbType, (k, mrefs) -> {
            mrefs.forEach(mref -> mref.accept(null, component));
            return mrefs;
         });
    }

    private void ensureHasNoFactoryRef() {
        if (m_hasFactoryRef) {
            throw new IllegalStateException("Can't mix factory method name and factory method reference");
        }
    }
    
    private void ensureHasNoFactory() {
        if (m_hasFactory) {
            throw new IllegalStateException("Can't mix factory method name and factory method reference");
        }
    }
}
