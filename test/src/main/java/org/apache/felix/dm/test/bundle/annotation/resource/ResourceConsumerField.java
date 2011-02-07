package org.apache.felix.dm.test.bundle.annotation.resource;

import java.net.URL;

import junit.framework.Assert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ResourceDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

/**
 * A Component which as a resource dependency, using a class field.
 */
@Component
public class ResourceConsumerField
{
    @ServiceDependency(required=true,filter = "(test=resourceField)")
    Sequencer m_sequencer;
    
    @ResourceDependency(filter = "(&(path=*/test1.txt)(host=localhost))")
    URL m_resource;
    
    @Init
    void init() 
    {          
        if (m_resource != null)
        {
            Assert.assertTrue("file://localhost/path/to/test1.txt".equals(m_resource.toString()));
            m_sequencer.step(1);
        }
    }
}
