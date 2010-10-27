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
package org.apache.felix.coordination.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import org.apache.felix.service.coordination.Coordination;
import org.apache.felix.service.coordination.Participant;

@SuppressWarnings("deprecation")
public class CoordinationImpl implements Coordination {

    /** Active */
    private static final int ACTIVE = 1;

    /** Coordination termination started */
    private static final int TERMINATING = 2;

    /** Coordination completed */
    private static final int TERMINATED = 3;

    /** Coordination failed */
    private static final int FAILED = 4;

    private final CoordinationMgr mgr;

    private final long id;

    private final String name;

    // TODO: timeout must be enforced
    private long timeOutInMs;

    /**
     * Access to this field must be synchronized as long as the expected state
     * is {@link #ACTIVE}. Once the state has changed, further updates to this
     * instance will not take place any more and the state will only be modified
     * by the thread successfully setting the state to {@link #TERMINATING}.
     */
    private volatile int state;

    private int mustFail;

    private Throwable failReason;

    private ArrayList<Participant> participants;

    private HashMap<Class<?>, Object> variables;

    private TimerTask timeoutTask;

    private Thread initiatorThread;

    public CoordinationImpl(final CoordinationMgr mgr, final long id,
            final String name, final long defaultTimeOutInMs) {
        this.mgr = mgr;
        this.id = id;
        this.name = name;
        this.mustFail = 0;
        this.state = ACTIVE;
        this.participants = new ArrayList<Participant>();
        this.variables = new HashMap<Class<?>, Object>();
        this.timeOutInMs = -defaultTimeOutInMs;
        this.initiatorThread = Thread.currentThread();

        scheduleTimeout(defaultTimeOutInMs);
    }

    public String getName() {
        return name;
    }

    long getId() {
        return this.id;
    }

    void mustFail(final Throwable reason) {
        this.mustFail = FAILED;
        this.failReason = reason;
    }

    /**
     * Initiates a coordination timeout. Called from the timer task scheduled by
     * the {@link #scheduleTimeout(long)} method.
     * <p>
     * This method is inteded to only be called from the scheduled timer task.
     */
    void timeout() {
        // If a timeout happens, the coordination thread is set to always fail
        this.mustFail = TIMEOUT;

        // Faile the Coordination upon timeout
        CoordinationImpl.this.fail(null);
    }

    long getTimeOut() {
        return this.timeOutInMs;
    }

    public int end() throws IllegalStateException {
        if (startTermination()) {
            if (mustFail != 0) {
                failInternal();
                return mustFail;
            }
            return endInternal();
        }

        // already terminated
        throw new IllegalStateException();
    }

    public boolean fail(Throwable reason) {
        if (startTermination()) {
            this.failReason = reason;
            failInternal();
            return true;
        }
        return false;
    }

    public boolean terminate() {
        if (state == ACTIVE) {
            try {
                end();
                return true;
            } catch (IllegalStateException ise) {
                // another thread might have started the termination just
                // after the current thread checked the state but before the
                // end() method called on this thread was able to change the
                // state. Just ignore this exception and continue.
            }
        }
        return false;
    }

    /**
     * Returns whether the coordination has ended in failure.
     * <p>
     * The return value of <code>false</code> may be a transient situation if
     * the coordination is in the process of terminating due to a failure.
     */
    public boolean isFailed() {
        return state == FAILED;
    }

    /**
     * Returns whether the coordination has ended.
     * <p>
     * The return value of <code>false</code> may be a transient situation if
     * the coordination is in the process of terminating.
     */
    public boolean isTerminated() {
        return state == TERMINATED;
    }

    public void addTimeout(long timeOutInMs) {
        if (this.timeOutInMs > 0) {
            // already set, ignore
        }

        this.timeOutInMs = timeOutInMs;
        scheduleTimeout(timeOutInMs);
    }

    /**
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
    public boolean participate(Participant p) {

        // ensure participant only pariticipates on a single coordination
        // this blocks until the participant can participate or until
        // a timeout occurrs (or a deadlock is detected)
        mgr.lockParticipant(p, this);

        // synchronize access to the state to prevent it from being changed
        // while adding the participant
        synchronized (this) {
            if (state == ACTIVE) {
                if (!participants.contains(p)) {
                    participants.add(p);
                }
                return true;
            }
            return false;
        }
    }

    public Collection<Participant> getParticipants() {
        // synchronize access to the state to prevent it from being changed
        // while we create a copy of the participant list
        synchronized (this) {
            if (state == ACTIVE) {
                return new ArrayList<Participant>(participants);
            }
        }

        return Collections.<Participant> emptyList();
    }

    public Map<Class<?>, ?> getVariables() {
        return variables;
    }

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
    private synchronized boolean startTermination() {
        if (state == ACTIVE) {
            state = TERMINATING;
            mgr.unregister(this);
            scheduleTimeout(-1);
            return true;
        }

        // this coordination is not active any longer, nothing to do
        return false;
    }

    /**
     * Internal implemenation of successful termination of the coordination.
     * <p>
     * This method must only be called after the {@link #state} field has been
     * set to {@link State#TERMINATING} and only be the method successfully
     * setting this state.
     *
     * @return OK or PARTIALLY_ENDED depending on whether all participants
     *         succeeded or some of them failed ending the coordination.
     */
    private int endInternal() {
        int reason = OK;
        for (Participant part : participants) {
            try {
                part.ended(this);
            } catch (Exception e) {
                // TODO: log
                reason = PARTIALLY_ENDED;
            }

            // release the participant for other coordinations
            mgr.releaseParticipant(part);
        }
        state = TERMINATED;
        return reason;
    }

    /**
     * Internal implemenation of coordination failure.
     * <p>
     * This method must only be called after the {@link #state} field has been
     * set to {@link State#TERMINATING} and only be the method successfully
     * setting this state.
     */
    private void failInternal() {
        // consider failure reason (if not null)
        for (Participant part : participants) {
            try {
                part.failed(this);
            } catch (Exception e) {
                // TODO: log
            }

            // release the participant for other coordinations
            mgr.releaseParticipant(part);
        }
        state = FAILED;
    }

    /**
     * Helper method for timeout scheduling. If a timer is currently scheduled
     * it is canceled. If the new timeout value is a positive value a new timer
     * is scheduled to fire of so many milliseconds from now.
     *
     * @param timeout The new timeout value
     */
    private void scheduleTimeout(final long timeout) {
        if (timeoutTask != null) {
            mgr.schedule(timeoutTask, -1);
            timeoutTask = null;
        }

        if (timeout > 0) {
            timeoutTask = new TimerTask() {
                @Override
                public void run() {
                    CoordinationImpl.this.timeout();
                }
            };

            mgr.schedule(timeoutTask, timeout);
        }
    }
}
