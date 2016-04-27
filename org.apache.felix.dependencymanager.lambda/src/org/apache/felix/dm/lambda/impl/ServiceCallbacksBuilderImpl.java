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
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.lambda.ServiceCallbacksBuilder;
import org.apache.felix.dm.lambda.callbacks.CbRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbRefServiceRefServiceComponent;
import org.apache.felix.dm.lambda.callbacks.CbService;
import org.apache.felix.dm.lambda.callbacks.CbServiceComponent;
import org.apache.felix.dm.lambda.callbacks.CbServiceComponentRef;
import org.apache.felix.dm.lambda.callbacks.CbServiceDict;
import org.apache.felix.dm.lambda.callbacks.CbServiceMap;
import org.apache.felix.dm.lambda.callbacks.CbServiceRef;
import org.apache.felix.dm.lambda.callbacks.CbServiceService;
import org.apache.felix.dm.lambda.callbacks.CbServiceServiceComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.InstanceCbRefServiceRefServiceComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbService;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceComponentRef;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceDict;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceMap;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceRef;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceService;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceServiceComponent;
import org.osgi.framework.ServiceReference;

/**
 * Service Dependency Callback management.
 *
 * @param <S> the type of the service dependency
 * @param <B> the type of the sub-classes which may extend this class
 */
@SuppressWarnings({"unchecked", "unused"})
public class ServiceCallbacksBuilderImpl<S, B extends ServiceCallbacksBuilder<S, B>> implements ServiceCallbacksBuilder<S, B> {
    protected boolean m_autoConfig = true;
    protected boolean m_autoConfigInvoked = false;
    protected String m_autoConfigField;
    protected Object m_callbackInstance;
    protected String m_added;
    protected String m_changed;
    protected String m_removed;
    protected String m_swapped;
    protected final Class<S> m_serviceClass;
    
    enum Cb {
        ADD,        
        CHG,        
        REM
    };
    
	/**
	 * List of service (add/change/remove) callbacks.
	 */
    protected final Map<Cb, List<MethodRef<Object, S>>> m_refs = new HashMap<>();

	/**
	 * List of swap callbacks
	 */
	protected final List<SwapMethodRef<?, S>> m_swapRefs = new ArrayList<>();
	
	/**
	 * This interface (lambda) is called when we want to invoke a method reference. the lambda is called with all necessary service dependency 
	 * informations.
	 * 
	 * When the lambda is called, it will invoke the proper callback on the given component instance.
	 *
	 * @param <I> type of a component instance
	 * @param <T> service dependency type
	 */
	@FunctionalInterface
    interface MethodRef<I, S> {
    	public void accept(I instance, Component c, ServiceReference<S> ref, S service);
    }

	/**
	 * This interface (lambda) is called when we want to invoke a swap method reference. the lambda is called with all necessary swap info.
	 * When the lambda is called, it will invoke the proper swap callback on the given component instance.
	 *
	 * @param <I> type of a component instance
	 * @param <T> service dependency type
	 */
	@FunctionalInterface
	interface SwapMethodRef<I, S> {
    	public void accept(I instance, Component c, ServiceReference<S> oldRef, S oldService, ServiceReference<S> newRef, S newService);
    }
	  
	public ServiceCallbacksBuilderImpl(Class<S> serviceClass) {
	    m_serviceClass = serviceClass;
	}
	
    public B autoConfig() {
        autoConfig(true);
        return (B) this;
    }

    public  B autoConfig(String field) {
        m_autoConfigField = field;
        m_autoConfigInvoked = true;
        return (B) this;
    }

    public B autoConfig(boolean autoConfig) {
        m_autoConfig = autoConfig;
        m_autoConfigInvoked = true;
        return (B) this;
    }
	
    public B callbackInstance(Object callbackInstance) {
        m_callbackInstance = callbackInstance;
        return (B) this;
    }
    
    public B add(String add) {
        return callbacks(add, null, null, null);
    }
        
    public B change(String change) {
        return callbacks(null, change, null, null);
    }
        
    public B remove(String remove) {
        return callbacks(null, null, remove, null);
    }
        
    public B swap(String swap) {
        return callbacks(null, null, null, swap);
    }
        
    private B callbacks(String added, String changed, String removed, String swapped) {
		requiresNoMethodRefs();
		m_added = added != null ? added : m_added;
		m_changed = changed != null ? changed : m_changed;
		m_removed = removed != null ? removed : m_removed;
		m_swapped = swapped != null ? swapped : m_swapped;
        if (! m_autoConfigInvoked) m_autoConfig = false;
		return (B) this;
	}

    public <T> B add(CbService<T, S> add) {
        return callbacks(add, null, null);
    }
    
    public <T> B change(CbService<T, S> change) {
        return callbacks(null, change, null);
    }
    
    public <T> B remove(CbService<T, S> remove) {
        return callbacks(null, null, remove);
    }
    
    private <T> B callbacks(CbService<T, S> add, CbService<T, S> change, CbService<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv));
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv));
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv));
        return (B) this;
    }

    public B add(InstanceCbService<S> add) {
        return callbacks(add, null, null);
    }
    
    public B change(InstanceCbService<S> change) {
        return callbacks(null, change, null);
    }

    public B remove(InstanceCbService<S> remove) {
        return callbacks(null, null, remove);
    }

    public B callbacks(InstanceCbService<S> add, InstanceCbService<S> change, InstanceCbService<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv));
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv));
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv));
        return (B) this;        
    }
    
    public <T> B add(CbServiceMap<T, S> add) {
        return callbacks(add, null, null);
    }
    
    public <T> B change(CbServiceMap<T, S> change) {
        return callbacks(null, change, null);
    }
    
    public <T> B remove(CbServiceMap<T, S> remove) {
        return callbacks(null, null, remove);
    }
    
    public <T> B callbacks(CbServiceMap<T, S> add, CbServiceMap<T, S> change, CbServiceMap<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv, new SRefAsMap(ref)));
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv, new SRefAsMap(ref)));
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv, new SRefAsMap(ref)));
        return (B) this;        
    }
    
    public B add(InstanceCbServiceMap<S> add) {
        return callbacks(add, null, null);
    }
    
    public B change(InstanceCbServiceMap<S> change) {
        return callbacks(null, change, null);
    }

    public B remove(InstanceCbServiceMap<S> remove) {
        return callbacks(null, null, remove);
    }

    public B callbacks(InstanceCbServiceMap<S> add, InstanceCbServiceMap<S> change, InstanceCbServiceMap<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv, new SRefAsMap(ref)));   
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv, new SRefAsMap(ref)));   
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv, new SRefAsMap(ref)));
        return (B) this;        
    }

    public <T> B add(CbServiceDict<T, S> add) {
        return callbacks(add, null, null);
    }
    
    public <T> B change(CbServiceDict<T, S> change) {
        return callbacks(null, change, null);
    }
    
    public <T> B remove(CbServiceDict<T, S> remove) {
        return callbacks(null, null, remove);
    }
    
    public <T> B callbacks(CbServiceDict<T, S> add, CbServiceDict<T, S> change, CbServiceDict<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv, new SRefAsDictionary(ref)));
        if (change != null) 
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv, new SRefAsDictionary(ref)));
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv, new SRefAsDictionary(ref))); 
        return (B) this;  
    }

    public B add(InstanceCbServiceDict<S> add) {
        return callbacks(add, null, null);
    }
    
    public B change(InstanceCbServiceDict<S> change) {
        return callbacks(null, change, null);
    }

    public B remove(InstanceCbServiceDict<S> remove) {
        return callbacks(null, null, remove);
    }

    public B callbacks(InstanceCbServiceDict<S> add, InstanceCbServiceDict<S> change, InstanceCbServiceDict<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv, new SRefAsDictionary(ref)));   
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv, new SRefAsDictionary(ref)));   
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv, new SRefAsDictionary(ref)));
        return (B) this;        
    }
    
    public <T> B add(CbServiceRef<T, S> add) {
        return callbacks(add, null, null);
    }

    public <T> B change(CbServiceRef<T, S> change) {
        return callbacks(null, change, null);
    }

    public <T> B remove(CbServiceRef<T, S> remove) {
        return callbacks(null, null, remove);
    }

    public <T> B callbacks(CbServiceRef<T, S> add, CbServiceRef<T, S> change, CbServiceRef<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv, ref));
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv, ref)); 
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv, ref)); 
        return (B) this;
    }

    public B add(InstanceCbServiceRef<S> add) {
        return callbacks(add, null, null);
    }

    public B change(InstanceCbServiceRef<S> change) {
        return callbacks(null, change, null);
    }

    public B remove(InstanceCbServiceRef<S> remove) {
        return callbacks(null, null, remove);
    }

    public B callbacks(InstanceCbServiceRef<S> add, InstanceCbServiceRef<S> change, InstanceCbServiceRef<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv, ref)); 
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv, ref)); 
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv, ref)); 
        return (B) this;
    }
    
    public <T> B add(CbServiceComponent<T, S>  add) {
        return callbacks(add, null, null);
    }

    public <T> B change(CbServiceComponent<T, S>  change) {
        return callbacks(null, change, null);
    }

    public <T> B remove(CbServiceComponent<T, S>  remove) {
        return callbacks(null, null, remove);
    }

    private <T> B callbacks(CbServiceComponent<T, S>  add, CbServiceComponent<T, S>  change, CbServiceComponent<T, S>  remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv, comp)); 
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv, comp)); 
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv, comp)); 
        return (B) this;
    }

    public B add(InstanceCbServiceComponent<S> add) {
        return callbacks(add, null, null);
    }

    public B change(InstanceCbServiceComponent<S> change) {
        return callbacks(null, change, null);
    }

    public B remove(InstanceCbServiceComponent<S> remove) {
        return callbacks(null, null, remove);
    }

    public B callbacks(InstanceCbServiceComponent<S> add, InstanceCbServiceComponent<S> change, InstanceCbServiceComponent<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv, comp));
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv, comp));
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv, comp));
        return (B) this;
    }

    public <T> B add(CbServiceComponentRef<T, S>  add) {
        return callbacks(add, null, null);
    }

    public <T> B change(CbServiceComponentRef<T, S>  change) {
        return callbacks(null, change, null);
    }

    public <T> B remove(CbServiceComponentRef<T, S>  remove) {
        return callbacks(null, null, remove);
    }

    private <T> B callbacks(CbServiceComponentRef<T, S>  add, CbServiceComponentRef<T, S>  change, CbServiceComponentRef<T, S>  remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv, comp, ref)); 
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv, comp, ref)); 
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv, comp, ref)); 
        return (B) this;
    }

    public B add(InstanceCbServiceComponentRef<S> add) {
        return callbacks(add, null, null);
    }

    public B change(InstanceCbServiceComponentRef<S> change) {
        return callbacks(null, change, null);
    }

    public B remove(InstanceCbServiceComponentRef<S> remove) {
        return callbacks(null, null, remove);
    }

    public B callbacks(InstanceCbServiceComponentRef<S> add, InstanceCbServiceComponentRef<S> change, InstanceCbServiceComponentRef<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv, comp, ref));
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv, comp, ref));
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv, comp, ref));
        return (B) this;
    }

    public <T> B swap(CbServiceService<T, S> swap) {
        Class<T> type = Helpers.getLambdaArgType(swap, 0);
        return setComponentSwapCallbackRef(type, (inst, component, oref, oserv, nref, nserv) ->
            swap.accept((T) inst, oserv, nserv));                              
    }

    @Override
    public <T> B swap(CbServiceServiceComponent<T, S> swap) {
        Class<T> type = Helpers.getLambdaArgType(swap, 0);
        return setComponentSwapCallbackRef(type, (inst, component, oref, oserv, nref, nserv) ->
            swap.accept((T) inst, oserv, nserv, component));                              
    }

    public <T> B swap(CbRefServiceRefService<T, S> swap) {
        Class<T> type = Helpers.getLambdaArgType(swap, 0);
        return setComponentSwapCallbackRef(type, (inst, component, oref, oserv, nref, nserv) ->
            swap.accept((T) inst, oref, oserv, nref, nserv));                              
    }
    
    public <T> B swap(CbRefServiceRefServiceComponent<T, S> swap) {
        Class<T> type = Helpers.getLambdaArgType(swap, 0);
        return setComponentSwapCallbackRef(type, (inst, component, oref, oserv, nref, nserv) ->
            swap.accept((T) inst, oref, oserv, nref, nserv, component));                              
    }
    
    public B swap(InstanceCbServiceService<S> swap) {
        return setInstanceSwapCallbackRef((inst, component, oref, oserv, nref, nserv) -> swap.accept(oserv, nserv));
    }

    public B swap(InstanceCbServiceServiceComponent<S> swap) {
        return setInstanceSwapCallbackRef((inst, component, oref, oserv, nref, nserv) -> swap.accept(oserv, nserv, component));
    }

    public B swap(InstanceCbRefServiceRefService<S> swap) {
        return setInstanceSwapCallbackRef((inst, component, oref, oserv, nref, nserv) -> swap.accept(oref, oserv, nref, nserv));
    }
    
    public B swap(InstanceCbRefServiceRefServiceComponent<S> swap) {
        return setInstanceSwapCallbackRef((inst, component, oref, oserv, nref, nserv) -> swap.accept(oref, oserv, nref, nserv, component));
    }
    
    protected <I> B setComponentCallbackRef(Cb cbType, Class<I> type, MethodRef<I, S> ref) {
       requiresNoCallbacks();
       if (! m_autoConfigInvoked) m_autoConfig = false;
       List<MethodRef<Object, S>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
       list.add((instance, component, sref, service) -> {           
           Object componentImpl = Stream.of(component.getInstances())
               .filter(impl -> type.isAssignableFrom(Helpers.getClass(impl)))
               .findFirst()
               .orElseThrow(() -> new IllegalStateException("The method reference " + ref + " does not match any available component impl classes."));           
           ref.accept((I) componentImpl, component, sref, service);           
       });
       return (B) this;
    }

    protected <T> B setInstanceCallbackRef(Cb cbType, MethodRef<T, S> ref) {
        requiresNoCallbacks();
        if (! m_autoConfigInvoked) m_autoConfig = false;
        List<MethodRef<Object, S>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
        list.add((instance, component, sref, service) -> {
            ref.accept((T) component.getInstance(), component, sref, service);
        });
        return (B) this;
    }

    public <I> B setComponentSwapCallbackRef(Class<I> type, SwapMethodRef<I, S> ref) {
       requiresNoCallbacks();
       if (! m_autoConfigInvoked) m_autoConfig = false;
       m_swapRefs.add((instance, component, oref, oservice, nref, nservice) -> {
           Object componentImpl = Stream.of(component.getInstances())
               .filter(impl -> type.isAssignableFrom(Helpers.getClass(impl)))
               .findFirst()
               .orElseThrow(() -> new IllegalStateException("The method reference " + ref + " does not match any available component impl classes."));
           ref.accept((I) componentImpl, component, oref, oservice, nref, nservice);
       });
       return (B) this;
    }

    public <I> B setInstanceSwapCallbackRef(SwapMethodRef<I, S> ref) {
        requiresNoCallbacks();
        if (! m_autoConfigInvoked) m_autoConfig = false;
        m_swapRefs.add((instance, component, oref, oservice, nref, nservice) -> {
            ref.accept((I) component.getInstance(), component, oref, oservice, nref, nservice);
        });
        return (B) this;
     }
    
    Object createCallbackInstance() {
       Object cb = null;
       
       cb = new Object() {
           void add(Component c, ServiceReference<S> ref, Object service) {
               invokeMethodRefs(Cb.ADD, c, ref, (S) service);
           }

           void change(Component c, ServiceReference<S> ref, Object service) {
               invokeMethodRefs(Cb.CHG, c, ref, (S) service);
           }

           void remove(Component c, ServiceReference<S> ref, Object service) {
               invokeMethodRefs(Cb.REM, c, ref, (S) service);
           }

           void swap(Component c, ServiceReference<S> oldRef, Object oldSrv, ServiceReference<S> newRef, Object newSrv) {                
               invokeSwapMethodRefs(c, oldRef, (S) oldSrv, newRef, (S) newSrv);
           }
       };

       return cb;
    }
    
    boolean hasRefs() {
       return m_refs.size() > 0 || m_swapRefs.size() > 0;
    }
    
    boolean hasCallbacks() {
       return m_callbackInstance != null || m_added != null || m_changed != null || m_removed != null || m_swapped != null;
    }
    
    String getAutoConfigField() {
        return m_autoConfigField;
    }
    
    Object getCallbackInstance() {
        return m_callbackInstance;
    }
    
    String getAdded() {
        return m_added;
    }
    
    String getChanged() {
        return m_changed;
    }
    
    String getRemoved() {
        return m_removed;
    }
    
    String getSwapped() {
        return m_swapped;
    }
     
    private void invokeMethodRefs(Cb cbType, Component comp, ServiceReference<S> ref, S service) {
	   m_refs.computeIfPresent(cbType, (k, mrefs) -> {
		   mrefs.forEach(mref -> mref.accept(null, comp, ref, service));
		   return mrefs;
		});
    }
   
    private void invokeSwapMethodRefs(Component c, ServiceReference<S> oref, S osrv, ServiceReference<S> nref, S nsrv) {
	   m_swapRefs.forEach(ref -> ref.accept(null, c, oref, osrv, nref, nsrv));
    }   
   
    private void requiresNoCallbacks() {
	   if (hasCallbacks()) { 
		   throw new IllegalStateException("can't mix method references and string callbacks.");
	   }
    }
   
    private void requiresNoMethodRefs() {
	   if (hasRefs()) {
		   throw new IllegalStateException("can't mix method references and string callbacks.");
	   }
    }   
}
