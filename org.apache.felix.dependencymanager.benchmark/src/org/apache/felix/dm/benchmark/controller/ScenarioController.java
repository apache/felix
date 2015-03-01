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
package org.apache.felix.dm.benchmark.controller;

import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Artist;
import org.apache.felix.dm.benchmark.scenario.Track;

/**
 * This service is injected in each scenario bundle. All scenario bundle components must depend on this 
 * service, and must invoke the xxAdded() method once the component is fully initialized, and 
 * the xxRemoved() method when the component is stopped.
 * This benchmark expect scenario bundles to register some "Artists" components. Each "Artist" component is
 * then expected to depend on many "Albums", and each "Album" then depends on many music "Tracks".
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ScenarioController {
    /**
     * An Artist is added (service is started)
     */
    void artistAdded(Artist artist);

    /**
     * An Artist is removed (service is stopped)
     */
    void artistRemoved(Artist artist);

    /**
     * An Album is added (service is started)
     */
    void albumAdded(Album artist);
 
    /**
     * An Album is removed (service is stopped)
     */
    void albumRemoved(Album artist);
 
    /**
     * A Music Track is added (service is started)
     */
    void trackAdded(Track artist);
  
    /**
     * A Music Track is removed (service is stopped)
     */
    void trackRemoved(Track artist);
}
