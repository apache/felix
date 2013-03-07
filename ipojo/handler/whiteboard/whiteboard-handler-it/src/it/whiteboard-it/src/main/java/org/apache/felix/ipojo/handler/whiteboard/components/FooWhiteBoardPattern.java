package org.apache.felix.ipojo.handler.whiteboard.components;

import org.apache.felix.ipojo.handler.whiteboard.services.Observable;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FooWhiteBoardPattern implements Observable {

    List list = new ArrayList();
    int modifications = 0;

    long validate_time = 0;
    long first_arrival_time = 0;
    long first_departure_time = 0;
    long invalidate_time = 0;

    public void onArrival(ServiceReference ref) {
    	if (first_arrival_time == 0) {
    		first_arrival_time = System.currentTimeMillis();
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {	}
    	}
        list.add(ref);
    }

    public void onDeparture(ServiceReference ref) {
        list.remove(ref);
        if (first_departure_time == 0) {
        	first_departure_time = System.currentTimeMillis();
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {	}
    	}
    }

    public void onModification(ServiceReference ref) {
        modifications = modifications + 1;
    }

    public Map getObservations() {
        Map map = new HashMap();
        map.put("list", list);
        map.put("modifications", new Integer(modifications));
        map.put("validate", new Long(validate_time));
        map.put("invalidate", new Long(invalidate_time));
        map.put("arrival", new Long(first_arrival_time));
        map.put("departure", new Long(first_departure_time));
        return map;
    }

    public void start() {
    	if (validate_time == 0) {
    		validate_time = System.currentTimeMillis();
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {	}
    	}
    }

    public void stop() {
    	if (invalidate_time == 0) {
    		invalidate_time = System.currentTimeMillis();
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {	}
    	}
    }


}
