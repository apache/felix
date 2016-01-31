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
import org.apache.felix.dm.lambda.callbacks.CbComponentBundle;
import org.apache.felix.dm.lambda.callbacks.CbTypeBundle;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentBundle;
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

    public BundleAdapterBuilder cb(String ... callbacks) {
        return cbi(null, callbacks);
    }
    
    public BundleAdapterBuilder cbi(Object callbackInstance, String ... callbacks) {
        switch (callbacks.length) {
        case 1:
            return cbi(callbackInstance, callbacks[0], null, null);
            
        case 2:
            return cbi(callbackInstance, callbacks[0], null, callbacks[1]);
            
        case 3:
            return cbi(callbackInstance, callbacks[0], callbacks[1], callbacks[2]);
            
        default:
            throw new IllegalArgumentException("wrong number of arguments: " + callbacks.length + ". " +
                "Possible arguments: [add], [add, remove] or [add, change, remove]");
        }
    }
    
    private BundleAdapterBuilder cbi(Object callbackInstance, String add, String change, String remove) {
        checkHasNoMethodRefs();
        m_callbackInstance = callbackInstance;
        m_add = add;
        m_change = change;
        m_remove = remove;
        return this;
    }
    
    public <T> BundleAdapterBuilder cb(CbTypeBundle<T> add) {
        return cb(add, (CbTypeBundle<T>) null, (CbTypeBundle<T>) null);
    }

    public <T> BundleAdapterBuilder cb(CbTypeBundle<T> add, CbTypeBundle<T> remove) {
        return cb(add, null, remove);
    }

    public <T> BundleAdapterBuilder cb(CbTypeBundle<T> add, CbTypeBundle<T> change, CbTypeBundle<T> remove) {
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
    
    public BundleAdapterBuilder cbi(CbBundle add) {
        return cbi(add, null, null);
    }

    public BundleAdapterBuilder cbi(CbBundle add, CbBundle remove) {
        return cbi(add, null, remove);
    }
    
    public BundleAdapterBuilder cbi(CbBundle add, CbBundle change, CbBundle remove) {
        if (add != null) setInstanceCallbackRef(Cb.ADD, (instance, component, bundle) -> { add.accept(bundle); });
        if (change != null) setInstanceCallbackRef(Cb.CHG, (instance, component, bundle) -> { change.accept(bundle); });
        if (remove != null) setInstanceCallbackRef(Cb.REM, (instance, component, bundle) -> { remove.accept(bundle); });
        return this;
    }
    
    public <T> BundleAdapterBuilder cb(CbTypeComponentBundle<T> add) {
        return cb((CbTypeComponentBundle<T>) add, (CbTypeComponentBundle<T>) null, (CbTypeComponentBundle<T>) null);
    }
    
    public <T> BundleAdapterBuilder cb(CbTypeComponentBundle<T> add, CbTypeComponentBundle<T> remove) {
        return cb(add, null, remove);
    }
    
    public <T> BundleAdapterBuilder cb(CbTypeComponentBundle<T> add, CbTypeComponentBundle<T> change, CbTypeComponentBundle<T> remove) {
        if (add != null) {
            Class<T> type = Helpers.getLambdaArgType(add, 0);
            return setComponentCallbackRef(Cb.ADD, type, (instance, component, bundle) -> { add.accept((T) instance, component, bundle); });
        }
        if (change != null) {
            Class<T> type = Helpers.getLambdaArgType(change, 0);
            return setComponentCallbackRef(Cb.CHG, type, (instance, component, bundle) -> { change.accept((T) instance, component, bundle); });
        }
        if (remove != null) {
            Class<T> type = Helpers.getLambdaArgType(remove, 0);
            return setComponentCallbackRef(Cb.ADD, type, (instance, component, bundle) -> { remove.accept((T) instance, component, bundle); });
        }
        return this;
    }
    
    public BundleAdapterBuilder cbi(CbComponentBundle add) {
        return cbi(add, null, null);
    }
    
    public BundleAdapterBuilder cbi(CbComponentBundle add, CbComponentBundle remove) {
        return cbi(add, null, remove);
    }

    public BundleAdapterBuilder cbi(CbComponentBundle add, CbComponentBundle change, CbComponentBundle remove) {
        if (add != null) setInstanceCallbackRef(Cb.ADD, (instance, component, bundle) -> { add.accept(component, bundle); });
        if (change != null) setInstanceCallbackRef(Cb.CHG, (instance, component, bundle) -> { change.accept(component, bundle); });
        if (remove != null) setInstanceCallbackRef(Cb.REM, (instance, component, bundle) -> { remove.accept(component, bundle); });
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
                .filter(impl -> Helpers.getClass(impl).equals(type))
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
