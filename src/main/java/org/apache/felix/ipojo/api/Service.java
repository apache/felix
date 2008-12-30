package org.apache.felix.ipojo.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

public class Service {
    
    private List m_specifications; // null be default computed. 
    private int m_strategy = ProvidedService.SINGLETON_STRATEGY;
    private String m_customStrategy;
    private List m_properties = new ArrayList();
    
    public Element getElement() {
        ensureValidity();
        Element element = new Element("provides", "");
        if (m_specifications != null) {
            element.addAttribute(new Attribute("specifications", getSpecificationsArray()));
        }
        element.addAttribute(new Attribute("strategy", getStringStrategy()));
        for (int i = 0; i < m_properties.size(); i++) {
            element.addElement(((ServiceProperty) m_properties.get(i)).getElement());
        }
        return element;   
    }

    
    private void ensureValidity() {
        // No check required.
    }


    private String getSpecificationsArray() {
        if (m_specifications.size() == 1) {
            return (String) m_specifications.get(0);
        } else {
            StringBuffer buffer = new StringBuffer("{");
            for (int i = 0; i < m_specifications.size(); i++) {
                if (i != 0) {
                    buffer.append(',');
                }
                buffer.append(m_specifications.get(i));
            }
            buffer.append('}');
            return buffer.toString();
        }
    }
    
    public Service addProperty(ServiceProperty ps) {
            m_properties.add(ps);
            return this;
    }

    public Service setSpecification(String spec) {
        m_specifications = new ArrayList(1);
        m_specifications.add(spec);
        return this;
    }
    
    public Service setSpecifications(List specs) {
        m_specifications  = specs;
        return this;
    }
    
    public Service setCreationStrategy(int strategy) {
        m_strategy = strategy;
        return this;
    }
    
    public Service setCreationStrategy(String strategy) {
        m_strategy = -1; // Custom
        m_customStrategy = strategy;
        return this;
    }
    
    private String getStringStrategy() {   
        switch (m_strategy) {
            case -1: // Custom policies
                   return m_customStrategy;
            case ProvidedService.SINGLETON_STRATEGY:
                    return "singleton";
            case  ProvidedService.STATIC_STRATEGY:
                    return "method";
            case ProvidedService.SERVICE_STRATEGY:
                    return "service";
            case ProvidedService.INSTANCE_STRATEGY:
                    return "instance";
            default:
                throw new IllegalStateException("Unknown creation strategy :  " + m_strategy);
        }
    }
    
    

}
