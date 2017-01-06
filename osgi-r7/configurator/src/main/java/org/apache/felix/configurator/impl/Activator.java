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
package org.apache.felix.configurator.impl;

import org.apache.felix.configurator.impl.logger.LogServiceLogger;
import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator to start the configurator once
 * configuration admin is ready.
 */
public class Activator implements BundleActivator {

    private volatile LogServiceLogger logger;

    private volatile ServicesListener listener;

    @Override
    public final void start(final BundleContext context)
    throws Exception {
        this.logger = new LogServiceLogger(context);
        SystemLogger.setLogService(this.logger);

        listener = new ServicesListener(context);
    }

    @Override
    public final void stop(BundleContext context)
    throws Exception {
        if ( listener != null ) {
            listener.deactivate();
            listener = null;
        }
        this.logger.close();
    }
}
