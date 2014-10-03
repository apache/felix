package org.apache.felix.dm.itest;

public class ServiceRaceParallelTest extends ServiceRaceTest {
    public ServiceRaceParallelTest() {
        super(false); // Don't use a custom thread pool, since we'll use a parallel Dependency Manager
        setParallel(); // Configure DM to use a threadpool
    }
}
