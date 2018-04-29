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

<<<<<<< HEAD
import java.io.IOException;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
<<<<<<< HEAD
=======
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
<<<<<<< HEAD
import java.util.regex.Pattern;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.felix.jmx.service.coordinator.CoordinatorMBean;
import org.apache.felix.service.coordinator.Coordination;
import org.apache.felix.service.coordinator.CoordinationException;
import org.apache.felix.service.coordinator.Participant;

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
@SuppressWarnings("deprecation")
public class CoordinationMgr implements CoordinatorMBean
{

    private ThreadLocal<Stack<Coordination>> threadStacks;
=======

import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Participant;

/**
 * The <code>CoordinationMgr</code> is the actual back-end manager of all
 * Coordinations created by the Coordinator implementation.
 */
public class CoordinationMgr
{
    public static final class CreationResult 
    {
        public CoordinationImpl coordination;
        public CoordinationHolder holder;
    }

    private ThreadLocal<Stack<CoordinationImpl>> perThreadStack;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    private final AtomicLong ctr;

    private final Map<Long, CoordinationImpl> coordinations;

    private final Map<Participant, CoordinationImpl> participants;

    private final Timer coordinationTimer;

    /**
<<<<<<< HEAD
     * Default coordination timeout. Currently hard coded to be 30s (the
     * specified minimum timeout). Should be made configurable, but not less
     * than 30s.
     */
    private long defaultTimeOut = 30 * 1000L;

    /**
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * Wait at most 60 seconds for participant to be eligible for participation
     * in a coordination.
     *
     * @see #singularizeParticipant(Participant, CoordinationImpl)
     */
    private long participationTimeOut = 60 * 1000L;

    CoordinationMgr()
    {
<<<<<<< HEAD
        threadStacks = new ThreadLocal<Stack<Coordination>>();
        ctr = new AtomicLong(-1);
        coordinations = new HashMap<Long, CoordinationImpl>();
        participants = new HashMap<Participant, CoordinationImpl>();
=======
        perThreadStack = new ThreadLocal<Stack<CoordinationImpl>>();
        ctr = new AtomicLong(-1);
        coordinations = new HashMap<Long, CoordinationImpl>();
        participants = new IdentityHashMap<Participant, CoordinationImpl>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        coordinationTimer = new Timer("Coordination Timer", true);
    }

    void cleanUp()
    {
        // terminate coordination timeout timer
        coordinationTimer.purge();
        coordinationTimer.cancel();

        // terminate all active coordinations
<<<<<<< HEAD
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
=======
        final List<CoordinationImpl> coords = new ArrayList<CoordinationImpl>();
        synchronized ( this.coordinations ) {
            coords.addAll(this.coordinations.values());
            this.coordinations.clear();
        }
        for(final CoordinationImpl c : coords)
        {
            if ( !c.isTerminated() )
            {
                c.fail(Coordination.RELEASED);
            }
        }

        // release all participants
        synchronized ( this.participants )
        {
            participants.clear();
        }

        // cannot really clear out the thread local but we can let it go
        perThreadStack = null;
    }

    private Stack<CoordinationImpl> getThreadStack(final boolean create)
    {
        final ThreadLocal<Stack<CoordinationImpl>> tl = this.perThreadStack;
        Stack<CoordinationImpl> stack = null;
        if ( tl != null )
        {
            stack = tl.get();
            if ( stack == null && create ) {
                stack = new Stack<CoordinationImpl>();
                tl.set(stack);
            }
        }
        return stack;
    }

    void configure(final long participationTimeout)
    {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
            long cutOff = System.currentTimeMillis() + participationTimeOut;
            long waitTime = (participationTimeOut > 500) ? participationTimeOut / 500 : participationTimeOut;
            CoordinationImpl current = participants.get(p);
            while (current != null && current != c)
            {
                if (current.getThread() == c.getThread())
=======
            long completeWaitTime = participationTimeOut;
            long cutOff = System.currentTimeMillis() + completeWaitTime;

            CoordinationImpl current = participants.get(p);
            while (current != null && current != c)
            {
                final long waitTime = (completeWaitTime > 500) ? 500 : completeWaitTime;
                completeWaitTime = completeWaitTime - waitTime;
                if (current.getThread() != null && current.getThread() == c.getThread())
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
                if (System.currentTimeMillis() > cutOff)
                {
                    throw new CoordinationException("Timed out waiting to join coordinaton", c,
                        CoordinationException.UNKNOWN);
=======
                if (System.currentTimeMillis() >= cutOff)
                {
                    throw new CoordinationException("Timed out waiting to join coordinaton", c,
                        CoordinationException.FAILED, Coordination.TIMEOUT);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
    Coordination create(final CoordinatorImpl owner, final String name, final int timeout)
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
=======
    CreationResult create(final CoordinatorImpl owner, final String name, final long timeout)
    {
        final long id = ctr.incrementAndGet();
        final CreationResult result = CoordinationImpl.create(owner, id, name, timeout);
        synchronized ( this.coordinations )
        {
            coordinations.put(id, result.coordination);
        }
        return result;
    }

    void unregister(final CoordinationImpl c, final boolean removeFromThread)
    {
        synchronized ( this.coordinations )
        {
            coordinations.remove(c.getId());
        }
        if ( removeFromThread )
        {
            final Stack<CoordinationImpl> stack = this.getThreadStack(false);
            if (stack != null)
            {
                stack.remove(c);
            }
        }
    }

    void push(final CoordinationImpl c)
    {
        Stack<CoordinationImpl> stack = this.getThreadStack(true);
        if ( stack != null)
        {
            if ( stack.contains(c) )
            {
                throw new CoordinationException("Coordination already pushed", c, CoordinationException.ALREADY_PUSHED);
            }
            c.setAssociatedThread(Thread.currentThread());
            stack.push(c);
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    Coordination pop()
    {
<<<<<<< HEAD
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null && !stack.isEmpty())
        {
            return stack.pop();
=======
        final Stack<CoordinationImpl> stack = this.getThreadStack(false);
        if (stack != null && !stack.isEmpty())
        {
            final CoordinationImpl c = stack.pop();
            if ( c != null ) {
                c.setAssociatedThread(null);
            }
            return c;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return null;
    }

    Coordination peek()
    {
<<<<<<< HEAD
        Stack<Coordination> stack = threadStacks.get();
=======
        final Stack<CoordinationImpl> stack = this.getThreadStack(false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if (stack != null && !stack.isEmpty())
        {
            return stack.peek();
        }
        return null;
    }

    Collection<Coordination> getCoordinations()
    {
<<<<<<< HEAD
        ArrayList<Coordination> result = new ArrayList<Coordination>();
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null)
        {
            result.addAll(stack);
=======
        final ArrayList<Coordination> result = new ArrayList<Coordination>();
        synchronized ( this.coordinations )
        {
            for(final CoordinationImpl c : this.coordinations.values() )
            {
                result.add(c.getHolder());
            }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return result;
    }

    Coordination getCoordinationById(final long id)
    {
<<<<<<< HEAD
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
=======
        synchronized ( this.coordinations )
        {
            final CoordinationImpl c = coordinations.get(id);
            return (c == null || c.isTerminated()) ? null : c;
        }
    }

	public Coordination getEnclosingCoordination(final CoordinationImpl c)
	{
        final Stack<CoordinationImpl> stack = this.getThreadStack(false);
        if ( stack != null )
        {
        	final int index = stack.indexOf(c);
        	if ( index > 0 )
        	{
        		return stack.elementAt(index - 1);
        	}
        }
		return null;
	}

	public CoordinationException endNestedCoordinations(final CoordinationImpl c)
	{
	    CoordinationException partiallyFailed = null;
        final Stack<CoordinationImpl> stack = this.getThreadStack(false);
        if ( stack != null )
        {
        	final int index = stack.indexOf(c) + 1;
        	if ( index > 0 && stack.size() > index )
        	{
        		final int count = stack.size()-index;
        		for(int i=0;i<count;i++)
        		{
        			final CoordinationImpl nested = stack.pop();
        			try
        			{
        			    if ( partiallyFailed != null)
        			    {
        			        nested.fail(partiallyFailed);
        			    }
    			        nested.end();
        			}
        			catch ( final CoordinationException ce)
        			{
        			    partiallyFailed = ce;
        			}
        		}
        	}
        }
        return partiallyFailed;
	}

	/**
	 * Dispose all coordinations for that bundle
	 * @param owner The owner bundle
	 */
    public void dispose(final Bundle owner) {
        final List<CoordinationImpl> candidates = new ArrayList<CoordinationImpl>();
        synchronized ( this.coordinations )
        {
            final Iterator<Map.Entry<Long, CoordinationImpl>> iter = this.coordinations.entrySet().iterator();
            while ( iter.hasNext() )
            {
                final Map.Entry<Long, CoordinationImpl> entry = iter.next();
                final CoordinationImpl c = entry.getValue();
                if ( c.getBundle().getBundleId() == owner.getBundleId() )
                {
                    candidates.add(c);
                }
            }
        }
        if ( candidates.size() > 0 )
        {
            for(final CoordinationImpl c : candidates)
            {
                if ( !c.isTerminated() )
                {
                    c.fail(Coordination.RELEASED);
                }
                else
                {
                    this.unregister(c, true);
                }
            }
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
