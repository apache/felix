package org.apache.felix.dm.benchmark.scenario;

import java.util.List;

/**
 * An individual who creates musical Albums
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
