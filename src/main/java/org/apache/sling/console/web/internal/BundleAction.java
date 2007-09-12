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
package org.apache.sling.console.web.internal;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.console.web.Action;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * The <code>BundleAction</code> TODO
 *
 * @scr.reference name="log" interface="org.osgi.service.log.LogService"
 */
abstract class BundleAction implements Action {

    private BundleContext bundleContext;

    private LogService log;

    protected BundleContext getBundleContext() {
        return this.bundleContext;
    }

    protected long getBundleId(HttpServletRequest request) {
        String bundleIdPar = request.getParameter(BundleListRender.BUNDLE_ID);
        if (bundleIdPar != null) {
            try {
                return Long.parseLong(bundleIdPar);
            } catch (NumberFormatException nfe) {
                // TODO: log
            }
        }

        // no bundleId or wrong format
        return -1;
    }

    protected void log(Bundle bundle, String message, Throwable t) {
        this.log.log(LogService.LOG_ERROR, message, t);
    }

    //--------- SCR Integration -----------------------------------------------

    protected void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();
    }

    protected void deactivate(ComponentContext context) {
        this.bundleContext = null;
    }

    protected void bindLog(LogService logService) {
        this.log = logService;
    }

    protected void unbindLog(LogService logService) {
        this.log = null;
    }
}
