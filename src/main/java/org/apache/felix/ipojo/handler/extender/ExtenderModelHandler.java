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
package org.apache.felix.ipojo.handler.extender;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;

public class ExtenderModelHandler extends PrimitiveHandler {
    
    public final static String NAMESPACE = "org.apache.felix.ipojo.extender";
    
    private List m_managers = new ArrayList(1);

    public void configure(Element elem, Dictionary dict) throws ConfigurationException {
        Element[] elems = elem.getElements("extender",NAMESPACE);
        for (int i = 0; i < elems.length; i++) {
            String extension = elems[i].getAttribute("extension");
            String onArrival = elems[i].getAttribute("onArrival");
            String onDeparture = elems[i].getAttribute("onDeparture");
            
            if (extension == null) {
                throw new ConfigurationException("The extender element requires an 'extender' attribute");
            }
            if (onArrival == null || onDeparture == null) {
                throw new ConfigurationException("The extender element requires the onArrival and onDeparture attributes");
            }
            
            ExtenderManager wbm = new ExtenderManager(this, extension, onArrival, onDeparture);
            m_managers.add(wbm);
        }
        
    }

    public void start() {
        for (int i = 0; i < m_managers.size(); i++) {
            ((ExtenderManager) m_managers.get(i)).start();
        }
    }

    public void stop() {
        for (int i = 0; i < m_managers.size(); i++) {
            ((ExtenderManager) m_managers.get(i)).stop();
        } 
    }

}
