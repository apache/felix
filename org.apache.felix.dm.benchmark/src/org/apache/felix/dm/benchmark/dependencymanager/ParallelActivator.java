package org.apache.felix.dm.benchmark.dependencymanager;

/**
 * Parallel version of our default Activator.
 */
public class ParallelActivator extends Activator {
    public ParallelActivator() {    
        super(true /* use thread pool */);
    }
}
