package org.apache.felix.ipojo.handler.wbp;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.InvalidSyntaxException;

public class WhiteBoardPatternHandler extends PrimitiveHandler {
    
    public final static String NAMESPACE = "org.apache.felix.ipojo.white-board-pattern";
    
    private List m_managers = new ArrayList(1);

    public void configure(Element elem, Dictionary dict) throws ConfigurationException {
        Element[] elems = elem.getElements("wbp",NAMESPACE);
        for (int i = 0; i < elems.length; i++) {
            String filter = elems[i].getAttribute("filter");
            String onArrival = elems[i].getAttribute("onArrival");
            String onDeparture = elems[i].getAttribute("onDeparture");
            String onModification = elems[i].getAttribute("onModification");
            
            if (filter == null) {
                throw new ConfigurationException("The white board pattern element requires a filter attribute");
            }
            if (onArrival == null || onDeparture == null) {
                throw new ConfigurationException("The white board pattern element requires the onArrival and onDeparture attributes");
            }
            
            try {
                WhiteBoardManager wbm = new WhiteBoardManager(this, getInstanceManager().getContext().createFilter(filter), onArrival, onDeparture, onModification);
                m_managers.add(wbm);
            } catch (InvalidSyntaxException e) {
                throw new ConfigurationException("The filter " + filter + " is invalid : " + e);
            }
        }
        
    }

    public void start() {
        for (int i = 0; i < m_managers.size(); i++) {
            ((WhiteBoardManager) m_managers.get(i)).start();
        }
    }

    public void stop() {
        for (int i = 0; i < m_managers.size(); i++) {
            ((WhiteBoardManager) m_managers.get(i)).stop();
        } 
    }

}
