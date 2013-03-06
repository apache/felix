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

package org.apache.felix.ipojo.extender.internal.queue;

import org.apache.felix.ipojo.extender.queue.JobInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objects wrapping the {@link org.apache.felix.ipojo.extender.queue.QueueService} statistics.
 */
public class Statistic {
    /**
     * The synchronized list of waiting jobs.
     */
    private final List<JobInfo> m_waiters = Collections.synchronizedList(new ArrayList<JobInfo>());

    /**
     * The number of completed jobs.
     */
    private final AtomicInteger m_finished = new AtomicInteger(0);

    /**
     * The number of job being processed.
     */
    private final AtomicInteger m_currents = new AtomicInteger(0);

    /**
     * @return the number of completed jobs.
     */
    public AtomicInteger getFinishedCounter() {
        return m_finished;
    }

    /**
     * @return the list of waiting jobs.
     */
    public List<JobInfo> getWaiters() {
        return m_waiters;
    }

    /**
     * @return the number of jobs under processing.
     */
    public AtomicInteger getCurrentsCounter() {
        return m_currents;
    }


}
