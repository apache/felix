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
package org.apache.felix.deploymentadmin.test.rp2.impl;

import java.io.InputStream;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.deploymentadmin.spi.DeploymentSession;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;

/**
 * Provides a bundle activator and resource processor implementation that can delay methods for a 
 * certain amount of time (defined by system property "rp2.delay", defaults to 2000 milliseconds).
 */
public class Activator implements BundleActivator, ResourceProcessor {

    public void start(BundleContext context) throws Exception {
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, "org.apache.felix.deploymentadmin.bundle.rp2");

        context.registerService(ResourceProcessor.class.getName(), this, props);
    }

    public void stop(BundleContext context) throws Exception {
        delayMethod("stop");
    }

    public void begin(DeploymentSession session) {
        delayMethod("begin");
    }

    public void process(String name, InputStream stream) throws ResourceProcessorException {
        delayMethod("process");
    }

    public void dropped(String resource) throws ResourceProcessorException {
        delayMethod("dropped");
    }

    public void dropAllResources() throws ResourceProcessorException {
        delayMethod("dropAllResources");
    }

    public void prepare() throws ResourceProcessorException {
        delayMethod("prepare");
    }

    public void commit() {
        delayMethod("commit");
    }

    public void rollback() {
        delayMethod("rollback");
    }

    public void cancel() {
        delayMethod("cancel");
    }

    private void delayMethod(String methodName) {
        try {
            if (shouldDelayMethod(methodName)) {
                String value = System.getProperty("rp2.delay", "2000");

                Thread.sleep(Long.parseLong(value));
            }
        }
        catch (NumberFormatException exception) {
            exception.printStackTrace();
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean shouldDelayMethod(String methodName) {
        String value = System.getProperty("rp2", "");
        return methodName.equals(value);
    }
}
