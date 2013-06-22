/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core.api;

import org.apache.felix.ipojo.api.HandlerConfiguration;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

public class Whiteboard implements HandlerConfiguration {
    
    public static final String NAME = "wbp";
    
    public static final String NAMESPACE = "org.apache.felix.ipojo.whiteboard";
    
    private String arrival;
    
    private String departure;
    
    private String modification;
    
    private String filter;
    
    public Whiteboard onArrival(String method) {
        arrival = method;
        return this;
    }
    
    public Whiteboard onDeparture(String method) {
        departure = method;
        return this;
    }
    
    public Whiteboard onModification(String method) {
        modification = method;
        return this;
    }
    
    public Whiteboard setFilter(String fil) {
        filter = fil;
        return this;
    }

    public Element getElement() {
        ensureValidity();
        // Create the root element.
        Element element = new Element(NAME, NAMESPACE);
        // Mandatory attributes
        element.addAttribute(new Attribute("onArrival", arrival));
        element.addAttribute(new Attribute("onDeparture", departure));
        element.addAttribute(new Attribute("filter", filter));
        
        // Optional attribute
        if (modification != null) {
            element.addAttribute(new Attribute("onModification", modification));
        }        
        
        return element;
    }

    private void ensureValidity() {
        if (arrival == null) {
            throw new IllegalStateException("The whiteboard pattern configuration must have a onArrival method");
        }
        if (departure == null) {
            throw new IllegalStateException("The whiteboard pattern configuration must have a onDeparture method");
        }
        if (filter == null) {
            throw new IllegalStateException("The whiteboard pattern configuration must have a filter");
        }
        
    }

}
