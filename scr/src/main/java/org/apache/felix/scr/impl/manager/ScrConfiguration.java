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
package org.apache.felix.scr.impl.manager;


/**
 * The <code>ScrConfiguration</code> class conveys configuration for the
 * Felix DS implementation bundle.
 * <p>
 * <b>Configuration Source</b>
 * <p>
 * <ol>
 * <li>Framework properties: These are read when the Declarative Services
 * implementation is first started.</li>
 * <li>Configuration Admin Service: Properties are provided by means of a
 * <code>ManagedService</code> with Service PID
 * <code>org.apache.felix.scr.ScrService</code>. This class uses an OSGi Service Factory
 * ({@link org.apache.felix.scr.impl.config.ScrManagedServiceServiceFactory})
 * to register the managed service without requiring the Configuration Admin
 * Service API to be required upfront.
 * </li>
 * </ol>
 * <p>
 * See the <i>Configuration</i> section of the
 * <a href="http://felix.apache.org/site/apache-felix-service-component-runtime.html">Apache Felix Service Component Runtime</a>
 * documentation page for detailed information.
 */
public interface ScrConfiguration
{

    String PID = "org.apache.felix.scr.ScrService";

    String PROP_FACTORY_ENABLED = "ds.factory.enabled";

    String PROP_DELAYED_KEEP_INSTANCES = "ds.delayed.keepInstances";

    String PROP_INFO_SERVICE = "ds.info.service";

    String PROP_LOCK_TIMEOUT = "ds.lock.timeout.milliseconds";

    String PROP_STOP_TIMEOUT = "ds.stop.timeout.milliseconds";

    long DEFAULT_LOCK_TIMEOUT_MILLISECONDS = 5000;

    long DEFAULT_STOP_TIMEOUT_MILLISECONDS = 60000;

    String PROP_LOGLEVEL = "ds.loglevel";

    String PROP_GLOBAL_EXTENDER="ds.global.extender";

    /**
     * Returns the current log level.
     * @return
     */
    int getLogLevel();


    boolean isFactoryEnabled();


    boolean keepInstances();
    
    boolean infoAsService();

    long lockTimeout();

    long stopTimeout();

}
