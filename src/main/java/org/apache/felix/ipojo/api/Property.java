package org.apache.felix.ipojo.api;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

public class Property {
    
    private String m_name;
    private String m_field;
    private String m_value;
    private String m_method;
    private boolean m_mandatory;
    private boolean m_immutable;
    
    public Property setName(String name) {
        m_name = name;
        return this;
    }
    
    public Property setField(String name) {
        m_field = name;
        return this;
    }
    
    public Property setMethod(String name) {
        m_method = name;
        return this;
    }
    
    public Property setValue(String name) {
        m_value = name;
        return this;
    }
    
    public Property setMandatory(boolean mandatory) {
        m_mandatory = mandatory;
        return this;
    }
    
    public Property setImmutable(boolean immutable) {
        m_immutable = immutable;
        return this;
    }
    
    public Element getElement() {
        ensureValidity();
        Element element = new Element("property", "");
        if (m_name != null) {
            element.addAttribute(new Attribute("name", m_name));
        }
        if (m_method != null) {
            element.addAttribute(new Attribute("method", m_method));
        }
        if (m_value != null) {
            element.addAttribute(new Attribute("value", m_value));
        }
        if (m_field != null) {
            element.addAttribute(new Attribute("field", m_field));
        }
        if (m_mandatory) {
            element.addAttribute(new Attribute("mandatory", Boolean.toString(m_mandatory)));
        }
        if (m_immutable) {
            element.addAttribute(new Attribute("immutable", Boolean.toString(m_immutable)));
        }
        return element;
    }

    private void ensureValidity() {
        // Two cases
        // Field or Method
        if (m_field == null && m_method == null) {
            throw new IllegalStateException("A property must have either a field or a method");
        }
        if (m_immutable && m_value == null) {
            throw new IllegalStateException("A immutable service property must have a value");
        }
    }
    
    

}
