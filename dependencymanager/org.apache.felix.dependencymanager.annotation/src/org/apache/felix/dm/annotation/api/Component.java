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
 * Annotates an OSGi Component class with its dependencies. Components are the main building 
 * blocks for OSGi applications. They can publish themselves as a service, and/or they can have 
 * dependencies. These dependencies will influence their life cycle as component will only be 
 * activated when all required dependencies are available. 
 * By default, all directly implemented interfaces are registered into the OSGi registry,
 * and the component is instantiated automatically, when the component bundle is started and 
 * when the component dependencies are available. If you need to take control of when and how 
 * much component instances must be created, then you can use the <code>factoryName</code> 
 * annotation attribute.<p> 
 * If a <code>factoryPid</code> attribute is set, the component is not started automatically 
 * during bundle startup, and the component can then be instantiated multiple times using
 * Configuration Admin "Factory Configurations".
 *
 * <h3>Usage Examples</h3>
 * 
 * Here is a sample showing a Hello component, which depends on a configuration dependency:
 * <blockquote>
 * 
 * <pre>
 * &#47;**
 *   * This component will be activated once the bundle is started and when all required dependencies
 *   * are available.
 *   *&#47;
 * &#64;Component
 * class Hello implements HelloService {
 *     &#64;ConfigurationDependency(pid="my.pid")
 *     void configure(Dictionary conf) {
 *          // Configure or reconfigure our component.
 *     }
 *   
 *     &#64;Start
 *     void start() {
 *         // Our component is starting and is about to be registered in the OSGi registry as a Z service.
 *     }   
 * }
 * </pre>
 * </blockquote>
 * 
 * Here is a sample showing how a HelloFactory component may dynamically instantiate several Hello component instances, 
 * using Configuration Admin "Factory Configurations":
 * <blockquote>
 * 
 * <pre>
 *  &#47;**
 *    * All component instances will be created/updated/removed by the "HelloFactory" component
 *    *&#47;
 *  &#64;Component(factoryPid="my.factory.pid")
 *  class Hello implements HelloService {                 
 *      void updated(Dictionary conf) {
 *          // Configure or reconfigure our component. The conf is provided by the factory,
 *      }
 *       
 *      &#64;Start
 *      void start() {
 *          // Our component is starting and is about to be registered in the OSGi registry as a Hello service.
 *      }       
 *  } 
 *
 *  &#47;**
 *    * This class will instantiate some Hello component instances
 *    *&#47;
 *  &#64;Component 
 *  class HelloFactory {
 *     &#64;ServiceDependency
 *     void bind(ConfigurationAdmin cm) {
 *          // instantiate a first instance of Hello component
 *          Configuration c1 = cm.createFactoryConfiguration("my.factory.pid", "?");
 *          Hashtable props = new Hashtable();
 *          newprops.put("key", "value1");
 *          c1.update(props);
 *          
 *          // instantiate another instance of Hello component
 *          Configuration c2 = cm.createFactoryConfiguration("my.factory.pid", "?");
 *          props = new Hashtable();
 *          newprops.put("key", "value2");
 *          c2.update(props);
 *          
 *          // destroy the two instances of X component
 *          c1.delete();
 *          c2.delete();
 *     }
 *  }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Component
{
    /**
     * Sets list of provided interfaces. By default, the directly implemented interfaces are provided.
     * @return the provided interfaces
     */
    Class<?>[] provides() default {};
        
    /**
     * Sets the static method used to create the components implementation instance.
     * @return the factory method used to instantiate the component
     */
    String factoryMethod() default "";        
    
    /**
     * Returns the factory pid whose configurations will instantiate the annotated service class. Leaving this attribute
     * unset means your component is a singleton. If you specify a factory pid, then this component will be instantiated
     * each time a corresponding factory configuration is created.
     * 
     * @return the factory pid
     */
    String factoryPid() default "";
    
    /**
     * The Update method to invoke (defaulting to "updated"), when a factory configuration is created or updated.
     * Only used if the factoryPid attribute is set.
     * The updated callback supported signatures are the following:<p>
     * <ul><li>callback(Dictionary) 
     * <li>callback(Component, Dictionary) 
     * <li>callback(Component, Configuration ... configTypes) // type safe configuration interface(s)
     * <li>callback(Configuration ... configTypes) // type safe configuration interface(s)
     * <li>callback(Dictionary, Configuration ... configTypes) // type safe configuration interfaces(s)
     * <li>callback(Component, Dictionary, Configuration ... configTypes) // type safe configuration interfaces(s)
     * </ul>
     * @return the updated callback
     */
    String updated() default "updated";

    /**
     * Returns true if the factory configuration properties must be published to the service properties. 
     * Only used if the factoryPid attribute is set.
     * @return true if configuration must be published along with the service, false if not.
     */
    boolean propagate() default false;

	/**
	 * The service scope for the service of this Component.
	 * 
	 * <p>
	 * If not specified, the {@link ServiceScope#SINGLETON singleton} service
	 * scope is used. 
	 */
	ServiceScope scope() default ServiceScope.SINGLETON;

    /**
     * Sets list of provided service properties. Since R7 version, Property annotation is repeatable and you can directly
     * apply it on top of the component class multiple times, instead of using the Component properties attribute.
     * @return the component properties.
     * @deprecated you can apply {@link Property} annotation directly on the component class.
     */
    Property[] properties() default {};
}
