/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.coordinator.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.TimerTask;

import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Participant;

@SuppressWarnings("deprecation")
public class CoordinatorImpl implements org.osgi.service.coordinator.Coordinator
{

    private final Bundle owner;

    private final CoordinationMgr mgr;

    private final HashSet<Coordination> coordinations;

    CoordinatorImpl(final Bundle owner, final CoordinationMgr mgr)
    {
        this.owner = owner;
        this.mgr = mgr;
        this.coordinations = new HashSet<Coordination>();
    }

    /**
     * Ensure all active Coordinations started by this CoordinatorImpl instance
     * are terminated before the service is ungotten by the bundle.
     * <p>
     * Called by the Coordinator ServiceFactory when this CoordinatorImpl
     * instance is not used any longer by the owner bundle.
     *
     * @see FELIX-2671/OSGi Bug 104
     */
    void dispose()
    {
        final Coordination[] active;
        synchronized (coordinations)
        {
            if (coordinations.isEmpty())
            {
                active = null;
            }
            else
            {
                active = coordinations.toArray(new CoordinationImpl[coordinations.size()]);
                coordinations.clear();
            }
        }

        if (active != null)
        {
            Throwable reason = new Exception("Coordinator service released");
            for (int i = 0; i < active.length; i++)
            {
                active[i].fail(reason);
            }
        }
    }

    public Coordination create(final String name, final long timeout)
    {
        // TODO: check permission
        Coordination c = mgr.create(this, name, timeout);
        synchronized (coordinations)
        {
            coordinations.add(c);
        }
        return c;
    }

    public Collection<Coordination> getCoordinations()
    {
        // TODO: check permission
        return mgr.getCoordinations();
    }

    public boolean fail(Throwable reason)
    {
        // TODO: check permission
        CoordinationImpl current = (CoordinationImpl) peek();
        if (current != null)
        {
            return current.fail(reason);
        }
        return false;
    }

    public Coordination peek()
    {
        // TODO: check permission
        return mgr.peek();
    }

    public Coordination begin(final String name, final long timeoutInMillis)
    {
        // TODO: check permission
        return push(create(name, timeoutInMillis));
    }

    public Coordination pop()
    {
        // TODO: check permission
        return mgr.pop();
    }

    public boolean addParticipant(Participant participant) throws CoordinationException
    {
        // TODO: check permission
        Coordination current = peek();
        if (current != null)
        {
            current.addParticipant(participant);
            return true;
        }
        return false;
    }

    public Coordination getCoordination(long id)
    {
        // TODO: check permission
        return mgr.getCoordinationById(id);
    }

    //----------

    Coordination push(Coordination c)
    {
        // TODO: check permission
        return mgr.push(c);
    }

    void unregister(final CoordinationImpl c)
    {
        mgr.unregister(c);
        synchronized (coordinations)
        {
            coordinations.remove(c);
        }
    }

    void schedule(final TimerTask task, final long deadLine)
    {
        mgr.schedule(task, deadLine);
    }

    void lockParticipant(final Participant p, final CoordinationImpl c)
    {
        mgr.lockParticipant(p, c);
    }

    void releaseParticipant(final Participant p)
    {
        mgr.releaseParticipant(p);
    }
}
