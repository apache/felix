/*
 * Copyright (c) OSGi Alliance (2004, 2010). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.service.coordination;

/**
 * Thrown when an implementation detects a potential deadlock situation that it
 * cannot solve. The name of the current coordination is given as argument.
 *
 * @Provisional
 */
@Deprecated
public class CoordinationException extends RuntimeException {

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
     * The Coordination took too long to finish.
     */
    public static final int TIMEOUT = 2;

    private final String name;

    private final int reason;

    /**
     * Create a new Coordination Exception.
     *
     * @param message The message
     * @param name The name of the Coordination
     * @param reason The reason for the exception.
     */
    public CoordinationException(String message, String name, int reason) {
        super(message);
        this.name = name;
        this.reason = reason;
    }

    /**
     * Answer the name of the Coordination associated with this exception.
     *
     * @return the Coordination name
     */
    public String getName() {
        return name;
    }

    /**
     * Answer the reason.
     *
     * @return the reason
     */
    public int getReason() {
        return reason;
    }
}
