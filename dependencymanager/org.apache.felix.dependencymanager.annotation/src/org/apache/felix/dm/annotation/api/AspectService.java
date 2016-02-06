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
package org.apache.felix.dm.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an Aspect service. Aspects allow you to define an interceptor, or chain of interceptors 
 * for a service (to add features like caching or logging, etc ...). The dependency manager intercepts 
 * the original service, and allows you to execute some code before invoking the original service ...
 * The aspect will be applied to any service that matches the specified interface and filter and 
 * will be registered with the same interface and properties as the original service, plus any 
 * extra properties you supply here. It will also inherit all dependencies, 
 * and if you declare the original service as a member it will be injected.
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> Here, the AspectService is registered into the OSGI registry each time an InterceptedService
 * is found from the registry. The AspectService class intercepts the InterceptedService, and decorates
 * its "doWork()" method. This aspect uses a rank with value "10", meaning that it will intercept some
 * other eventual aspects with lower ranks. The Aspect also uses a service property (param=value), and 
 * include eventual service properties found from the InterceptedService:
 * <blockquote>
 * <pre>
 * 
 * &#64;AspectService(ranking=10))
 * &#64;Property(name="param", value="value")
 * class AspectService implements InterceptedService {
 *     // The service we are intercepting (injected by reflection)
 *     protected InterceptedService intercepted;
 *   
 *     public void doWork() {
 *        intercepted.doWork();
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AspectService
{
    /**
     * Sets the service interface to apply the aspect to. By default, the directly implemented interface is used.
     * @return the service aspect
     */
    Class<?> service() default Object.class;

    /**
     * Sets the filter condition to use with the service interface this aspect is applying to.
     * @return the service aspect filter
     */
    String filter() default "";
    
    /**
     * Sets Additional properties to use with the aspect service registration
     * @return the aspect service properties.
     */
    Property[] properties() default {};
    
    /**
     * Sets the ranking of this aspect. Since aspects are chained, the ranking defines the order in which they are chained.
     * Chain ranking is implemented as a service ranking so service lookups automatically retrieve the top of the chain.
     * @return the aspect service rank
     */
    int ranking();
    
    /**
     * Sets the field name where to inject the original service. By default, the original service is injected
     * in any attributes in the aspect implementation that are of the same type as the aspect interface.
     * @return the field used to inject the original service
     */
    String field() default "";
    
    /**
     * The callback method to be invoked when the original service is available. This attribute can't be mixed with
     * the field attribute.
     * @return the add callback
     */
    String added() default "";

    /**
     * The callback method to be invoked when the original service properties have changed. When this attribute is used, 
     * then the added attribute must also be used.
     * @return the changed callback
     */
    String changed() default "";

    /**
     * The callback method to invoke when the service is lost. When this attribute is used, then the added attribute 
     * must also be used.
     * @return the remove callback
     */
    String removed() default "";
    
    /**
     * name of the callback method to invoke on swap.
     * @return the swap callback
     */
    String swap() default "";

    /**
     * Sets the static method used to create the AspectService implementation instance. The
     * default constructor of the annotated class is used. The factoryMethod can be used to provide a specific
     * aspect implements, like a DynamicProxy.
     * @return the aspect service factory method
     */
    String factoryMethod() default "";
}
