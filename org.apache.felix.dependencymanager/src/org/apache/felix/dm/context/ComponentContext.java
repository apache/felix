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
package org.apache.felix.dm.context;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;

public interface ComponentContext extends Component {
    public Component setThreadPool(Executor threadPool);
    public void start();
    public void stop();
    public boolean isActive();
    public boolean isAvailable();
    public void handleAdded(DependencyContext dc, Event e);
    public void handleChanged(DependencyContext dc, Event e);
    public void handleRemoved(DependencyContext dc, Event e);
    public void handleSwapped(DependencyContext dc, Event event, Event newEvent);
    public List<DependencyContext> getDependencies(); // for testing only...
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters);
    public Object[] getInstances();
    public String getAutoConfigInstance(Class<?> clazz);
    public boolean getAutoConfig(Class<?> clazz);
    public Event getDependencyEvent(DependencyContext dc);
    public Set<Event> getDependencyEvents(DependencyContext dc);
}
