package org.apache.felix.dm.benchmark.ipojo;

import static java.util.stream.Collectors.toList;
import static org.apache.felix.dm.benchmark.scenario.Artist.ALBUMS;
import static org.apache.felix.dm.benchmark.scenario.Artist.ARTISTS;
import static org.apache.felix.dm.benchmark.scenario.Artist.TRACKS;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Artist;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.apache.felix.dm.benchmark.scenario.Track;
import org.apache.felix.dm.benchmark.scenario.Unchecked;
import org.apache.felix.dm.benchmark.scenario.impl.AlbumImpl;
import org.apache.felix.dm.benchmark.scenario.impl.ArtistImpl;
import org.apache.felix.dm.benchmark.scenario.impl.TrackImpl;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.api.Dependency;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.api.Service;
import org.apache.felix.ipojo.api.ServiceProperty;
import org.osgi.framework.BundleContext;

/**
 * *Caution*: this test seems invalid and has to be reworked because it creates too much
 * PrimitiveComponentType instances.
 */
public class IpojoScenario {
    /**
     * Our Scenario controller. We only depend on it, in order to not start if the controller is not yet available
     */
    volatile ScenarioController m_controller;

    /**
     * List of "Artist" components, each one depending on some Albums.
     */
    List<PrimitiveComponentType> m_artists = new ArrayList<>();
    
    /**
     * List of "Albums" components, each one depending on some Tracks.
     */
    List<PrimitiveComponentType> m_albums= new ArrayList<>();
    
    /**
     * List of "Tracks".
     */
    List<PrimitiveComponentType> m_tracks= new ArrayList<>();
    
    /**
     * List of "Artist" component instances.
     */
    List<ComponentInstance> m_artistInstances = new ArrayList<>();

    /**
     * List of "Album" component instances.
     */
    List<ComponentInstance> m_albumInstances = new ArrayList<>();

    /**
     * List of "Track" component instances.
     */
    List<ComponentInstance> m_trackInstances = new ArrayList<>();
    
    final BundleContext m_ctx;
    
    public IpojoScenario(BundleContext ctx) {
        m_ctx = ctx;
    }
    
    /**
     * Initialize our Artists, Albums/Tracks.
     */
    public void start() {  
        try {
            Helper.debug(() -> "IPojoActivator.start()");

            // Create many artists.
            m_artists = Stream.iterate(0, i -> i + 1).limit(ARTISTS)
                .map(i -> createArtists()).collect(toList());
        
            // Each artist has some albums.
            Helper.debug(() -> "IPojoActivator: creating albums...");
            m_albums = m_artists.stream()
                .flatMap(artist -> createAlbums(artist)).collect(toList());
            
            // Each album has tracks
            Helper.debug(() -> "IPojoActivator: creating tracks...");
            m_tracks = m_albums.stream()
                .flatMap(album -> createTracks(album)).collect(toList());
        
            // start all components
            Helper.debug(() -> "IPojoActivator: starting artists...");
            m_artistInstances = m_artists.stream()
                .map(Unchecked.func(PrimitiveComponentType::createInstance)).collect(toList());
            
            Helper.debug(() -> "IPojoActivator: starting albums...");
            m_albumInstances = m_albums.stream()
                .map(Unchecked.func(PrimitiveComponentType::createInstance)).collect(toList());
        
            Helper.debug(() -> "IPojoActivator: starting tracks...");        
            m_trackInstances = m_tracks.stream()
                .map(Unchecked.func(PrimitiveComponentType::createInstance)).collect(toList());
        
            Helper.debug(() -> "started all ipojo components");
        }
        
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public void stop() {
        try {
            Helper.debug(() -> "IPojoActivator.stop()");
            m_artistInstances.forEach(ComponentInstance::dispose);
            m_albumInstances.forEach(ComponentInstance::dispose);
            m_trackInstances.forEach(ComponentInstance::dispose);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private PrimitiveComponentType createArtists() {
        PrimitiveComponentType artist = new PrimitiveComponentType()
            .setBundleContext(m_ctx)            
            .setClassName(ArtistImpl.class.getName())
            .setValidateMethod("start")
            .setInvalidateMethod("stop")
            .addDependency(new Dependency().setBindMethod("bindController"))
            .setImmediate(true)
            .addService(new Service()
                .setSpecification(Artist.class.getName())
                .setCreationStrategy(Service.INSTANCE_STRATEGY));
        return artist;
    }
    
    private Stream<PrimitiveComponentType> createAlbums(PrimitiveComponentType artist) {
        return IntStream.iterate(0, n -> n+1).limit(ALBUMS).mapToObj(i -> {
            long id = Helper.generateId();
            String filter = "(id=" + id + ")";
            artist.addDependency(new Dependency()
                .setSpecification(Album.class.getName())
                .setFilter(filter)
                .setBindMethod("addAlbum"));            
            
            PrimitiveComponentType c = new PrimitiveComponentType()
                .setBundleContext(m_ctx)
                .setValidateMethod("start")
                .setInvalidateMethod("stop")
                .setClassName(AlbumImpl.class.getName())
                .setImmediate(false)
                .addDependency(new Dependency()
                    .setSpecification(ScenarioController.class.getName())
                    .setBindMethod("bindController"))   
                .addService(new Service()
                    .setSpecification(Album.class.getName())
                    .addProperty(new ServiceProperty().setName("id").setValue(String.valueOf(id)).setType("string"))
                    .setCreationStrategy(Service.INSTANCE_STRATEGY));
            return c;
        });
    }
        
    private Stream<PrimitiveComponentType> createTracks(PrimitiveComponentType album) {
        return IntStream.iterate(0, n -> n+1).limit(TRACKS).mapToObj(i -> {
            long id = Helper.generateId();
            String f = "(id=" + String.valueOf(id) + ")";
            album.addDependency(new Dependency()
                .setSpecification(Track.class.getName())
                .setFilter(f)
                .setBindMethod("addTrack"));
                        
            PrimitiveComponentType c = new PrimitiveComponentType()
                .setBundleContext(m_ctx)
                .setClassName(TrackImpl.class.getName())
                .setValidateMethod("start")
                .setInvalidateMethod("stop")
                .addDependency(new Dependency()
                    .setSpecification(ScenarioController.class.getName())
                    .setBindMethod("bindController"))               
                .addService(new Service()
                    .setSpecification(Track.class.getName())
                    .addProperty(new ServiceProperty().setName("id").setValue(String.valueOf(id)).setType("string"))
                    .setCreationStrategy(Service.INSTANCE_STRATEGY));
            return c;
        });
    }
}
