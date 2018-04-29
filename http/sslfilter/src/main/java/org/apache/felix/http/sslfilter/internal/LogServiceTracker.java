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
package org.apache.felix.http.sslfilter.internal;

<<<<<<< HEAD
=======
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
<<<<<<< HEAD
=======
 *
 *
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class LogServiceTracker extends ServiceTracker
{
<<<<<<< HEAD
=======
    private final Map<ServiceReference, LogService> logServices = new TreeMap<ServiceReference, LogService>(Collections.reverseOrder());

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public LogServiceTracker(BundleContext context)
    {
        super(context, LogService.class.getName(), null);
    }

    @Override
<<<<<<< HEAD
    public Object addingService(ServiceReference reference)
    {
        LogService result = (LogService) super.addingService(reference);
        SystemLogger.setLogService(result);
=======
    public Object addingService(final ServiceReference reference)
    {
        final LogService result = (LogService) super.addingService(reference);
        if ( result != null ) {
            synchronized ( logServices ) {
                logServices.put(reference, result);
                SystemLogger.setLogService(logServices.values().iterator().next());
            }
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        return result;
    }

    @Override
<<<<<<< HEAD
    public void removedService(ServiceReference reference, Object service)
    {
        SystemLogger.setLogService(null);
=======
    public void removedService(final ServiceReference reference, final Object service)
    {
        synchronized ( logServices ) {
            logServices.remove(reference);
            final Collection<LogService> services = logServices.values();
            if ( services.isEmpty() ) {
                SystemLogger.setLogService(null);
            } else {
                SystemLogger.setLogService(services.iterator().next());
            }
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        super.removedService(reference, service);
    }
}
