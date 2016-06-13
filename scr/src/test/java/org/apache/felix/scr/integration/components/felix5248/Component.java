package org.apache.felix.scr.integration.components.felix5248;

import java.util.Map;

public class Component
{
    
    void activate(Map<String, Object> props)
    {
        if (props.containsKey( "FAIL" ))
        {
            throw new IllegalStateException("you said to fail");
        }
    }

}
