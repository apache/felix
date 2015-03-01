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
package org.apache.felix.dm.benchmark.scenario;

import java.util.List;

/**
 * An individual who creates musical Albums
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Artist {
    /**
     * When a scenario bundles starts, it creates the following number of Artists (service)
     * (you have to regenerate the SCR xml descriptor if you modify this, see README)
     */
    public final int ARTISTS = 30;
    
    /**
     * Each Artist creates the following number of musical Albums.
     * (you have to regenerate the SCR xml descriptor if you modify this, see README)
     */
    public final int ALBUMS = 5;
    
    /**
     * Each Album contains the following number of musical Tracks.
     * (you have to regenerate the SCR xml descriptor if you modify this, see README)
     */
    public final int TRACKS = 3;
    
    /**
     * Returns the Albums that this Artist has created
     */
    List<Album> getAlbums();
    
    /**
     * Play all tracks from all albums (this test method invocation time between services).
     */
    void play();
}
