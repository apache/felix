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

package org.apache.felix.ipojo.runtime.core.components.strategies;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.handlers.providedservice.strategy.ServiceObjectFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DummyServiceFactory implements ServiceObjectFactory {

    /**
     * Map [ComponentInstance->ServiceObject] storing created service objects.
     */
    private Map/*<ComponentInstance, ServiceObject>*/ m_instances = new HashMap();
    
    private InstanceManager m_manager;

    public DummyServiceFactory(InstanceManager manager) {
        m_manager = manager;
    }

    /**
     * A service object is required.
     * This policy returns a service object per asking instance.
     * @param instance the instance requiring the service object
     * @return the service object for this instance
     * @see org.apache.felix.ipojo.IPOJOServiceFactory#getService(org.apache.felix.ipojo.ComponentInstance)
     */
    public Object getService(ComponentInstance instance) {
        Object obj = m_instances.get(instance);
        if (obj == null) {
            obj = m_manager.createPojoObject();
            m_instances.put(instance, obj);
        }
        return obj;
    }

    /**
     * A service object is unget.
     * The service object is removed from the map and deleted.
     * @param instance the instance releasing the service
     * @param svcObject the service object
     * @see org.apache.felix.ipojo.IPOJOServiceFactory#ungetService(org.apache.felix.ipojo.ComponentInstance, Object)
     */
    public void ungetService(ComponentInstance instance, Object svcObject) {
        Object pojo = m_instances.remove(instance);
        m_manager.deletePojoObject(pojo);
    }

    public void close() {
        Collection col = m_instances.values();
        Iterator it = col.iterator();
        while (it.hasNext()) {
            m_manager.deletePojoObject(it.next());
        }
        m_instances.clear();
    }
}
