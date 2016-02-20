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
package org.apache.felix.dm.lambda;

import java.util.concurrent.Executor;

import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.lambda.callbacks.CbFuture;
import org.apache.felix.dm.lambda.callbacks.InstanceCbFuture;

/**
 * Defines a builder for a CompletableFuture dependency.
 * <p> Using such dependency allows your component to wait for the completion of a given asynchronous task
 * represented by a standard jdk <code>CompletableFuture</code> object.
 * 
 * A FutureDependency is required and unblock the Component once the CompletableFuture result has completed.
 * 
 * <h3>Usage Example</h3>
 * 
 * <p> Here is an Activator that downloads a page from the web and injects the string result to the component before it is started.
 * When the web page is downloaded, the result is injected in the MyComponent::setPage method and
 * the component is then called in its "start" method:
 * 
 * <pre>{@code
 * 
 * public class Activator extends DependencyManagerActivator {
 *   public void init(BundleContext ctx, DependencyManager dm) throws Exception {  
 *      // Download a web page asynchronously, using a CompletableFuture:
 *        	
 *      String url = "http://felix.apache.org/";
 *      CompletableFuture<String> page = CompletableFuture.supplyAsync(() -> downloadSite(url));				
 *
 *      // The component depends on a log service and on the content of the Felix site.
 *      // The lambda passed to the "withFuture" method configures the callback that is 
 *      // invoked with the result of the CompletableFuture (the page content).
 *      
 *      component(comp -> comp
 *          .impl(MyComponent.class)
 *          .withService(LogService.class)
 *          .withFuture(page, result -> result.complete(MyComponent::setPage)));
 *   }
 * }
 * 
 * public class MyComponent {
 *   volatile LogService log; // injected.
 *   
 *   void setPage(String page) {
 *      // injected by the FutureDependency.
 *   }
 *   
 *   void start() {
 *      // all required dependencies injected.
 *   }
 * }
 * 
 * }</pre>
 * 
 * @param <F> the type of the CompletableFuture result.
 */
public interface FutureDependencyBuilder<F> extends DependencyBuilder<Dependency> { 
    /**
     * Sets the callback method name to invoke on the component implementation, once the CompletableFuture has completed.
     * @param callback the callback method name to invoke on the component implementation, once the CompletableFuture on which we depend has completed.
     * @return this dependency.
     */
    FutureDependencyBuilder<F> complete(String callback);
    
    /**
     * Sets the callback instance method name to invoke on a given Object instance, once the CompletableFuture has completed.
     * @param callbackInstance the object instance on which the callback must be invoked
     * @param callback the callback method name to invoke on Object instance, once the CompletableFuture has completed.
     * @return this dependency.
     */
    FutureDependencyBuilder<F> complete(Object callbackInstance, String callback);
    
    /**
     * Sets the function to invoke when the future task has completed. The function is from one of the Component implementation classes, and it accepts the
     * result of the completed future.
     * 
     * @param <T> the type of the CompletableFuture result.
     * @param callback the function to perform when the future task as completed. 
     * @return this dependency
     */
    <T> FutureDependencyBuilder<F> complete(CbFuture<T, ? super F> callback);
    
    /**
     * Sets the function to invoke asynchronously when the future task has completed. The function is from one of the Component implementation classes, 
     * and it accepts the result of the completed future.
     * 
     * @param <T> the type of the CompletableFuture result.
     * @param callback the function to perform when the future task as completed.
     * @param async true if the callback should be invoked asynchronously using the default jdk execution facility, false if not.
     * @return this dependency
     */
    <T> FutureDependencyBuilder<F> complete(CbFuture<T, ? super F> callback, boolean async);

    /**
     * Sets the function to invoke asynchronously when the future task has completed. The function is from one of the Component implementation classes, 
     * and it accepts the result of the completed future.
     * 
     * @param <T> the type of the CompletableFuture result.
     * @param callback the function to perform when the future task as completed. 
     * @param executor the executor used to schedule the callback.
     * @return this dependency
     */
    <T> FutureDependencyBuilder<F> complete(CbFuture<T, ? super F> callback, Executor executor);   
        
    /**
     * Sets the callback instance to invoke when the future task has completed. The callback is a Consumer instance which accepts the
     * result of the completed future.
     * @param callback a Consumer instance which accepts the result of the completed future.
     * @return this dependency
     */
    FutureDependencyBuilder<F> complete(InstanceCbFuture<? super F> callback);
    
    /**
     * Sets the callback instance to invoke when the future task has completed. The callback is a Consumer instance which accepts the
     * result of the completed future.
     * 
     * @param callback a Consumer instance which accepts the result of the completed future.
     * @param async true if the callback should be invoked asynchronously using the default jdk execution facility, false if not.
     * @return this dependency
     */
    FutureDependencyBuilder<F> complete(InstanceCbFuture<? super F> callback, boolean async);

    /**
     * Sets the callback instance to invoke when the future task has completed. The callback is a Consumer instance which accepts the
     * result of the completed future.
     * @param callback the action to perform when the future task as completed. 
     * @param executor the executor to use for asynchronous execution of the callback.
     * @return this dependency
     */
    FutureDependencyBuilder<F> complete(InstanceCbFuture<? super F> callback, Executor executor);   
}
