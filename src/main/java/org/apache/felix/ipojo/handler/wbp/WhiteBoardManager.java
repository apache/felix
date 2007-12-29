package org.apache.felix.ipojo.handler.wbp;

import java.lang.reflect.InvocationTargetException;

import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

public class WhiteBoardManager implements TrackerCustomizer {
    
    private Filter m_filter;
    private Callback m_onArrival;
    private Callback m_onDeparture;
    private Callback m_onModification;
    private Tracker m_tracker;
    private PrimitiveHandler m_handler;
    
    public WhiteBoardManager(WhiteBoardPatternHandler handler, Filter filter, String bind, String unbind, String modification) {
        m_handler = handler;
        m_onArrival = new Callback(bind, new Class[] {ServiceReference.class}, false, m_handler.getInstanceManager());
        m_onDeparture = new Callback(unbind, new Class[] {ServiceReference.class}, false, m_handler.getInstanceManager());
        if (modification != null) {
            m_onModification = new Callback(modification, new Class[] {ServiceReference.class}, false, m_handler.getInstanceManager());
        }
        m_filter = filter;
        m_tracker = new Tracker(handler.getInstanceManager().getContext(), m_filter, this);
    }
    
    public void start() {
        m_tracker.open();
    }
    
    public void stop() {
        m_tracker.close();
    }

    public void addedService(ServiceReference arg0) {
        try {
            m_onArrival.call(new Object[] {arg0});
        } catch (NoSuchMethodException e) {
            m_handler.error("The onArrival method " + m_onArrival.getMethod() + " does not exist in the class", e);
            m_handler.getInstanceManager().stop();
        } catch (IllegalAccessException e) {
            m_handler.error("The onArrival method " + m_onArrival.getMethod() + " cannot be invoked", e);
            m_handler.getInstanceManager().stop();
        } catch (InvocationTargetException e) {
            m_handler.error("The onArrival method " + m_onArrival.getMethod() + " has thrown an exception", e.getTargetException());
            m_handler.getInstanceManager().stop();
        }
    }

    public boolean addingService(ServiceReference arg0) {
        return true;
    }

    public void modifiedService(ServiceReference arg0, Object arg1) {
        if (m_onModification != null) {
            try {
                m_onModification.call(new Object[] {arg0});
            } catch (NoSuchMethodException e) {
                m_handler.error("The onModification method " + m_onModification.getMethod() + " does not exist in the class", e);
                m_handler.getInstanceManager().stop();
            } catch (IllegalAccessException e) {
                m_handler.error("The onModification method " + m_onModification.getMethod() + " cannot be invoked", e);
                m_handler.getInstanceManager().stop();
            } catch (InvocationTargetException e) {
                m_handler.error("The onModification method " + m_onModification.getMethod() + " has thrown an exception", e.getTargetException());
                m_handler.getInstanceManager().stop();
            }
        }
    }

    public void removedService(ServiceReference arg0, Object arg1) {
        try {
            m_onDeparture.call(new Object[] {arg0});
        } catch (NoSuchMethodException e) {
            m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " does not exist in the class", e);
            m_handler.getInstanceManager().stop();
        } catch (IllegalAccessException e) {
            m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " cannot be invoked", e);
            m_handler.getInstanceManager().stop();
        } catch (InvocationTargetException e) {
            m_handler.error("The onDeparture method " + m_onDeparture.getMethod() + " has thrown an exception", e.getTargetException());
            m_handler.getInstanceManager().stop();
        }
    }

}
