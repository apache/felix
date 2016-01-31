package org.apache.felix.dm.lambda.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.lambda.ConfigurationDependencyBuilder;
import org.apache.felix.dm.lambda.callbacks.CbComponentDictionary;
import org.apache.felix.dm.lambda.callbacks.CbDictionary;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentDictionary;
import org.apache.felix.dm.lambda.callbacks.CbTypeDictionary;

public class ConfigurationDependencyBuilderImpl implements ConfigurationDependencyBuilder {
    private String m_pid;
    private boolean m_propagate;
    private final Component m_component;
    private String m_updateMethodName;
    private Object m_updateCallbackInstance;
    private boolean m_hasMethodRefs;
    private boolean m_hasReflectionCallback;
    private final List<MethodRef<Object>> m_refs = new ArrayList<>();
    private boolean m_hasComponentCallbackRefs;
    private boolean m_needsInstance = false;
    
    @FunctionalInterface
    interface MethodRef<I> {
        public void accept(I instance, Component c, Dictionary<String, Object> props);
    }
    
    public ConfigurationDependencyBuilderImpl(Component component) {
        m_component = component;
    }

    @Override
    public ConfigurationDependencyBuilder pid(String pid) {
        m_pid = pid;
        return this;
    }

    @Override
    public ConfigurationDependencyBuilder pid(Class<?> pidClass) {
        m_pid = pidClass.getName();
        return this;
    }

    @Override
    public ConfigurationDependencyBuilder propagate() {
        m_propagate = true;
        return this;
    }

    @Override
    public ConfigurationDependencyBuilder propagate(boolean propagate) {
        m_propagate = propagate;
        return this;
    }

    public ConfigurationDependencyBuilder cb(String update) {
        checkHasNoMethodRefs();
        m_hasReflectionCallback = true;
        m_updateMethodName = update;
        return this;
    }
    
    public ConfigurationDependencyBuilder cbi(Object callbackInstance, String update) {
        m_updateCallbackInstance = callbackInstance;
        cb(update);
        return this;
    }

    public ConfigurationDependencyBuilder needsInstance(boolean needsInstance) {
        m_needsInstance = needsInstance;
        return this;
    }

    @Override
    public <T> ConfigurationDependencyBuilder cb(CbTypeDictionary<T>  callback) {
        Class<T> type = Helpers.getLambdaArgType(callback, 0);
        return setComponentCallbackRef(type, (instance, component, props) -> { callback.accept((T) instance, props); });
    }

    @Override
    public <T> ConfigurationDependencyBuilder cb(CbTypeComponentDictionary<T>  callback) {
        Class<T> type = Helpers.getLambdaArgType(callback, 0);
        return setComponentCallbackRef(type, (instance, component, props) -> { callback.accept((T) instance, component, props); });
    }

    @Override
    public ConfigurationDependencyBuilder cbi(CbDictionary callback) {
        return setInstanceCallbackRef((instance, component, props) -> { callback.accept(props); });
    }
    
    @Override
    public ConfigurationDependencyBuilder cbi(CbComponentDictionary callback) {
        return setInstanceCallbackRef((instance, component, props) -> { callback.accept(component, props); });
    }

    @Override
    public ConfigurationDependency build() {
        ConfigurationDependency dep = m_component.getDependencyManager().createConfigurationDependency();
        Objects.nonNull(m_pid);
        dep.setPid(m_pid);
        dep.setPropagate(m_propagate);
        if (m_updateMethodName != null) {
            if (m_updateCallbackInstance != null) {
                dep.setCallback(m_updateCallbackInstance, m_updateMethodName, m_needsInstance);
            } else {
                dep.setCallback(m_updateMethodName);
            }
        } else if (m_refs.size() > 0) {
            // setup an internal callback object. When config is updated, we have to call each registered 
            // method references. 
            // Notice that we need the component to be instantiated in case there is a mref on one of the component instances (unbound method ref), or is used
            // called "needsInstance(true)".
            dep.setCallback(new Object() {
                @SuppressWarnings("unused")
                void updated(Component comp, Dictionary<String, Object> props) {
                    m_refs.forEach(mref -> mref.accept(null, comp, props));
                }
            }, "updated", m_hasComponentCallbackRefs||m_needsInstance);
        }
        return dep;
    }
    
    private <T> ConfigurationDependencyBuilder setInstanceCallbackRef(MethodRef<T> ref) {
        checkHasNoReflectionCallbacks();
        m_hasMethodRefs = true;
        m_refs.add((instance, component, props) -> ref.accept(null, component, props));
        return this;
    }
    
    @SuppressWarnings("unchecked")
    private <T> ConfigurationDependencyBuilder setComponentCallbackRef(Class<T> type, MethodRef<T> ref) {
        checkHasNoReflectionCallbacks();
        m_hasMethodRefs = true;
        m_hasComponentCallbackRefs = true;
        m_refs.add((instance, component, props) -> {
            Object componentImpl = Stream.of(component.getInstances())
                .filter(impl -> Helpers.getClass(impl).equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The method reference " + ref + " does not match any available component impl classes."));           
            ref.accept((T) componentImpl, component, props);
        });
        return this;
    }
    
    private void checkHasNoMethodRefs() {
        if (m_hasMethodRefs) {
            throw new IllegalStateException("Can't mix method references with reflection based callbacks");
        }
    }
    
    private void checkHasNoReflectionCallbacks() {
        if (m_hasReflectionCallback) {
            throw new IllegalStateException("Can't mix method references with reflection based callbacks");
        }
    }
}
