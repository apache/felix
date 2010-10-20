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
import java.util.Map;

/**
 * A Coordination object is used to coordinate a number of independent
 * participants. Once a Coordination is created, it can be used to add
 * Participant objects. When the Coordination is ended, the participants are
 * called back. A Coordination can also fail for various reasons, in that case
 * the participants are informed of this failure.
 *
 * @ThreadSafe
 * @Provisional
 */
@Deprecated
public interface Coordination {
    /**
     * Return value of end(). The Coordination ended normally, no participant
     * threw an exception.
     */
    public static final int OK = 0;

    /**
     * Return value of end(). The Coordination did not end normally, a
     * participant threw an exception making the outcome unclear.
     */
    public static final int PARTIALLY_ENDED = 1;

    /**
     * Return value of end(). The Coordination was set to always fail
     * (fail(Throwable)).
     */
    public static final int FAILED = 2;

    /**
     * Return value of end(). The Coordination failed because it had timed out.
     */
    public static final int TIMEOUT = 3;

    /**
     * Return the name of this Coordination. The name is given in the
     * Coordinator.begin(String) or Coordinator.create(String) method.
     *
     * @return the name of this Coordination
     */
    String getName();

    /**
     * Fail and then end this Coordination while returning the outcome. Any
     * participants will be called on their Participant.failed(Coordination)
     * method. Participants must assume that the Coordination failed and should
     * discard and cleanup any work that was processed during this Coordination.
     * The fail method must terminate the current Coordination before any of the
     * failed methods are called. That is, the Participant.failed(Coordination)
     * methods must be running outside the current coordination, no participants
     * can be added during the termination phase. A fail method must return
     * silently when the Coordination has already finished.
     *
     * @param reasonThrowable describing the reason of the failure for
     *            documentation
     * @return true if the Coordination was still active, otherwise false
     */
    boolean fail(Throwable reason);

    /**
     * If the Coordination is terminated then return, otherwise set the
     * Coordination to fail. This method enables the following fail-safe pattern
     * to ensure Coordinations are properly terminated.
     *
     * <pre>
     * Coordination c = coordinator.begin("show_fail");
     *     try {
     *         work1();
     *         work2();
     *         if ( end() != OK )
     *             log("...");
     *     } catch( SomeException e) {
     *         ...
     *     } finally {
     *         c.terminate();
     *     }
     * </pre>
     * <p>
     * With this pattern, it is easy to ensure that the coordination is always
     * terminated.
     *
     * @return true if this method actually terminated the coordination (that
     *         is, it was not properly ended). false if the Coordination was
     *         already properly terminate by an end() or fail(Throwable) method.
     */
    boolean terminate();

    /**
     * End the current Coordination. Any participants will be called on their
     * Participant.ended(Coordination) method. This end() method indicates that
     * the Coordination has properly terminated and any participants should The
     * end method must terminate the current Coordination before any of the
     * Participant.ended(Coordination) methods is called. That is, the
     * Participant.ended(Coordination) methods must be running outside the
     * current coordination, no participants can be added during the termination
     * phase. This method returns the outcome of the Coordination:
     * <ol>
     * <li>OK - Correct outcome, no exceptions thrown</li>
     * <li>PARTIALLY_ENDED - One of the participants threw an exception</li>
     * <li>FAILED - The Coordination was set to always fail</li>
     * </ol>
     *
     * @return OK, PARTIALLY_ENDED, FAILED
     * @throws IllegalStateException when the Coordination is already
     *             terminated.
     */
    int end() throws IllegalStateException;

    /**
     * Return the current list of participants that joined the Coordination.
     * This list is only valid as long as the Coordination has not been
     * terminated. That is, after end() or fail(Throwable) is called this method
     * will return an empty list.
     *
     * @return list of participants.
     * @throws SecurityException This method requires the action for the
     *             CoordinationPermission.
     */
    Collection<Participant> getParticipants();

    /**
     * @return true if this Coordination has failed, false otherwise.
     */
    boolean isFailed();

    /**
     * @return true if this Coordination has terminated, false otherwise.
     */
    boolean isTerminated();

    /**
     * Add a minimum timeout for this Coordination. If this timeout expires,
     * then the Coordination will fail and the initiating thread will be
     * interrupted. This method must only be called on an active Coordination,
     * that is, before end() or fail(Throwable) is called. If the current
     * deadline is arriving later than the given timeout then the timeout is
     * ignored.
     *
     * @param timeOutInMsNumber of ms to wait, zero means forever.
     * @throws SecurityException This method requires the or action for the
     *             CoordinationPermission. participate
     */
    void addTimeout(long timeOutInMs);

    /**
     * Add a Participant to this Coordination. If this method returns true then
     * there was a current Coordination and the participant has successfully
     * joined it. If there was no current Coordination then false is returned.
     * Once a Participant is participating it is guaranteed to receive a call
     * back on either the Participant.ended(Coordination) or
     * Participant.failed(Coordination) method when the Coordination is
     * terminated. A participant can be added to the Coordination multiple times
     * but it must only be called back once when the Coordination is terminated.
     * A Participant can only participate at a single Coordination, if it
     * attempts to block at another Coordination, then it will block until prior
     * Coordinations are finished. Notice that in edge cases the call back can
     * happen before this method returns. The ordering of the call-backs must
     * follow the order of participation. If participant is participating
     * multiple times the first time it participates defines this order.
     *
     * @return true if the Coordination was active, otherwise false.
     * @throws CoordinationException - This exception should normally not be
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
    boolean participate(Participant p);

    /**
     * A utility map associated with the current Coordination. Each coordination
     * carries a map that can be used for communicating between different
     * participants. To namespace of the map is a class, allowing for private
     * date to be stored in the map by using implementation classes or shared
     * data by interfaces.
     *
     * @return The map
     */
    Map<Class<?>, ?> getVariables();
}
