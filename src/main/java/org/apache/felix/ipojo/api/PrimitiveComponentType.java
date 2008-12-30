package org.apache.felix.ipojo.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.manipulation.Manipulator;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

public class PrimitiveComponentType extends ComponentType {

    private BundleContext m_context;
    private String m_classname;
    private String m_name;
    private boolean m_immediate;
    private Element m_manipulation;
    private ComponentFactory m_factory;
    private Element m_metadata;
    private List m_services = new ArrayList(1);
    private List m_dependencies = new ArrayList();
    private List m_properties = new ArrayList();
    private String m_validate;
    private String m_invalidate;
    private boolean m_propagation;
    private String m_factoryMethod;
    private boolean m_public = true;
    private String m_msPID;
    
    private void ensureNotInitialized() {
        if (m_factory != null) {
            throw new IllegalStateException("The component type was already initialized, cannot modify metadata");
        }
    }
    
    private void ensureValidity() {
        if (m_classname == null) {
            throw new IllegalStateException("The primitive component type as no implementation class");
        }
    }

    public Factory getFactory() {
        initializeFactory();
        return m_factory;
    }

    public void start() {
        initializeFactory();
        m_factory.start();
    }

    public void stop() {
        initializeFactory();
        m_factory.stop();
    }
    
    private void initializeFactory() {
        if (m_factory == null) {
            createFactory();
        }
    }
    
    public PrimitiveComponentType setBundleContext(BundleContext bc) {
        ensureNotInitialized();
        m_context = bc;
        return this;
    }
    
    public PrimitiveComponentType setClassName(String classname) {
        ensureNotInitialized();
        m_classname = classname;
        return this;
    }
    
    public PrimitiveComponentType setComponentTypeName(String name) {
        ensureNotInitialized();
        m_name = name;
        return this;
    }
    
    public PrimitiveComponentType setImmediate(boolean immediate) {
        ensureNotInitialized();
        m_immediate = immediate;
        return this;
    }
    
    public PrimitiveComponentType setFactoryMethod(String method) {
        ensureNotInitialized();
        m_factoryMethod = method;
        return this;
    }
    
    public PrimitiveComponentType setPropagation(boolean propagation) {
        ensureNotInitialized();
        m_propagation = propagation;
        return this;
    }
    
    public PrimitiveComponentType setPublic(boolean visible) {
        ensureNotInitialized();
        m_public = visible;
        return this;
    }
    
    public PrimitiveComponentType setManagedServicePID(String pid) {
        ensureNotInitialized();
        m_msPID = pid;
        return this;
    }
    
    public PrimitiveComponentType setValidateMethod(String method) {
        ensureNotInitialized();
        m_validate = method;
        return this;
    }
    
    public PrimitiveComponentType setInvalidateMethod(String method) {
        ensureNotInitialized();
        m_invalidate = method;
        return this;
    }
    
    private Element generateComponentMetadata() {
        Element element = new Element("component", "");
        element.addAttribute(new Attribute("classname", m_classname));
        if (m_name != null) {
            element.addAttribute(new Attribute("name", m_name));
        }
        if (m_factoryMethod != null) {
            element.addAttribute(new Attribute("factory-method", m_factoryMethod));
        }
        if (! m_public) {
            element.addAttribute(new Attribute("public", "false"));
        }
        if (m_immediate) {
            element.addAttribute(new Attribute("immediate", "true"));
        }
        for (int i = 0; i < m_services.size(); i++) {
            Service svc = (Service) m_services.get(i);
            element.addElement(svc.getElement());
        }
        for (int i = 0; i < m_dependencies.size(); i++) {
            Dependency dep = (Dependency) m_dependencies.get(i);
            element.addElement(dep.getElement());
        }
        if (m_validate != null) {
            Element callback = new Element("callback", "");
            callback.addAttribute(new Attribute("transition", "validate"));
            callback.addAttribute(new Attribute("method", m_validate));
            element.addElement(callback);
        }
        if (m_invalidate != null) {
            Element callback = new Element("callback", "");
            callback.addAttribute(new Attribute("transition", "invalidate"));
            callback.addAttribute(new Attribute("method", m_invalidate));
            element.addElement(callback);
        }
        
        // Properties
        // First determine if we need the properties element
        if (m_propagation || m_msPID != null || ! m_properties.isEmpty()) {
            Element properties = new Element("properties", "");
            if (m_propagation) {
                properties.addAttribute(new Attribute("propagation", "true"));
            }
            if (m_msPID != null) {
                properties.addAttribute(new Attribute("pid", m_msPID));
            }
            for (int i = 0; i < m_properties.size(); i++) {
                Property prop = (Property) m_properties.get(i);
                properties.addElement(prop.getElement());
            }
            element.addElement(properties);
        }
        
        return element;
    }
    
    private void createFactory() {
        ensureValidity();
        byte[] clazz = manipulate();
        m_metadata = generateComponentMetadata();
        Element meta = m_metadata;
        meta.addElement(m_manipulation);
        try {
            m_factory = new ComponentFactory(m_context, clazz, meta);
        } catch (ConfigurationException e) {
            throw new IllegalStateException("An exception occurs during factory initialization : " + e.getMessage());
        }
       
    }
    
    private byte[] manipulate() {
        Manipulator manipulator = new Manipulator();
        try {
            byte[] array = getClassByteArray();
            byte[] newclazz = manipulator.manipulate(array);
            m_manipulation = manipulator.getManipulationMetadata();
            return newclazz;
        } catch (IOException e) {
            throw new IllegalStateException("An exception occurs during implementation class manipulation : " + e.getMessage());
        }
    }
    
    private byte[] getClassByteArray() throws IOException {
        String filename = m_classname.replace('.', '/') + ".class";
        URL url = m_context.getBundle().getResource(filename);
        if (url == null) {
            throw new IllegalStateException("An exception occurs during implementation class manipulation : cannot found the class file " + filename);
        }
        InputStream is = url.openStream();
        if (is == null) {
            throw new IllegalStateException("An exception occurs during implementation class manipulation : cannot read the class file " + url);
        }
        byte[] b = new byte[is.available()]; 
        is.read(b); 
        return b;
    }

    public PrimitiveComponentType addService(Service svc) {
        ensureNotInitialized();
        m_services.add(svc);
       return this;
    }
    
    public PrimitiveComponentType addDependency(Dependency dep) {
        ensureNotInitialized();
        m_dependencies.add(dep);
        return this;
    }
    
    public PrimitiveComponentType addProperty(Property prop) {
        ensureNotInitialized();
        m_properties.add(prop);
        return this;
    }

}
