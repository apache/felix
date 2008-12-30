package org.apache.felix.ipojo.api;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

public class Dependency {
    
    public static final int DYNAMIC = org.apache.felix.ipojo.handlers.dependency.Dependency.DYNAMIC_BINDING_POLICY;
    public static final int STATIC = org.apache.felix.ipojo.handlers.dependency.Dependency.STATIC_BINDING_POLICY;
    public static final int DYNAMIC_PRIORITY = org.apache.felix.ipojo.handlers.dependency.Dependency.DYNAMIC_PRIORITY_BINDING_POLICY;

    private String m_specification;
    private String m_filter;
    private String m_field;
    private boolean m_optional;
    private boolean m_aggregate;
    private String m_bind;
    private String m_unbind;
    private int m_policy;
    private String m_comparator;
    private String m_di;
    private String m_from;
    private String m_id;
    private boolean m_nullable;

    public Element getElement() {
        ensureValidity();
        
        Element dep = new Element("requires", "");
        if (m_specification != null) {
            dep.addAttribute(new Attribute("specification", m_specification));
        }
        if (m_filter != null) {
            dep.addAttribute(new Attribute("filter", m_filter));
        }
        if (m_field != null) {
            dep.addAttribute(new Attribute("field", m_field));
        }
        if (m_bind != null) {
            Element cb = new Element("callback", "");
            cb.addAttribute(new Attribute("type", "bind"));
            cb.addAttribute(new Attribute("method", m_bind));
            dep.addElement(cb);
        }
        if (m_unbind != null) {
            Element cb = new Element("callback", "");
            cb.addAttribute(new Attribute("type", "unbind"));
            cb.addAttribute(new Attribute("method", m_unbind));
            dep.addElement(cb);
        }
        if (m_comparator != null) {
            dep.addAttribute(new Attribute("comparator", m_comparator));
        }
        if (m_di != null) {
            dep.addAttribute(new Attribute("default-implementation", m_di));
        }
        if (m_from != null) {
            dep.addAttribute(new Attribute("from", m_from));
        }
        if (m_id != null) {
            dep.addAttribute(new Attribute("id", m_id));
        }
        if (! m_nullable) {
            dep.addAttribute(new Attribute("nullable", "false"));
        }
        if (m_optional) {
            dep.addAttribute(new Attribute("optional", "true"));
        }
        if (m_aggregate) {
            dep.addAttribute(new Attribute("aggregate", "true"));
        }
        if (m_policy != -1) {
            if (m_policy == DYNAMIC) {
                dep.addAttribute(new Attribute("policy", "dynamic"));
            } else if (m_policy == STATIC) {
                dep.addAttribute(new Attribute("policy", "static"));
            } else if (m_policy == DYNAMIC_PRIORITY) {
                dep.addAttribute(new Attribute("policy", "dynamic-priority"));
            }
            // No other possibilities.
        }
        return dep;
    }
    
    public Dependency setSpecification(String spec) {
        m_specification = spec;
        return this;
    }
    
    public Dependency setFilter(String filter) {
        m_filter = filter;
        return this;
    }
    
    public Dependency setField(String field) {
        m_field = field;
        return this;
    }
    
    public Dependency setOptional(boolean opt) {
        m_optional = opt;
        return this;
    }
    
    public Dependency setAggregate(boolean agg) {
        m_aggregate = agg;
        return this;
    }
    
    public Dependency setNullable(boolean nullable) {
        m_nullable = nullable;
        return this;
    }
    
    public Dependency setBindMethod(String bind) {
        m_bind = bind;
        return this;
    }
    
    public Dependency setUnbindMethod(String unbind) {
        m_unbind = unbind;
        return this;
    }
    
    public Dependency setBindingPolicy(int policy) {
        m_policy = policy;
        return this;
    }
    
    public Dependency setComparator(String cmp) {
        m_comparator = cmp;
        return this;
    }
    
    public Dependency setDefaultImplementation(String di) {
        m_di = di;
        return this;
    }
    
    public Dependency setFrom(String from) {
        m_from = from;
        return this;
    }
    
    public Dependency setId(String id) {
        m_id = id;
        return this;
    }
    
    /**
     * Checks dependency configuration validity.
     */
    private void ensureValidity() {
        // At least a field or methods.
        if (m_field == null && m_bind == null && m_unbind == null) {
            throw new IllegalStateException("A dependency must have a field or bind/unbind methods");
        }
        // Check binding policy.
        if (m_policy != -1) {
            if (!(m_policy == DYNAMIC || m_policy == STATIC || m_policy == DYNAMIC_PRIORITY)) {
                throw new IllegalStateException("Unknow binding policy : " + m_policy);
            }
        }
    }
    
    
}
