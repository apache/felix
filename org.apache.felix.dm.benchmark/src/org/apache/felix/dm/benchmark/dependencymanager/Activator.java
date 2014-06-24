package org.apache.felix.dm.benchmark.dependencymanager;

import static java.util.stream.Collectors.toList;
import static org.apache.felix.dm.benchmark.scenario.Artist.ALBUMS;
import static org.apache.felix.dm.benchmark.scenario.Artist.ARTISTS;
import static org.apache.felix.dm.benchmark.scenario.Artist.TRACKS;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Artist;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.apache.felix.dm.benchmark.scenario.Track;
import org.apache.felix.dm.benchmark.scenario.impl.AlbumImpl;
import org.apache.felix.dm.benchmark.scenario.impl.ArtistImpl;
import org.apache.felix.dm.benchmark.scenario.impl.TrackImpl;
import org.osgi.framework.BundleContext;

/**
 * Activator for a scenario based on Dependency Manager 4.0
 * We'll create many Artists, each one is depending on many Albums, and each Album depends on many Tracks.
 */
public class Activator extends DependencyActivatorBase {
    /**
     * List of "Artist" components, each one depending on some Albums.
     */
    List<Component> m_artists = new ArrayList<>();
    
    /**
     * List of "Albums" components, each one depending on some Tracks.
     */
    List<Component> m_albums= new ArrayList<>();
    
    /**
     * List of "Tracks".
     */
    List<Component> m_tracks= new ArrayList<>();
    
    /**
     * Our BenchMark controller. We only depend on it in order to not start if the controller is not available
     */
    volatile ScenarioController m_controller;
    
    /**
     * Flag used to check if our scenario can use a parallel dependency manager.
     */
    volatile boolean m_useThreadPool;
        
    /**
     * Activator (no parallelism).
     */
    public Activator() {
        this(false);
    }
    
    /**
     * Activator (possibly parallel).
     */
    public Activator(boolean useThreadPool) {        
        m_useThreadPool = useThreadPool;
    }
        
    /**
     * First, we have to depend on the BenchmarkController service.
     */
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {  
        Helper.debug(() -> Activator.class.getName() + ".init()");
        // Only start the stress test if the scenario controller service is available
        dm.add(createComponent()
            .setImplementation(this)
            .add(createServiceDependency().setService(ScenarioController.class).setRequired(true)));
    }
            
    /**
     * Initialize our Artists, Albums/Tracks, possibly using a parallel dependency manager.
     */
    private void start(Component c) {
        Helper.debug(() -> "DependencyManager.Activator: start");

        DependencyManager dm = c.getDependencyManager();
        if (m_useThreadPool) {
            // dm.setThreadPool(Helper.getThreadPool());
        }
        
        // Create many artists.
        m_artists = Stream.iterate(0, i -> i + 1).limit(ARTISTS)
            .map(i -> createArtists(dm)).collect(toList());
        
        // Each artist has some albums.
        m_albums = m_artists.stream()
            .flatMap(artist -> createAlbums(dm, artist)).collect(toList());
        
        // Each album has tracks
        m_tracks = m_albums.stream()
            .flatMap(album -> createTracks(dm, album)).collect(toList());
        
        // add all components
        m_artists.stream().forEach(dm::add);
        m_albums.stream().forEach(dm::add);
        m_tracks.stream().forEach(dm::add);
    }

    private Component createArtists(DependencyManager dm) {
        Component artist = dm.createComponent()
            .setInterface(Artist.class.getName(), null)
            .setImplementation(ArtistImpl.class)
            .add(dm.createServiceDependency().setService(ScenarioController.class).setRequired(true)
                .setCallbacks("bindController", null));
        return artist;
    }
    
    private Stream<Component> createAlbums(DependencyManager dm, Component artist) {
        return IntStream.iterate(0, n -> n+1).limit(ALBUMS).mapToObj(i -> {
            long id = Helper.generateId();
            String filter = "(id=" + id + ")";
            artist.add(dm.createServiceDependency().setService(Album.class, filter).setRequired(true)
                         .setCallbacks("addAlbum", null));            
            
            Properties props = new Properties();
            props.put("id", String.valueOf(id));
            Component c = dm.createComponent()
                .setInterface(Album.class.getName(), props)
                .setImplementation(AlbumImpl.class)
                .add(dm.createServiceDependency().setService(ScenarioController.class).setRequired(true)
                       .setCallbacks("bindController", null));
            return c;
        });
    }
        
    private Stream<Component> createTracks(DependencyManager dm, Component album) {
        return IntStream.iterate(0, n -> n+1).limit(TRACKS).mapToObj(i -> {
            long id = Helper.generateId();
            String f = "(id=" + String.valueOf(id) + ")";
            album.add(dm.createServiceDependency()
                .setService(Track.class, f).setRequired(true).setCallbacks("addTrack", null));

            Properties p = new Properties();
            p.put("id", String.valueOf(id));
            Component c = dm.createComponent()
                .setInterface(Track.class.getName(), p)
                .setImplementation(TrackImpl.class)
                .add(dm.createServiceDependency().setService(ScenarioController.class).setRequired(true)
                       .setCallbacks("bindController", null));
            return c;
        });
    }
}
