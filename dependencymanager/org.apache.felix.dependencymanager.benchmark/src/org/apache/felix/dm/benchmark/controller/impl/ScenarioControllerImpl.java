/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.benchmark.controller.impl;

import static java.lang.System.out;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.felix.dm.benchmark.scenario.Artist.ALBUMS;
import static org.apache.felix.dm.benchmark.scenario.Artist.ARTISTS;
import static org.apache.felix.dm.benchmark.scenario.Artist.TRACKS;
import static org.apache.felix.dm.benchmark.scenario.Helper.debug;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

/**
 * The controller which perform microbenchmarks on some scenario bundles.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ScenarioControllerImpl implements Runnable, ScenarioController {
    /**
     * List of bundles to be executed by the benchmark.
     */
    final List<String> TESTS = Arrays.asList(
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
     * When a component is called in its start or stop method, we'll perform some processing if the following
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
        
        out.println("Starting benchmarks (each tested bundle will add/remove " + (ARTISTS + (ARTISTS * (ALBUMS + (ALBUMS * TRACKS)))) 
           + " components during bundle activation).");
       
        // Stop all tested bundles.
        forEachScenarioBundle(TESTS, Unchecked.consumer(bundle -> {
            debug(() -> "Stopping bundle " + bundle.getSymbolicName());
            bundle.stop();
        }));
        
        // Register our controller service
        m_bctx.registerService(ScenarioController.class.getName(), this, null);
        
        // Start/stop several times the tested bundles. (no processing done in components start methods).
        m_doProcessingInStartStop = false;
        out.println("\n\t[Starting benchmarks with no processing done in components start methods]");
        startStopScenarioBundles(TESTS, 50);
       
        // Start/stop several times the tested bundles (processing is done in components start methods).
        m_doProcessingInStartStop = true;
        out.println("\n\t[Starting benchmarks with processing done in components start methods]");
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
            out.print("\nBenchmarking bundle: " + bundle.getSymbolicName() + " ");            
            List<Long> sortedResults = LongStream.range(0, iterations)
                .peek(i -> out.print("."))
                .map(n -> durationOf(() -> start(bundle)))
                .peek(n -> stop(bundle))
                .sorted().boxed().collect(toList());
            out.println();
            displaySortedResults(sortedResults);
            Unchecked.run(() -> Thread.sleep(500));
        });               
    }

    /**
     * Displays meaningful values in the sorted results (first=fastest, midle=average, last entry=slowest)
     * @param sortedResults
     */
    private void displaySortedResults(List<Long> sortedResults) {
        // We don't display an average of the duration times; Instead, we sort the results,
        // and we display the significant results (the first entry is the fastest, the middle entry is the
        // average, the last entry is the slowest ...)
        out.printf("-> results in nanos: [%s]%n",  
            Stream.of(0f, 24.99f, 49.99f, 74.99f, 99.99f)
                .mapToInt(perc -> (int) (perc * sortedResults.size() / 100))
                .mapToObj(sortedResults::get)
                .map(this::formatNano)
                .collect(joining(" | ")));
    }
    
    /**
     * Displays a nanosecond value using thousands separator. 
     * Example: 1000000 -> 1,000,000
     */
    private String formatNano(Long nanoseconds) {
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(',');
		return formatter.format(nanoseconds);
    }

    private void componentAdded() {
        doProcessing();
        m_startLatch.countDown();
    }

    private void componentRemoved() {
        //doProcessing();
        m_stopLatch.countDown();
    }

    private void doProcessing() {
        if (m_doProcessingInStartStop) {
            long duration = TimeUnit.MILLISECONDS.toNanos(ThreadLocalRandom.current().nextLong(5));
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
            bundle.ifPresent(consumer::accept);
        });   
    }
    
   /**
     * This function does this:
     * 
     * 1) start a bundle, and register the ScenarioController service (this will trigger all components activation)
     * 2) wait for all expected components to be fully started
     * 
     * @param b the benchmarked scenario bundle
     */
    void start(Bundle b) {
        try {
            m_startLatch = new CountDownLatch(ARTISTS
                + (ARTISTS * (ALBUMS + (ALBUMS * TRACKS))));
            
            debug(() -> "starting bundle " + b.getSymbolicName());
            b.start();
                                    
            if (! m_startLatch.await(60, TimeUnit.SECONDS)) {
                out.println("Could not start components timely: current start latch=" + m_startLatch.getCount() + ", stop latch=" + m_stopLatch.getCount());
                Unchecked.run(() -> Thread.sleep(Integer.MAX_VALUE)); // FIXME
            }
            
            // Make sure the threadpool is quiescent and has finished to register all components
            if (! Helper.getThreadPool().awaitQuiescence(5, TimeUnit.SECONDS)) {
                out.println("could not start components timely (thread pool is still active after 5 seconds)");
                Unchecked.run(() -> Thread.sleep(Integer.MAX_VALUE)); // FIXME
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * This function stops the bundle and wait for all expected components to be fully stopped
     * 
     * @param b the benchmarked scenario bundle
     */
    void stop(Bundle b) {
        try {
            m_stopLatch = new CountDownLatch(ARTISTS
                + (ARTISTS * (ALBUMS + (ALBUMS * TRACKS))));
                        
            debug(() -> "stopping bundle " + b.getSymbolicName());
            b.stop();
            
            // Make sure the threadpool is quiescent and has finished to register all components
            if (! Helper.getThreadPool().awaitQuiescence(5, TimeUnit.SECONDS)) {
                out.println("could not start components timely (thread pool is still active after 5 seconds)");
                Unchecked.run(() -> Thread.sleep(Integer.MAX_VALUE)); // FIXME
            }
            
            // Wait for all component deactivations
            if (! m_stopLatch.await(60, TimeUnit.SECONDS)) {
                out.println("Could not stop components timely: current start latch=" + m_startLatch.getCount() + ", stop latch=" + m_stopLatch.getCount());
                Unchecked.run(() -> Thread.sleep(Integer.MAX_VALUE));
            }            
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * Returns the time consumed by the given runnable, ²ch is executed by this method.
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
