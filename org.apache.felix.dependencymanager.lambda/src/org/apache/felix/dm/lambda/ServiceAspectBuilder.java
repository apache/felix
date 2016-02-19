package org.apache.felix.dm.lambda;

/**
 * Builds a Dependency Manager Aspect Component.
 * The aspect will be applied to any service that matches the specified interface and filter (if any). For each matching service an aspect will be created based 
 * on the aspect implementation class. 
 * The aspect will be registered with the same interface and properties as the original service, plus any extra properties you supply here.
 * Multiple Aspects of the same service are chained and ordered using aspect ranks.
 * 
 * <p> Code example which provides a "LogService" aspect that performs spell-checking of each log message. 
 * The aspect decorates a LogService. The aspect also depends on an Dictionary service that is internally used to perform log spell checking.
 * The LogService and Dictionary services are injected in the aspect implementation using reflection on class fields:
 * 
 * <pre>{@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *       aspect(LogService.class, asp -> asp.impl(SpellCheckLogAspect.class).rank(10).withSvc(Dictionary.class));
 *    }
 * }} </pre>
 *
 * Same example, but using callbacks for injecting LogService and Dictionary services in the aspect implementation class:
 * 
 * <pre>{@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *       aspect(LogService.class, asp -> asp
 *          .impl(SpellCheckLogAspect.class).rank(10)
 *          .add(SpellCheckLogAspect::setLogService)
 *          .withSvc(Dictionary.class, svc -> svc.add(SpellCheckLogAspect::setDictionary)));
 *    }
 * }} </pre>
 *
 * @param <T> the aspect service
 */
public interface ServiceAspectBuilder<T> extends ComponentBuilder<ServiceAspectBuilder<T>>, ServiceCallbacksBuilder<T, ServiceAspectBuilder<T>> {
    /**
     * Specifies the aspect service filter. 
     * 
     * @param filter the filter condition to use with the service interface the aspect will apply on
     * @return this builder
     */
    ServiceAspectBuilder<T> filter(String filter);
    
    /**
     * Specifies the aspect ranking. Aspects of a given service are ordered by their ranking property.
     * 
     * @param ranking the aspect ranking
     * @return this builder
     */
    ServiceAspectBuilder<T> rank(int ranking);
    
    /**
     * Injects the aspect in all fields matching the aspect type.
     * @return this builder
     */
    ServiceAspectBuilder<T> autoConfig();
    
    /**
     * Configures whether or not the aspect service can be injected in all fields matching the aspect type.
     *  
     * @param autoConfig true if the aspect service can be injected in all fields matching the dependency type
     * @return this builder
     */
    ServiceAspectBuilder<T> autoConfig(boolean autoConfig);
    
    /**
     * Injects the aspect service on the field with the given name.
     * 
     * @param field the field name where the aspect service must be injected
     * @return this builder
     */
    ServiceAspectBuilder<T> autoConfig(String field);     
}
