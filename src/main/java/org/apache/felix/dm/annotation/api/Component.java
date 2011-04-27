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
 * much component instances must be created, then you can use the <code>factorySet</code> 
 * annotation attribute.<p> 
 * If a <code>factorySet</code> attribute is set, the component is not started automatically 
 * during bundle startup, and a <code>java.util.Set&lt;Dictionary&gt;</code> 
 * object is registered into the OSGi registry on behalf of the component. This Set will act 
 * as a Factory API, and another component may use this Set and add some configuration 
 * dictionaries in it, in order to fire some component activations (there is one component 
 * instantiated per dictionary, which is passed to component instances via a configurable 
 * callback method).
 *
 * <h3>Usage Examples</h3>
 * 
 * <p> Here is a sample showing a X component, which depends on a configuration dependency:<p>
 * <blockquote>
 * 
 * <pre>
 * &#47;**
 *   * This component will be activated once the bundle is started and when all required dependencies
 *   * are available.
 *   *&#47;
 * &#64;Component
 * class X implements Z {
 *     &#64;ConfigurationDependency(pid="MyPid")
 *     void configure(Dictionary conf) {
 *          // Configure or reconfigure our component.
 *     }
 *   
 *     &#64;Start
 *     void start() {
 *         // Our component is starting and is about to be registered in the OSGi registry as a Z service.
 *     }
 *   
 *     public void doService() {
 *         // ...
 *     }   
 * }
 * </pre>
 * </blockquote>
 * 
 * Here is a sample showing how a Y component may dynamically instantiate several X component instances, 
 * using the {@link #factorySet()} attribute:<p>
 * <blockquote>
 * 
 * <pre>
 *  &#47;**
 *    * All component instances will be created/updated/removed by the "Y" component
 *    *&#47;
 *  &#64;Component(factorySet="MyComponentFactory", factoryConfigure="configure")
 *  class X implements Z {                 
 *      void configure(Dictionary conf) {
 *          // Configure or reconfigure our component. The conf is provided by the factory,
 *          // and all public properties (which don't start with a dot) are propagated with the
 *          // service properties specified in the properties annotation attribute.
 *      }
 * 
 *      &#64;ServiceDependency
 *      void bindOtherService(OtherService other) {
 *          // store this require dependency
 *      }
 *      
 *      &#64;Start
 *      void start() {
 *          // Our component is starting and is about to be registered in the OSGi registry as a Z service.
 *      } 
 *      
 *      public void doService() {
 *          // ...
 *      }   
 *  }
 * 
 *  &#47;**
 *    * This class will instantiate some X component instances
 *    *&#47;
 *  &#64;Component 
 *  class Y {
 *      &#64;ServiceDependency(filter="(dm.factory.name=MyComponentFactory)")
 *      Set&lt;Dictionary&gt; _XFactory; // This Set acts as a Factory API for creating X component instances.
 *    
 *      &#64;Start
 *      void start() {
 *          // Instantiate a X component instance
 *          Dictionary x1 = new Hashtable() {{ put("foo", "bar1"); }};
 *          _XFactory.add(x1);
 *      
 *          // Instantiate another X component instance
 *          Dictionary x2 = new Hashtable() {{ put("foo", "bar2"); }};
 *          _XFactory.add(x2);
 *      
 *          // Update the first X component instance
 *          x1.put("foo", "bar1_modified");
 *          _XFactory.add(x1);
 *      
 *          // Destroy x1/x2 components (Notice that invoking XFactory.clear() will destroy all X component  instances).
 *          _XFactory.remove(x1);
 *          _XFactory.remove(x2); 
 *      }
 *  }
 * </pre>
 * 
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
     */
    Class<?>[] provides() default {};

    /**
     * Sets list of provided service properties.
     */
    Property[] properties() default {};

    /**
     * Returns the name of the <code>Factory Set</code> used to dynamically instantiate this component.
     * When you set this attribute, a <code>java.util.Set&lt;java.lang.Dictionary&gt;</code> OSGi Service will 
     * be provided with a <code>dm.factory.name</code> service property matching your specified <code>factorySet</code> attribute.
     * This Set will be provided once the component bundle is started, even if required dependencies are not available, and the
     * Set will be unregistered from the OSGi registry once the component bundle is stopped or being updated.<p>
     * So, basically, another component may then be injected with this set in order to dynamically instantiate some component instances:
     * <ul>
     * <li> Each time a new Dictionary is added into the Set, then a new instance of the annotated component will be instantiated.</li>
     * <li> Each time an existing Dictionary is re-added into the Set, then the corresponding component instance will be updated.</li>
     * <li> Each time an existing Dictionary is removed from the Set, then the corresponding component instance will be destroyed.</li>
     * </ul>
     * 
     * <p>The dictionary registered in the Set will be provided to the created component instance using a callback method that you can 
     * optionally specify in the {@link Component#factoryConfigure()} attribute. Each public properties from that dictionary 
     * (which don't start with a dot) will be propagated along with the annotated component service properties.
     */
    String factorySet() default "";

    /**
     * Sets "configure" callback method name to be called with the factory configuration. This attribute only makes sense if the 
     * {@link #factorySet()} attribute is used. If specified, then this attribute references a callback method, which is called 
     * for providing the configuration supplied by the factory that instantiated this component. The current component service properties will be 
     * also updated with all public properties (which don't start with a dot).
     */
    String factoryConfigure() default "";
    
    /**
     * Sets the static method used to create the components implementation instance.
     */
    String factoryMethod() default "";    
}
