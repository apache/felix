package org.apache.felix.dm.benchmark.scenario.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Track;

/**
 * An album comprising several music tracks.
 */
public class AlbumImpl implements Album {
    final List<Track> m_musicTracks = new ArrayList<>();
    ScenarioController m_controller;

    void bindController(ScenarioController controller) {
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
