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

import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * The <code>BundleLogger</code> interface defines a simple API to enable some logging
 * for an extended bundle. This avoids avoids that all clients doing logging on behalf of
 * a component bundle need to pass in things like {@code BundleContext}.
 */
public class BundleLogger extends ScrLogger
{
    private final ScrLogger parent;

    public BundleLogger(final BundleContext bundleContext, final ScrConfiguration config, final ScrLogger parent)
    {
        super(bundleContext, config);
        this.parent = parent;
    }

    @Override
    protected LogService getLogService() {
        // try log service of extended bundle first
        LogService logService = super.getLogService();
        if ( logService == null )
        {
            // revert to scr log service (if available)
            logService = parent.getLogService();
        }
        return logService;
    }
}
