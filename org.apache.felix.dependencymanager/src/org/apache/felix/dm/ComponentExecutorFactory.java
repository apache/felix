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
package org.apache.felix.dm;

import java.util.concurrent.Executor;

/**
 * A <code>ComponentExecutorFactory</code> service can be registered by any management agent bundle 
 * in order to enable parallel activation of Components.<p>
 * 
 * A <code>ComponentExecutorFactory</code> is part of the new concurrency model that forms the basis 
 * of Dependency Manager 4.0. Let's first give a brief overview of the default thread model used when 
 * no ComponentExecutorFactory is used. Then we'll explain the rationale and the usage of a 
 * <code>ComponentExecutorFactory</code> service.
 * <p> 
 * 
 * <h3>Default Thread Model</h3>
 * 
 * By default, Dependency Manager uses a <b>lock-free/single thread</b> model:
 * <p><ul>
 * 
 * <li> When an external event that influence the state of a Component is taking place (for example, 
 * when a service dependency on which the Component is depending on is registered in the registry by 
 * a given thread), then DependencyManager does not perform any locking for the handling of the event. 
 * Instead of that, a job that will handle the event is inserted in an internal lock-free 
 * <b><code>Serial Queue</code></b> which is internally maintained in each Component.
 * 
 * <li> all jobs scheduled in the <code>Serial Queue</code> are then executed in FIFO order, by the first
 * thread which has triggered the first event. This avoid to use some blocking locks in DM internals, and 
 * also it simplifies the development of DM components, because all lifecycle callbacks 
 * (init/start/stop/destroy) and dependency injections are scheduled through the <code>Serial Queue</code>: 
 * This means that your component is not concurrently called in lifecycle callbacks and in dependency injection 
 * methods.
 * 
 * <li> Now let's describe which thread is executing the jobs scheduled in a Component <code>Serial Queue</code>: 
 * When a job (J1) is scheduled in the queue while it is empty, then the current thread becomes the "master"
 * and will immediately execute the </code>Serial Queue</code> tasks (synchronously). And if another thread 
 * triggers another event concurrently while the "master" thread is executing the job J1, then a job (J2) 
 * for this new event is just enqueued in the <code>Serial Queue</code>, but the other thread returns 
 * immediately to the caller, and the job J2 will then be executed by the "master" thread (after J1).
 * </ul>
 * 
 * <p>
 * This mechanism allows to serially handle all Component events (service dependencies) in FIFO order 
 * without maintaining any locks.
 * 
 * <h3>Enabling parallelism with a <code>ComponentExecutorFactory</code></h3>
 *  
 * As described above, all the external events that influence the state of a given component are handed by 
 * jobs scheduled in the <code>Serial Queue</code> of the Component, and the jobs are getting executed serially 
 * by a single "master" thread. So usually, bundles are started from a single thread, meaning that all Components
 * are then activated synchronously.
 * <p>
 * 
 * But when you register in the OSGi service registry a <code>ComponentExecutorFactory</code>, that factory 
 * will be used by DependencyManager to create an Executor of your choice for each Component, typically a shared 
 * threadpool configured by yourself. And all the Component <code>Serial Queues</code> will be executed using 
 * the Executor returned by the {@link #getExecutorFor(Component)} method.
 * However, jobs scheduled in the <code>Serial Queue</code> of a given Component are still executed one at a 
 * time, in FIFO order and the Component remains single threaded, and <b>independent Components 
 * may then each be managed and activated concurrently with respect to each other</b>.
 * <p>
 * If you want to ensure that all Components are initialized <b>after</b> the ComponentExecutorFactory is 
 * registered in the OSGI registry, you can use the "org.apache.felix.dependencymanager.parallel" OSGi 
 * system property which specifies the list of components which must wait for the ComponentExecutorFactory 
 * service. This property value can be set to a wildcard ("*"), or a list of components implementation class 
 * prefixes (comma separated). So, all components whose class name starts with the specified prefixes will be cached 
 * until the ComponentExecutorFactory service is registered (In this way, it is not necessary to use
 * the StartLevel service if you want to ensure that all components are started concurrently).
 * <p>
 * 
 * Some class name prefixes can also be negated (using "!"), in order to exclude some components from the 
 * list of components using the ComponentExecutorFactory service.
 * <p>
 * 
 * Notice that if the ComponentExecutorFactory itself and all its dependent services are defined using 
 * the Dependency Manager API, then you have to list the package of such components with a "!" 
 * prefix, in order to indicate that those components must not wait for a ComponentExecutorFactory service
 * (since they are part of the ComponentExecutorFactory implementation !).
 * <p>
 * 
 * <h3>Examples for the usage of the "org.apache.felix.dependencymanager.parallel" property:</h3>
 * 
 * <blockquote><pre>
 * org.apache.felix.dependencymanager.parallel=*   
 *      -> means all components must be cached until a ComponentExecutorFactory comes up.
 * 
 * org.apache.felix.dependencymanager.parallel=foo.bar, foo.zoo
 *      -> means only components whose implementation class names are starting with "foo.bar" or "foo.zoo" 
 *      must be handled using an Executor returned by the ComponentExecutorFactory service. Other Components
 *      will be handled normally, as when there is no ComponentExecutorFactory available.
 * 
 * org.apache.felix.dependencymanager.parallel=!foo.threadpool, *
 *      -> means all components must be delayed until the ComponentExecutorFactory comes up, except the 
 *      components whose implementations class names are starting with "foo.threadpool" prefix). 
 * </pre></blockquote>
 * 
 * <h3>Examples of a ComponentExecutorFactory that provides a shared threadpool:</h3>
 * 
 * First, we define the OSGi bundle context system property to enable parallelism for all DM Components
 * excepts the one which declares the ComponentExecutorFactory:
 * 
 * <blockquote> <pre>
 *   org.apache.felix.dependencymanager.parallel=!com.acme.management.threadpool, *
 * </pre></blockquote>
 * 
 * Next, here is the Activator which declares the ComponentExecutorFactory:
 * 
 * <blockquote> <pre>
 *   package com.acme.management.threadpool;
 *   import org.apache.felix.dm.*;
 *   
 *   public class Activator extends DependencyActivatorBase {      
 *      public void init(BundleContext context, DependencyManager mgr) throws Exception {
 *         mgr.add(createComponent()
 *            .setInterface(ComponentExecutorFactory.class.getName(), null)
 *            .setImplementation(ComponentExecutorFactoryImpl.class)
 *            .add(createConfigurationDependency()
 *                 .setPid("com.acme.management.threadpool.ComponentExecutorFactoryImpl")));
 *      }
 *   }
 * </pre></blockquote>
 * 
 * And here is the implementation for our ComponentExecutorFactory:
 * 
 * <blockquote> <pre>
 *   package com.acme.management.threadpool;
 *   import org.apache.felix.dm.*;
 *
 *  public class ComponentExecutorFactoryImpl implements ComponentExecutorFactory {
 *      volatile Executor m_threadPool;
 *      
 *      void updated(Dictionary conf) {
 *          m_sharedThreadPool = Executors.newFixedThreadPool(Integer.parseInt("threadpool.size"));
 *      }
 *
 *      &#64;Override
 *      public Executor getExecutorFor(Component component) {
 *          return m_sharedThreadPool; // Use a shared threadpool for all Components
 *      }
 *  }
 * </pre></blockquote>
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @since 4.0.0
 */
public interface ComponentExecutorFactory {
    /**
     * Returns an Executor (typically a shared thread pool) used to manage a given DependencyManager Component.
     * 
     * @param component the Component to be managed by the returned Executor
     * @return an Executor used to manage the given component, or null if the component must not be managed using any executor.
     */
    Executor getExecutorFor(Component component);
}
