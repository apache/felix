package org.apache.felix.dm.lambda.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.ComponentBuilder;
import org.apache.felix.dm.lambda.FactoryPidAdapterBuilder;
import org.apache.felix.dm.lambda.callbacks.CbComponentDictionary;
import org.apache.felix.dm.lambda.callbacks.CbDictionary;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentDictionary;
import org.apache.felix.dm.lambda.callbacks.CbTypeDictionary;

public class FactoryPidAdapterBuilderImpl implements AdapterBase<FactoryPidAdapterBuilder>, FactoryPidAdapterBuilder {
    private String m_factoryPid;
    private boolean m_propagate;
    private final DependencyManager m_dm;
    private boolean m_autoAdd = true;
    private String m_updateMethodName;
    private Object m_updateCallbackInstance;
    private boolean m_hasMethodRefs;
    private boolean m_hasReflectionCallback;
    private Consumer<ComponentBuilder<?>> m_compBuilder = (componentBuilder -> {});
    private final List<MethodRef<Object>> m_refs = new ArrayList<>();

    @FunctionalInterface
    interface MethodRef<I> {
        public void accept(I instance, Component c, Dictionary<String, Object> props);
    }

    public FactoryPidAdapterBuilderImpl(DependencyManager dm) {
        m_dm = dm;
    }
    
    public void andThenBuild(Consumer<ComponentBuilder<?>> builder) {
        m_compBuilder = m_compBuilder.andThen(builder);
    }

    @Override
    public FactoryPidAdapterBuilderImpl autoAdd(boolean autoAdd) {
        m_autoAdd = autoAdd;
        return this;
    }
        
    public boolean isAutoAdd() {
        return m_autoAdd;
    }

    @Override
    public FactoryPidAdapterBuilder factoryPid(String pid) {
        m_factoryPid = pid;
        return this;
    }

    @Override
    public FactoryPidAdapterBuilder factoryPid(Class<?> pidClass) {
        m_factoryPid = pidClass.getName();
        return this;
    }

    @Override
    public FactoryPidAdapterBuilder propagate() {
        m_propagate = true;
        return this;
    }

    @Override
    public FactoryPidAdapterBuilder propagate(boolean propagate) {
        m_propagate = propagate;
        return this;
    }

    public FactoryPidAdapterBuilder cb(String update) {
        checkHasNoMethodRefs();
        m_hasReflectionCallback = true;
        m_updateMethodName = update;
        return this;
    }
    
    public FactoryPidAdapterBuilder cb(Object callbackInstance, String update) {
        cb(update);
        m_updateCallbackInstance = callbackInstance;
        return this;
    }
    
    @Override
    public <U> FactoryPidAdapterBuilder cb(CbTypeDictionary<U> callback) {
        Class<U> type = Helpers.getLambdaArgType(callback, 0);
        return setComponentCallbackRef(type, (instance, component, props) -> { callback.accept((U) instance, props); });
    }
    
    @Override
    public <U> FactoryPidAdapterBuilder cb(CbTypeComponentDictionary<U> callback) {
        Class<U> type = Helpers.getLambdaArgType(callback, 0);
        return setComponentCallbackRef(type, (instance, component, props) -> { callback.accept((U) instance, component, props); });
    }

    @Override
    public FactoryPidAdapterBuilder cbi(CbDictionary callback) {
        return setInstanceCallbackRef((instance, component, props) -> { callback.accept(props); });
    }

    @Override
    public FactoryPidAdapterBuilder cbi(CbComponentDictionary callback) {
        return setInstanceCallbackRef((instance, component, props) -> { callback.accept(component, props); });
    }

    @Override
    public Component build() {        
        Objects.nonNull(m_factoryPid);
        Component c = null;
        
        if (m_hasMethodRefs) {
            Object wrapCallback = new Object() {
                @SuppressWarnings("unused")
                public void updated(Component comp, Dictionary<String, Object> conf) {
                    m_refs.forEach(mref -> mref.accept(null, comp, conf));
                }
            };
            c = m_dm.createFactoryConfigurationAdapterService(m_factoryPid, "updated", m_propagate, wrapCallback);
        } else {
            c = m_dm.createFactoryConfigurationAdapterService(m_factoryPid, m_updateMethodName, m_propagate, m_updateCallbackInstance);
        }
        ComponentBuilderImpl cb = new ComponentBuilderImpl(c, false);
        m_compBuilder.accept (cb);
        return cb.build();
    }
    
    private <U> FactoryPidAdapterBuilder setInstanceCallbackRef(MethodRef<U> ref) {
        checkHasNoReflectionCallbacks();
        m_hasMethodRefs = true;
        m_refs.add((instance, component, props) -> ref.accept(null, component, props));
        return this;
    }
    
    @SuppressWarnings("unchecked")
    private <U> FactoryPidAdapterBuilder setComponentCallbackRef(Class<U> type, MethodRef<U> ref) {
        checkHasNoReflectionCallbacks();
        m_hasMethodRefs = true;
        m_refs.add((instance, component, props) -> {
            Object componentImpl = Stream.of(component.getInstances())
                .filter(impl -> Helpers.getClass(impl).equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The method reference " + ref + " does not match any available component impl classes."));
            ref.accept((U) componentImpl, component, props);
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
