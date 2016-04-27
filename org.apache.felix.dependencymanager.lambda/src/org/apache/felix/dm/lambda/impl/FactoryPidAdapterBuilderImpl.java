/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.lambda.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.context.ComponentContext;
import org.apache.felix.dm.lambda.ComponentBuilder;
import org.apache.felix.dm.lambda.FactoryPidAdapterBuilder;
import org.apache.felix.dm.lambda.callbacks.CbConfiguration;
import org.apache.felix.dm.lambda.callbacks.CbConfigurationComponent;
import org.apache.felix.dm.lambda.callbacks.CbDictionary;
import org.apache.felix.dm.lambda.callbacks.CbDictionaryComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbConfiguration;
import org.apache.felix.dm.lambda.callbacks.InstanceCbConfigurationComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbDictionary;
import org.apache.felix.dm.lambda.callbacks.InstanceCbDictionaryComponent;

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
    private Class<?> m_configType;

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
    public FactoryPidAdapterBuilder propagate() {
        m_propagate = true;
        return this;
    }

    @Override
    public FactoryPidAdapterBuilder propagate(boolean propagate) {
        m_propagate = propagate;
        return this;
    }

    @Override
    public FactoryPidAdapterBuilder update(String update) {
        checkHasNoMethodRefs();
        m_hasReflectionCallback = true;
        m_updateMethodName = update;
        return this;
    }
    
    @Override
    public FactoryPidAdapterBuilder update(Class<?> configType, String updateMethod) {
        update(updateMethod);
        m_configType = configType;
        return this;
    }
    
    @Override
    public FactoryPidAdapterBuilder update(Object callbackInstance, String update) {
        update(update);
        m_updateCallbackInstance = callbackInstance;
        return this;
    }
    
    @Override
    public FactoryPidAdapterBuilder update(Class<?> configType, Object callbackInstance, String updateMethod) {
        update(callbackInstance, updateMethod);
        m_configType = configType;
        return this;
    }
    
    @Override
    public <T> FactoryPidAdapterBuilder update(CbDictionary<T> callback) {
        Class<T> type = Helpers.getLambdaArgType(callback, 0);
        return setComponentCallbackRef(type, (instance, component, props) -> { callback.accept((T) instance, props); });
    }
    
    @Override
    public <T, U> FactoryPidAdapterBuilder update(Class<U> configType, CbConfiguration<T, U> callback) {
        Class<T> type = Helpers.getLambdaArgType(callback, 0);
        m_factoryPid = m_factoryPid == null ? configType.getName() : m_factoryPid;
        return setComponentCallbackRef(type, (instance, component, props) -> {
            U configProxy = ((ComponentContext) component).createConfigurationType(configType, props);            
            callback.accept((T) instance, configProxy); 
        });
    }
    
    @Override
    public <T> FactoryPidAdapterBuilder update(CbDictionaryComponent<T> callback) {
        Class<T> type = Helpers.getLambdaArgType(callback, 0);
        return setComponentCallbackRef(type, (instance, component, props) -> { callback.accept((T) instance, props, component); });
    }

    @Override
    public <T, U> FactoryPidAdapterBuilder update(Class<U> configType, CbConfigurationComponent<T, U> callback) {
        Class<T> type = Helpers.getLambdaArgType(callback, 0);
        m_factoryPid = m_factoryPid == null ? configType.getName() : m_factoryPid;
        return setComponentCallbackRef(type, (instance, component, props) -> { 
            U configProxy = ((ComponentContext) component).createConfigurationType(configType, props);            
            callback.accept((T) instance, configProxy, component); 
        });
    }

    @Override
    public FactoryPidAdapterBuilder update(InstanceCbDictionary callback) {
        return setInstanceCallbackRef((instance, component, props) -> { callback.accept(props); });
    }
    
    @Override
    public <T> FactoryPidAdapterBuilder update(Class<T> configType, InstanceCbConfiguration<T> callback) {
        return setInstanceCallbackRef((instance, component, props) -> { 
            T configProxy = ((ComponentContext) component).createConfigurationType(configType, props);            
            callback.accept(configProxy);
        });
    }

    @Override
    public FactoryPidAdapterBuilder update(InstanceCbDictionaryComponent callback) {
        return setInstanceCallbackRef((instance, component, props) -> { callback.accept(props, component); });
    }

    @Override
    public <T> FactoryPidAdapterBuilder update(Class<T> configType, InstanceCbConfigurationComponent<T> callback) {
        return setInstanceCallbackRef((instance, component, props) -> { 
            T configProxy = ((ComponentContext) component).createConfigurationType(configType, props);            
            callback.accept(configProxy, component);
        });
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
            c = m_dm.createFactoryConfigurationAdapterService(m_factoryPid, m_updateMethodName, m_propagate, m_updateCallbackInstance, m_configType);
        }
        ComponentBuilderImpl cb = new ComponentBuilderImpl(c, false);
        m_compBuilder.accept (cb);
        return cb.build();
    }
    
    private <T> FactoryPidAdapterBuilder setInstanceCallbackRef(MethodRef<T> ref) {
        checkHasNoReflectionCallbacks();
        m_hasMethodRefs = true;
        m_refs.add((instance, component, props) -> ref.accept(null, component, props));
        return this;
    }
    
    @SuppressWarnings("unchecked")
    private <T> FactoryPidAdapterBuilder setComponentCallbackRef(Class<T> type, MethodRef<T> ref) {
        checkHasNoReflectionCallbacks();
        m_hasMethodRefs = true;
        m_refs.add((instance, component, props) -> {
            Object componentImpl = Stream.of(component.getInstances())
                .filter(impl -> type.isAssignableFrom(Helpers.getClass(impl)))
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
