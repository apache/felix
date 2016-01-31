package org.apache.felix.dm.lambda;

import java.util.concurrent.Executor;

import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.lambda.callbacks.CbFuture;
import org.apache.felix.dm.lambda.callbacks.CbTypeFuture;

/**
 * Defines a builder for a CompletableFuture dependency.
 * Using such dependency allows your component to wait for the completion of a given asynchronous task
 * represented by a standard jdk <code>CompletableFuture</code> object.
 * 
 * A FutureDependency is required and unblock the Component once the CompletableFuture result has completed.
 * 
 * <h3>Usage Example</h3>
 * 
 * <p> Here is an Activator that downloads a page from the web and injects the string result to a component.
 * When the web page is downloaded, the result is injected in the MyComponent::setPage method and
 * the component is then called in its "start" method:
 * 
 * <pre>{@code
 * 
 * public class Activator extends DependencyManagerActivator {
 *   public void activate() throws Exception {    	
 *      String url = "http://felix.apache.org/";
 *      CompletableFuture<String> page = CompletableFuture.supplyAsync(() -> downloadSite(url));				
 *
 *      // The component depends on a log service and on the content of the Felix site.
 *      // The lambda passed to the "withFuture" method configures the callback that is 
 *      // invoked with the result of the CompletableFuture (the page content).
 *      component(comp -> comp
 *          .impl(MyComponent.class)
 *          .withService(LogService.class)
 *          .withFuture(page, result -> result.cb(MyComponent::setPage)));
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
     * Sets the callback method name to invoke on the component instances, once the CompletableFuture has completed.
     * @param callback the callback method name to invoke on the component instances, once the CompletableFuture on which we depend has completed.
     * @return this dependency.
     */
    FutureDependencyBuilder<F> cb(String callback);
    
    /**
     * Sets the function to invoke when the future task has completed. The function is from one of the Component implementation classes, and it accepts the
     * result of the completed future.
     * 
     * @param <T> the type of the CompletableFuture result.
     * @param callback the function to perform when the future task as completed. 
     * @return this dependency
     */
    <T> FutureDependencyBuilder<F> cb(CbTypeFuture<T, ? super F> callback);
    
    /**
     * Sets the function to invoke asynchronously when the future task has completed. The function is from one of the Component implementation classes, 
     * and it accepts the result of the completed future.
     * 
     * @param <T> the type of the CompletableFuture result.
     * @param callback the function to perform when the future task as completed.
     * @param async true if the callback should be invoked asynchronously using the default jdk execution facility, false if not.
     * @return this dependency
     */
    <T> FutureDependencyBuilder<F> cb(CbTypeFuture<T, ? super F> callback, boolean async);

    /**
     * Sets the function to invoke asynchronously when the future task has completed. The function is from one of the Component implementation classes, 
     * and it accepts the result of the completed future.
     * 
     * @param <T> the type of the CompletableFuture result.
     * @param callback the function to perform when the future task as completed. 
     * @param executor the executor used to schedule the callback.
     * @return this dependency
     */
    <T> FutureDependencyBuilder<F> cb(CbTypeFuture<T, ? super F> callback, Executor executor);   
        
    /**
     * Sets the callback instance method name to invoke on a given Object instance, once the CompletableFuture has completed.
     * @param callbackInstance the object instance on which the callback must be invoked
     * @param callback the callback method name to invoke on Object instance, once the CompletableFuture has completed.
     * @return this dependency.
     */
    FutureDependencyBuilder<F> cbi(Object callbackInstance, String callback);
    
    /**
     * Sets the callback instance to invoke when the future task has completed. The callback is a Consumer instance which accepts the
     * result of the completed future.
     * @param callback a Consumer instance which accepts the result of the completed future.
     * @return this dependency
     */
    FutureDependencyBuilder<F> cbi(CbFuture<? super F> callback);
    
    /**
     * Sets the callback instance to invoke when the future task has completed. The callback is a Consumer instance which accepts the
     * result of the completed future.
     * 
     * @param callback a Consumer instance which accepts the result of the completed future.
     * @param async true if the callback should be invoked asynchronously using the default jdk execution facility, false if not.
     * @return this dependency
     */
    FutureDependencyBuilder<F> cbi(CbFuture<? super F> callback, boolean async);

    /**
     * Sets the callback instance to invoke when the future task has completed. The callback is a Consumer instance which accepts the
     * result of the completed future.
     * @param callback the action to perform when the future task as completed. 
     * @param executor the executor to use for asynchronous execution of the callback.
     * @return this dependency
     */
    FutureDependencyBuilder<F> cbi(CbFuture<? super F> callback, Executor executor);   
}
