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
package org.apache.felix.dm.benchmark.dependencymanager;

import static org.apache.felix.dm.benchmark.scenario.Artist.ALBUMS;
import static org.apache.felix.dm.benchmark.scenario.Artist.ARTISTS;
import static org.apache.felix.dm.benchmark.scenario.Artist.TRACKS;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Artist;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.apache.felix.dm.benchmark.scenario.Track;
import org.apache.felix.dm.benchmark.scenario.impl.AlbumImpl;
import org.apache.felix.dm.benchmark.scenario.impl.ArtistImpl;
import org.apache.felix.dm.benchmark.scenario.impl.TrackImpl;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Benchmark {
    volatile DependencyManager m_dm;
    volatile ScenarioController m_controller;
    final List<Component> m_components = new ArrayList<>();

    /**
     * Initialize our Artists, Albums/Tracks, possibly using a parallel dependency manager.
     */
    @SuppressWarnings("unused")
    private void start() {
        Helper.debug(() -> "Benchmark.start");
        
        IntStream.range(0, ARTISTS)
            // Creates a stream of Artist components
        	.mapToObj(i -> createArtists(m_dm)).peek(m_components::add)
        	// For each artist in the stream, creates a new stream of Album components
            .flatMap(artist -> createAlbums(m_dm, artist)).peek(m_components::add)
            // For each Album, creates a new stream of Track components
            .flatMap(album -> createTracks(m_dm, album)).forEach(m_components::add);
                            
        m_components.stream().forEach(m_dm::add);
    }
    
    @SuppressWarnings("unused")
    private void stop() {
        m_components.forEach(m_dm::remove);
    }

    private Component createArtists(DependencyManager dm) {
        return dm.createComponent().setInterface(Artist.class.getName(), null).setImplementation(new ArtistImpl(m_controller));            
    }
    
    private Stream<Component> createAlbums(DependencyManager dm, Component artist) {
        return IntStream.range(0, ALBUMS).mapToObj(i -> {
            long id = Helper.generateId();
            String filter = "(id=" + id + ")";
            artist.add(dm.createServiceDependency().setService(Album.class, filter).setRequired(true).setCallbacks("addAlbum", null));     
            
            Hashtable<String, Object> props = new Hashtable<>();
            props.put("id", String.valueOf(id));
            return dm.createComponent().setInterface(Album.class.getName(), props).setImplementation(new AlbumImpl(m_controller));
        });
    }
        
    private Stream<Component> createTracks(DependencyManager dm, Component album) {
        return IntStream.range(0, TRACKS).mapToObj(i -> {
            long id = Helper.generateId();
            String f = "(id=" + String.valueOf(id) + ")";
            album.add(dm.createServiceDependency().setService(Track.class, f).setRequired(true).setCallbacks("addTrack", null));

            Hashtable<String, Object> p = new Hashtable<>();
            p.put("id", String.valueOf(id));
            return dm.createComponent().setInterface(Track.class.getName(), p).setImplementation(new TrackImpl(m_controller));
        });
    }
}
