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
package org.apache.felix.scr.impl.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

public class ExtendedServiceEvent extends ServiceEvent
{
    private List<AbstractComponentManager<?>> managers;

    public ExtendedServiceEvent(ServiceEvent source)
    {
        super(source.getType(), source.getServiceReference());
    }

    public ExtendedServiceEvent(int type, ServiceReference ref)
    {
        super(type, ref);
    }

    public void addComponentManager(AbstractComponentManager<?> manager)
    {
        if (managers == null)
            managers = new ArrayList<AbstractComponentManager<?>>();
        managers.add(manager);
    }

    public List<AbstractComponentManager<?>> getManagers()
    {
        return managers == null ? Collections.<AbstractComponentManager<?>> emptyList()
            : managers;
    }

    public void activateManagers()
    {
        for (AbstractComponentManager<?> manager : getManagers())
        {
            manager.activateInternal();
        }
    }

}