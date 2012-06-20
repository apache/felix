/**
 * 
 */
package org.apache.felix.deploymentadmin.itest.util;

import java.io.File;

/**
 * Provides a resource data builder.
 */
public class ResourceDataBuilder extends ArtifactDataBuilder<ResourceDataBuilder> {
    
    private String m_resourceProcessorPID;

    public ResourceDataBuilder() {
        super();
    }
    
    public ResourceDataBuilder setResourceProcessorPID(String resourceProcessorPID) {
        m_resourceProcessorPID = resourceProcessorPID;
        return this;
    }
    
    @Override ArtifactData build() {
        ArtifactData result = super.build();
        result.setArtifactResourceProcessor(m_resourceProcessorPID);
        return result;
    }

    @Override
    ResourceDataBuilder getThis() {
        return this;
    }

    @Override
    void validate() throws RuntimeException {
        if (m_resourceProcessorPID == null || "".equals(m_resourceProcessorPID.trim())) {
            throw new RuntimeException("Artifact resource processor PID is missing!");
        }

        if (m_filename == null || "".equals(m_filename.trim())) {
            setFilename(new File(m_url.getFile()).getName());
        }

        super.validate();
    }
}
