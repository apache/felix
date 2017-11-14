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
import org.eclipse.jetty.server.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Dictionary;
import java.util.Hashtable;

class FileRequestLog {

    public static final String SVC_PROP_NAME = "name";
    public static final String DEFAULT_NAME = "file";
    public static final String SVC_PROP_FILEPATH = "filepath";

    private final NCSARequestLog delegate;
    private final String logFilePath;
    private final String serviceName;
    private ServiceRegistration<RequestLog> registration = null;

    FileRequestLog(JettyConfig config) {
        logFilePath = config.getRequestLogFilePath();
        serviceName = config.getRequestLogFileServiceName() != null ? config.getRequestLogFileServiceName() : DEFAULT_NAME;
        if (config.isRequestLogFileAsync()) {
            delegate = new AsyncNCSARequestLog(logFilePath);
        } else {
            delegate = new NCSARequestLog(logFilePath);
        }

        delegate.setAppend(config.isRequestLogFileAppend());
        delegate.setRetainDays(config.getRequestLogFileRetainDays());
        delegate.setFilenameDateFormat(config.getRequestLogFilenameDateFormat());
        delegate.setExtended(config.isRequestLogFileExtended());
        delegate.setIgnorePaths(config.getRequestLogFileIgnorePaths());
        delegate.setLogCookies(config.isRequestLogFileLogCookies());
        delegate.setLogServer(config.isRequestLogFileLogServer());
        delegate.setLogLatency(config.isRequestLogFileLogLatency());
    }

    synchronized void start(BundleContext context) throws IOException, IllegalStateException {
        File logFile = new File(logFilePath).getAbsoluteFile();
        File logFileDir = logFile.getParentFile();
        if (logFileDir != null && !logFileDir.isDirectory()) {
            SystemLogger.info("Creating directory " + logFileDir.getAbsolutePath());
            Files.createDirectories(logFileDir.toPath(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        }

        if (registration != null) {
            throw new IllegalStateException(getClass().getSimpleName() + " is already started");
        }
        try {
            delegate.start();
            Dictionary<String, Object> svcProps = new Hashtable<>();
            svcProps.put(SVC_PROP_NAME, serviceName);
            svcProps.put(SVC_PROP_FILEPATH, logFilePath);
            registration = context.registerService(RequestLog.class, delegate, svcProps);
        } catch (Exception e) {
            SystemLogger.error("Error starting File Request Log", e);
        }
    }

    synchronized void stop() {
        try {
            if (registration != null) {
                registration.unregister();
            }
            delegate.stop();;
        } catch (Exception e) {
            SystemLogger.error("Error shutting down File Request Log", e);
        } finally {
            registration = null;
        }
    }

}
