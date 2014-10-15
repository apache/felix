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

import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * A filter index is an interface you can implement to create your own, optimized index for specific filter expressions.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface FilterIndex {
    /** Opens this filter index. */
    public void open(BundleContext context);
    /** Closes this filter index. */
    public void close();
    /** Determines if the combination of class and filter is applicable for this filter index. */
    public boolean isApplicable(String clazz, String filter);
    /** Returns all service references that match the specified class and filter. Never returns null. */
    public List<ServiceReference> getAllServiceReferences(String clazz, String filter);
    /** Invoked whenever a service event occurs. */
    public void serviceChanged(ServiceEvent event);
    /** Adds a service listener to this filter index. */
    public void addServiceListener(ServiceListener listener, String filter);
    /** Removes a service listener from this filter index. If the listener is not present in the filter index, this method does nothing. */
    public void removeServiceListener(ServiceListener listener);
}
