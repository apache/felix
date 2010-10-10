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

import java.util.Collection;
import org.apache.felix.service.coordination.Coordination;
import org.apache.felix.service.coordination.CoordinationException;
import org.apache.felix.service.coordination.Coordinator;
import org.apache.felix.service.coordination.Participant;
import org.osgi.framework.Bundle;

@SuppressWarnings("deprecation")
public class CoordinatorImpl implements Coordinator {

    private final Bundle owner;

    private final CoordinationMgr mgr;

    CoordinatorImpl(final Bundle owner, final CoordinationMgr mgr) {
        this.owner = owner;
        this.mgr = mgr;
    }

    public Coordination create(String name) {
        // TODO: check permission
        return mgr.create(name);
    }

    void unregister(final CoordinationImpl c) {
        // TODO: check permission
        mgr.unregister(c);
    }

    public Coordination begin(String name) {
        // TODO: check permission
        return mgr.begin(name);
    }

    public Coordination push(Coordination c) {
        // TODO: check permission
        return mgr.push(c);
    }

    public Coordination pop() {
        // TODO: check permission
        return mgr.pop();
    }

    public Coordination getCurrentCoordination() {
        // TODO: check permission
        return mgr.getCurrentCoordination();
    }

    public boolean alwaysFail(Throwable reason) {
        // TODO: check permission
        return mgr.alwaysFail(reason);
    }

    public Collection<Coordination> getCoordinations() {
        // TODO: check permission
        return mgr.getCoordinations();
    }

    public boolean participate(Participant participant)
            throws CoordinationException {
        // TODO: check permission
        return mgr.participate(participant);
    }

    public Coordination participateOrBegin(Participant ifActive) {
        // TODO: check permission
        return mgr.participateOrBegin(ifActive);
    }

}
