package org.apache.felix.dm.lambda.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.osgi.framework.ServiceReference;

/**
 * Maps a ServiceReference to a Dictionary.
 */
public class SRefAsDictionary extends Dictionary<String, Object> {
    private final ServiceReference<?> m_ref;
    private volatile int m_size = -1;

    public SRefAsDictionary(ServiceReference<?> ref) {
        m_ref = ref;
    }
    
    @Override
    public Object get(Object key) {
        return m_ref.getProperty(key.toString());
    }
    
    @Override
    public int size() {
        return m_size != -1 ? m_size : (m_size = m_ref.getPropertyKeys().length);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Enumeration<String> keys() {
        return Collections.enumeration(Arrays.asList(m_ref.getPropertyKeys())); 
    }

    @Override
    public Enumeration<Object> elements() {
        final String[] keys = m_ref.getPropertyKeys();
        
        return new Enumeration<Object>() {
            int m_index = 0;
            
            @Override
            public boolean hasMoreElements() {
                return m_index < keys.length;
            }

            @Override
            public Object nextElement() {
                if (m_index >= keys.length) {
                    throw new NoSuchElementException();
                }
                return m_ref.getProperty(keys[m_index ++]);
            }
        };
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }
    
    public String toString() {
        int max = size() - 1;
        if (max == -1)
            return "{}";

        StringBuilder sb = new StringBuilder();
        String[] keys = m_ref.getPropertyKeys();
        sb.append('{');
        for (int i = 0; ; i++) {
            String key = keys[i];
            Object value = m_ref.getProperty(key);
            sb.append(key);
            sb.append('=');
            sb.append(value == this ? "(this Dictionary)" : value.toString());

            if (i == max)
                return sb.append('}').toString();
            sb.append(", ");
        }
    }
}
