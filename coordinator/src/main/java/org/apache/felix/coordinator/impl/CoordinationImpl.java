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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import org.apache.felix.service.coordinator.Coordination;
import org.apache.felix.service.coordinator.CoordinationException;
import org.apache.felix.service.coordinator.Participant;

@SuppressWarnings("deprecation")
public class CoordinationImpl implements Coordination
{

    /** Active */
    private static final int ACTIVE = 1;

    /** Coordination termination started */
    private static final int TERMINATING = 2;

    /** Coordination completed */
    private static final int TERMINATED = 3;
=======
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.CoordinationPermission;
import org.osgi.service.coordinator.Participant;

public class CoordinationImpl implements Coordination
{

    private enum State {
        /** Active */
        ACTIVE,

        /** failed() called */
        FAILED,

        /** Coordination termination started */
        TERMINATING,

        /** Coordination completed */
        TERMINATED
    }

    private final WeakReference<CoordinationHolder> holderRef;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    private final CoordinatorImpl owner;

    private final long id;

    private final String name;

<<<<<<< HEAD
    // TODO: timeout must be enforced
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    private long deadLine;

    /**
     * Access to this field must be synchronized as long as the expected state
<<<<<<< HEAD
     * is {@link #ACTIVE}. Once the state has changed, further updates to this
     * instance will not take place any more and the state will only be modified
     * by the thread successfully setting the state to {@link #TERMINATING}.
     */
    private volatile int state;

    private Throwable failReason;

    private ArrayList<Participant> participants;

    private HashMap<Class<?>, Object> variables;

    private TimerTask timeoutTask;

    private Thread initiatorThread;

    public CoordinationImpl(final CoordinatorImpl owner, final long id, final String name, final int timeOutInMs)
    {
        // TODO: validate name against Bundle Symbolic Name pattern

        this.owner = owner;
        this.id = id;
        this.name = name;
        this.state = ACTIVE;
        this.participants = new ArrayList<Participant>();
        this.variables = new HashMap<Class<?>, Object>();
        this.deadLine = (timeOutInMs > 0) ? System.currentTimeMillis() + timeOutInMs : 0;
        this.initiatorThread = Thread.currentThread();
=======
     * is {@link State#ACTIVE}. Once the state has changed, further updates to this
     * instance will not take place any more and the state will only be modified
     * by the thread successfully setting the state to {@link State#TERMINATING}.
     */
    private volatile State state;

    private Throwable failReason;

    private final ArrayList<Participant> participants;

    private final Map<Class<?>, Object> variables;

    private TimerTask timeoutTask;

    private Thread associatedThread;

    private final Object waitLock = new Object();

    public static CoordinationMgr.CreationResult create(final CoordinatorImpl owner, final long id, final String name, final long timeOutInMs)
    {
        final CoordinationMgr.CreationResult result = new CoordinationMgr.CreationResult();
        result.holder = new CoordinationHolder();
        result.coordination = new CoordinationImpl(owner, id, name, timeOutInMs, result.holder);
        return result;
    }

    private CoordinationImpl(final CoordinatorImpl owner,
            final long id,
            final String name,
            final long timeOutInMs,
            final CoordinationHolder holder)
    {
        this.owner = owner;
        this.id = id;
        this.name = name;
        this.state = State.ACTIVE;
        this.participants = new ArrayList<Participant>();
        this.variables = new HashMap<Class<?>, Object>();
        this.deadLine = (timeOutInMs > 0) ? System.currentTimeMillis() + timeOutInMs : 0;
        holder.setCoordination(this);
        this.holderRef = new WeakReference<CoordinationHolder>(holder);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        scheduleTimeout(deadLine);
    }

<<<<<<< HEAD
=======
    /**
     * @see org.osgi.service.coordinator.Coordination#getId()
     */
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public long getId()
    {
        return this.id;
    }

<<<<<<< HEAD
=======
    /**
     * @see org.osgi.service.coordinator.Coordination#getName()
     */
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public String getName()
    {
        return name;
    }

<<<<<<< HEAD
    public boolean fail(Throwable reason)
    {
=======
    /**
     * @see org.osgi.service.coordinator.Coordination#fail(java.lang.Throwable)
     */
    public boolean fail(final Throwable reason)
    {
        this.owner.checkPermission(name, CoordinationPermission.PARTICIPATE);
        if ( reason == null)
        {
            throw new IllegalArgumentException("Reason must not be null");
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if (startTermination())
        {
            this.failReason = reason;

<<<<<<< HEAD
            // consider failure reason (if not null)
            for (Participant part : participants)
            {
=======
            final List<Participant> releaseList = new ArrayList<Participant>();
            synchronized ( this.participants )
            {
                releaseList.addAll(this.participants);
                this.participants.clear();
            }
            // consider failure reason (if not null)
            for (int i=releaseList.size()-1;i>=0;i--)
            {
                final Participant part = releaseList.get(i);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                try
                {
                    part.failed(this);
                }
<<<<<<< HEAD
                catch (Exception e)
                {
                    // TODO: log
=======
                catch (final Exception e)
                {
                    LogWrapper.getLogger()
                        .log(LogWrapper.LOG_ERROR, "Participant threw exception during call to fail()", e);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }

                // release the participant for other coordinations
                owner.releaseParticipant(part);
            }

<<<<<<< HEAD
            state = TERMINATED;

            synchronized (this)
            {
                this.notifyAll();
=======
            this.owner.unregister(this, false);
            state = State.FAILED;

            synchronized (this.waitLock)
            {
                this.waitLock.notifyAll();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }

            return true;
        }
        return false;
    }

<<<<<<< HEAD
    public void end()
    {
        if (startTermination())
        {
            boolean partialFailure = false;
            for (Participant part : participants)
            {
                try
                {
                    part.ended(this);
                }
                catch (Exception e)
                {
                    // TODO: log
=======
    /**
     * @see org.osgi.service.coordinator.Coordination#end()
     */
    public void end()
    {
        this.owner.checkPermission(name, CoordinationPermission.INITIATE);
        if ( !this.isTerminated() && this.associatedThread != null && Thread.currentThread() != this.associatedThread )
        {
            throw new CoordinationException("Coordination is associated with different thread", this, CoordinationException.WRONG_THREAD);
        }

        if (startTermination())
        {

            final CoordinationException nestedFailed = this.owner.endNestedCoordinations(this);
            if ( nestedFailed != null )
            {
                this.failReason = nestedFailed;
            }
            boolean partialFailure = false;
            this.owner.unregister(this, true);

            final List<Participant> releaseList = new ArrayList<Participant>();
            synchronized ( this.participants )
            {
                releaseList.addAll(this.participants);
                this.participants.clear();
            }
            // consider failure reason (if not null)
            for (int i=releaseList.size()-1;i>=0;i--)
            {
                final Participant part = releaseList.get(i);
                try
                {
                    if ( this.failReason != null )
                    {
                        part.failed(this);
                    }
                    else
                    {
                        part.ended(this);
                    }
                }
                catch (final Exception e)
                {
                    LogWrapper.getLogger()
                        .log(LogWrapper.LOG_ERROR, "Participant threw exception during call to fail()", e);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    partialFailure = true;
                }

                // release the participant for other coordinations
                owner.releaseParticipant(part);
            }

<<<<<<< HEAD
            state = TERMINATED;

            synchronized (this)
            {
                this.notifyAll();
            }

=======
            state = State.TERMINATED;

            synchronized (this.waitLock)
            {
                this.waitLock.notifyAll();
            }

            this.associatedThread = null;

            if ( this.failReason != null )
            {
                throw new CoordinationException("Nested coordination failed", this,
                        CoordinationException.FAILED, this.failReason);
            }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            if (partialFailure)
            {
                throw new CoordinationException("One or more participants threw while ending the coordination", this,
                    CoordinationException.PARTIALLY_ENDED);
            }
        }
<<<<<<< HEAD
=======
        else if ( state == State.FAILED )
        {
            this.owner.unregister(this, true);
            state = State.TERMINATED;
            throw new CoordinationException("Coordination failed", this, CoordinationException.FAILED, failReason);
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        else
        {
            // already terminated
            throw new CoordinationException("Coordination " + id + "/" + name + " has already terminated", this,
                CoordinationException.ALREADY_ENDED);
        }
    }


<<<<<<< HEAD
    public Collection<Participant> getParticipants()
    {
=======
    /**
     * @see org.osgi.service.coordinator.Coordination#getParticipants()
     */
    public List<Participant> getParticipants()
    {
        this.owner.checkPermission(name, CoordinationPermission.INITIATE);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        // synchronize access to the state to prevent it from being changed
        // while we create a copy of the participant list
        synchronized (this)
        {
<<<<<<< HEAD
            if (state == ACTIVE)
            {
                return new ArrayList<Participant>(participants);
=======
            if (state == State.ACTIVE)
            {
                synchronized ( this.participants )
                {
                    return new ArrayList<Participant>(participants);
                }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }

        return Collections.<Participant> emptyList();
    }

<<<<<<< HEAD
    public Throwable getFailure()
    {
=======
    /**
     * @see org.osgi.service.coordinator.Coordination#getFailure()
     */
    public Throwable getFailure()
    {
        this.owner.checkPermission(name, CoordinationPermission.INITIATE);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        return failReason;
    }


    /**
<<<<<<< HEAD
     * Adds the participant to the end of the list of participants of this
     * coordination.
     * <p>
     * This method blocks if the given participant is currently participating in
     * another coordination.
     * <p>
     * Participants can only be added to a coordination if it is active.
     *
     * @throws org.apache.felix.service.coordination.CoordinationException if
     *             the participant cannot currently participate in this
     *             coordination
     */
    public void addParticipant(Participant p)
    {

        // ensure participant only pariticipates on a single coordination
        // this blocks until the participant can participate or until
        // a timeout occurrs (or a deadlock is detected)
=======
     * @see org.osgi.service.coordinator.Coordination#addParticipant(org.osgi.service.coordinator.Participant)
     */
    public void addParticipant(final Participant p)
    {
        this.owner.checkPermission(name, CoordinationPermission.PARTICIPATE);
        if ( p == null ) {
            throw new IllegalArgumentException("Participant must not be null");
        }
        // ensure participant only participates on a single coordination
        // this blocks until the participant can participate or until
        // a timeout occurs (or a deadlock is detected)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        owner.lockParticipant(p, this);

        // synchronize access to the state to prevent it from being changed
        // while adding the participant
        synchronized (this)
        {
            if (isTerminated())
            {
                owner.releaseParticipant(p);

                throw new CoordinationException("Cannot add Participant " + p + " to terminated Coordination", this,
<<<<<<< HEAD
                    (getFailure() != null) ? CoordinationException.FAILED : CoordinationException.ALREADY_ENDED);
            }

            if (!participants.contains(p))
            {
                participants.add(p);
=======
                    (getFailure() != null) ? CoordinationException.FAILED : CoordinationException.ALREADY_ENDED, getFailure());
            }

            synchronized ( this.participants )
            {
                boolean found = false;
                final Iterator<Participant> iter = this.participants.iterator();
                while ( !found && iter.hasNext() )
                {
                    if ( iter.next() == p )
                    {
                        found = true;
                    }
                }
                if (!found)
                {
                    participants.add(p);
                }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }

<<<<<<< HEAD
    public Map<Class<?>, ?> getVariables()
    {
        return variables;
    }

    public long extendTimeout(long timeOutInMs)
    {
        synchronized (this)
        {
            if (isTerminated())
            {
                throw new CoordinationException("Cannot extend timeout on terminated Coordination", this,
                    (getFailure() != null) ? CoordinationException.FAILED : CoordinationException.ALREADY_ENDED);
            }

            if (timeOutInMs > 0)
            {
                this.deadLine += timeOutInMs;
                scheduleTimeout(this.deadLine);
            }

            return this.deadLine;
        }
    }

    /**
     * Returns whether the coordination has ended.
     * <p>
     * The return value of <code>false</code> may be a transient situation if
     * the coordination is in the process of terminating.
     */
    public boolean isTerminated()
    {
        return state != ACTIVE;
    }

    public Thread getThread()
    {
        return initiatorThread;
    }

    public void join(long timeoutInMillis) throws InterruptedException
    {
        synchronized (this)
        {
            if (!isTerminated())
            {
                this.wait(timeoutInMillis);
=======
    /**
     * @see org.osgi.service.coordinator.Coordination#getVariables()
     */
    public Map<Class<?>, Object> getVariables()
    {
        this.owner.checkPermission(name, CoordinationPermission.PARTICIPATE);
        return variables;
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#extendTimeout(long)
     */
    public long extendTimeout(final long timeOutInMs)
    {
        this.owner.checkPermission(name, CoordinationPermission.PARTICIPATE);
        if ( timeOutInMs < 0 )
        {
            throw new IllegalArgumentException("Timeout must not be negative");
        }
        if ( this.deadLine > 0 )
        {
            synchronized (this)
            {
                if (isTerminated())
                {
                    throw new CoordinationException("Cannot extend timeout on terminated Coordination", this,
                        (getFailure() != null) ? CoordinationException.FAILED : CoordinationException.ALREADY_ENDED, getFailure());
                }

                if (timeOutInMs > 0)
                {
                    this.deadLine += timeOutInMs;
                    scheduleTimeout(this.deadLine);
                }

            }
        }
        return this.deadLine;
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#isTerminated()
     */
    public boolean isTerminated()
    {
        return state != State.ACTIVE;
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#getThread()
     */
    public Thread getThread()
    {
        this.owner.checkPermission(name, CoordinationPermission.ADMIN);
        return associatedThread;
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#join(long)
     */
    public void join(final long timeOutInMs) throws InterruptedException
    {
        this.owner.checkPermission(name, CoordinationPermission.PARTICIPATE);
        if ( timeOutInMs < 0 )
        {
            throw new IllegalArgumentException("Timeout must not be negative");
        }

        if ( !isTerminated() )
        {
            synchronized ( this.waitLock )
            {
                this.waitLock.wait(timeOutInMs);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }

<<<<<<< HEAD
    public Coordination push()
    {
        // TODO: Check whether this has already been pushed !
        // throw new CoordinationException("Coordination already pushed", this, CoordinationException.ALREADY_PUSHED);

        return owner.push(this);
=======
    /**
     * @see org.osgi.service.coordinator.Coordination#push()
     */
    public Coordination push()
    {
        this.owner.checkPermission(name, CoordinationPermission.INITIATE);
    	if ( isTerminated() )
    	{
            throw new CoordinationException("Coordination already ended", this, CoordinationException.ALREADY_ENDED);
    	}

        owner.push(this);
        return this;
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#getBundle()
     */
    public Bundle getBundle()
    {
        this.owner.checkPermission(name, CoordinationPermission.ADMIN);
        return this.owner.getBundle();
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#getEnclosingCoordination()
     */
    public Coordination getEnclosingCoordination()
    {
        this.owner.checkPermission(name, CoordinationPermission.ADMIN);
        Coordination c = this.owner.getEnclosingCoordination(this);
        if ( c != null )
        {
            c = ((CoordinationImpl)c).holderRef.get();
        }
        return c;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    //-------

    /**
     * Initiates a coordination timeout. Called from the timer task scheduled by
     * the {@link #scheduleTimeout(long)} method.
     * <p>
<<<<<<< HEAD
     * This method is inteded to only be called from the scheduled timer task.
     */
    void timeout()
=======
     * This method is intended to only be called from the scheduled timer task.
     */
    private void timeout()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        // Fail the Coordination upon timeout
        fail(TIMEOUT);
    }

<<<<<<< HEAD
    long getDeadLine()
    {
        return this.deadLine;
    }

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     * If this coordination is still active, this method initiates the
     * termination of the coordination by setting the state to
     * {@value State#TERMINATING}, unregistering from the
     * {@link CoordinationMgr} and ensuring there is no timeout task active any
     * longer to timeout this coordination.
     *
     * @return <code>true</code> If the coordination was active and termination
     *         can continue. If <code>false</code> is returned, the coordination
     *         must be considered terminated (or terminating) in the current
     *         thread and no further termination processing must take place.
     */
    private synchronized boolean startTermination()
    {
<<<<<<< HEAD
        if (state == ACTIVE)
        {
            state = TERMINATING;
            owner.unregister(this);
=======
        if (state == State.ACTIVE)
        {
            state = State.TERMINATING;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            scheduleTimeout(-1);
            return true;
        }

        // this coordination is not active any longer, nothing to do
        return false;
    }

    /**
     * Helper method for timeout scheduling. If a timer is currently scheduled
     * it is canceled. If the new timeout value is a positive value a new timer
     * is scheduled to fire at the desired time (in the future)
     *
     * @param deadline The at which to schedule the timer
     */
    private void scheduleTimeout(final long deadLine)
    {
        if (timeoutTask != null)
        {
            owner.schedule(timeoutTask, -1);
            timeoutTask = null;
        }

        if (deadLine > System.currentTimeMillis())
        {
            timeoutTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    CoordinationImpl.this.timeout();
                }
            };

            owner.schedule(timeoutTask, deadLine);
        }
    }
<<<<<<< HEAD
=======

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (obj instanceof CoordinationHolder )
		{
		    return obj.equals(this);
		}
        if ( !(obj instanceof CoordinationImpl) )
        {
			return false;
        }
		final CoordinationImpl other = (CoordinationImpl) obj;
		return id == other.id;
	}

	void setAssociatedThread(final Thread t) {
	    this.associatedThread = t;
	}

    public Coordination getHolder() {
        return this.holderRef.get();
    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
