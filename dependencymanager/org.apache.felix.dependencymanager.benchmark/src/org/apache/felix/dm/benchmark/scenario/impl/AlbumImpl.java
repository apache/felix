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
package org.apache.felix.dm.benchmark.scenario.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Track;

/**
 * An album comprising several music tracks.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AlbumImpl implements Album {
    final List<Track> m_musicTracks = new ArrayList<>();
    final ScenarioController m_controller;
    
    public AlbumImpl(ScenarioController controller) {
        m_controller = controller;
    }
    
    void addTrack(Track dep) {
        m_musicTracks.add(dep);
    }
        
    void start() {
        m_controller.albumAdded(this);
    }
    
    void stop() {
        m_controller.albumRemoved(this);
    }

    @Override
    public List<Track> getMusicTracks() {
        return m_musicTracks;
    }

    @Override
    public void play() {
        for (Track track : m_musicTracks) {
            track.play();
        }
    }
}
