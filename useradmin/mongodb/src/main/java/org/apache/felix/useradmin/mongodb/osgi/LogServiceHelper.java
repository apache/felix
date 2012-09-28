/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.mongodb.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Provides a facade for {@link LogService} allowing it to be not 
 * available as actual service without its users knowing about this.
 */
public class LogServiceHelper extends ServiceTracker implements LogService {

    /**
     * Creates a new {@link LogServiceHelper} instance.
     * 
     * @param context the bundle context to use, cannot be <code>null</code>.
     */
    public LogServiceHelper(BundleContext context) {
        super(context, LogService.class.getName(), null);
    }
    
    @Override
    public void log(int level, String message) {
        LogService logService = getLogService();
        if (logService != null) {
            logService.log(level, message);
        }
    }

    @Override
    public void log(int level, String message, Throwable exception) {
        LogService logService = getLogService();
        if (logService != null) {
            logService.log(level, message, exception);
        }
    }

    @Override
    public void log(ServiceReference sr, int level, String message) {
        LogService logService = getLogService();
        if (logService != null) {
            logService.log(sr, level, message);
        }
    }

    @Override
    public void log(ServiceReference sr, int level, String message, Throwable exception) {
        LogService logService = getLogService();
        if (logService != null) {
            logService.log(sr, level, message, exception);
        }
    }
    
    /**
     * @return a {@link LogService} instance, or <code>null</code> if not available.
     */
    private LogService getLogService() {
        return (LogService) getService();
    }
}
