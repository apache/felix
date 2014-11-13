package org.apache.felix.dm.benchmark.ipojo;

import static org.apache.felix.dm.benchmark.scenario.Artist.ALBUMS;
import static org.apache.felix.dm.benchmark.scenario.Artist.ARTISTS;
import static org.apache.felix.dm.benchmark.scenario.Artist.TRACKS;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Artist;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.apache.felix.dm.benchmark.scenario.Track;
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
 * Benchark using iPojo.
 */
public class IpojoScenario {
    /**
     * List of Component instances (Artists, Albums, Tracks).
     */
    List<ComponentInstance> m_component = new ArrayList<>();
            
    /**
     * Component type for our "Artist" components
     */
    final PrimitiveComponentType m_artistType;

    /**
     * Component type for our "Album" components
     */
    final PrimitiveComponentType m_albumType;

    /**
     * Component type for our "Artist" components
     */
    final PrimitiveComponentType m_trackType;
    
    /**
     * This is all our created component instances
     */
    final List<ScenarioComponentInstance> m_instances = new ArrayList<>();

    public IpojoScenario(BundleContext ctx) {
        // Create our Artist component type
        m_artistType = new PrimitiveComponentType()
            .setBundleContext(ctx)            
            .setClassName(ArtistImpl.class.getName())
            .setValidateMethod("start")
            .setInvalidateMethod("stop")
            .addDependency(new Dependency().setBindMethod("bindController"))
            .setImmediate(true)
            .addService(new Service()
                .setSpecification(Artist.class.getName())
                .setCreationStrategy(Service.SINGLETON_STRATEGY));
        // Create the Artist's albums dependencies.
        IntStream.iterate(0, n -> n+1).limit(ALBUMS).forEach(i -> {
            m_artistType.addDependency(new Dependency()
                .setSpecification(Album.class.getName())
                .setId("album" + i)
                .setBindMethod("addAlbum"));
        });
        
        // Create our Album component type
        m_albumType = new PrimitiveComponentType()
            .setBundleContext(ctx)
            .setValidateMethod("start")
            .setInvalidateMethod("stop")
            .setClassName(AlbumImpl.class.getName())
            .setImmediate(false)
            .addDependency(new Dependency()
                .setSpecification(ScenarioController.class.getName())
                .setBindMethod("bindController"))
            .addService(new Service()
                .setSpecification(Album.class.getName())
                .addProperty(new ServiceProperty().setName("id").setType("string"))
            .setCreationStrategy(Service.SINGLETON_STRATEGY));
        // Create the Album's tracks dependencies.
        IntStream.iterate(0, n -> n+1).limit(TRACKS).forEach(i -> {
            m_albumType.addDependency(new Dependency()
                .setSpecification(Track.class.getName())
                .setId("track" + i)
                .setBindMethod("addTrack"));
        });
        
        // Create our Track component type
        m_trackType = new PrimitiveComponentType()
            .setBundleContext(ctx)
            .setClassName(TrackImpl.class.getName())
            .setValidateMethod("start")
            .setInvalidateMethod("stop")
            .addDependency(new Dependency()
                .setSpecification(ScenarioController.class.getName())
                .setBindMethod("bindController"))               
            .addService(new Service()
                .setSpecification(Track.class.getName())
                .addProperty(new ServiceProperty().setName("id").setType("string"))
            .setCreationStrategy(Service.SINGLETON_STRATEGY));
    }
    
    /**
     * Initialize our Artists, Albums/Tracks.
     */
    public void start() {  
        try {
            Helper.debug(() -> "IPojoActivator.start()");

            IntStream.range(0, ARTISTS)
                .mapToObj(i -> createArtist()).peek(m_instances::add)
                .flatMap(artist -> createAlbums(artist)).peek(m_instances::add)
                .flatMap(album -> createTracks(album)).forEach(m_instances::add);
                            
            Helper.debug(() -> "started all ipojo components");
        }
        
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public void stop() {
        try {
            Helper.debug(() -> "IPojoActivator.stop()");
            m_instances.forEach(scenarioInstance -> scenarioInstance.getComponentInstance().dispose());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private ScenarioComponentInstance createArtist() {
        // Create one artist with filters on all its albums.    
        String id = String.valueOf(Helper.generateId());
        Dictionary<String, Object> conf = new Hashtable<>();
        Dictionary<String, String> confFilters = new Hashtable<>();
        IntStream.iterate(0, n -> n+1).limit(ALBUMS).forEach(i -> {
            String filterId = new StringBuilder(id).append("-").append(i).toString();
            confFilters.put("album" + i, "(id=" + filterId + ")");
        });
        
        conf.put("requires.filters", confFilters);
        return new ScenarioComponentInstance(id, m_artistType, conf);
    }
    
    /**
     * Creates some albums for one given artist component.
     */
    private Stream<ScenarioComponentInstance> createAlbums(ScenarioComponentInstance artist) {
        return IntStream.iterate(0, n -> n+1).limit(ALBUMS).mapToObj(a -> {
            String albumId = new StringBuilder(artist.getId()).append("-").append(String.valueOf(a)).toString();
            Dictionary<String, Object> conf = new Hashtable<>();
            conf.put("id", albumId);
            
            Dictionary<String, String> confFilters = new Hashtable<>();
            IntStream.iterate(0, n -> n+1).limit(TRACKS).forEach(t -> {
                String filterId = new StringBuilder(albumId).append("-").append(t).toString();
                confFilters.put("track" + t, "(id=" + filterId + ")");
            });
            
            return new ScenarioComponentInstance(albumId, m_albumType, conf);           
        });
    }
        
    private Stream<ScenarioComponentInstance> createTracks(ScenarioComponentInstance album) {
        return IntStream.iterate(0, n -> n+1).limit(TRACKS).mapToObj(i -> {
            String trackId = new StringBuilder(album.getId()).append("-").append(String.valueOf(i)).toString();
            Dictionary<String, Object> conf = new Hashtable<>();
            conf.put("id", trackId);                        
            return new ScenarioComponentInstance(trackId, m_trackType, conf);           
        });
   }
}
