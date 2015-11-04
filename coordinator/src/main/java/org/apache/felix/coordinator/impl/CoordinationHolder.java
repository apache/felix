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

import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

/**
 * This is a simple wrapper around a {@link CoordinationImpl} to handle
 * orphaned coordinations.
 * While clients of the coordinator service get a holder object,
 * all internal classes hold the real coordination impl instead.
 * Therefore if no clients holds a reference to a holder anymore
 * we can assume that the coordination is orphaned and remove it.
 */
public class CoordinationHolder implements Coordination {

    private CoordinationImpl coordination;

	public void setCoordination(final CoordinationImpl coordination) 
	{
	    this.coordination = coordination;
	}

    /**
     * @see org.osgi.service.coordinator.Coordination#addParticipant(org.osgi.service.coordinator.Participant)
     */
    public void addParticipant(final Participant participant) {
        coordination.addParticipant(participant);
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#end()
     */
    public void end() {
        coordination.end();
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#extendTimeout(long)
     */
    public long extendTimeout(final long timeMillis) {
        return coordination.extendTimeout(timeMillis);
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#fail(java.lang.Throwable)
     */
    public boolean fail(final Throwable cause) {
        return coordination.fail(cause);
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#getBundle()
     */
    public Bundle getBundle() {
        return coordination.getBundle();
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#getEnclosingCoordination()
     */
    public Coordination getEnclosingCoordination() {
        return coordination.getEnclosingCoordination();
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#getFailure()
     */
    public Throwable getFailure() {
        return coordination.getFailure();
    }

	/**
	 * @see org.osgi.service.coordinator.Coordination#getId()
	 */
	public long getId() {
		return coordination.getId();
	}

    /**
	 * @see org.osgi.service.coordinator.Coordination#getName()
	 */
	public String getName() {
		return coordination.getName();
	}

    /**
     * @see org.osgi.service.coordinator.Coordination#getParticipants()
     */
    public List<Participant> getParticipants() {
        return coordination.getParticipants();
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#getThread()
     */
    public Thread getThread() {
        return coordination.getThread();
    }

    /**
     * @see org.osgi.service.coordinator.Coordination#getVariables()
     */
    public Map<Class<?>, Object> getVariables() {
        return coordination.getVariables();
    }

    /**
	 * @see org.osgi.service.coordinator.Coordination#isTerminated()
	 */
	public boolean isTerminated() {
		return coordination.isTerminated();
	}

    /**
	 * @see org.osgi.service.coordinator.Coordination#join(long)
	 */
	public void join(final long timeMillis) throws InterruptedException {
		coordination.join(timeMillis);
	}

    /**
	 * @see org.osgi.service.coordinator.Coordination#push()
	 */
	public Coordination push() {
	     coordination.push();
	     return this;
	}

	@Override
    public boolean equals(final Object object) {
		if ( object instanceof CoordinationImpl )
		{
		    return coordination.equals(object);
		}
		if (!(object instanceof CoordinationHolder))
		{
			return false;
		}
		return coordination.equals(((CoordinationHolder)object).coordination);
	}

	@Override
	public int hashCode() {
		return coordination.hashCode();
	}

	@Override
	public String toString() {
		return coordination.toString();
	}

    @Override
    protected void finalize() throws Throwable {
        if ( !this.coordination.isTerminated() )
        {
            this.coordination.fail(Coordination.ORPHANED);
        }
        super.finalize();
    }

    public Coordination getCoordination() {
        return this.coordination;
    }
}
