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
package org.apache.felix.ipojo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declares a component type (needed to create instances of the component).
 * This annotation is mandatory to declares an iPOJO component.
 *
 * Its usual to find it on top of class definition:
 * <pre>
 *     {@code @Component}
 *     public class MyComponent {
 *         // ...
 *     }
 * </pre>
 *
 * But, it is also possible to have it associated to a
 * {@linkplain org.apache.felix.ipojo.annotations.Stereotype stereotyped} annotation definition:
 * <pre>
 *     {@code @Component}
 *     {@linkplain org.apache.felix.ipojo.annotations.Instantiate @Instantiate}
 *     {@linkplain org.apache.felix.ipojo.annotations.Stereotype @Stereotype}
 *     public @interface AutoInstantiatedComponent {
 *         // ...
 *     }
 * </pre>
 *
 * <h2>See also</h2>
 * <ul>
 *     <li><a href="http://felix.apache.org/documentation/subprojects/apache-felix-ipojo/apache-felix-ipojo-userguide/ipojo-advanced-topics/how-to-use-ipojo-factories.html">Use iPOJO Factories</a></li>
 * </ul>
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface Component {

    /**
     * Set if the component type is public.
     * @see #publicFactory()
     * @deprecated renamed to publicFactory.
     */
    boolean public_factory() default true;

    /**
     * Set if the component type is public or private (defaults to public).
     * A private factory does not expose a {@code Factory} service.
     * Only instances declared in the same bundle are created.
     * <pre>
     *     {@code @Component(publicFactory = false)}
     *     public class MyComponent {
     *         // ...
     *     }
     * </pre>
     * Default: {@literal true}
     */
    boolean publicFactory() default true;

    /**
     * Set the component type name.
     * <pre>
     *     {@code @Component(name = "my-component")}
     *     public class MyComponent {
     *         // ...
     *     }
     * </pre>
     * Default : implementation class name.
     */
    String name() default "";

    /**
     * Enable / Disable the architecture exposition (no {@code Architecture}
     * service will be exposed for component's instances).
     * <pre>
     *     {@code @Component(architecture = false)}
     *     public class MyComponent {
     *         // ...
     *     }
     * </pre>
     * Default : {@literal true}
     */
    boolean architecture() default true;

    /**
     * Set if the component is immediate.
     * By default, iPOJO tries to be as lazy as possible and will create the POJO instance at the last possible time.
     * Notice this setting is only effective when the component provides a service ({@linkplain org.apache.felix.ipojo.annotations.Provides @Provides}).
     * <pre>
     *     {@code @Component(immediate = true)}
     *     {@linkplain org.apache.felix.ipojo.annotations.Provides @Provides}
     *     public class MyComponent implements MyService {
     *         // ...
     *     }
     * </pre>
     * Default : {@literal false}
     */
    boolean immediate() default false;

    /**
     * Enable or disable the configuration propagation to service properties.
     * <pre>
     *     {@code @Component(propagation = false)}
     *     public class MyComponent {
     *         // ...
     *     }
     * </pre>
     * default: {@literal true}
     */
    boolean propagation() default true;

    /**
     * Set the Managed Service PID for Configuration Admin.
     * default no PID (i.e. the managed service will not be exposed).
     * <pre>
     *     {@code @Component(managedservice = "my.Pid")}
     *     public class MyComponent {
     *         // ...
     *     }
     * </pre>
     */
    String managedservice() default "";

    /**
     * Set the factory-method, if the pojo has to be created
     * from a static method. The specified method must be a static
     * method and return a pojo object.
     * By default, iPOJO uses the 'regular' constructor.
     * @see #factoryMethod()
     * @deprecated now is called <tt>factoryMethod</tt>.
     */
    String factory_method() default "";

    /**
     * Set the factory-method, if the pojo has to be created
     * from a static method. The specified method must be a static
     * method and return a pojo object.
     * By default, iPOJO uses the 'regular' constructor.
     * <pre>
     *     {@code @Component(factoryMethod = "createInstance")}
     *     public class MyComponent {
     *         // ...
     *         public static MyComponent createInstance() {
     *             return new MyComponent("some static configuration");
     *         }
     *     }
     * </pre>
     */
    String factoryMethod() default "";

    /**
     * Set the version of the component type.
     * <pre>
     *     {@code @Component(version = "1.3")}
     *     public class MyComponent {
     *         // ...
     *     }
     * </pre>
     */
    String version() default "";
}
