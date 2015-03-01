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
package org.apache.felix.dm.impl.index;

import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FilterIndexBundleContext extends BundleContextInterceptorBase {
    public FilterIndexBundleContext(BundleContext context) {
        super(context);
    }

	public void serviceChanged(ServiceEvent event) {
        Entry<ServiceListener, String>[] entries = synchronizeCollection();
        for (int i = 0; i < entries.length; i++) {
            Entry<ServiceListener, String> serviceListenerFilterEntry = entries[i];
            ServiceListener serviceListener = serviceListenerFilterEntry.getKey();
            String filter = serviceListenerFilterEntry.getValue();
            if (filter == null) {
                serviceListener.serviceChanged(event);
            }
            else {
                // call service changed on the listener if the filter matches the event
                // TODO review if we can be smarter here
                try {
                    if ("(objectClass=*)".equals(filter)) {
                        serviceListener.serviceChanged(event);
                    }
                    else {
                        if (m_context.createFilter(filter).match(event.getServiceReference())) {
                            serviceListener.serviceChanged(event);
                        }
                    }
                }
                catch (InvalidSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
