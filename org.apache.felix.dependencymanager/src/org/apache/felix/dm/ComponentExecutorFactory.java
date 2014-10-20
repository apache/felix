package org.apache.felix.dm;

import java.util.concurrent.Executor;

/**
 * A ComponentExecutorFactory service allows to provide a custom Executor (typically a threadpool) 
 * for a given Component.<p>
 * When a Component is added to a DependencyManager, the ComponentExecutorFactory is then 
 * used to create an Executor for that component and the Executor will be used to execute 
 * all the DM internal code related to the component's dependency management. The Executor 
 * will also be used to invoke all the component's lifecycle callbacks.<p>
 * 
 * All the component dependency management/lifecycle callbacks will be handled in the Executor, 
 * but serially, in FIFO order (it's actually a kind of actor thread model). This means that you then 
 * don't have to deal with synchronizations anymore in your component dependencies/lifecycle callbacks; 
 * and multiple components will be managed and started concurrently, in parallel.<p>
 * 
 * If you want to ensure that the ComponentExecutorFactory is registered in the OSGI registry 
 * before all other DM components are added to their respective DependencyManagers, then you 
 * can simply use the "org.apache.felix.dependencymanager.parallel" OSGi system property, which 
 * can specify the list of components which must wait for the ComponentExecutorFactory service.
 * This property value can be set to a wildcard ("*"), or a list of components implementation class prefixes 
 * (comma separated). So, all components class names starting with the specified prefixes will be cached 
 * until the ComponentExecutorFactory service is registered (In this way, it is not necessary to use
 * the StartLevel service if you want to ensure that all components are started concurrently).
 * 
 * Some class name prefixes can also be negated (using "!"), in order to exclude some components from the 
 * list of components using the ComponentExecutorFactory service.<p>
 * 
 * Notice that if the ComponentExecutorFactory itself and all it's dependent services are defined using 
 * the Dependency Manager API, then you have to list the package of such components with a "!" 
 * prefix, in order to indicate that those components must not wait for a ComponentExecutorFactory service
 * (since they are part of the ComponentExecutorFactory implementation !).<p>
 * 
 * Examples:
 * 
 *  * <blockquote>
 * 
 * <pre>
 * org.apache.felix.dependencymanager.parallel=*   
 *      -> means all components must be cached until a ComponentExecutorFactory comes up.
 * 
 * org.apache.felix.dependencymanager.parallel=foo.bar, foo.zoo
 *      -> means only components whose implementation class names are starting with "foo.bar" or "foo.zoo" 
 *      must be handled using an Executor returned by the ComponentExecutorFactory service.
 * 
 * org.apache.felix.dependencymanager.parallel=!foo.threadpool, *
 *      -> means all components must be delayed until the ComponentExecutorFactory comes up, except the 
 *      components whose implementations class names are starting with "foo.threadpool" prefix). 
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a> 
 */
public interface ComponentExecutorFactory {
    /**
     * Returns an Executor (typically or thread pool) used to handle the component's dependencies 
     * and invoke all component lifecycle callbacks.
     * 
     * @param component the Component to be managed by the returned Executor
     * @return an Executor used to manage the given component, or null if the component must not be managed using any executor.
     */
    Executor getExecutorFor(Component component);
}
