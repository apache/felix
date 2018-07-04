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
package org.apache.felix.log.extension;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private volatile BundleActivator m_felixLogService;
    private volatile BundleActivator m_felixLogback;

    @Override
    public void start(BundleContext context) throws Exception {
        try {
            Class<? extends BundleActivator> activatorClass = context.getBundle().loadClass("org.apache.felix.log.Activator").asSubclass(BundleActivator.class);

            m_felixLogService = activatorClass.newInstance();

            m_felixLogService.start(context);
        }
        catch (Exception e) {
            System.err.println("Could not load Felix Log Service activator");
        }

        try {
            Class<? extends BundleActivator> activatorClass = context.getBundle().loadClass("org.apache.felix.logback.internal.Activator").asSubclass(BundleActivator.class);

            m_felixLogback = activatorClass.newInstance();

            m_felixLogback.start(context);
        }
        catch (Exception e) {
            System.err.println("Could not load Felix Logback activator");
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (m_felixLogService != null) {
            m_felixLogService.stop(context);
        }
        if (m_felixLogback != null) {
            m_felixLogback.stop(context);
        }
    }

}
