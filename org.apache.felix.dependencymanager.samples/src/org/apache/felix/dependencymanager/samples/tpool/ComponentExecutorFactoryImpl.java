package org.apache.felix.dependencymanager.samples.tpool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentExecutorFactory;

public class ComponentExecutorFactoryImpl implements ComponentExecutorFactory {
    final static int SIZE = Runtime.getRuntime().availableProcessors();
    final static Executor m_threadPool = Executors.newFixedThreadPool(SIZE);

    @Override
    public Executor getExecutorFor(Component component) {
        return m_threadPool;
    }
}
