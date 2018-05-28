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
package org.apache.felix.logback.test;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.slf4j.MDCContextMap;
import org.apache.logging.slf4j.SLF4JLoggerContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        Provider slf4jProvider = new Provider(
            15, "2.6.0", SLF4JLoggerContextFactory.class, MDCContextMap.class);

        Dictionary<String, Object> properties = new Hashtable<>();

        // The following value is pulled from here [1] but I think it's a typo [2]
        // [1] https://git-wip-us.apache.org/repos/asf?p=logging-log4j2.git;a=blob;f=log4j-api/src/main/java/org/apache/logging/log4j/util/Activator.java;h=c7910e505e18b9de339c7671641d04aceb2d9b37;hb=HEAD#l103
        // [2] https://issues.apache.org/jira/browse/LOG4J2-2343
        properties.put("APIVersion", "2.60");

        serviceRegistration = bundleContext.registerService(Provider.class, slf4jProvider, properties);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        serviceRegistration.unregister();
    }

    private volatile ServiceRegistration<Provider> serviceRegistration;

}
