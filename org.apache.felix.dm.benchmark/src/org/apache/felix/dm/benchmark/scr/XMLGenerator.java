package org.apache.felix.dm.benchmark.scr;

import static java.util.stream.Collectors.toList;
import static org.apache.felix.dm.benchmark.scenario.Artist.ALBUMS;
import static org.apache.felix.dm.benchmark.scenario.Artist.ARTISTS;
import static org.apache.felix.dm.benchmark.scenario.Artist.TRACKS;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

/**
 * Class used to generate SCR xml descritors. When you run this class, a file will be generated in /tmp/scr.xml.
 * You then have to copy this file in org/apache/felix/dm/benchmark/scr/scr.xml
 */
public class XMLGenerator {    
    static class Reference {
        final String m_name, m_service, m_bind;
        final Optional<String> m_target;
        Reference(String name, String service, String bind, String target) {
            m_name = name; m_service = service; m_bind = bind; 
            m_target = target == null ? Optional.empty() : Optional.of(target);
        }
        
        String name() { return m_name; }
        String service() { return m_service; }
        String bind() { return m_bind; }
        Optional<String> target() { return m_target; }
    }
    
    static class Component {
        final String m_name;
        final String m_impl;
        final Optional<String> m_service;
        final Optional<String[]> m_serviceProps; // array[0] = propname, array[1] = propvalue
        final List<Reference> m_refs = new ArrayList<>();
        final boolean m_immediate;
        
        Component(String name, String impl, String service, String[] serviceProps, boolean immediate) {
            m_name = name; m_impl = impl;
            m_service = service == null ? Optional.empty() : Optional.of(service);
            m_serviceProps = serviceProps == null ? Optional.empty() : Optional.of(serviceProps);
            m_immediate = immediate;
        }
        
        void add(Reference ref) {
            m_refs.add(ref);
        }

        public String getName() {
            return m_name;
        }
        
        public String getImpl() {
            return m_impl;
        }
        
        List<Reference> getReferences() {
            return m_refs;
        }
        
        Optional<String> getService() {
            return m_service;
        }
        
        Optional<String[]> getServiceProps() {
            return m_serviceProps;
        }
        
        boolean isImmediate() {
            return m_immediate;
        }
    }
    
    public static void main(String ... args) throws Exception {
        File xml = new File(System.getProperty("java.io.tmpdir")
            + File.separator + "scr.xml");
    
        try(PrintWriter out = new PrintWriter(new FileWriter(xml, false))) {
            List<Component> artists = Stream.iterate(0, i -> i + 1).limit(ARTISTS).map(
                i -> createArtist(i + 1)).collect(toList());

            List<Component> albums = artists.stream().flatMap(artist -> createAlbums(artist)).collect(
                toList());

            List<Component> tracks = albums.stream().flatMap(album -> createTracks(album)).collect(
                toList());

            out.println("<?xml version='1.0' encoding='utf-8'?>");
            out.println("<components>");
            artists.stream().forEach(artist -> toXml(out, artist));
            albums.stream().forEach(album -> toXml(out, album));
            tracks.stream().forEach(track -> toXml(out, track));
            out.println("</components>");
        }
    }
    
    static void toXml(PrintWriter out, Component comp) {
        out.println("  <scr:component xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.1.0\" name='" + 
            comp.getName() + "' activate='start' deactivate='stop'" + (comp.isImmediate() ? " immediate='true'" : "") + ">");
        out.println("    <implementation class='" + comp.getImpl() + "'/>");
        comp.getService().ifPresent(service -> {
            out.println("    <service>");
            out.println("      <provide interface='" + service + "'/>");
            out.println("    </service>");
            comp.getServiceProps().ifPresent(props -> {
                out.println("    <property name='" + props[0] + "' value='" + props[1] + "'/>");
            });
        });
        comp.getReferences().stream().forEach(ref -> {
            out.print("    <reference name='" + ref.name() + "' interface='" + ref.service() + 
                "' bind='" + ref.bind() + "'");
            ref.target().ifPresent(t -> out.print(" target='" + ref.target().get() + "'"));
            out.println("/>");
        });
        out.println("  </scr:component>");
        out.println("");
    }
    
    private static Component createArtist(int id) {
        String idstr = String.valueOf(id);
        Component app = new Component(ArtistImpl.class.getName() + idstr, ArtistImpl.class.getName(), Artist.class.getName(), null, true);
        app.add(new Reference("Controller", ScenarioController.class.getName(), "bindController", null));
        return app;
    }
    
    private static Stream<Component> createAlbums(Component artist) {
        return IntStream.iterate(0, n -> n+1).limit(ALBUMS).mapToObj(i -> {
            long id = Helper.generateId();
            String idstr = String.valueOf(id);
            String filter = "(id=" + idstr + ")";
            artist.add(new Reference("album" + String.valueOf(i+1), Album.class.getName(), 
                "addAlbum", filter));
                
            Component album = new Component(AlbumImpl.class.getName() + idstr, AlbumImpl.class.getName(),
                Album.class.getName(), new String[] { "id", idstr }, false);
            album.add(new Reference("Controller", ScenarioController.class.getName(), "bindController", null));
            return album;
        });
    }

    private static Stream<Component> createTracks(Component album) {
        return IntStream.iterate(0, n -> n+1).limit(TRACKS).mapToObj(i -> {
            long id = Helper.generateId();
            String idstr = String.valueOf(id);
            String filter = "(id=" + idstr + ")";
            album.add(new Reference("track" + idstr, Track.class.getName(), "addTrack", filter));
                
            Component track = new Component(TrackImpl.class.getName() + idstr, TrackImpl.class.getName(),
                Track.class.getName(), new String[] { "id", idstr }, false);
            track.add(new Reference("Controller", ScenarioController.class.getName(), "bindController", null));
            return track;
        });
    }
}
