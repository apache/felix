package org.apache.felix.ipojo.handlers.providedservice;

import java.beans.MethodDescriptor;
import java.lang.reflect.Method;

public class ComponentTestWithAnotherSuperClass extends MethodDescriptor {

    public ComponentTestWithAnotherSuperClass(Method method) {
        super(method);
    }

}
