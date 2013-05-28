package org.apache.felix.scr.impl.config;

import junit.framework.TestCase;

public class ConfigurationSupportTest extends TestCase
{

    public void testEscape()
    {
        assertEquals("foo \\(&\\)", ConfigurationSupport.escape("foo (&)"));
    }
    
}
