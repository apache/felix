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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import org.apache.felix.jmx.service.coordination.CoordinatorMBean;
import org.apache.felix.service.coordination.Coordination;
import org.apache.felix.service.coordination.CoordinationException;
import org.apache.felix.service.coordination.Participant;

@SuppressWarnings("deprecation")
public class CoordinationMgr implements CoordinatorMBean {

    private ThreadLocal<Stack<Coordination>> threadStacks;

    private final AtomicLong ctr;

    private final Map<Long, CoordinationImpl> coordinations;

    CoordinationMgr() {
        ctr = new AtomicLong(-1);
        coordinations = new HashMap<Long, CoordinationImpl>();
    }

    void unregister(final CoordinationImpl c) {
        coordinations.remove(c.getId());
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null) {
            stack.remove(c);
        }
    }

    void cleanUp() {
        final Exception reason = new Exception();
        for (Coordination c : coordinations.values()) {
            c.fail(reason);
        }
        coordinations.clear();
    }

    public Coordination create(String name) {
        long id = ctr.incrementAndGet();
        CoordinationImpl c = new CoordinationImpl(id, name);
        coordinations.put(id, c);
        return c;
    }

    public Coordination begin(String name) {
        return push(create(name));
    }

    public Coordination push(Coordination c) {
        Stack<Coordination> stack = threadStacks.get();
        if (stack == null) {
            stack = new Stack<Coordination>();
            threadStacks.set(stack);
        }
        return stack.push(c);
    }

    public Coordination pop() {
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null && !stack.isEmpty()) {
            return stack.pop();
        }
        return null;
    }

    public Coordination getCurrentCoordination() {
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null && !stack.isEmpty()) {
            return stack.peek();
        }
        return null;
    }

    public boolean alwaysFail(Throwable reason) {
        CoordinationImpl current = (CoordinationImpl) getCurrentCoordination();
        if (current != null) {
            current.mustFail();
            return true;
        }
        return false;
    }

    public Collection<Coordination> getCoordinations() {
        ArrayList<Coordination> result = new ArrayList<Coordination>();
        Stack<Coordination> stack = threadStacks.get();
        if (stack != null) {
            result.addAll(stack);
        }
        return result;
    }

    public boolean participate(Participant participant)
            throws CoordinationException {
        // TODO: check for multi-pariticipation and block
        Coordination current = getCurrentCoordination();
        if (current != null) {
            current.participate(participant);
            return true;
        }
        return false;
    }

    public Coordination participateOrBegin(Participant ifActive) {
        // TODO: check for multi-pariticipation and block
        Coordination current = getCurrentCoordination();
        if (current == null) {
            current = begin("implicit");
        }
        current.participate(ifActive);
        return current;
    }

    // ---------- CoordinatorMBean interface

    public TabularData listCoordinations(String regexFilter) {
        Pattern p = Pattern.compile(regexFilter);
        TabularData td = new TabularDataSupport(COORDINATIONS_TYPE);
        for (CoordinationImpl c : coordinations.values()) {
            if (p.matcher(c.getName()).matches()) {
                try {
                    td.put(fromCoordination(c));
                } catch (OpenDataException e) {
                    // TODO: log
                }
            }
        }
        return td;
    }

    public CompositeData getCoordination(long id) throws IOException {
        CoordinationImpl c = coordinations.get(id);
        if (c != null) {
            try {
                return fromCoordination(c);
            } catch (OpenDataException e) {
                throw new IOException(e.toString());
            }
        }
        throw new IOException("No such Coordination " + id);
    }

    public boolean fail(long id, String reason) {
        Coordination c = coordinations.get(id);
        if (c != null) {
            return c.fail(new Exception(reason));
        }
        return false;
    }

    public void addTimeout(long id, long timeout) {
        Coordination c = coordinations.get(id);
        if (c != null) {
            c.addTimeout(timeout);
        }
    }

    private CompositeData fromCoordination(final CoordinationImpl c)
            throws OpenDataException {
        return new CompositeDataSupport(COORDINATION_TYPE, new String[] { ID,
            NAME, TIMEOUT }, new Object[] { c.getId(), c.getName(),
            c.getTimeOut() });
    }
}
