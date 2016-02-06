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
 * Annotates an Adapater service. Adapters, like {@link AspectService}, are used to "extend" 
 * existing services, and can publish different services based on the existing one. 
 * An example would be implementing a management interface for an existing service, etc .... 
 * <p>When you annotate an adapter class with the <code>@AdapterService</code> annotation, it will be applied 
 * to any service that matches the implemented interface and filter. The adapter will be registered 
 * with the specified interface and existing properties from the original service plus any extra 
 * properties you supply here. If you declare the original service as a member it will be injected.
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> Here, the AdapterService is registered into the OSGI registry each time an AdapteeService
 * is found from the registry. The AdapterImpl class adapts the AdapteeService to the AdapterService.
 * The AdapterService will also have a service property (param=value), and will also include eventual
 * service properties found from the AdapteeService:
 * <blockquote>
 * <pre>
 * 
 * &#64;AdapterService(adapteeService = AdapteeService.class)
 * &#64;Property(name="param", value="value")
 * class AdapterImpl implements AdapterService {
 *     // The service we are adapting (injected by reflection)
 *     protected AdapteeService adaptee;
 *   
 *     public void doWork() {
 *        adaptee.mehod1();
 *        adaptee.method2();
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AdapterService
{
    /**
     * Sets the adapter service interface(s). By default, the directly implemented interface(s) is (are) used.
     * @return the adapter service interface(s)
     */
    Class<?>[] provides() default {};

    /**
     * Sets some additional properties to use with the adapter service registration. By default, 
     * the adapter will inherit all adaptee service properties.
     * @return some additional properties
     */
    Property[] properties() default {};

    /**
     * Sets the adaptee service interface this adapter is applying to.
     * @return the adaptee service interface this adapter is applying to.
     */
    Class<?> adapteeService();
    
    /**
     * Sets the filter condition to use with the adapted service interface.
     * @return the adaptee filter
     */
    String adapteeFilter() default "";
    
    /**
     * Sets the static method used to create the adapter service implementation instance.
     * By default, the default constructor of the annotated class is used.
     * @return the factory method
     */
    String factoryMethod() default "";
    
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
     * name of the callback method to invoke on swap.
     * @return the swap callback
     */
    String swap() default "";

    /**
     * The callback method to invoke when the service is lost. When this attribute is used, then the added attribute 
     * must also be used.
     * @return the remove callback
     */
    String removed() default "";
    
    /**
     * Specifies if adaptee service properties should be propagated to the adapter service.
     * @return the service propagation flag
     */
    boolean propagate() default true;
}
