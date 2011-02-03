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

import java.util.Collection;

/**
A Coordinator service coordinates activities between different parties. The Coordinator can create Coordination
objects. Once a Coordination object is created, it can be pushed on a thread local stack {@link Coordination#push()} as
an implicit parameter for calls to other parties, or it can be passed as an argument. The current top of the thread
local stack can be obtained with #peek(). The addParticipant(Participant) method on this service or the
Coordination.addParticipant(Participant) method can be used to participate in a Coordination. Participants
participate only in a single Coordination, if a Participant object is added to a second Coordination the
Coordination.addParticipant(Participant) method is blocked until the first Coordination is terminated. A
Coordination ends correctly when the Coordination.end() method is called before termination or when the
Coordination fails due to a timeout or a failure. If the Coordination ends correctly, all its participants are called on
the Participant.ended(Coordination) method, in all other cases the Participant.failed(Coordination) is
called. The typical usage of the Coordinator service is as follows:
<pre>
Coordination coordination = coordinator.begin("mycoordination",0);
try {
doWork();
}
finally {
coordination.end();
}
</pre>
In the doWork() method, code can be called that requires a callback at the end of the Coordination. The doWork
method can then add a Participant to the coordination. This code is for a Participant.
<pre>
void doWork() {
if (coordinator.addParticipant(this)) {
beginWork();
}
else {
beginWork();
finishWork();
}
}
void ended() {
finishWork();
}
void failed() {
undoWork();
}
</pre>
Life cycle. All Coordinations that are begun through this service must automatically fail before this service is
ungotten.
 *
 * @ThreadSafe
 * @Provisional
 */
@Deprecated
public interface Coordinator
{

    /**
     * Create a new Coordination that is not associated with the current thread.
     * Parameters:
     *
     * @param name The name of this coordination, a name does not have to be
     *            unique.
     * @param timeout Timeout in milliseconds, less or equal than 0 means no
     *            timeout
     * @return The new Coordination object, never <code>null</code>
     * @throws SecurityException This method requires the
     *             {@link CoordinationPermission#INITIATE} action, no bundle
     *             check is done.
     * @throws IllegalArgumentException when the name does not match the Bundle
     *             Symbolic Name pattern
     */
    Coordination create(String name, int timeout);

    /**
     * Provide a mutable snapshot collection of all Coordination objects
     * currently not terminated. Coordinations in
     * this list can have terminated before this list is returned or any time
     * thereafter. The returned collection must
     * only contain the Coordinations for which the caller has
     * {@link CoordinationPermission#ADMIN}, without this
     * permission an empty list must be returned.
     *
     * @return a list of Coordination objects filtered by
     *         {@link CoordinationPermission#ADMIN}
     */
    Collection<Coordination> getCoordinations();

    /**
     * Always fail the current Coordination, if it exists. If this is no current
     * Coordination return <code>false</code>. Otherwise return the result of
     * {@link Coordination#fail(Throwable)}, which is <code>true</code> in the
     * case this call terminates the Coordination and <code>false</code>
     * otherwise.
     *
     * <pre>
     * false - No current Coordination
     * false - Current Coordination was already terminated
     * true - Current Coordination got terminated due to this call
     * </pre>
     *
     * @param reason The reason for failure, must not be <code>null</code>.
     * @return <code>true</code> if there was a current Coordination and it was
     *         terminated, otherwise <code>false</code>.
     */
    boolean fail(Throwable reason);

    /**
     * Return the current Coordination or <code>null</code>. The current
     * Coordination is the top of the thread local stack of Coordinations. If
     * the stack is empty, there is no current Coordination.
     *
     * @return <code>null</code> when the thread local stack is empty, otherwise
     *         the top of the thread local stack of
     *         Coordinations.
     */
    Coordination peek();

    /**
     * Begin a new Coordination and push it on the thread local stack with
     * {@link Coordination#push()}.
     *
     * @param name The name of this coordination, a name does not have to be
     *            unique.
     * @param timeoutInMillis Timeout in milliseconds, less or equal than 0
     *            means no timeout
     * @return A new Coordination object
     * @throws SecurityException This method requires the
     *             {@link CoordinationPermission#INITIATE} action, no bundle
     *             check is done.
     * @throws IllegalArgumentException when the name does not match the Bundle
     *             Symbolic Name pattern
     */
    Coordination begin(String name, int timeoutInMillis);

    /**
     * Pop the top of the thread local stack of Coordinations. If no current
     * Coordination is present, return <code>null</code>.
     *
     * @return The top of the stack or <code>null</code>
     */
    Coordination pop();

    /**
     * Participate in the current Coordination and return <code>true</code> or
     * return <code>false</code> if there is none. This method calls
     * {@link #peek()}, if it is <code>null</code>, it will return
     * <code>false</code>. Otherwise it will call
     * {@link Coordination#addParticipant(Participant)}.
     *
     * @param participant The participant of the Coordination
     * @return <code>true</code> if there was a current Coordination that could
     *         be successfully used to participate, otherwise <code>false</code>
     *         .
     * @throws CoordinationException This exception should normally not be
     *             caught by the caller but allowed to bubble up to the
     *             initiator of the coordination, it is therefore a
     *             <code>RuntimeException</code>. It signals that this
     *             participant could not participate the current coordination.
     *             This can be cause by the following reasons:
     *             <ol>
     *             <li>{@link CoordinationException#DEADLOCK_DETECTED}</li>
     *             <li>{@link CoordinationException#ALREADY_ENDED}</li>
     *             <li>{@link CoordinationException#TIMEOUT}</li>
     *             <li>{@link CoordinationException#UNKNOWN}</li>
     *             </ol>
     * @throws SecurityException This method requires the
     *             {@link CoordinationPermission#PARTICIPATE} action for the
     *             current
     *             Coordination, if any.
     */
    boolean addParticipant(Participant participant) throws CoordinationException;

    /**
     * Answer the coordination associated with the given id if it exists.
     *
     * @param id The id of the requested Coordination
     * @return a Coordination with the given ID or <code>null</code> when
     *         Coordination cannot be found because it never existed or had
     *         terminated before this call.
     * @throws SecurityException if the caller has no
     *             {@link CoordinationPermission#ADMIN} for the requested
     *             Coordination.
     */
    Coordination getCoordination(long id);
}
