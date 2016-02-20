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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;
import org.apache.felix.dm.lambda.FutureDependencyBuilder;
import org.apache.felix.dm.lambda.callbacks.CbFuture;
import org.apache.felix.dm.lambda.callbacks.InstanceCbFuture;
import org.osgi.service.log.LogService;

public class CompletableFutureDependencyImpl<F> extends AbstractDependency<CompletableFutureDependencyImpl<F>> implements FutureDependencyBuilder<F> {

	private final CompletableFuture<F> m_future;
	private Component m_comp;
	private boolean m_async;
	private Executor m_exec;
    private InstanceCbFuture<F> m_accept = (future) -> {};
    private CbFuture<Object, F> m_accept2;
    private Class<?> m_accept2Type;
    
	public CompletableFutureDependencyImpl(Component c, CompletableFuture<F> future) {
		super.setRequired(true);
		m_future = future;
		m_comp = c;
	}

	/**
	 * Create a new PathDependency from an existing prototype.
	 * 
	 * @param prototype
	 *            the existing PathDependency.
	 */
	public CompletableFutureDependencyImpl(Component component, CompletableFutureDependencyImpl<F> prototype) {
		super(prototype);
		m_future = prototype.m_future;
		m_comp = component;
	}

	@Override
	public Dependency build() {
		return this;
	}

	@Override
    public FutureDependencyBuilder<F> complete(String callback) {
	    return complete(null, callback);
	}
	
	@Override
    public FutureDependencyBuilder<F> complete(Object callbackInstance, String callback) {
	    super.setCallbacks(callbackInstance, callback, null);
	    return this;
	}

	@Override
	public <T> FutureDependencyBuilder<F> complete(CbFuture<T, ? super F> consumer) {
	    return complete(consumer, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> FutureDependencyBuilder<F> complete(CbFuture<T, ? super F> consumer, boolean async) {
	    m_accept2Type = Helpers.getLambdaArgType(consumer, 0);;
	    m_accept2 = (instance, result) -> consumer.accept((T) instance, result);
	    m_async = async;
	    return this;
	}   

	@Override
	public <T> FutureDependencyBuilder<F> complete(CbFuture<T, ? super F> consumer, Executor executor) {
	    complete(consumer, true /* async */);
	    m_exec = executor;
	    return this;
	}

	@Override
	public FutureDependencyBuilder<F> complete(InstanceCbFuture<? super F> consumer) {
	    complete(consumer, false);
		return this;
	}
	
	@Override
	public FutureDependencyBuilder<F> complete(InstanceCbFuture<? super F> consumer, boolean async) {
	    m_accept = m_accept.andThen(future -> consumer.accept(future));
	    m_async = async;
	    return this;
	}   

    @Override
    public FutureDependencyBuilder<F> complete(InstanceCbFuture<? super F> consumer, Executor executor) {
        complete(consumer, true /* async */);
        m_exec = executor;
        return this;
    }

	// ---------- DependencyContext interface ----------

	@Override
	public void start() {
		try {
			if (m_async) {
				if (m_exec != null) {
					m_future.whenCompleteAsync((result, error) -> completed(result, error), m_exec);
				} else {
					m_future.whenCompleteAsync((result, error) -> completed(result, error));
				}
			} else {
				m_future.whenComplete((result, error) -> completed(result, error));
			}
		} catch (Throwable error) {
			super.getComponentContext().getLogger().log(LogService.LOG_ERROR, "completable future failed", error);
		}
		super.start();
	}

	@Override
	public DependencyContext createCopy() {
		return new CompletableFutureDependencyImpl<F>(m_comp, this);
	}

	@Override
	public Class<?> getAutoConfigType() {
		return null; // we don't support auto config mode
	}

	// ---------- ComponentDependencyDeclaration interface -----------

	/**
	 * Returns the name of this dependency (a generic name with optional info
	 * separated by spaces). The DM Shell will use this method when displaying
	 * the dependency
	 **/
	@Override
	public String getSimpleName() {
		return m_future.toString();
	}

	/**
	 * Returns the name of the type of this dependency. Used by the DM shell
	 * when displaying the dependency.
	 **/
	@Override
	public String getType() {
		return "future";
	}
	
	/**
	 * Called by DM component implementation when all required dependencies are satisfied.
	 */
    @Override
    public void invokeCallback(EventType type, Event ... events) {
        try {
            switch (type) {
            case ADDED:
                if (m_add != null) {
                    // Inject result by reflection on a method name
                    injectByReflection(events[0].getEvent());
                    return;
                }
                F result = events[0].getEvent();
                if (m_accept2 != null) {
                    if (m_accept2Type != null) {
                        // find the component instance that matches the given type
                        Object componentInstance = Stream.of(getComponentContext().getInstances())
                            .filter(instance -> Helpers.getClass(instance).equals(m_accept2Type))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                "accept callback is not on one of the component instances: " + m_accept2 + " (type=" + m_accept2Type + ")"));                                    

                        m_accept2.accept(componentInstance, result);
                    } else {
                        // invoke a method in the main component instance that will handle the completed future.
                        m_accept2.accept(getComponentContext().getInstance(), result);
                    }
                } else {
                    // Just invoke the Consumer with the completed future
                    m_accept.accept(result);
                }
                break;
                
            default:
                break;
            }
        } catch (Throwable exc) {
            super.getComponentContext().getLogger().log(LogService.LOG_ERROR, "completable future failed", exc);
        }
    }

	// ---------- Private methods -----------

    /**
     * Triggers component activation when the future has completed.
     * @param result
     * @param error
     */
    private void completed(F result, Throwable error) {
		if (error != null) {
			super.getComponentContext().getLogger().log(LogService.LOG_ERROR, "completable future failed", error);
		} else {
		    // Will trigger component activation (if other dependencies are satisfied), and our invokeCallback method will then be called.
		    m_component.handleEvent(this, EventType.ADDED, new Event(result));
		}
	}
    
    /**
     * Injects the completed future result in a method by reflection.
     * We try to find a method which has in its signature a parameter that is compatible with the future result
     * (including any interfaces the result may implements).
     * 
     * @param result the result of the completable future.
     */
    private void injectByReflection(Object result) {
        List<Class<?>> types = new ArrayList<>();
        Class<?> currentClazz = result.getClass();
        
        while (currentClazz != null && currentClazz != Object.class) {
            types.add(currentClazz);
            Stream.of(currentClazz.getInterfaces()).forEach(types::add);
            currentClazz = currentClazz.getSuperclass();
        }
        
        Class<?>[][] classes = new Class<?>[types.size() + 1][1];
        Object[][] results = new Object[types.size() + 1][1];
        for (int i = 0; i < types.size(); i ++) {
            classes[i] = new Class<?>[] { types.get(i) };
            results[i] = new Object[] { result };
        }
        classes[types.size()] = new Class<?>[0];
        results[types.size()] = new Object[0];        
        m_component.invokeCallbackMethod(getInstances(), m_add, classes, results);
    }
}
