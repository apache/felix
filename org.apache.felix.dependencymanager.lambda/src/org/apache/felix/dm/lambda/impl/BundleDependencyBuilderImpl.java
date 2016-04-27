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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.felix.dm.BundleDependency;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.BundleDependencyBuilder;
import org.apache.felix.dm.lambda.callbacks.CbBundle;
import org.apache.felix.dm.lambda.callbacks.CbBundleComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbBundle;
import org.apache.felix.dm.lambda.callbacks.InstanceCbBundleComponent;
import org.osgi.framework.Bundle;

@SuppressWarnings("unchecked")
public class BundleDependencyBuilderImpl implements BundleDependencyBuilder {
	private String m_added;
	private String m_changed;
	private String m_removed;
	private Object m_instance;
	private boolean m_autoConfig = true;
	private boolean m_autoConfigInvoked = false;
	private boolean m_required;
	private Bundle m_bundle;
	private String m_filter;
	private int m_stateMask = -1;
	private boolean m_propagate;
	private Object m_propagateInstance;
	private String m_propagateMethod;
	private Function<Bundle, Dictionary<?, ?>> m_propagateCallback;
	private final Component m_component;
	private boolean m_requiredSet;
    
    enum Cb {
        ADD,        
        CHG,        
        REM
    };

	private final Map<Cb, List<MethodRef<Object>>> m_refs = new HashMap<>();

    @FunctionalInterface
    interface MethodRef<I> {
        public void accept(I instance, Component c, Bundle bundle);
    }

	/**
	 * Class used to call a supplier that returns Propagated properties
	 */
	private class Propagate {
		@SuppressWarnings("unused")
		Dictionary<?, ?> propagate(Bundle bundle) {
			return m_propagateCallback.apply(bundle);
		}
	}

    public BundleDependencyBuilderImpl (Component component) {
        m_component = component;
    }

    @Override
    public BundleDependencyBuilder autoConfig(boolean autoConfig) {
        m_autoConfig = autoConfig;
        m_autoConfigInvoked = true;
        return this;
    }

    @Override
    public BundleDependencyBuilder autoConfig() {
        autoConfig(true);
        return this;
    }

    @Override
    public BundleDependencyBuilder required(boolean required) {
        m_required = required;
        m_requiredSet = true;
        return this;
    }
    
    @Override
    public BundleDependencyBuilder optional() {
        return required(false);
    }

    @Override
    public BundleDependencyBuilder required() {
        required(true);
        return this;
    }

    @Override
    public BundleDependencyBuilder bundle(Bundle bundle) {
        m_bundle = bundle;
        return this;
    }

    @Override
    public BundleDependencyBuilder filter(String filter) throws IllegalArgumentException {
        m_filter = filter;
        return this;
    }

    @Override
    public BundleDependencyBuilder mask(int mask) {
        m_stateMask = mask;
        return this;
    }

    @Override
    public BundleDependencyBuilder propagate(boolean propagate) {
        m_propagate = propagate;
        return this;
    }

    @Override
    public BundleDependencyBuilder propagate() {
        propagate(true);
        return this;
    }

    @Override
    public BundleDependencyBuilder propagate(Object instance, String method) {
        if (m_propagateCallback != null || m_propagate) throw new IllegalStateException("Propagate callback already set.");
        Objects.nonNull(method);
        Objects.nonNull(instance);
        m_propagateInstance = instance;
        m_propagateMethod = method;
        return this;
    }

    @Override
    public BundleDependencyBuilder propagate(Function<Bundle, Dictionary<?, ?>> instance) {
        if (m_propagateInstance != null || m_propagate) throw new IllegalStateException("Propagate callback already set.");
        m_propagateCallback = instance;
        return this;
    }
    
    @Override
    public BundleDependencyBuilder callbackInstance(Object callbackInstance) {
        m_instance = callbackInstance;
        return this;
    }

    @Override
    public BundleDependencyBuilder add(String add) {
        callbacks(add, null, null);
        return this;
    }
    
    @Override
    public BundleDependencyBuilder change(String change) {
        callbacks(null, change, null);
        return this;
    }
    
    @Override
    public BundleDependencyBuilder remove(String remove) {
        callbacks(null, null, remove);
        return this;
    }
            
    private BundleDependencyBuilder callbacks(String added, String changed, String removed) {
        requiresNoMethodRefs();
        m_added = added != null ? added : m_added;
        m_changed = changed != null ? changed : m_changed;
        m_removed = removed != null ? removed : m_removed;
        if (! m_autoConfigInvoked) m_autoConfig = false;
        return this;
    }

    @Override
    public <T> BundleDependencyBuilder add(CbBundle<T> add) {
        return callbacks(add, null, null);
    }
    
    @Override
    public <T> BundleDependencyBuilder change(CbBundle<T> change) {
        return callbacks(null, change, null);
    }
    
    @Override
    public <T> BundleDependencyBuilder remove(CbBundle<T> remove) {
        return callbacks(null, null, remove);
    }
    
    private <T> BundleDependencyBuilder callbacks(CbBundle<T> add, CbBundle<T> change, CbBundle<T> remove) {
        if (add != null) {
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, component, bundle) -> add.accept ((T) inst, bundle));
        }
        if (change != null) {
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, component, bundle) -> change.accept ((T) inst, bundle));
        }
        if (remove != null) {
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, component, bundle) -> remove.accept ((T) inst, bundle));
        }
        return this;
    }

    @Override
    public <T> BundleDependencyBuilder add(CbBundleComponent<T> add) {
        return callbacks(add, null, null);
    }
    
    @Override
    public <T> BundleDependencyBuilder change(CbBundleComponent<T> change) {
        return callbacks(null, change, null);
    }
    
    @Override
    public <T> BundleDependencyBuilder remove(CbBundleComponent<T> remove) {
        return callbacks(null, null, remove);
    }
    
    private <T> BundleDependencyBuilder callbacks(CbBundleComponent<T> add, CbBundleComponent<T> change, CbBundleComponent<T> remove) {
        if (add != null) {
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, component, bundle) -> add.accept ((T) inst, bundle, component));
        }
        if (change != null) {
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, component, bundle) -> change.accept ((T) inst, bundle, component));
        }
        if (remove != null) {
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, component, bundle) -> remove.accept ((T) inst, bundle, component));
        }
        return this;  
    }
    
    @Override
    public BundleDependencyBuilder add(InstanceCbBundle add) {
        return callbacks(add, null, null);
    }
    
    @Override
    public BundleDependencyBuilder change(InstanceCbBundle change) {
        return callbacks(null, change, null);
    }
    
    @Override
    public BundleDependencyBuilder remove(InstanceCbBundle remove) {
        return callbacks(null, null, remove);
    }
    
    private BundleDependencyBuilder callbacks(InstanceCbBundle add, InstanceCbBundle change, InstanceCbBundle remove) {
        if (add != null) setInstanceCallbackRef(Cb.ADD, (inst, component, bundle) -> add.accept(bundle));
        if (change != null) setInstanceCallbackRef(Cb.CHG, (inst, component, bundle) -> change.accept(bundle));
        if (remove != null) setInstanceCallbackRef(Cb.REM, (inst, component, bundle) -> remove.accept(bundle));
        return this;
    }

    @Override
    public BundleDependencyBuilder add(InstanceCbBundleComponent add) {
        return callbacks(add, null, null);
    }
    
    @Override
    public BundleDependencyBuilder change(InstanceCbBundleComponent add) {
        return callbacks(add, null, null);
    }

    @Override
    public BundleDependencyBuilder remove(InstanceCbBundleComponent remove) {
        return callbacks(null, null, remove);
    }
    
    private BundleDependencyBuilder callbacks(InstanceCbBundleComponent add, InstanceCbBundleComponent change, InstanceCbBundleComponent remove) {
        if (add != null) setInstanceCallbackRef(Cb.ADD, (inst, component, bundle) -> add.accept(bundle, component));
        if (change != null) setInstanceCallbackRef(Cb.CHG, (inst, component, bundle) -> change.accept(bundle, component));
        if (remove != null) setInstanceCallbackRef(Cb.REM, (inst, component, bundle) -> remove.accept(bundle, component));
        return this;
    }

	@Override
	public BundleDependency build() {
        DependencyManager dm = m_component.getDependencyManager();

        BundleDependency dep = dm.createBundleDependency();
        if (! m_requiredSet) {
            m_required = Helpers.isDependencyRequiredByDefault(m_component);
        }
        dep.setRequired(m_required);
        
        if (m_filter != null) {
        	dep.setFilter(m_filter);
        }
        
        if (m_bundle != null) {
        	dep.setBundle(m_bundle);
        }
        
        if (m_stateMask != -1) {
        	dep.setStateMask(m_stateMask);
        }
        
        if (m_propagate) {
            dep.setPropagate(true);
        } else if (m_propagateInstance != null) {
            dep.setPropagate(m_propagateInstance, m_propagateMethod);
        } else if (m_propagateCallback != null) {
        	dep.setPropagate(new Propagate(), "propagate");
        }
        
        if (m_added != null || m_changed != null || m_removed != null) {
            dep.setCallbacks(m_instance, m_added, m_changed, m_removed);
        } else if (m_refs.size() > 0) {
            Object cb = createCallbackInstance();
            dep.setCallbacks(cb, "add", "change", "remove");
        } 
        
        dep.setAutoConfig(m_autoConfig);
        return dep;
	}

	private <T> BundleDependencyBuilder setInstanceCallbackRef(Cb cbType, MethodRef<T> ref) {
		requiresNoStringCallbacks();
		if (! m_autoConfigInvoked) m_autoConfig = false;
		List<MethodRef<Object>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
		list.add((instance, component, bundle) -> ref.accept(null, component, bundle));
		return this;
	}
	
	private <T> BundleDependencyBuilder setComponentCallbackRef(Cb cbType, Class<T> type, MethodRef<T> ref) {
	    requiresNoStringCallbacks();
		if (! m_autoConfigInvoked) m_autoConfig = false;
		List<MethodRef<Object>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
		list.add((instance, component, bundle) -> {
            Object componentImpl = Stream.of(component.getInstances())
                .filter(impl -> type.isAssignableFrom(Helpers.getClass(impl)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The method reference " + ref + " does not match any available component impl classes."));           
            ref.accept((T) componentImpl, component, bundle);
		});
		return this;
	}
	    
	@SuppressWarnings("unused")
	private Object createCallbackInstance() {
		Object cb = null;

		cb = new Object() {
			void add(Component c, Bundle bundle) {
				invokeMethodRefs(Cb.ADD, c, bundle);
			}

			void change(Component c, Bundle bundle) {
				invokeMethodRefs(Cb.CHG, c, bundle);
			}

			void remove(Component c, Bundle bundle) {
				invokeMethodRefs(Cb.REM, c, bundle);
			}
		};

		return cb;
	}

	private void invokeMethodRefs(Cb cbType, Component c, Bundle bundle) {
		m_refs.computeIfPresent(cbType, (k, mrefs) -> {
			mrefs.forEach(mref -> mref.accept(null, c, bundle));
			return mrefs;
		});
	}
	
	private void requiresNoStringCallbacks() {
		if (m_added != null || m_changed != null || m_removed != null) {
			throw new IllegalStateException("can't mix method references and string callbacks.");
		}
	}
	
	private void requiresNoMethodRefs() {
		if (m_refs.size() > 0) {
			throw new IllegalStateException("can't mix method references and string callbacks.");
		}
	}
}
