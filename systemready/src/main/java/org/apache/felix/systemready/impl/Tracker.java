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
package org.apache.felix.systemready.impl;

import java.io.Closeable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

class Tracker implements Closeable {
    private ServiceTracker<?,?> stracker;

    public Tracker(BundleContext context, String nameOrFilter) {
        String filterSt = nameOrFilter.startsWith("(") ? nameOrFilter : String.format("(objectClass=%s)", nameOrFilter);
        Filter filter;
        try {
            filter = FrameworkUtil.createFilter(filterSt);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Error creating filter for " + nameOrFilter);
        }
        this.stracker = new ServiceTracker<>(context, filter, null);
        this.stracker.open();
    }
    
    public boolean present() {
        return this.stracker.getTrackingCount() > 0;
    }

    @Override
    public void close() {
        stracker.close();
    }
    
}
