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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;

import org.apache.felix.dm.Dependency;

public interface DependencyContext extends Dependency {
	public void invokeAdd(Event e);
	public void invokeChange(Event e);
	public void invokeRemove(Event e);
	public void invokeSwap(Event event, Event newEvent);
	public void setComponentContext(ComponentContext component);
	/** Invoked by the component when the dependency should start working. */
	public void start();
	/** Invoked by the component when the dependency should stop working. */
	public void stop();
	
	public boolean isStarted();
	public void setAvailable(boolean available);
		
	public boolean isInstanceBound();
	public void setInstanceBound(boolean instanceBound);
	
	/** Does this dependency need the component instances to determine if the dependency is available or not */
	public boolean needsInstance();
	
    public Class<?> getAutoConfigType();
    public Event getService();
    public void copyToCollection(Collection<Object> coll);
    public void copyToMap(Map<Object, Dictionary<String, ?>> map);
    public DependencyContext createCopy();
}
