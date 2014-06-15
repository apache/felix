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
package org.apache.felix.scr.impl.manager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.service.log.LogService;

/**
 * EdgeInfo holds information about the service event tracking counts for creating (open) and disposing (close) 
 * implementation object instances per dependency manager.  These need to be maintained for each implementation object instance
 * because each instance (for a service factory) will have different sets of service references available.  These need to be 
 * maintained for each dependency manager because the open/close tracking counts are obtained when the set of current
 * service references is obtained, using a lock internal to the service tracker.
 * 
 *
 * The information in the open/close counts is used in the outOfRange method which determines if a service event tracking count 
 * occurred before the "open" event (in which case it is reflected in the open set already and does not need to be processed separately)
 * or after the "close" event (in which case it is reflected in the close set already).
 * 
 * The open latch is used to make sure that elements in the open set are completely processed before updated or unbind events
 *  are processed
 * The close latch is used to make sure that unbind events that are out of range wait for the close to complete before returning; 
 * in this case the unbind is happening in the "close" thread rather than the service event thread, so we wait for the close to complete 
 * so that when the service event returns the unbind will actually have been completed.
 * 
 * Related to this functionality is the missing tracking in AbstractComponentManager.  This is used on close of an instance to make 
 * sure all service events occuring before close starts complete processing before the close takes action.
 *
 */

class EdgeInfo
{
    private int open = -1;
    private int close = -1;
    private final CountDownLatch openLatch = new CountDownLatch(1);
    private final CountDownLatch closeLatch = new CountDownLatch(1);

    public void setClose( int close )
    {
        this.close = close;
    }

    public CountDownLatch getOpenLatch()
    {
        return openLatch;
    }
    
    public void waitForOpen(AbstractComponentManager<?> m_componentManager, String componentName, String methodName)
    {
        
        CountDownLatch latch = getOpenLatch();
        String latchName = "open";
        waitForLatch( m_componentManager, latch, componentName, methodName, latchName );
    }

    public void waitForClose(AbstractComponentManager<?> m_componentManager, String componentName, String methodName)
    {
        
        CountDownLatch latch = getCloseLatch();
        String latchName = "close";
        waitForLatch( m_componentManager, latch, componentName, methodName, latchName );
    }

    private void waitForLatch(AbstractComponentManager<?> m_componentManager, CountDownLatch latch, String componentName,
            String methodName, String latchName)
    {
        try
        {
            if (!latch.await( m_componentManager.getLockTimeout(), TimeUnit.MILLISECONDS ))
            {
                m_componentManager.log( LogService.LOG_ERROR,
                        "DependencyManager : {0} : timeout on {1} latch {2}",  new Object[] {methodName, latchName, componentName}, null );
                m_componentManager.dumpThreads();
            }
        }
        catch ( InterruptedException e )
        {
            try
            {
                if (!latch.await( m_componentManager.getLockTimeout(), TimeUnit.MILLISECONDS ))
                {
                    m_componentManager.log( LogService.LOG_ERROR,
                            "DependencyManager : {0} : timeout on {1} latch {2}",  new Object[] {methodName, latchName, componentName}, null );
                    m_componentManager.dumpThreads();
                }
            }
            catch ( InterruptedException e1 )
            {
                m_componentManager.log( LogService.LOG_ERROR,
                        "DependencyManager : {0} : Interrupted twice on {1} latch {2}",  new Object[] {methodName, latchName, componentName}, null );
                Thread.currentThread().interrupt();
            }
            Thread.currentThread().interrupt();
        }
    }

    public CountDownLatch getCloseLatch()
    {
        return closeLatch;
    }

    public void setOpen( int open )
    {
        this.open = open;
    }
    
    public void ignore()
    {
        open = Integer.MAX_VALUE;
        close = Integer.MAX_VALUE - 1;
        openLatch.countDown();
        closeLatch.countDown();
    }

    /**
     * Returns whether the tracking count is before the open count or after the close count (if set)
     * This must be called from within a block synchronized on m_tracker.tracked().
     * Setting open occurs in a synchronized block as well, to the tracker's current tracking count.
     * Therefore if this outOfRange call finds open == -1 then open will be set to a tracking count 
     * at least as high as the argument tracking count.
     * @param trackingCount tracking count from tracker to compare with range
     * @return true if open not set, tracking count before open, or close set and tracking count after close.
     */
    public boolean outOfRange( int trackingCount )
    {
        return open == -1 
                || trackingCount < open
                || (close != -1 && trackingCount > close);
    }
    
    public boolean beforeRange( int trackingCount )
    {
        return open == -1 || trackingCount < open;
    }
    
    public boolean afterRange( int trackingCount )
    {
        return close != -1 && trackingCount > close;
    }
}