package org.apache.felix.dm.lambda.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.lambda.ServiceCallbacksBuilder;
import org.apache.felix.dm.lambda.callbacks.CbComponent;
import org.apache.felix.dm.lambda.callbacks.CbComponentRef;
import org.apache.felix.dm.lambda.callbacks.CbComponentRefService;
import org.apache.felix.dm.lambda.callbacks.CbComponentRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbComponentService;
import org.apache.felix.dm.lambda.callbacks.CbComponentServiceService;
import org.apache.felix.dm.lambda.callbacks.CbRef;
import org.apache.felix.dm.lambda.callbacks.CbRefService;
import org.apache.felix.dm.lambda.callbacks.CbRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbService;
import org.apache.felix.dm.lambda.callbacks.CbServiceDict;
import org.apache.felix.dm.lambda.callbacks.CbServiceMap;
import org.apache.felix.dm.lambda.callbacks.CbServiceService;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponent;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentRef;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentRefService;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentService;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentServiceService;
import org.apache.felix.dm.lambda.callbacks.CbTypeRef;
import org.apache.felix.dm.lambda.callbacks.CbTypeRefService;
import org.apache.felix.dm.lambda.callbacks.CbTypeRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbTypeService;
import org.apache.felix.dm.lambda.callbacks.CbTypeServiceDict;
import org.apache.felix.dm.lambda.callbacks.CbTypeServiceMap;
import org.apache.felix.dm.lambda.callbacks.CbTypeServiceService;
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
	
    public B cb(String ... callbacks) {
        return cbi(null, callbacks);
    }
    
    public B cbi(Object callbackInstance, String ... callbacks) {
        Objects.nonNull(callbacks);
        switch (callbacks.length) {
        case 1:
            cb(callbackInstance, callbacks[0], null, null, null);
            break;
            
        case 2:
            cb(callbackInstance, callbacks[0], null, callbacks[1], null);
            break;
            
        case 3:
            cb(callbackInstance, callbacks[0], callbacks[1], callbacks[2], null);
            break;
            
        case 4:
            cb(callbackInstance, callbacks[0], callbacks[1], callbacks[2], callbacks[3]);
            break;
            
        default:
            throw new IllegalArgumentException("wrong number of arguments: " + callbacks.length + ". " +
                "Possible arguments: [add], [add, remove], [add, change, remove], or [add, change, remove, swap]");
        }
        return (B) this;
    }
    
    private B cb(Object callbackInstance, String added, String changed, String removed, String swapped) {
		requiresNoMethodRefs();
        m_callbackInstance = callbackInstance;
		m_added = added != null ? added : m_added;
		m_changed = changed != null ? changed : m_changed;
		m_removed = removed != null ? removed : m_removed;
		m_swapped = swapped != null ? swapped : m_swapped;
        if (! m_autoConfigInvoked) m_autoConfig = false;
		return (B) this;
	}

    public <T> B cb(CbTypeService<T, S> add) {
        return cb(add, null, null);
    }
    
    public <T> B cb(CbTypeService<T, S> add, CbTypeService<T, S> remove) {
        return cb(add, null, remove);
    }
    
    public <T> B cb(CbTypeService<T, S> add, CbTypeService<T, S> change, CbTypeService<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv));
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv));
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv));
        return (B) this;
    }

    public B cbi(CbService<S> add) {
        return cbi(add, null, null);
    }
    
    public B cbi(CbService<S> add, CbService<S> remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbService<S> add, CbService<S> change, CbService<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv));
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv));
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv));
        return (B) this;        
    }
    
    public <T> B cb(CbTypeServiceMap<T, S> add) {
        return cb(add, null, null);
    }
    
    public <T> B cb(CbTypeServiceMap<T, S> add, CbTypeServiceMap<T, S> remove) {
        return cb(add, null, remove);
    }

    public <T> B cb(CbTypeServiceMap<T, S> add, CbTypeServiceMap<T, S> change, CbTypeServiceMap<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv, new SRefAsMap(ref)));
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv, new SRefAsMap(ref)));
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv, new SRefAsMap(ref)));
        return (B) this;        
    }
    
    public B cbi(CbServiceMap<S> add) {
        return cbi(add, null, null);
    }
    
    public B cbi(CbServiceMap<S> add, CbServiceMap<S> remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbServiceMap<S> add, CbServiceMap<S> change, CbServiceMap<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv, new SRefAsMap(ref)));   
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv, new SRefAsMap(ref)));   
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv, new SRefAsMap(ref)));
        return (B) this;        
    }

    public <T> B cb(CbTypeServiceDict<T, S> add) {
        return cb(add, null, null);
    }

    public <T> B cb(CbTypeServiceDict<T, S> add, CbTypeServiceDict<T, S> remove) {
        return cb(add, null, remove);
    }
    
    public <T> B cb(CbTypeServiceDict<T, S> add, CbTypeServiceDict<T, S> change, CbTypeServiceDict<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, srv, new SRefAsDictionary(ref)));
        if (change != null) 
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, srv, new SRefAsDictionary(ref)));
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, srv, new SRefAsDictionary(ref))); 
        return (B) this;  
    }

    public B cbi(CbServiceDict<S> add) {
        return cbi(add, null, null);
    }
    
    public B cbi(CbServiceDict<S> add, CbServiceDict<S> remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbServiceDict<S> add, CbServiceDict<S> change, CbServiceDict<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(srv, new SRefAsDictionary(ref)));   
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(srv, new SRefAsDictionary(ref)));   
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(srv, new SRefAsDictionary(ref)));
        return (B) this;        
    }

    public <T> B cb(CbTypeRefService<T, S> add) {
        return cb(add, null, null);
    }

    public <T> B cb(CbTypeRefService<T, S> add, CbTypeRefService<T, S> remove) {
        return cb(add, null, remove);
    }

    public <T> B cb(CbTypeRefService<T, S> add, CbTypeRefService<T, S> change, CbTypeRefService<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, ref, srv)); 
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, ref, srv)); 
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, ref, srv)); 
        return (B) this;
    }

    public B cbi(CbRefService<S> add) {
        return cbi(add, null, null);
    }

    public B cbi(CbRefService<S> add, CbRefService<S> remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbRefService<S> add, CbRefService<S> change, CbRefService<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(ref, srv)); 
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(ref, srv)); 
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(ref, srv)); 
        return (B) this;
    }
    
    public <T> B cb(CbTypeRef<T, S> add) {
        return cb(add, null, null);
    }

    public <T> B cb(CbTypeRef<T, S> add, CbTypeRef<T, S> remove) {
        return cb(add, null, remove);
    }

    public <T> B cb(CbTypeRef<T, S> add, CbTypeRef<T, S> change, CbTypeRef<T, S> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, ref));
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, ref)); 
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, ref)); 
        return (B) this;
    }

    public B cbi(CbRef<S> add) {
        return cbi(add, null, null);
    }

    public B cbi(CbRef<S> add, CbRef<S> remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbRef<S> add, CbRef<S> change, CbRef<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(ref)); 
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(ref)); 
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(ref)); 
        return (B) this;
    }

    public <T> B cb(CbTypeComponent<T> add) {
        return cb(add, null, null);
    }

    public <T> B cb(CbTypeComponent<T> add, CbTypeComponent<T> remove) {
        return cb(add, null, remove);
    }

    public <T> B cb(CbTypeComponent<T> add, CbTypeComponent<T> change, CbTypeComponent<T> remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, comp));              
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, comp));              
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, comp)); 
        return (B) this;
    }

    public B cbi(CbComponent add) {
        return cbi(add, null, null);
    }

    public B cbi(CbComponent add, CbComponent remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbComponent add, CbComponent change, CbComponent remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(comp)); 
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(comp)); 
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(comp)); 
        return (B) this;
    }

    public <T> B cb(CbTypeComponentRef<T, S>  add) {
        return cb(add, null, null);
    }

    public <T> B cb(CbTypeComponentRef<T, S>  add, CbTypeComponentRef<T, S>  remove) {
        return cb(add, null, remove);
    }

    public <T> B cb(CbTypeComponentRef<T, S>  add, CbTypeComponentRef<T, S>  change, CbTypeComponentRef<T, S>  remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, comp, ref));              
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, comp, ref)); 
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, comp, ref)); 
        return (B) this;      
    }

    public B cbi(CbComponentRef<S> add) {
        return cbi(add, null, null);
    }

    public B cbi(CbComponentRef<S> add, CbComponentRef<S> remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbComponentRef<S> add, CbComponentRef<S> change, CbComponentRef<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(comp, ref));
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(comp, ref));
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(comp, ref));
        return (B) this;
    }

    public <T> B cb(CbTypeComponentService<T, S>  add) {
        return cb(add, null, null);
    }

    public <T> B cb(CbTypeComponentService<T, S>  add, CbTypeComponentService<T, S>  remove) {
        return cb(add, null, remove);
    }

    public <T> B cb(CbTypeComponentService<T, S>  add, CbTypeComponentService<T, S>  change, CbTypeComponentService<T, S>  remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, comp, srv)); 
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, comp, srv)); 
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, comp, srv)); 
        return (B) this;
    }

    public B cbi(CbComponentService<S> add) {
        return cbi(add, null, null);
    }

    public B cbi(CbComponentService<S> add, CbComponentService<S> remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbComponentService<S> add, CbComponentService<S> change, CbComponentService<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(comp, srv));
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(comp, srv));
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(comp, srv));
        return (B) this;
    }

    public <T> B cb(CbTypeComponentRefService<T, S>  add) {
        return cb(add, null, null);
    }

    public <T> B cb(CbTypeComponentRefService<T, S>  add, CbTypeComponentRefService<T, S>  remove) {
        return cb(add, null, remove);
    }

    public <T> B cb(CbTypeComponentRefService<T, S>  add, CbTypeComponentRefService<T, S>  change, CbTypeComponentRefService<T, S>  remove) {
        if (add != null)
            setComponentCallbackRef(Cb.ADD, Helpers.getLambdaArgType(add, 0), (inst, comp, ref, srv) -> add.accept((T) inst, comp, ref, srv)); 
        if (change != null)
            setComponentCallbackRef(Cb.CHG, Helpers.getLambdaArgType(change, 0), (inst, comp, ref, srv) -> change.accept((T) inst, comp, ref, srv)); 
        if (remove != null)
            setComponentCallbackRef(Cb.REM, Helpers.getLambdaArgType(remove, 0), (inst, comp, ref, srv) -> remove.accept((T) inst, comp, ref, srv)); 
        return (B) this;
    }

    public B cbi(CbComponentRefService<S> add) {
        return cbi(add, null, null);
    }

    public B cbi(CbComponentRefService<S> add, CbComponentRefService<S> remove) {
        return cbi(add, null, remove);
    }

    public B cbi(CbComponentRefService<S> add, CbComponentRefService<S> change, CbComponentRefService<S> remove) {
        if (add != null)
            setInstanceCallbackRef(Cb.ADD, (inst, comp, ref, srv) -> add.accept(comp, ref, srv));
        if (change != null)
            setInstanceCallbackRef(Cb.CHG, (inst, comp, ref, srv) -> change.accept(comp, ref, srv));
        if (remove != null)
            setInstanceCallbackRef(Cb.REM, (inst, comp, ref, srv) -> remove.accept(comp, ref, srv));
        return (B) this;
    }

    public <T> B sw(CbTypeServiceService<T, S> swap) {
        Class<T> type = Helpers.getLambdaArgType(swap, 0);
        return setComponentSwapCallbackRef(type, (inst, component, oref, oserv, nref, nserv) ->
            swap.accept((T) inst, oserv, nserv));                              
    }

    public <T> B sw(CbTypeComponentServiceService<T, S> swap) {
        Class<T> type = Helpers.getLambdaArgType(swap, 0);
        return setComponentSwapCallbackRef(type, (inst, component, oref, oserv, nref, nserv) ->
            swap.accept((T) inst, component, oserv, nserv));                              
    }

    public <T> B sw(CbTypeRefServiceRefService<T, S> swap) {
        Class<T> type = Helpers.getLambdaArgType(swap, 0);
        return setComponentSwapCallbackRef(type, (inst, component, oref, oserv, nref, nserv) ->
            swap.accept((T) inst, oref, oserv, nref, nserv));                              
    }
    
    public <T> B sw(CbTypeComponentRefServiceRefService<T, S> swap) {
        Class<T> type = Helpers.getLambdaArgType(swap, 0);
        return setComponentSwapCallbackRef(type, (inst, component, oref, oserv, nref, nserv) ->
            swap.accept((T) inst, component, oref, oserv, nref, nserv));                              
    }
    
    public B swi(CbServiceService<S> swap) {
        return setInstanceSwapCallbackRef((inst, component, oref, oserv, nref, nserv) -> swap.accept(oserv, nserv));
    }

    public B swi(CbComponentServiceService<S> swap) {
        return setInstanceSwapCallbackRef((inst, component, oref, oserv, nref, nserv) -> swap.accept(component, oserv, nserv));
    }

    public B swi(CbRefServiceRefService<S> swap) {
        return setInstanceSwapCallbackRef((inst, component, oref, oserv, nref, nserv) -> swap.accept(oref, oserv, nref, nserv));
    }
    
    public B swi(CbComponentRefServiceRefService<S> swap) {
        return setInstanceSwapCallbackRef((inst, component, oref, oserv, nref, nserv) -> swap.accept(component, oref, oserv, nref, nserv));
    }
    
    protected <I> B setComponentCallbackRef(Cb cbType, Class<I> type, MethodRef<I, S> ref) {
       requiresNoCallbacks();
       if (! m_autoConfigInvoked) m_autoConfig = false;
       List<MethodRef<Object, S>> list = m_refs.computeIfAbsent(cbType, l -> new ArrayList<>());
       list.add((instance, component, sref, service) -> {           
           Object componentImpl = Stream.of(component.getInstances())
               .filter(impl -> Helpers.getClass(impl).equals(type))
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
               .filter(impl -> Helpers.getClass(impl).equals(type))
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
