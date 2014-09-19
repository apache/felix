package org.apache.felix.dm.benchmark.scenario.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Artist;

/**
 * One artist who depends on multiple Albums.
 */
public class ArtistImpl implements Artist {
    final List<Album> m_albums = new ArrayList<>();
    volatile ScenarioController m_controller;
    
    void bindController(ScenarioController controller) {
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
