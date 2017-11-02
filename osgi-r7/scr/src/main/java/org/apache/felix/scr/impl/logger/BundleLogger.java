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
package org.apache.felix.scr.impl.logger;

import org.osgi.framework.BundleContext;

/**
 * The {@code BundleLogger} defines a simple API to enable some logging on behalf of
 * an extended bundle. This avoids that all clients doing logging on behalf of
 * a component bundle need to pass in things like {@code BundleContext}.
 */
public class BundleLogger extends LogServiceEnabledLogger
{
    private final ScrLogger parent;

    public BundleLogger(final BundleContext bundleContext, final ScrLogger parent)
    {
        super(parent.getConfiguration(), bundleContext);
        this.parent = parent;
    }

    @Override
    InternalLogger getDefaultLogger()
    {
        return new InternalLogger()
        {

            @Override
            public void log(final int level, final String message, final Throwable ex)
            {
                parent.getLogger().log(level, message, ex);
            }

            @Override
            public boolean isLogEnabled(final int level)
            {
                return parent.getLogger().isLogEnabled(level);
            }
        };
    }

    int getTrackingCount()
    {
        return trackingCount;
    }

    InternalLogger getLogger(final String className)
    {
        if ( className != null )
        {
            final Object logServiceSupport = this.logServiceTracker.getService();
            if ( logServiceSupport != null )
            {
                return ((LogServiceSupport)logServiceSupport).getLogger(className);
            }
        }
        return this.getLogger();
    }
}
