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
package org.apache.felix.webconsole.plugins.gogo.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private SessionTerminalManager terminalManager;

    private GogoPlugin plugin;

    public void start(BundleContext context) throws Exception {
        this.terminalManager = new SessionTerminalManager(context);
        this.plugin = new GogoPlugin(this.terminalManager);
        this.plugin.register(context);
    }

    public void stop(BundleContext context) throws Exception {
        if (this.plugin != null) {
            this.plugin.unregister();
            this.plugin = null;
        }
        if (this.terminalManager != null) {
            this.terminalManager.shutdown();
            this.terminalManager = null;
        }
    }

}
