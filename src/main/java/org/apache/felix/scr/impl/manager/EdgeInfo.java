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