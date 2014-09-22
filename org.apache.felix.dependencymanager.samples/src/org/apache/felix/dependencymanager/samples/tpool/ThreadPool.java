package org.apache.felix.dependencymanager.samples.tpool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool extends ThreadPoolExecutor {
    final static int SIZE = Runtime.getRuntime().availableProcessors();
    public ThreadPool() {
        super(SIZE, SIZE, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }
}
