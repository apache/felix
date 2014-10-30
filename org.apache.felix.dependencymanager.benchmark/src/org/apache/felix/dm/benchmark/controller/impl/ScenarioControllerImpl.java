package org.apache.felix.dm.benchmark.controller.impl;

import static java.lang.System.out;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.felix.dm.benchmark.scenario.Artist.ALBUMS;
import static org.apache.felix.dm.benchmark.scenario.Artist.ARTISTS;
import static org.apache.felix.dm.benchmark.scenario.Artist.TRACKS;
import static org.apache.felix.dm.benchmark.scenario.Helper.debug;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.felix.dm.benchmark.controller.ScenarioController;
import org.apache.felix.dm.benchmark.scenario.Album;
import org.apache.felix.dm.benchmark.scenario.Artist;
import org.apache.felix.dm.benchmark.scenario.Helper;
import org.apache.felix.dm.benchmark.scenario.Track;
import org.apache.felix.dm.benchmark.scenario.Unchecked;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The controller which perform microbenchmarks on some scenario bundles.
 */
public class ScenarioControllerImpl implements Runnable, ScenarioController {
    /**
     * List of bundles to be executed by the benchmark.
     */
    final List<String> TESTS = Arrays.asList(
        "org.apache.felix.dependencymanager.benchmark.ipojo",
        "org.apache.felix.dependencymanager.benchmark.scr",
        "org.apache.felix.dependencymanager.benchmark.dependencymanager",
        "org.apache.felix.dependencymanager.benchmark.dependencymanager.parallel"
    );
    
    /**
     * Our injected bundle context, used to lookup the bundles to benchmark.
     */
    private volatile BundleContext m_bctx;
    
    /**
     * Latches used to detect when expected services are registered, or unregistered.
     */
    private volatile CountDownLatch m_startLatch, m_stopLatch;

    /**
     * When a components is called in its start or stop method, we'll perform some processing if the following
     * attribute is true.
     */
    private volatile boolean m_doProcessingInStartStop;
    
    /**
     * Our component is starting: we'll first stop all bundles participating in the benchmark, then we'll 
     * fire a thread, and from that thread we'll iterate on all bundles in order to do a benchmark on each.
     * (we'll call start/stop N times, and will display the elapsed times for each bundle).
     */
    void start() {
        new Thread(this).start();
    }
    
    void stop() {
    }
    
    @Override
    public void run() {
        // wait a bit in order to let the gogo banner be displayed before we start the bench.
        Unchecked.run(() -> Thread.sleep(500)); 
        
        out.println("Starting benchmarks (each tested bundle will create " + (ARTISTS + (ARTISTS * (ALBUMS + (ALBUMS * TRACKS)))) 
           + " components).");
       
        // Stop all tested bundles.
        forEachScenarioBundle(TESTS, Unchecked.consumer((bundle) -> {
            debug(() -> "Stopping bundle " + bundle.getSymbolicName());
            bundle.stop();
        }));

        // Start/stop several times the tested bundles. (no processing done in components start/stop methods).
        m_doProcessingInStartStop = false;
        out.println("\n\t[Starting benchmarks without processing done in components start/stop methods]");
        startStopScenarioBundles(TESTS, 15);
       
        // Start/stop several times the tested bundles (processing is done in components start/stop methods).
        m_doProcessingInStartStop = true;
        out.println("\n\t[Starting benchmarks with processing done in components start/stop methods]");
        startStopScenarioBundles(TESTS, 5);
    }

    @Override
    public void artistAdded(Artist artist) {
        int size = artist.getAlbums().size();
        if (size != Artist.ALBUMS) {
            throw new IllegalStateException("Artist has not created expected number of albums:" + size);
        }
        artist.play();
        componentAdded();
        Helper.debug(() -> "Artist added : " + artist);
    }
    
    @Override
    public void artistRemoved(Artist artist) {
        componentRemoved();
        Helper.debug(() -> "Artist removed : " + artist);
    }
    
    @Override
    public void albumAdded(Album album) {
        int size = album.getMusicTracks().size();
        if (size != Artist.TRACKS) {
            throw new IllegalStateException("Album does not contain expected number of music tracks:" + size);
        }
        componentAdded();
        Helper.debug(() -> "Album added : " + album);
    }
    
    @Override
    public void albumRemoved(Album album) {
        componentRemoved();
        Helper.debug(() -> "Album removed : " + album);
    }
    
    @Override
    public void trackAdded(Track track) {
        componentAdded();
        Helper.debug(() -> "Track added : " + track);
    }
    
    @Override
    public void trackRemoved(Track track) {
        componentRemoved();
        Helper.debug(() -> "Track removed : " + track);
    }
            
    // ------------------- Private methods -----------------------------------------------------
        
    private void startStopScenarioBundles(List<String> tests, int iterations) {
        forEachScenarioBundle(tests, bundle -> {
            out.print("\nBenchmarking bundle: " + bundle.getSymbolicName() + "\n");            
            List<Long> sortedResults = LongStream.range(0, iterations)
                .peek(i -> out.print("."))
                .map(n -> durationOf(() -> startAndStop(bundle)))
                .sorted().boxed().collect(toList());
            displaySortedResults(sortedResults);
            Unchecked.run(() -> Thread.sleep(500));
        });        
    }

    private void displaySortedResults(List<Long> sortedResults) {
        // We don't display an average of the duration times; Instead, we sort the results,
        // and we display the significant results (the first entry is the fastest, the middle entry is the
        // average, the last entry is the slowest ...)
        out.println("\n  results=" +  
            Stream.of(0f, 24.99f, 49.99f, 74.99f, 99.99f)
                .mapToInt(perc -> (int) (perc * sortedResults.size() / 100))
                .mapToObj(sortedResults::get).map(Object::toString)
                .collect(joining(",")));
    }

    private void componentAdded() {
        doProcessing();
        m_startLatch.countDown();
    }

    private void componentRemoved() {
        doProcessing();
        m_stopLatch.countDown();
    }

    private void doProcessing() {
        if (m_doProcessingInStartStop) {
            long duration = TimeUnit.MILLISECONDS.toNanos(ThreadLocalRandom.current().nextLong(10));
            long t1 = System.nanoTime();
            while (System.nanoTime() - t1 < duration)
                ;
        }
    }
    
    /**
     * Maps a function to all bundles participating in the benchmark.
     */
    private void forEachScenarioBundle(List<String> tests, Consumer<Bundle> consumer) {
        tests.stream().forEach(test -> {
            Optional<Bundle> bundle = Stream.of(m_bctx.getBundles()).filter(b -> b.getSymbolicName().equals(test)).findFirst();
            bundle.ifPresent(b -> {
                consumer.accept(b);
            });
        });   
    }
    
   /**
     * This function does this:
     * 
     * 1) start a bundle, and register the ScenarioController service (this will trigger all components activation)
     * 2) wait for all expected components to be fully started
     * 3) unregister our ScenarioController service, and wait for all expected components to be fully stopped
     * 4) stop the bundle
     * 
     * @param b the benchmarked scenario bundle
     */
    void startAndStop(Bundle b) {
        try {
            initLatches();
            ServiceRegistration registration = m_bctx.registerService(ScenarioController.class.getName(), this, null);

            debug(() -> "starting bundle " + b.getSymbolicName());
            b.start();
                        
            if (! m_startLatch.await(60, TimeUnit.SECONDS)) {
                out.println("Could not start components timely: current start latch=" + m_startLatch.getCount() + ", stop latch=" + m_stopLatch.getCount());
                Unchecked.run(() -> Thread.sleep(Integer.MAX_VALUE));
            }
            
            debug(() -> "all components started, unregistering benchmark controller");
            registration.unregister();
            
            if (! m_stopLatch.await(60, TimeUnit.SECONDS)) {
                out.println("Could not stop components timely: current start latch=" + m_startLatch.getCount() + ", stop latch=" + m_stopLatch.getCount());
                Unchecked.run(() -> Thread.sleep(Integer.MAX_VALUE));
            }
            debug(() -> "all components unregistered, stopping bundle " + b.getSymbolicName());
            b.stop();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * Initialize the latches used to track when all scenario bundle components are started or stopped.
     */
    private void initLatches() {
        m_startLatch = new CountDownLatch(ARTISTS
            + (ARTISTS * (ALBUMS + (ALBUMS * TRACKS))));
        
        m_stopLatch = new CountDownLatch(ARTISTS
            + (ARTISTS * (ALBUMS + (ALBUMS * TRACKS))));
    }

    /**
     * Returns the time consumed by the given runnable, which is executed by this method.
     */
    private long durationOf(Runnable scenario) {
        long start = System.nanoTime();
        long end = 0;
        try {
            scenario.run();
            end = System.nanoTime();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return (end - start);
    }
}
