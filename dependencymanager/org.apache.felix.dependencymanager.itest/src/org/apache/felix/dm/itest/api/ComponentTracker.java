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
package org.apache.felix.dm.itest.api;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;

/**
 * Helper class used to wait for a group of components to be started and stopped.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
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
