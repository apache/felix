<<<<<<< HEAD
/**
 * 
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
 */
package org.apache.felix.deploymentadmin.itest.util;

import java.io.File;

/**
 * Provides a resource data builder.
 */
public class ResourceDataBuilder extends ArtifactDataBuilder<ResourceDataBuilder> {
<<<<<<< HEAD
    
    private String m_resourceProcessorPID;

    public ResourceDataBuilder() {
        super();
=======
    private boolean m_needRP;
    private String m_resourceProcessorPID;

    public ResourceDataBuilder() {
        m_needRP = true;
    }
    
    public ResourceDataBuilder setNeedResourceProcessor(boolean needRP) {
        m_needRP = needRP;
        return this;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
    
    public ResourceDataBuilder setResourceProcessorPID(String resourceProcessorPID) {
        m_resourceProcessorPID = resourceProcessorPID;
        return this;
    }
    
    @Override ArtifactData build() {
        ArtifactData result = super.build();
        result.setArtifactResourceProcessor(m_resourceProcessorPID);
<<<<<<< HEAD
=======
        result.setNeedResourceProcessor(m_needRP);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        return result;
    }

    @Override
    ResourceDataBuilder getThis() {
        return this;
    }

    @Override
    void validate() throws RuntimeException {
<<<<<<< HEAD
        if (m_resourceProcessorPID == null || "".equals(m_resourceProcessorPID.trim())) {
=======
        if (m_needRP && ((m_resourceProcessorPID == null || "".equals(m_resourceProcessorPID.trim())))) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            throw new RuntimeException("Artifact resource processor PID is missing!");
        }

        if (m_filename == null || "".equals(m_filename.trim())) {
            setFilename(new File(m_url.getFile()).getName());
        }

        super.validate();
    }
}
