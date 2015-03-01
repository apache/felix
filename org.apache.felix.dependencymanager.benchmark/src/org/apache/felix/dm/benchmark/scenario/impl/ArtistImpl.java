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
import org.apache.felix.dm.benchmark.scenario.Artist;

/**
 * One artist who depends on multiple Albums.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ArtistImpl implements Artist {
    final List<Album> m_albums = new ArrayList<>();
    final ScenarioController m_controller;
    
    public ArtistImpl(ScenarioController controller) {
        m_controller = controller;
    }
    
    void addAlbum(Album dep) {
        m_albums.add(dep);
    }

    void start() {
        m_controller.artistAdded(this);
    }
    
    void stop() {
        m_controller.artistRemoved(this);
    }

    @Override
    public List<Album> getAlbums() {
        return m_albums;
    }
    
    public void play() {
        for (Album album : m_albums) {
            album.play();
        }
    }
}
