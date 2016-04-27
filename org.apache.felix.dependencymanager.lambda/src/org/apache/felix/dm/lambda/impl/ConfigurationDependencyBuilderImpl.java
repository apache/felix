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
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.context.ComponentContext;
import org.apache.felix.dm.lambda.ConfigurationDependencyBuilder;
import org.apache.felix.dm.lambda.callbacks.CbConfiguration;
import org.apache.felix.dm.lambda.callbacks.CbConfigurationComponent;
import org.apache.felix.dm.lambda.callbacks.CbDictionary;
import org.apache.felix.dm.lambda.callbacks.CbDictionaryComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbConfiguration;
import org.apache.felix.dm.lambda.callbacks.InstanceCbConfigurationComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbDictionary;
import org.apache.felix.dm.lambda.callbacks.InstanceCbDictionaryComponent;

public class ConfigurationDependencyBuilderImpl implements ConfigurationDependencyBuilder {
    private String m_pid;
    private boolean m_propagate;
    private final Component m_component;
    private String m_updateMethodName = "updated";
    private Object m_updateCallbackInstance;
    private boolean m_hasMethodRefs;
    private boolean m_hasReflectionCallback;
    private final List<MethodRef<Object>> m_refs = new ArrayList<>();
    private boolean m_hasComponentCallbackRefs;
    private Class<?> m_configType;
    
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
    public ConfigurationDependencyBuilder propagate() {
        m_propagate = true;
        return this;
    }

    @Override
    public ConfigurationDependencyBuilder propagate(boolean propagate) {
        m_propagate = propagate;
        return this;
    }

    public ConfigurationDependencyBuilder update(String update) {
        checkHasNoMethodRefs();
        m_hasReflectionCallback = true;
        m_updateMethodName = update;
        return this;
    }
    
    public ConfigurationDependencyBuilder update(Class<?> configType, String updateMethod) {
        m_configType = configType;
        return update(updateMethod);
    }

    public ConfigurationDependencyBuilder update(Object callbackInstance, String update) {
        m_updateCallbackInstance = callbackInstance;
        update(update);
        return this;
    }
    
    public ConfigurationDependencyBuilder update(Class<?> configType, Object callbackInstance, String update) {
        m_updateCallbackInstance = callbackInstance;
        return update(callbackInstance, update);        
    }

    @Override
    public <T> ConfigurationDependencyBuilder update(CbDictionary<T> callback) {
        Class<T> componentType = Helpers.getLambdaArgType(callback, 0);
        return setComponentCallbackRef(componentType, (instance, component, props) -> { 
            callback.accept((T) instance, props);
        }); 
    }

    @Override
    public <T> ConfigurationDependencyBuilder update(CbDictionaryComponent<T> callback) {
        Class<T> componentType = Helpers.getLambdaArgType(callback, 0);
        return setComponentCallbackRef(componentType, (instance, component, props) -> { 
            callback.accept((T) instance, props, component);
        }); 
    }

    @Override
    public <T, U> ConfigurationDependencyBuilder update(Class<U> configClass, CbConfiguration<T, U> callback) {
        Class<T> componentType = Helpers.getLambdaArgType(callback, 0);
        m_pid = m_pid == null ? configClass.getName() : m_pid;
        return setComponentCallbackRef(componentType, (instance, component, props) -> { 
            U configProxy = ((ComponentContext) m_component).createConfigurationType(configClass, props);            
            callback.accept((T) instance, configProxy);
        }); 
    }

    @Override
    public <T, U> ConfigurationDependencyBuilder update(Class<U> configClass, CbConfigurationComponent<T, U> callback) {
        Class<T> componentType = Helpers.getLambdaArgType(callback, 0);
        m_pid = m_pid == null ? configClass.getName() : m_pid;
        return setComponentCallbackRef(componentType, (instance, component, props) -> { 
            U configProxy = ((ComponentContext) m_component).createConfigurationType(configClass, props);            
            callback.accept((T) instance, configProxy, component);
        }); 
    }

    @Override
    public ConfigurationDependencyBuilder update(InstanceCbDictionary callback) {
        return setInstanceCallbackRef((instance, component, props) -> { 
            callback.accept(props);
        });
    }

    @Override
    public ConfigurationDependencyBuilder update(InstanceCbDictionaryComponent callback) {
        return setInstanceCallbackRef((instance, component, props) -> { 
            callback.accept(props, component);
        });
    }

    public <T> ConfigurationDependencyBuilder update(Class<T> configClass, InstanceCbConfiguration<T> updated) {
        m_pid = m_pid == null ? configClass.getName() : m_pid;
        return setInstanceCallbackRef((instance, component, props) -> { 
            T configProxy = ((ComponentContext) m_component).createConfigurationType(configClass, props);
            updated.accept(configProxy);
        });
    }
    
    public <T> ConfigurationDependencyBuilder update(Class<T> configClass, InstanceCbConfigurationComponent<T> updated) {
        m_pid = m_pid == null ? configClass.getName() : m_pid;
        return setInstanceCallbackRef((instance, component, props) -> { 
            T configProxy = ((ComponentContext) m_component).createConfigurationType(configClass, props);
            updated.accept(configProxy, component);
        });
    }
    
    @Override
    public ConfigurationDependency build() {
        String pid = m_pid == null ? (m_configType != null ? m_configType.getName() : null) : m_pid;
        if (pid == null) {
            throw new IllegalStateException("Pid not specified");
        }
        ConfigurationDependency dep = m_component.getDependencyManager().createConfigurationDependency();
        Objects.nonNull(m_pid);
        dep.setPid(pid);
        dep.setPropagate(m_propagate);
        if (m_updateMethodName != null) {
            dep.setCallback(m_updateCallbackInstance, m_updateMethodName, m_configType);
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
            }, "updated", m_hasComponentCallbackRefs);
        }
        return dep;
    }
    
    private <T> ConfigurationDependencyBuilder setInstanceCallbackRef(MethodRef<T> ref) {
        checkHasNoReflectionCallbacks();
        m_hasMethodRefs = true;
        m_updateMethodName = null;
        m_refs.add((instance, component, props) -> ref.accept(null, component, props));
        return this;
    }
    
    @SuppressWarnings("unchecked")
    private <T> ConfigurationDependencyBuilder setComponentCallbackRef(Class<T> type, MethodRef<T> ref) {
        checkHasNoReflectionCallbacks();
        m_updateMethodName = null;
        m_hasMethodRefs = true;
        m_hasComponentCallbackRefs = true;
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
