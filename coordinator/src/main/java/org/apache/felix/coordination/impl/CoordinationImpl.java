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
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.coordination.Coordination;
import org.apache.felix.service.coordination.Participant;

@SuppressWarnings("deprecation")
public class CoordinationImpl implements Coordination {

    private enum State {
        /** Active */
        ACTIVE,

        /** Coordination termination started */
        TERMINATING,

        /** Coordination completed */
        TERMINATED,

        /** Coordination failed */
        FAILED;
    }

    private final long id;

    private final String name;

    // TODO: timeout must be enforced
    private long timeOutInMs;

    private State state;

    private boolean mustFail;

    private boolean timeout;

    private ArrayList<Participant> participants;

    private HashMap<Class<?>, Object> variables;

    public CoordinationImpl(final long id, final String name) {
        this.id = id;
        this.name = name;
        this.state = State.ACTIVE;
        this.participants = new ArrayList<Participant>();
        this.variables = new HashMap<Class<?>, Object>();
    }

    public String getName() {
        return name;
    }

    long getId() {
        return this.id;
    }

    void mustFail() {
        this.mustFail = true;
    }

    void timeout() {
        this.timeout = true;
    }

    long getTimeOut() {
        return this.timeOutInMs;
    }

    public int end() throws IllegalStateException {
        if (state == State.ACTIVE) {
            int reason = OK;
            if (mustFail || timeout) {
                fail(new Exception());
                reason = mustFail ? FAILED : TIMEOUT;
            } else {
                state = State.TERMINATING;
                for (Participant part : participants) {
                    try {
                        part.ended(this);
                    } catch (Exception e) {
                        // TODO: log
                        reason = PARTIALLY_ENDED;
                    }
                }
                state = State.TERMINATED;
            }
            return reason;
        }

        // already terminated
        throw new IllegalStateException();
    }

    public boolean fail(Throwable reason) {
        if (state == State.ACTIVE) {
            state = State.TERMINATING;
            for (Participant part : participants) {
                try {
                    part.failed(this);
                } catch (Exception e) {
                    // TODO: log
                }
            }
            state = State.FAILED;
            return true;
        }
        return false;
    }

    public boolean terminate() {
        if (state == State.ACTIVE) {
            end();
            return true;
        }
        return false;
    }

    public boolean isFailed() {
        return state == State.FAILED;
    }

    public void addTimeout(long timeOutInMs) {
        this.timeOutInMs = timeOutInMs;
    }

    public boolean participate(Participant p) {
        if (state == State.ACTIVE) {
            if (!participants.contains(p)) {
                participants.add(p);
            }
            return true;
        }
        return false;
    }

    public Collection<Participant> getParticipants() {
        return new ArrayList<Participant>(participants);
    }

    public Map<Class<?>, ?> getVariables() {
        return variables;
    }

}
