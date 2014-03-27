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
package org.apache.felix.deploymentadmin.itest.util;

import java.util.jar.Manifest;

/**
 * Provides a resource processor data builder.
 */
public class ResourceProcessorDataBuilder extends BundleDataBuilder<ResourceProcessorDataBuilder> {
    
    private String m_resourceProcessorPID;

    public ResourceProcessorDataBuilder() {
        super();
    }
    
    public ResourceProcessorDataBuilder setResourceProcessorPID(String resourceProcessorPID) {
        m_resourceProcessorPID = resourceProcessorPID;
        return this;
    }
    
    @Override ArtifactData build() {
        ArtifactData result = super.build();
        result.setResourceProcessor(m_resourceProcessorPID);
        return result;
    }

    @Override
    ResourceProcessorDataBuilder getThis() {
        return this;
    }

    @Override
    void setAdditionalBundleInformation(Manifest bundleManifest) {
        String processorPID = getRequiredHeader(bundleManifest.getMainAttributes(), "Deployment-ProvidesResourceProcessor");
        if (m_resourceProcessorPID == null || "".equals(m_resourceProcessorPID.trim())) {
            setResourceProcessorPID(processorPID);
        }        
    }
    
    @Override
    void validate() throws RuntimeException {
        super.validate();
        
        if (m_resourceProcessorPID == null || "".equals(m_resourceProcessorPID.trim())) {
            throw new RuntimeException("Resource processor PID is missing!");
        }
    }
}
