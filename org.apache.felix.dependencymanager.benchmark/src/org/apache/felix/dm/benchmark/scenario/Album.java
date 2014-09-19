package org.apache.felix.dm.benchmark.scenario;

import java.util.List;

/**
 * A single release of musics, comprising some music tracks.
 */
public interface Album {
    /**
     * Returns the music tracks this Album is comprising.
     */
    List<Track> getMusicTracks();

    /**
     * Play all tracks from all albums.
     */
    void play();
}
