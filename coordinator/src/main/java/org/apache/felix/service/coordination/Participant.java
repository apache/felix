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
 * A Participant participates in a Coordination. A Participant can participate
 * in a Coordination by calling Coordinator.participate(Participant) or
 * Coordinator.participateOrBegin(Participant). After successfully initiating
 * the participation, the Participant is called back when the Coordination is
 * terminated. If a Coordination ends with the Coordination.end() method, then
 * all the participants are called back on their ended(Coordination) method. If
 * the initiator decides to fail the Coordination (or another party has called
 * Coordinator.alwaysFail(Throwable)) then the failed(Coordination) method is
 * called back. Participants are required to be thread safe for the
 * ended(Coordination) method and the failed(Coordination) method. Both methods
 * can be called on another thread. A Coordinator service must block a
 * Participant when it tries to participate in multiple Coordinations.
 *
 * @ThreadSafe
 * @Provisional
 */
@Deprecated
public interface Participant {

    /**
     * The Coordination has failed and the participant is informed. A
     * participant should properly discard any work it has done during the
     * active coordination.
     *
     * @param c The Coordination that does the callback
     * @throws Exception Any exception thrown should be logged but is further
     *             ignored and does not influence the outcome of the
     *             Coordination.
     */
    void failed(Coordination c) throws Exception;

    /**
     * The Coordination is being ended.
     *
     * @param c The Coordination that does the callback
     * @throws Exception If an exception is thrown it should be logged and the
     *             return of the Coordination.end() method must be
     *             Coordination.PARTIALLY_ENDED.
     */
    void ended(Coordination c) throws Exception;
}
