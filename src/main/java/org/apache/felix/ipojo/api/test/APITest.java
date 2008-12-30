package org.apache.felix.ipojo.api.test;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.api.Dependency;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.api.Property;
import org.apache.felix.ipojo.api.Service;
import org.apache.felix.ipojo.api.ServiceProperty;
import org.osgi.framework.BundleContext;

public class APITest {
    
    private BundleContext m_context;
    private PrimitiveComponentType foo1;
    
    // Dependency
    FooService fs;
    BarService bs;
    private PrimitiveComponentType foo2;
    private PrimitiveComponentType foo3;
    private PrimitiveComponentType foo4;
    
    public APITest(BundleContext bc) {
        m_context = bc;
    }
    
    public void start() {
        
        testFoo1();
        testFoo2();
        testFoo3();
        testFoo4();
        
    }

    private void testFoo1() {
        System.out.println("Try to create a component type");
        
        ServiceProperty prop = new ServiceProperty();
        prop.setName("message");
        prop.setField("m_message");
        
        foo1 = new PrimitiveComponentType()
            .setBundleContext(m_context)
            .setClassName(Foo.class.getName())
            .addService(new Service().addProperty(prop));
        
        
                
        foo1.start();
        try {
            Properties props = new Properties();
            props.put("message", "hello");
            foo1.createInstance(props);
        } catch (UnacceptableConfiguration e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MissingHandlerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.out.println("fs : " + fs);
        
        System.out.println("Invoke 1 : " + fs.getMessage());
        
        foo1.stop();
        
    }
    
    private void testFoo2() {
        System.out.println("Try to create a second component type");
        foo2 = new PrimitiveComponentType()
            .setBundleContext(m_context)
            .setClassName(Foo.class.getName())
            .setValidateMethod("start")
            .setInvalidateMethod("stop");
                
        foo2.start();
        ComponentInstance ci = null;
        try {
            ci = foo2.createInstance();
        } catch (UnacceptableConfiguration e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MissingHandlerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        foo2.stop();
    }
    
    private void testFoo3() {
        System.out.println("Try to create a third component type");
        foo3 = new PrimitiveComponentType()
            .setBundleContext(m_context)
            .setClassName(Foo.class.getName())
            .addProperty(new Property().setField("m_message"))
            .addService(new Service());

        
                
        foo3.start();
        ComponentInstance ci = null;
        try {
            Properties props = new Properties();
            props.put("m_message", "bonjour");
            ci = foo3.createInstance(props);
        } catch (UnacceptableConfiguration e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MissingHandlerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.out.println("fs : " + fs);
        
        System.out.println("Invoke 2 : " + fs.getMessage());
        foo3.stop();
    }
    
    private void testFoo4() {
        foo3.start();
        
        foo4 = new PrimitiveComponentType()
        .setBundleContext(m_context)
        .setClassName(Bar.class.getName())
        .addService(new Service().setSpecification(BarService.class.getName()))
        .addDependency(new Dependency().setField("fs"));
        
        
        foo4.start();
        
        foo4.getFactory();
        try {
            foo4.createInstance();
        } catch (UnacceptableConfiguration e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MissingHandlerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            Properties props = new Properties();
            props.put("m_message", "hallo");
            ComponentInstance ci = foo3.createInstance(props);
        } catch (UnacceptableConfiguration e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MissingHandlerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }  
        
        System.out.println(bs.getMessage());
        
        foo4.stop();
        foo3.stop();
        
        
    }
    
    public void stop() {
        
    }

}
