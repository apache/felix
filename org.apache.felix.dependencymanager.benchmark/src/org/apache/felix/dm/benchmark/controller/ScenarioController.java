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
