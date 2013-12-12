/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.coordinator.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.felix.jmx.service.coordinator.CoordinatorMBean;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Participant;

/**
 * The <code>CoordinationMgr</code> is the actual backend manager of all
 * Coordinations created by the Coordinator implementation. The methods in this
 * class fall into three categories:
 * <ul>
 * <li>Actual implementations of the Coordinator interface on behalf of the
 * per-bundle Coordinator service instances</li>
 * <li>Implementation of the CoordinatorMBean interface allowing JMX management
 * of the coordinations</li>
 * <li>Management support to timeout and cleanup coordinations</li>
 * </ul>
 */
public class CoordinationMgr implements CoordinatorMBean
{

    private ThreadLocal<Stack<Coordination>> threadStacks;

    private final AtomicLong ctr;

    private final Map<Long, CoordinationImpl> coordinations;

    private final Map<Participant, CoordinationImpl> participants;

    private final Timer coordinationTimer;

    /**
     * Default coordination timeout. Currently hard coded to be 30s (the
     * specified minimum timeout). Should be made configurable, but not less
     * than 30s.
     */
    private long defaultTimeOut = 30 * 1000L;

    /**
     * Wait at most 60 seconds for participant to be eligible for participation
     * in a coordination.
     *
     * @see #singularizeParticipant(Participant, CoordinationImpl)
     */
    private long participationTimeOut = 60 * 1000L;

    CoordinationMgr()
    {
        threadStacks = new ThreadLocal<Stack<Coordination>>();
        ctr = new AtomicLong(-1);
        coordinations = new HashMap<Long, CoordinationImpl>();
        participants = new HashMap<Participant, CoordinationImpl>();
        coordinationTimer = new Timer("Coordination Timer", true);
    }

    void cleanUp()
    {
        // terminate coordination timeout timer
        coordinationTimer.purge();
        coordinationTimer.cancel();

        // terminate all active coordinations
        final Exception reason = new Exception();
        for (Coordination c : coordinations.values())
        {
            c.fail(reason);
        }
        coordinations.clear();

        // release all participants
        participants.clear();

        // cannot really clear out the thread local but we can let it go
        threadStacks = null;
    }

    void configure(final long coordinationTimeout, final long participationTimeout)
    {
        this.defaultTimeOut = coordinationTimeout;
        this.participationTimeOut = participationTimeout;
    }

    void schedule(final TimerTask task, final long deadLine)
    {
        if (deadLine < 0)
        {
            task.cancel();
        }
        else
        {
            coordinationTimer.schedule(task, new Date(deadLine));
        }
    }

    void lockParticipant(final Participant p, final CoordinationImpl c)
    {
        synchronized (participants)
        {
            // wait for participant to be released
            long cutOff = System.currentTimeMillis() + participationTimeOut;
            long waitTime = (participationTimeOut > 500) ? participationTimeOut / 500 : participationTimeOut;
            CoordinationImpl current = participants.get(p);
            while (current != null && current != c)
            {
                if (current.getThread() == c.getThread())
                {
                    throw new CoordinationException("Participant " + p + " already participating in Coordination "
                        + current.getId() + "/" + current.getName() + " in this thread", c,
                        CoordinationException.DEADLOCK_DETECTED);
                }

                try
                {
                    participants.wait(waitTime);
                }
                catch (InterruptedException ie)
                {
                    throw new CoordinationException("Interrupted waiting to add Participant " + p
                        + " currently participating in Coordination " + current.getId() + "/" + current.getName()
                        + " in this thread", c, CoordinationException.LOCK_INTERRUPTED);
                }

                // timeout waiting for participation
                if (System.currentTimeMillis() > cutOff)
                {
                    throw new CoordinationException("Timed out waiting to join coordinaton", c,
                        CoordinationException.UNKNOWN);
                }

                // check again
                current = participants.get(p);
            }

            // lock participant into coordination
            participants.put(p, c);
        }
    }

    void releaseParticipant(final Participant p)
    {
        synchronized (participants)
        {
            participants.remove(p);
            participants.notifyAll();
        }
    }

    // ---------- Coordinator back end implementation

    Coordination create(final CoordinatorImpl owner, final String name, final long timeout)
    {
        long id = ctr.incrementAndGet();
        CoordinationImpl c = new CoordinationImpl(owner, id, name, timeout);
        coordinations.put(id, c);
        return c;
    }

    void unregister(final CoordinationImpl c)
    {
        coordinations.remove(c.getId());
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null)
        {
            stack.remove(c);
        }
    }

    Coordination push(Coordination c)
    {
        Stack<Coordination> stack = threadStacks.get();
        if (stack == null)
        {
            stack = new Stack<Coordination>();
            threadStacks.set(stack);
        }
        return stack.push(c);
    }

    Coordination pop()
    {
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null && !stack.isEmpty())
        {
            return stack.pop();
        }
        return null;
    }

    Coordination peek()
    {
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null && !stack.isEmpty())
        {
            return stack.peek();
        }
        return null;
    }

    Collection<Coordination> getCoordinations()
    {
        ArrayList<Coordination> result = new ArrayList<Coordination>();
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null)
        {
            result.addAll(stack);
        }
        return result;
    }

    Coordination getCoordinationById(final long id)
    {
        CoordinationImpl c = coordinations.get(id);
        return (c == null || c.isTerminated()) ? null : c;
    }

    // ---------- CoordinatorMBean interface

    public TabularData listCoordinations(String regexFilter)
    {
        Pattern p = Pattern.compile(regexFilter);
        TabularData td = new TabularDataSupport(COORDINATIONS_TYPE);
        for (CoordinationImpl c : coordinations.values())
        {
            if (p.matcher(c.getName()).matches())
            {
                try
                {
                    td.put(fromCoordination(c));
                }
                catch (OpenDataException e)
                {
                    // TODO: log
                }
            }
        }
        return td;
    }

    public CompositeData getCoordination(long id) throws IOException
    {
        Coordination c = getCoordinationById(id);
        if (c != null)
        {
            try
            {
                return fromCoordination((CoordinationImpl) c);
            }
            catch (OpenDataException e)
            {
                throw new IOException(e.toString());
            }
        }
        throw new IOException("No such Coordination " + id);
    }

    public boolean fail(long id, String reason)
    {
        Coordination c = getCoordinationById(id);
        if (c != null)
        {
            return c.fail(new Exception(reason));
        }
        return false;
    }

    public void addTimeout(long id, long timeout)
    {
        Coordination c = getCoordinationById(id);
        if (c != null)
        {
            c.extendTimeout(timeout);
        }
    }

    private CompositeData fromCoordination(final CoordinationImpl c) throws OpenDataException
    {
        return new CompositeDataSupport(COORDINATION_TYPE, new String[]
            { ID, NAME, TIMEOUT }, new Object[]
            { c.getId(), c.getName(), c.getDeadLine() });
    }
}
