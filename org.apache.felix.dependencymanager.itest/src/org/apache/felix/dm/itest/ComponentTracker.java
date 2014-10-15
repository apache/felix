package org.apache.felix.dm.itest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;

/**
 * Helper class used to wait for a group of components to be started and stopped.
 */
public class ComponentTracker implements ComponentStateListener {
    
    private final CountDownLatch m_startLatch;
    private final CountDownLatch m_stopLatch;

    public ComponentTracker(int startCount, int stopCount) {
        m_startLatch = new CountDownLatch(startCount);
        m_stopLatch = new CountDownLatch(stopCount);
    }

    @Override
    public void changed(Component c, ComponentState state) {
        switch (state) {
        case TRACKING_OPTIONAL:
            m_startLatch.countDown();
            break;
            
        case INACTIVE:
            m_stopLatch.countDown();
            break;
        
        default:
        }
    }
    
    public boolean awaitStarted(long millis) throws InterruptedException {
        return m_startLatch.await(millis, TimeUnit.MILLISECONDS);
    }
    
    public boolean awaitStopped(long millis) throws InterruptedException {
        return m_stopLatch.await(millis, TimeUnit.MILLISECONDS);
    }

}
