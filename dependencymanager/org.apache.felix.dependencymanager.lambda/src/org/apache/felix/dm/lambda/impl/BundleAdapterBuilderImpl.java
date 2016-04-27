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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.BundleAdapterBuilder;
import org.apache.felix.dm.lambda.ComponentBuilder;
import org.apache.felix.dm.lambda.callbacks.CbBundle;
import org.apache.felix.dm.lambda.callbacks.CbBundleComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbBundle;
import org.apache.felix.dm.lambda.callbacks.InstanceCbBundleComponent;
import org.osgi.framework.Bundle;

public class BundleAdapterBuilderImpl implements AdapterBase<BundleAdapterBuilder>, BundleAdapterBuilder {
    private Consumer<ComponentBuilder<?>> m_compBuilder = (compBuilder -> {});
    protected final Map<Cb, List<MethodRef<Object>>> m_refs = new HashMap<>();
    private DependencyManager m_dm;
    private boolean m_autoAdd;
    private String m_added;
    private String m_changed;
    private String m_removed;
    private String m_filter;
    private int m_stateMask = -1;
    private boolean m_propagate;
    private Object m_callbackInstance;
    private String m_add;
    private String m_change;
    private String m_remove;

    enum Cb {
        ADD,        
        CHG,        
        REM
    };

    @FunctionalInterface
    interface MethodRef<I> {
        public void accept(I instance, Component c, Bundle b);
    }

    public BundleAdapterBuilderImpl(DependencyManager dm) {
        m_dm = dm;
    }
    
    public void andThenBuild(Consumer<ComponentBuilder<?>> builder) {
        m_compBuilder = m_compBuilder.andThen(builder);
    }

    @Override
    public BundleAdapterBuilderImpl autoAdd(boolean autoAdd) {
        m_autoAdd = autoAdd;
        return this;
    }
        
    public boolean isAutoAdd() {
        return m_autoAdd;
    }

    public BundleAdapterBuilder mask(int mask) {
        m_stateMask = mask;
        return this;
    }
    
    public BundleAdapterBuilder filter(String filter) {
        m_filter = filter;
        return this;
    }

    public BundleAdapterBuilder propagate(boolean propagate) {
        m_propagate = propagate;
        return this;
    }
    
    public BundleAdapterBuilder propagate() {
        m_propagate = true;
        return this;
    }

    public BundleAdapterBuilder add(String callback) {
        return callbacks(callback, null, null);
    }
    
    public BundleAdapterBuilder change(String callback) {
        return callbacks(null, callback, null);
    }
    
    public BundleAdapterBuilder remove(String callback) {
        return callbacks(null, null, callback);
    }
    
    public BundleAdapterBuilder callbackInstance(Object callbackInstance) {
        m_callbackInstance = callbackInstance;
        return this;
    }
    
    private BundleAdapterBuilder callbacks(String add, String change, String remove) {
        checkHasNoMethodRefs();
        m_add = add != null ? add : m_add;
        m_change = change != null ? change : m_change;
        m_remove = remove != null ? remove : m_remove;
        return this;
    }
    
    public <T> BundleAdapterBuilder add(CbBundle<T> add) {
        return callbacks(add, (CbBundle<T>) null, (CbBundle<T>) null);
    }

    public <T> BundleAdapterBuilder change(CbBundle<T> change) {
        return callbacks(null, change, null);
    }

    public <T> BundleAdapterBuilder remove(CbBundle<T> remove) {
        return callbacks(null, null, remove);
    }

    private <T> BundleAdapterBuilder callbacks(CbBundle<T> add, CbBundle<T> change, CbBundle<T> remove) {
        if (add != null) {
            Class<T> type = Helpers.getLambdaArgType(add, 0);
            setComponentCallbackRef(Cb.ADD, type, (instance, component, bundle) -> { add.accept((T) instance, bundle); });
        }
        if (change != null) {
            Class<T> type = Helpers.getLambdaArgType(change, 0);
            setComponentCallbackRef(Cb.CHG, type, (instance, component, bundle) -> { change.accept((T) instance, bundle); });
        }
        if (remove != null) {
            Class<T> type = Helpers.getLambdaArgType(remove, 0);
            setComponentCallbackRef(Cb.REM, type, (instance, component, bundle) -> { remove.accept((T) instance, bundle); });
        }
        return this;
    }
    
    public BundleAdapterBuilder add(InstanceCbBundle add) {
        return callbacks(add, null, null);
    }
    
    public BundleAdapterBuilder change(InstanceCbBundle change) {
        return callbacks(null, change, null);
    }

    public BundleAdapterBuilder remove(InstanceCbBundle remove) {
        return callbacks(null, null, remove);
    }

    public BundleAdapterBuilder callbacks(InstanceCbBundle add, InstanceCbBundle change, InstanceCbBundle remove) {
        if (add != null) setInstanceCallbackRef(Cb.ADD, (instance, component, bundle) -> { add.accept(bundle); });
        if (change != null) setInstanceCallbackRef(Cb.CHG, (instance, component, bundle) -> { change.accept(bundle); });
        if (remove != null) setInstanceCallbackRef(Cb.REM, (instance, component, bundle) -> { remove.accept(bundle); });
        return this;
    }
    
    public <T> BundleAdapterBuilder add(CbBundleComponent<T> add) {
        return callbacks(add, null, null);
    }
    
    public <T> BundleAdapterBuilder change(CbBundleComponent<T> change) {
        return callbacks(null, change, null);
    }
    
    public <T> BundleAdapterBuilder remove(CbBundleComponent<T> remove) {
        return callbacks(null, null, remove);
    }
    
    public <T> BundleAdapterBuilder callbacks(CbBundleComponent<T> add, CbBundleComponent<T> change, CbBundleComponent<T> remove) {
        if (add != null) {
            Class<T> type = Helpers.getLambdaArgType(add, 0);
            return setComponentCallbackRef(Cb.ADD, type, (instance, component, bundle) -> { add.accept((T) instance, bundle, component); });
        }
        if (change != null) {
            Class<T> type = Helpers.getLambdaArgType(change, 0);
            return setComponentCallbackRef(Cb.CHG, type, (instance, component, bundle) -> { change.accept((T) instance, bundle, component); });
        }
        if (remove != null) {
            Class<T> type = Helpers.getLambdaArgType(remove, 0);
            return setComponentCallbackRef(Cb.ADD, type, (instance, component, bundle) -> { remove.accept((T) instance, bundle, component); });
        }
        return this;
    }
    
    public BundleAdapterBuilder add(InstanceCbBundleComponent add) {
        return callbacks(add, null, null);
    }
    
    public BundleAdapterBuilder change(InstanceCbBundleComponent change) {
        return callbacks(null, change, null);
    }
    
    public BundleAdapterBuilder remove(InstanceCbBundleComponent remove) {
        return callbacks(null, null, remove);
    }
    
    public BundleAdapterBuilder callbacks(InstanceCbBundleComponent add, InstanceCbBundleComponent change, InstanceCbBundleComponent remove) {
        if (add != null) setInstanceCallbackRef(Cb.ADD, (instance, component, bundle) -> { add.accept(bundle, component); });
        if (change != null) setInstanceCallbackRef(Cb.CHG, (instance, component, bundle) -> { change.accept(bundle, component); });
        if (remove != null) setInstanceCallbackRef(Cb.REM, (instance, component, bundle) -> { remove.accept(bundle, component); });
        return this;
    }

    @Override
    public Component build() { 
        Component c = null;
        
        if (m_refs.size() > 0) {
            @SuppressWarnings("unused")
            Object wrapCallback = new Object() {
                public void add(Component comp, Bundle bundle) {
                    invokeMethodRefs(Cb.ADD, comp, bundle);
                }
                
                public void change(Component comp, Bundle bundle) {
                    invokeMethodRefs(Cb.CHG, comp, bundle);
                }

                public void remove(Component comp, Bundle bundle) {
                    invokeMethodRefs(Cb.REM, comp, bundle);
                }
            };
            c = m_dm.createBundleAdapterService(m_stateMask, m_filter, m_propagate, wrapCallback, "add", "change", "remove");
        } else {
            c = m_dm.createBundleAdapterService(m_stateMask, m_filter, m_propagate, m_callbackInstance, m_add, m_change, m_remove);
        }
        ComponentBuilderImpl cb = new ComponentBuilderImpl(c, false);
        m_compBuilder.accept (cb);
        return cb.build();        
    }
    
    private <U> BundleAdapterBuilder setInstanceCallbackRef(Cb cbType, MethodRef<U> ref) {
        checkHasNoReflectionCallbacks();
        List<MethodRef<Object>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
        list.add((instance, component, bundle) -> ref.accept(null, component, bundle));
        return this;
    }
    
    @SuppressWarnings("unchecked")
    private <U> BundleAdapterBuilder setComponentCallbackRef(Cb cbType, Class<U> type, MethodRef<U> ref) {
        checkHasNoReflectionCallbacks();
        List<MethodRef<Object>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
        list.add((instance, component, bundle) -> {
            Object componentImpl = Stream.of(component.getInstances())
                .filter(impl -> type.isAssignableFrom(Helpers.getClass(impl)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The method reference " + ref + " does not match any available component impl classes."));           
            ref.accept((U) componentImpl, component, bundle);
        });
        return this;
    }
    
    private void invokeMethodRefs(Cb cbType, Component comp, Bundle bundle) {
        m_refs.computeIfPresent(cbType, (k, mrefs) -> {
            mrefs.forEach(mref -> mref.accept(null, comp, bundle));
            return mrefs;
         });
     }

    private void checkHasNoMethodRefs() {
        if (m_refs.size() > 0) {
            throw new IllegalStateException("Can't mix method references with reflection based callbacks");
        }
    }
    
    private void checkHasNoReflectionCallbacks() {
        if (m_added != null || m_changed != null || m_removed != null) {
            throw new IllegalStateException("Can't mix method references with reflection based callbacks");
        }
    }
}
