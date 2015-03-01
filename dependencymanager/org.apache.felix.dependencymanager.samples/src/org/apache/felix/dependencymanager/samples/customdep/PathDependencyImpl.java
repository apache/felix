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
package org.apache.felix.dependencymanager.samples.customdep;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;

/**
 * This is our own "path" Dependency Manager Dependency, which can track the presence of files in a given path dir.
 * Every DM custom dependency must implement the DependencyContext interface, but we extends the AbstractDependency 
 * which already implements most of the DependencyContext methods.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PathDependencyImpl extends AbstractDependency<PathDependencyImpl> implements PathDependency, Runnable {
    private final String m_path;
    private volatile Thread m_thread;

    /**
     * Creates a new custom DM "path" dependency.
     * @param path the directory to watch for
     */
    public PathDependencyImpl(String path) {
        super.setRequired(true);
        m_path = path;
    }
    
    /**
     * Create a new PathDependency from an existing prototype.
     * @param prototype the existing PathDependency.
     */
    public PathDependencyImpl(PathDependencyImpl prototype) {
        super(prototype);
        m_path = prototype.m_path;
    }

    // ---------- DependencyContext interface ----------

    @Override
    public DependencyContext createCopy() {
        return new PathDependencyImpl(this);
    }
    
    @Override
    public Class<?> getAutoConfigType() {
        return null; // we don't support auto config mode
    }

    @Override
    public void start() {
        m_thread = new Thread(this);
        m_thread.start();
        super.start();
    }

    @Override   
    public void stop() {
        m_thread.interrupt();
        super.stop();
    }
    
    @Override   
    public void invokeCallback(EventType type, Event ...events) {
        switch (type) {
        case ADDED:   
            if (m_add != null) {
                invoke(m_add, events[0], getInstances());
            }
            break;
        case REMOVED:
            if (m_remove != null) {
                invoke(m_remove, events[0], getInstances());
            }
            break;
        default:
            // We don't support other kind of callbacks.
            break;
        }
    }
        
    // ---------- ComponentDependencyDeclaration interface -----------
    
    /**
     * Returns the name of this dependency (a generic name with optional info separated by spaces).
     * The DM Shell will use this method when displaying the dependency
     **/
    @Override
    public String getSimpleName() {
        return m_path;
    }

    /**
     * Returns the name of the type of this dependency. Used by the DM shell when displaying the dependency.
     **/
    @Override
    public String getType() {
        return "path";
    }

    // ---------- other methods -----------
    
    /**
     * Our start method fires a thread and this is our run method, which is watching for a given directory path
     */    
    public void run() {
        Path myDir = Paths.get(m_path);

        try {
            WatchService watcher = myDir.getFileSystem().newWatchService();
            myDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
            while (! Thread.currentThread().isInterrupted()) {
				WatchKey watckKey = watcher.take();

				List<WatchEvent<?>> events = watckKey.pollEvents();

				for (@SuppressWarnings("rawtypes") WatchEvent event : events) {
					final Kind<?> kind = event.kind();
					if (StandardWatchEventKinds.OVERFLOW == kind) {
						continue;
					}
					if (StandardWatchEventKinds.ENTRY_CREATE == kind) {
					    // Notify the component implementation context that a file has been created.
					    // Later, the component will call our invokeAdd method in order to inject the file
					    // in the component instance
				        m_component.handleEvent(this, EventType.ADDED, new Event(event.context().toString()));
                    } else if (StandardWatchEventKinds.ENTRY_DELETE == kind) {
					    // Notify the component implementation context that a file has been removed.
					    // Later, the component will call our invokeRemove method in order to call our component "remove" callback
					    m_component.handleEvent(this, EventType.REMOVED, new Event(event.context().toString()));
					}
				}
				
				watckKey.reset();
            }
        } catch (Throwable e) {
            m_component.getLogger().err("path dependency exception", e);
        }
    }

    /**
     * Invoke either the "add" or "remove" callback of the component instance(s).
     */
    private void invoke(String method, Event e, Object[] instances) {
        // specific for this type of dependency
        m_component.invokeCallbackMethod(instances, method, 
            new Class[][] { {String.class}, 
                            {}}, 
            new Object[][] { { e.getEvent() }, 
                            {}});
    }
}