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

import java.util.Collection;

/**
 * A Coordinator service provides a facility to coordinate activities between
 * different parties. The Coordinator is a factory of Coordination objects.
 * Coordination objects can be created. Once created, they can be pushed on a
 * thread local stack push(Coordination) as an implicit parameter for calls to
 * other parties, or they can be passed as an argument. The current top of the
 * thread local stack can be obtained with getCurrentCoordination(). The
 * participate(Participant) method on this service or the
 * Coordination.participate(Participant) method can be used to participate in a
 * Coordination. Participants can only participate in a single Coordination, if
 * a Participant object is added to two different Coordinations it will block
 * until any prior Coordination has been ended. A Coordination can end correctly
 * when the Coordination.end() method is called or when it fails. If the
 * Coordination ends correctly, all its participants are called on the
 * Participant.ended(Coordination) method, otherwise the
 * Participant.failed(Coordination) is called. A Coordination can fail because
 * it times out or it is explicitly failed. A Coordination will timeout after an
 * implementation defined amount of time that must be higher than 30 seconds
 * unless overridden with configuration. This time can be set on a per
 * Coordination basis with the Coordination.addTimeout(long) method. The typical
 * usage of the Coordinator service is as follows:
 *
 * <pre>
 * Coordination coordination = coordinator.begin(&quot;mycoordination&quot;);
 * try {
 *     doWork();
 *     if (coordination.end() != Coordination.OK) log(&quot;failed&quot;);
 * } finally {
 *     coordination.terminate();
 * }
 * </pre>
 * <p>
 * In the doWork() method, code can be called that requires a callback at the
 * end of the Coordination. This code is for a Participant.
 *
 * <pre>
 * void doWork() {
 *     if (coordinator.participate(this)) {
 *         beginWork();
 *     } else {
 *         beginWork();
 *         finishWork();
 *     }
 * }
 *
 * void ended() {
 *     finishWork();
 * }
 *
 * void failed() {
 *     undoWork();
 * }
 * </pre>
 * <p>
 * Life cycle. All Coordinations that are begun through this service must
 * automatically fail before this service is ungotten.
 *
 * @ThreadSafe
 * @Provisional
 */
@Deprecated
public interface Coordinator {

    /**
     * Create a new Coordination that is not associated with the current thread.
     * Parameters:
     *
     * @param name The name of this coordination, a name does not have to be
     *            unique.
     * @return The new Coordination object or null
     * @throws SecurityException This method requires the action, no bundle
     *             check is done.
     */
    Coordination create(String name);

    /**
     * Begin a new Coordination and push it on the thread local stack with
     * push(Coordination). Parameters:
     *
     * @param name The name of this coordination, a name does not have to be
     *            unique.
     * @return A new Coordination object
     * @throws SecurityException This method requires the action, no bundle
     *             check is done.
     */
    Coordination begin(String name);

    /**
     * Associate the given Coordination object with a thread local stack. The
     * top of the thread local stack is returned with the
     * getCurrentCoordination() method. To remove the Coordination from the top
     * call pop().
     *
     * @param c The Coordination to push
     * @return c (for the builder pattern purpose)
     */
    Coordination push(Coordination c);

    /**
     * Pop the top of the thread local stack of Coordinations. If no current
     * Coordination is present, return null.
     *
     * @return The top of the stack or null
     */
    Coordination pop();

    /**
     * Participate in the current Coordination or return false if there is none.
     * This method calls getCurrentCoordination(), if it is null, it will return
     * false. Otherwise it will call Coordination.participate(Participant) and
     * return the result of that method.
     *
     * @param participant The participant of the Coordination
     * @return true if there was a current Coordination that could be
     *         successfully used to participate, otherwise false.
     * @throws CoordinationException This exception should normally not be
     *             caught by the caller but allowed to bubble up to the
     *             initiator of the coordination, it is therefore a
     *             RuntimeException. It signals that this participant could not
     *             participate the current coordination. This can be cause by
     *             the following reasons:
     *             <ol>
     *             <li>CoordinationException.DEADLOCK_DETECTED</li>
     *             <li>CoordinationException.TIMEOUT</li>
     *             <li>CoordinationException.UNKNOWN</li>
     *             </ol>
     * @throws SecurityException This method requires the action for the current
     *             Coordination, if any.
     */
    boolean participate(Participant participant) throws CoordinationException;

    /**
     * Always fail the current Coordination, if exists. Must fail the current
     * Coordination and return true or return false if there is no current
     * Coordination.
     *
     * @param reason Throwable describing why the collaboration must always fail
     *            for debugging or null.
     * @return true if there was a current Coordination and false if not.
     */
    boolean alwaysFail(Throwable reason);

    /**
     * Return the current Coordination. The current Coordination is the top of
     * the thread local stack of Coordinations. If the stack is empty, there is
     * no current Coordination.
     *
     * @return null when the thread local stack is empty, otherwise the top of
     *         the thread local stack of Coordinations.
     */
    Coordination getCurrentCoordination();

    /**
     * Provide a list of all Coordination objects. Answer a read only list of
     * active Coordination. This list must be a mutable snapshot of the current
     * situation. Changes to the list must not affect the original. Coordination
     * objects are capabilities and designed to be used only on the Coordination
     * thread. The returned list must only contain the Coordinations for which
     * the caller has , without this permission an empty list must be returned.
     *
     * @return a list of Coordination objects
     */
    Collection<Coordination> getCoordinations();
}
