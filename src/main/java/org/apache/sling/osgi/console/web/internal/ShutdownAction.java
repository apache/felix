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
package org.apache.sling.osgi.console.web.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.osgi.console.web.Action;

/**
 * The <code>ShutdownAction</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.service
 */
public class ShutdownAction implements Action {

    public static final String NAME = "shutdown";

    public String getName() {
        return NAME;
    }

    public String getLabel() {
        return NAME;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.internal.Action#performAction(javax.servlet.http.HttpServletRequest)
     */
    public boolean performAction(HttpServletRequest request, HttpServletResponse response) {
        // simply terminate VM in case of shutdown :-)
        Thread t = new Thread("Stopper") {
            public void run() {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException ie) {
                    // ignore
                }

                // TODO: log("Shutting down server now!");
                System.exit(0);
            }
        };
        t.start();

        return true;
    }

}
