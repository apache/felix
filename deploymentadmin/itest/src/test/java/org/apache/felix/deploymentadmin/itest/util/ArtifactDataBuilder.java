/**
 * 
 */
package org.apache.felix.deploymentadmin.itest.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides a builder for creating {@link ArtifactData} instances.
 */
public abstract class ArtifactDataBuilder<TYPE extends ArtifactDataBuilder<?>> {
    
    protected URL m_url;
    protected String m_filename;
    protected ResourceFilter m_filter;
    protected boolean m_missing;
    
    ArtifactDataBuilder() {
        m_filter = null;
        m_missing = false;
    }
    
    public TYPE setFilename(String filename) {
        m_filename = filename;
        return getThis();
    }
    
    public TYPE setFilter(ResourceFilter filter) {
        m_filter = filter;
        return getThis();
    }
    
    public TYPE setMissing() {
        return setMissing(true);
    }
    
    public TYPE setMissing(boolean missing) {
        m_missing = missing;
        return getThis();
    }

    public TYPE setUrl(String url) throws MalformedURLException {
        return setUrl(new URL(url));
    }
 
    public TYPE setUrl(URL url) {
        m_url = url;
        return getThis();
    }
    
    ArtifactData build() {
        validate();

        ArtifactData result = new ArtifactData(m_url, m_filename);
        result.setFilter(m_filter);
        result.setMissing(m_missing);
        return result;
    }
    
    abstract TYPE getThis(); 
    
    void validate() throws RuntimeException {
        if (m_url == null) {
            throw new RuntimeException("URL is missing!");
        }
        if (m_filename == null || "".equals(m_filename.trim())) {
            throw new RuntimeException("Filename is missing!");
        }
    }
}
