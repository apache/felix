package org.apache.felix.dm.benchmark.scenario;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Helper class containing misc functions, and constants.
 */
public class Helper {
    /**
     * Activate this flag for debugging.
     */
    private final static boolean DEBUG = false;

    /** 
     * Generator used to create unique identifiers.
     */
    private final static AtomicLong m_idGenerator = new AtomicLong();

    /**
     * Threadpool which can be optionally used by parallel scenarios.
     */
    private final static int CORES = Runtime.getRuntime().availableProcessors();
    private final static ForkJoinPool TPOOL = new ForkJoinPool(CORES);
    
    /**
     * Get the threadpool, possibly needed by some scenario supporting parallel mode
     */
    public static ForkJoinPool getThreadPool() {
        return TPOOL;
    }
    
    /**
     * Display some debug messages.
     */
    public static void debug(Supplier<String> message) {
        if (DEBUG) {
            System.out.println(message.get());
        }
    }

    /**
     * Generates a unique id.
     */
    public static long generateId() {
        return m_idGenerator.incrementAndGet();
    }
}
