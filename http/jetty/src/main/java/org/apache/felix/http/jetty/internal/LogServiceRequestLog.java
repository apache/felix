/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.jetty.internal;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.eclipse.jetty.server.AbstractNCSARequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A RequestLog that logs to the OSGi LogService when present. Not registered by default.
 */
class LogServiceRequestLog extends AbstractNCSARequestLog {

    public static final String SVC_PROP_NAME = "name";
    public static final String DEFAULT_NAME = "osgi";
    public static final String PREFIX = "REQUEST: ";

    private static final int DEFAULT_LOG_LEVEL = 3; // LogService.LOG_INFO

    private final int logLevel;
    private final String serviceName;

    private ServiceRegistration<RequestLog> registration;

    LogServiceRequestLog(JettyConfig config) {
        this.serviceName = config.getRequestLogOSGiServiceName();
        this.logLevel = config.getRequestLogOSGiLevel();
    }

    public synchronized void register(BundleContext context) throws IllegalStateException {
        if (registration != null) {
            throw new IllegalStateException(getClass().getSimpleName() + " already registered");
        }
        Dictionary<String, Object> svcProps = new Hashtable<>();
        svcProps.put(SVC_PROP_NAME, serviceName);
        this.registration = context.registerService(RequestLog.class, this, svcProps);
    }

    public synchronized void unregister() {
        try {
            if (registration != null) {
                registration.unregister();;
            }
        } finally {
            registration = null;
        }
    }

    @Override
    public void write(String s) throws IOException {
        SystemLogger.info(PREFIX + s);
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

}
