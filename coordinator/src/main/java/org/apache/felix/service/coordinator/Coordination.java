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
public interface Coordination
{

    /**
     * The TIMEOUT exception is a singleton exception that will the reason for
     * the failure when the Coordination times out.
     */
    public static final Exception TIMEOUT = new Exception();

    /**
     * A system assigned ID unique for a specific registered Coordinator. This
     * id must not be reused as long as the Coordinator is registered and must
     * be monotonically increasing for each Coordination and always be positive.
     *
     * @return an id
     */
    long getId();

    /**
     * Return the name of this Coordination. The name is given in the
     * {@link Coordinator#begin(String, int)} or
     * {@link Coordinator#create(String, int)} method. The name should follow
     * the same naming pattern as a Bundle Symbolc Name.
     *
     * @return the name of this Coordination
     */
    String getName();

    /**
     * Fail this Coordination. If this Coordination is not terminated, fail it
     * and call the {@link Participant#failed(Coordination)} method on all
     * participant on the current thread. Participants must assume that the
     * Coordination failed and should discard and cleanup any work that was
     * processed during this Coordination. The {@link #fail(Throwable)} method
     * will return <code>true</code> if it caused the termination. A fail method
     * must return silently when the Coordination has already finished and
     * return <code>false</code>. The fail method must terminate the current
     * Coordination before any of the failed methods are called. That is, the
     * {@link Participant#failed(Coordination)} methods must be running outside
     * the current coordination, adding participants during this phase will
     * cause a Configuration Exception to be thrown. If the Coordination is
     * pushed on the Coordinator stack it is associated with a specific thread.
     *
     * @param reason The reason of the failure, must not be <code>null</code>
     * @return true if the Coordination was active and this coordination was
     *         terminated due to this call, otherwise false
     */
    boolean fail(Throwable reason);

    /**
     * End the current Coordination.
     *
     * <pre>
     * void foo() throws CoordinationException
     * {
     *     Coordination c = coordinator.begin(&quot;work&quot;, 0);
     *     try
     *     {
     *         doWork();
     *     }
     *     catch (Exception e)
     *     {
     *         c.fail(e);
     *     }
     *     finally
     *     {
     *         c.end();
     *     }
     * }
     * </pre>
     *
     * If the coordination was terminated this method throws a Configuration
     * Exception. Otherwise, any participants will be called on their
     * {@link Participant#ended(Coordination)} method. A successful return of
     * this {@link #end()} method indicates that the Coordination has properly
     * terminated and any participants have been informed of the positive
     * outcome. It is possible that one of the participants throws an exception
     * during the callback. If this happens, the coordination fails partially
     * and this is reported with an exception. This method must terminate the
     * current Coordination before any of the
     * {@link Participant#ended(Coordination)} methods are called. That is, the
     * {@link Participant#ended(Coordination)} methods must be running outside
     * the current coordination, no participants can be added during the
     * termination phase. If the Coordination is on a thread local stack then it
     * must be removed from this stack during termination.
     *
     * @throws CoordinationException when the Coordination has (partially)
     *             failed or timed out.
     *             <ol>
     *             <li>{@link CoordinationException#PARTIALLY_ENDED}</li>
     *             <li>{@link CoordinationException#ALREADY_ENDED}</li>
     *             <li>{@link CoordinationException#FAILED}</li>
     *             <li>{@link CoordinationException#UNKNOWN}</li>
     *             </ol>
     */
    void end() throws CoordinationException;

    /**
     * Return a mutable snapshot of the participants that joined the
     * Coordination. Each unique Participant object as defined by its identity
     * occurs only once in this list.
     *
     * @return list of participants.
     * @throws SecurityException This method requires the
     *             {@link CoordinationPermission#ADMIN} action for the
     *             {@link CoordinationPermission}.
     */
    Collection<Participant> getParticipants();

    /**
     * If the coordination has failed because {@link #fail(Throwable)} was
     * called then this method can provide the Throwable that was given as
     * argument to the {@link #fail(Throwable)} method. A timeout on this
     * Coordination will set the failure to a TimeoutException.
     *
     * @return a Throwable if this Coordination has failed, otherwise
     *         <code>null</code> if no failure occurred.
     */
    Throwable getFailure();

    /**
     * Add a Participant to this Coordination. Once a Participant is
     * participating it is guaranteed to receive a call back on either the
     * {@link Participant#ended(Coordination)} or
     * {@link Participant#failed(Coordination)} method when the Coordination is
     * terminated. A participant can be added to the Coordination multiple times
     * but it must only be called back once when the Coordination is terminated.
     * A Participant can only participate at a single Coordination, if it
     * attempts to block at another Coordination, then it will block until prior
     * Coordinations are finished. Notice that in edge cases the call back can
     * happen before this method returns. The ordering of the call-backs must
     * follow the order of participation. If participant is participating
     * multiple times the first time it participates defines this order.
     * *@param participant The participant of the Coordination
     *
     * @throws CoordinationException This exception should normally not be
     *             caught by the caller but allowed to bubble up to the
     *             initiator of the coordination, it is therefore a
     *             <code>RuntimeException</code>. It signals that this
     *             participant could not
     *             participate the current coordination. This can be cause by
     *             the following reasons:
     *             <ol>
     *             <li>{@link CoordinationException#DEADLOCK_DETECTED}</li>
     *             <li>{@link CoordinationException#ALREADY_ENDED}</li>
     *             <li>{@link CoordinationException#LOCK_INTERRUPTED}</li>
     *             <li>{@link CoordinationException#FAILED}</li>
     *             <li>{@link CoordinationException#UNKNOWN}</li>
     *             </ol>
     * @throws SecurityException This method requires the
     *             {@link CoordinationPermission#PARTICIPATE} action for the
     *             current Coordination, if any.
     */
    void addParticipant(Participant participant);

    /**
     * A utility map associated with the current Coordination. Each coordination
     * carries a map that can be used for communicating between different
     * participants. To namespace of the map is a class, allowing for private
     * date to be stored in the map by using implementation classes or shared
     * data by interfaces. The returned map is does not have to not
     * synchronized. Users of this map must synchronize on the Map object while
     * making changes.
     *
     * @return The map
     */
    Map<Class<?>, ?> getVariables();

    /**
     * Extend the time out. Allows participants to extend the timeout of the
     * coordination with at least the given amount. This can be done by
     * participants when they know a task will take more than normal time. This
     * method returns the new deadline. Passing 0 will return the existing
     * deadline.
     *
     * @param timeInMillis Add this timeout to the current timeout. If the
     *            current timeout was set to 0, no extension must take place. A
     *            zero or negative value must have no effect.
     * @return the new deadline in the format of
     *         <code>System.currentTimeMillis()</code> or 0 if no timeout was
     *         set.
     * @throws CoordinationException Can throw
     *             <ol>
     *             <li>{@link CoordinationException#ALREADY_ENDED}</li>
     *             <li>{@link CoordinationException#FAILED}</li>
     *             <li>{@link CoordinationException#UNKNOWN}</li>
     *             </ol>
     */
    long extendTimeout(long timeInMs) throws CoordinationException;

    /**
     * @return true if this Coordination has terminated otherwise false.
     */
    boolean isTerminated();

    /**
     * Answer the associated thread or null.
     *
     * @return Associated thread or null
     */
    Thread getThread();

    /**
     * Wait until the Coordination is terminated and all Participant objects
     * have been called.
     *
     * @param timeoutInMillis Maximum time to wait, 0 is forever
     * @throws InterruptedException If the wait is interrupted
     */
    void join(long timeoutInMillis) throws InterruptedException;

    /**
     * Associate the given Coordination object with a thread local stack of its
     * Coordinator. The top of the thread local stack is returned with the
     * {@link Coordinator#peek()} method. To remove the Coordination from the
     * top call {@link Coordinator#pop()}.
     *
     * @return this (for the builder pattern purpose)
     * @throws CoordinationException Can throw the
     *             <ol>
     *             <li>{@link CoordinationException#ALREADY_PUSHED}</li>
     *             <li>{@link CoordinationException#UNKNOWN}</li>
     *             </ol>
     */
    Coordination push() throws CoordinationException;
}
