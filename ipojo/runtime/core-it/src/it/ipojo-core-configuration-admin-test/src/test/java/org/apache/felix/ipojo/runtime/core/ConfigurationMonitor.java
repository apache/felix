/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core;

import junit.framework.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

public class ConfigurationMonitor implements ConfigurationListener {
    
    private String waitForEvent;
    private boolean detected;
    private ServiceRegistration reg;

    public synchronized void configurationEvent(ConfigurationEvent arg0) {
        System.out.println(arg0.getPid() + " reconfiguration received");
        if (waitForEvent != null) {
            if (arg0.getPid().equals(waitForEvent)) {
                this.detected = true;
            }
        }
    }
    
    public ConfigurationMonitor(BundleContext bc) {
        reg = bc.registerService(ConfigurationListener.class.getName(), this, null);
    }
    
    public void stop() {
        reg.unregister();
        reg = null;
    }
    
    public void waitForEvent(String pid, String mes) {
        waitForEvent = pid;
        detected = false;
        long begin = System.currentTimeMillis();
        long duration = 0;
        while( ! this.detected) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Interrupted
            }
            long end = System.currentTimeMillis();
            duration = end - begin;
            if (duration > 10000) {
                Assert.fail(mes + " -> Timeout when waiting for a reconfiguration of " + pid);
            }
        }
        System.out.println("Reconfiguration detected of " + pid);
        waitForEvent = null;
        detected = false;
    }

}
