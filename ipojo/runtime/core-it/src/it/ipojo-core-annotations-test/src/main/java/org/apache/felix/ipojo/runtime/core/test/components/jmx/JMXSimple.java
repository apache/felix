package org.apache.felix.ipojo.runtime.core.test.components.jmx;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handlers.jmx.Config;
import org.apache.felix.ipojo.handlers.jmx.JMXMethod;
import org.apache.felix.ipojo.handlers.jmx.JMXProperty;

@Component
@Config(domain="my-domain", usesMOSGi=false)
public class JMXSimple {

    @JMXProperty(name="prop", notification=true, rights="w")
    String m_foo;

    @JMXMethod(description="set the foo prop")
    public void setFoo(String mes) {
        System.out.println("Set foo to " + mes);
        m_foo = mes;
    }

    @JMXMethod(description="get the foo prop")
    public String getFoo() {
        return m_foo;
    }
}
