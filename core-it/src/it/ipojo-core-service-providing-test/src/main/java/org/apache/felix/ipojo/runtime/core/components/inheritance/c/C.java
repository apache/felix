package org.apache.felix.ipojo.runtime.core.components.inheritance.c;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.runtime.core.components.inheritance.b.IB;

@Component
@Provides
public class C implements IB {

    public String methOne() {
        return "one";
    }

    public String methTwo() {
        return "two";
    }

}
