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
    private CountDownLatch openLatch;
    private CountDownLatch closeLatch;

    public void setClose( int close )
    {
        this.close = close;
    }

    public CountDownLatch getOpenLatch()
    {
        return openLatch;
    }

    public void setOpenLatch( CountDownLatch latch )
    {
        this.openLatch = latch;
    }
    
    public CountDownLatch getCloseLatch()
    {
        return closeLatch;
    }

    public void setCloseLatch( CountDownLatch latch )
    {
        this.closeLatch = latch;
    }

    public void setOpen( int open )
    {
        this.open = open;
    }

    public boolean outOfRange( int trackingCount )
    {
        return (open != -1 && trackingCount < open)
            || (close != -1 && trackingCount > close);
    }
}