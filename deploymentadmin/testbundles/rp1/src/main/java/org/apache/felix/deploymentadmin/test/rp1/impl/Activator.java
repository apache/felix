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
package org.apache.felix.deploymentadmin.test.rp1.impl;

import java.io.InputStream;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.deploymentadmin.spi.DeploymentSession;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;

/**
 * Provides a bundle activator for registering the resource processor.
 */
public class Activator implements BundleActivator, ResourceProcessor {

    public void start(BundleContext context) throws Exception {
        checkMethodRuntimeFailure("start");

        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, "org.apache.felix.deploymentadmin.test.rp1");
        
        context.registerService(ResourceProcessor.class.getName(), this, props);
    }

    public void stop(BundleContext context) throws Exception {
        checkMethodRuntimeFailure("stop");
    }

    public void begin(DeploymentSession session) {
        checkMethodRuntimeFailure("begin");
    }

    public void process(String name, InputStream stream) throws ResourceProcessorException {
        checkMethodFailure(ResourceProcessorException.CODE_RESOURCE_SHARING_VIOLATION, "process");
    }

    public void dropped(String resource) throws ResourceProcessorException {
        checkMethodFailure(ResourceProcessorException.CODE_OTHER_ERROR, "dropped");
    }

    public void dropAllResources() throws ResourceProcessorException {
        checkMethodFailure(ResourceProcessorException.CODE_OTHER_ERROR, "dropAllResources");
    }

    public void prepare() throws ResourceProcessorException {
        checkMethodFailure(ResourceProcessorException.CODE_PREPARE, "prepare");
    }

    public void commit() {
        checkMethodRuntimeFailure("commit");
    }

    public void rollback() {
        checkMethodRuntimeFailure("rollback");
    }

    public void cancel() {
        checkMethodRuntimeFailure("cancel");
    }
    
    private void checkMethodFailure(int code, String methodName) throws ResourceProcessorException {
        if (shouldFail(methodName)) {
            throw new ResourceProcessorException(code, methodName + " fails forcedly!");
        }
    }
    
    private void checkMethodRuntimeFailure(String methodName) {
        if (shouldFail(methodName)) {
            throw new RuntimeException(methodName + " fails forcedly!");
        }
    }
    
    private boolean shouldFail(String methodName) {
        String value = System.getProperty("rp1", "");
        return methodName.equals(value);
    }
}
