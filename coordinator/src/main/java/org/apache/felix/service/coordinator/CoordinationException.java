/*
 * Copyright (c) OSGi Alliance (2004, 2010). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.service.coordinator;

/**
 * Thrown when an implementation detects a potential deadlock situation that it
 * cannot solve. The name of the current coordination is given as argument.
 *
 * @Provisional
 */
@Deprecated
public class CoordinationException extends RuntimeException
{

    private static final long serialVersionUID = -4466063711012717361L;

    /**
     * Unknown reason fot this exception.
     */
    public static final int UNKNOWN = 0;

    /**
     * Adding a participant caused a deadlock.
     */
    public static final int DEADLOCK_DETECTED = 1;

    /**
     * The Coordination was failed with {@link Coordination#fail(Throwable)}.
     * When this exception type is used, the {@link Coordination#getFailure()}
     * method must return a non-null value.
     */
    public static final int FAILED = 3;

    /**
     * The Coordination was partially ended.
     */
    public static final int PARTIALLY_ENDED = 4;

    /**
     * The Coordination was already ended.
     */
    public static final int ALREADY_ENDED = 5;

    /**
     * A Coordination was pushed on the stack that was already pushed.
     */
    public static final int ALREADY_PUSHED = 6;

    /**
     * Interrupted while trying to lock the participant.
     */
    public static final int LOCK_INTERRUPTED = 7;

    /**
     * The Coordination timed out.
     */
    public static final int TIMEOUT = 9;

    private final Coordination coordination;

    private final int type;

    /**
     * Create a new Coordination Exception.
     *
     * @param message The message
     * @param coordination The coordination that failed
     * @param type The reason for the exception
     * @param exception The exception
     */
    public CoordinationException(String message, Coordination coordination, int type, Throwable exception)
    {
        super(message, exception);
        this.coordination = coordination;
        this.type = type;
    }

    /**
     * Create a new Coordination Exception.
     *
     * @param message The message
     * @param coordination The coordination that failed
     * @param type The reason for the exception
     */
    public CoordinationException(String message, Coordination coordination, int type)
    {
        super(message);
        this.coordination = coordination;
        this.type = type;
    }

    /**
     * Answer the name of the Coordination associated with this exception.
     *
     * @return the Coordination name
     */
    public String getName()
    {
        return coordination.getName();
    }

    /**
     * Answer the reason.
     *
     * @return the reason
     */
    public int getType()
    {
        return type;
    }

    /**
     * Must be set if to the exception type is {@link #FAILED}
     *
     * @return If exception is {@link #FAILED} a Throwable
     */
    public Throwable getFailure()
    {
        return getCause();
    }

    /**
     * @return Answer the id
     */
    public long getId()
    {
        return coordination.getId();
    }
}
