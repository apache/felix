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
 * Injects a <code>Runnable</code> object in a Service for starting/stopping it programatically.
 * By default, a Service is implicitly started when the service's bundle is started and when 
 * all required dependencies are satisfied. However, it is sometimes required to programatically 
 * take control of when the service is started or stopped. In this case, the injected <code>Runnable</code> 
 * can be invoked in order to start/register (or stop/unregister) a Service at any time. When this annotation 
 * is used, then the Service on which this annotation is applied is not activated by default, and you have to 
 * call the injected Runnable yourself. 
 *
 * <h3>Usage Examples</h3>
 * <blockquote>
 * 
 * <pre>
 * &#47;**
 *   * This Service will be registered programmatically into the OSGi registry, using the LifecycleController annotation.
 *   *&#47;
 * &#64;Component
 * class X implements Z {
 *     &#64;LifecycleController
 *     Runnable starter
 *     
 *     &#64;LifecycleController(start=false)
 *     Runnable stopper
 *   
 *     &#64;Init
 *     void init() {
 *         // At this point, all required dependencies are there, but we'll activate our service in 2 seconds ...
 *         Thread t = new Thread() {
 *            public void run() {
 *              sleep(2000);
 *              // start our "Z" service (our "start" method will be called, juste before service registration
 *              starter.run();
 *              
 *              sleep(2000);
 *              // now, stop/unregister the "Z" service (we'll then be called in our stop() method
 *              stopper.run();
 *            }
 *          };
 *          t.start();
 *     }
 *     
 *     &#64;Start
 *     public void start() {
 *         // This method will be called after we invoke our starter Runnable, and our service will be
 *         // published after our method returns, as in normal case.
 *     }
 *
 *     &#64;Stop
 *     public void stop() {
 *         // This method will be called after we invoke our "stop" Runnable, and our service will be
 *         // unregistered before our method is invoked, as in normal case. Notice that the service won't
 *         // be destroyed here, and the "starter" runnable can be re-invoked later.
 *     }
 * }
 * </pre>
 * </blockquote> 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */ 
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface LifecycleController
{
    /**
     * Specifies the action to be performed when the injected runnable is invoked. By default, the
     * Runnable will fire a Service Component activation, when invoked. If you specify this attribute
     * to false, then the Service Component will be stopped, when the runnable is invoked.
     * @return true if the component must be started when you invoke the injected runnable, or false if
     * the component must stopped when invoking the runnable.
     */
    public boolean start() default true;
}
