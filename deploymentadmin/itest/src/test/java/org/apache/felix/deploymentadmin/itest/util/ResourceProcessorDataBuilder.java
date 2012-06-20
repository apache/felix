/**
 * 
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
