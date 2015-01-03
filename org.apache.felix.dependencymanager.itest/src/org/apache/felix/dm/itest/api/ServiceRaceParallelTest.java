package org.apache.felix.dm.itest.api;

public class ServiceRaceParallelTest extends ServiceRaceTest {
    public ServiceRaceParallelTest() {
        setParallel(); // Configure DM to use a threadpool
    }
}
