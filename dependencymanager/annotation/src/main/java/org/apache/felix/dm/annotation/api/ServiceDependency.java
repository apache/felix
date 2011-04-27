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
 * Annotates a method or a field for injecting a Service Dependency. When applied on a class 
 * field, optional unavailable dependencies are injected with a NullObject.
 * 
 * <h3>Usage Examples</h3>
 * Here, the MyComponent component is injected with a dependency over a "MyDependency" service
 * 
 * <blockquote><pre>
 * &#64;Component
 * class MyComponent {
 *     &#64;ServiceDependency(timeout=15000)
 *     MyDependency dependency;
 * </pre></blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ServiceDependency
{
    /**
     * The type if the service this dependency is applying on. By default, the method parameter 
     * (or the class field) is used as the type.
     */
    Class<?> service() default Object.class;

    /**
     * The Service dependency OSGi filter.
     */
    String filter() default "";

    /**
     * The class for the default implementation, if the dependency is not available.
     */
    Class<?> defaultImpl() default Object.class;

    /**
     * Whether the Service dependency is required or not.
     */
    boolean required() default true;

    /**
     * The callback method to be invoked when the service is available. This attribute is only meaningful when 
     * the annotation is applied on a class field.
     */
    String added() default "";

    /**
     * The callback method to be invoked when the service properties have changed.
     */
    String changed() default "";

    /**
     * The callback method to invoke when the service is lost.
     */
    String removed() default "";
    
    /** 
     * The max time in millis to wait for the dependency availability. 
     * Specifying a positive number allow to block the caller thread between service updates. Only
     * useful for required stateless dependencies that can be replaced transparently.
     * A Dynamic Proxy is used to wrap the actual service dependency (which must be an interface). 
     * When the dependency goes away, an attempt is made to replace it with another one which satisfies 
     * the service dependency criteria. If no service replacement is available, then any method invocation 
     * (through the dynamic proxy) will block during a configurable timeout. On timeout, an unchecked 
     * <code>IllegalStateException</code> exception is raised (but the service is not deactivated).<p>
     * Notice that the changed/removed callbacks are not used when the timeout parameter is > -1.
     * <p> 
     * 
     * -1 means no timeout at all (default). 0 means that invocation on a missing service will fail 
     * immediately. A positive number represents the max timeout in millis to wait for the service availability.
     * 
     * <p> Sample Code:<p>
     * <blockquote><pre>
     * &#64;Component
     * class MyServer implements Runnable {
     *   &#64;ServiceDependency(timeout=15000)
     *   MyDependency dependency;.
     *   
     *   &#64;Start
     *   void start() {
     *     (new Thread(this)).start();
     *   }
     *   
     *   public void run() {
     *     try {
     *       dependency.doWork();
     *     } catch (IllegalStateException e) {
     *       t.printStackTrace();
     *     }
     *   }   
     * </pre></blockquote>
     */
    long timeout() default -1;
    
    /**
     * The name used when dynamically configuring this dependency from the init method.
     * Specifying this attribute allows to dynamically configure the dependency 
     * <code>filter</code> and <code>required</code> flag from the Service's init method.
     * All unnamed dependencies will be injected before the init() method; so from the init() method, you can
     * then pick up whatever information needed from already injected (unnamed) dependencies, and configure dynamically
     * your named dependencies, which will then be calculated once the init() method returns.
     * 
     * <p> Usage example of a Service whose dependency filter is configured from ConfigAdmin:
     * 
     * <blockquote><pre>
     *  &#47;**
     *    * A Service whose service dependency "otherService" filter is configured from ConfigAdmin
     *    *&#47;
     *  &#64;Service
     *  class X {
     *      private Dictionary m_config;
     *      
     *      &#47;**
     *       * Initialize our service from config ... and store the config for later usage (from our init method)
     *       *&#47; 
     *      &#64;ConfigurationDependency(pid="MyPid")
     *      void configure(Dictionary conf) {
     *           m_config = config;
     *      }
     * 
     *      &#47;**
     *       * All unnamed dependencies are injected: we can now configure other named
     *       * dependencies, using the already injected configuration.
     *       * The returned Map will be used to configure our "otherService" Dependency.
     *       *&#47;
     *      &#64;Init
     *      Map init() {
     *          return new HashMap() {{
     *              put("otherService.filter", m_config.get("filter"));
     *              put("otherService.required", m_config.get("required"));
     *          }};
     *      } 
     *
     *      &#47;**
     *       * This named dependency filter/required flag will be configured by our init method (see above).
     *       *&#47;
     *      &#64;ServiceDependency(name="otherService") 
     *      void bindOtherService(OtherService other) {
     *      }
     *      
     *      &#47;**
     *       * All dependencies are injected and our service is now ready to be published.
     *       * Notice that you can also use the publisher service attribute if you need 
     *       * to take control on service exposition.
     *       *&#47;
     *      &#64;Start
     *      void start() {
     *      }
     *  }
     *  </pre></blockquote>
     */
    String name() default "";
    
    /**
     * Returns true if the dependency service properties must be published along with the service. 
     * Any additional service properties specified directly are merged with these.
     * @return true if dependency service properties must be published along with the service, false if not.
     */
    boolean propagate() default false;
}
