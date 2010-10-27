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
package org.apache.felix.coordination.impl;

import org.apache.felix.service.coordination.Coordination;
import org.apache.felix.service.coordination.CoordinationException;
import org.apache.felix.service.coordination.Participant;

import junit.framework.TestCase;

@SuppressWarnings("deprecation")
public class CoordinatorImplTest extends TestCase
{

    private CoordinationMgr mgr;
    private CoordinatorImpl coordinator;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        mgr = new CoordinationMgr();
        coordinator = new CoordinatorImpl(null, mgr);
    }

    public void test_createCoordination()
    {
        final String name = "test";
        final Coordination c1 = coordinator.create(name);
        assertNotNull(c1);
        assertEquals(name, c1.getName());
        assertNull(coordinator.getCurrentCoordination());
        assertFalse(c1.isFailed());
        assertFalse(c1.isTerminated());
        assertTrue(c1.getParticipants().isEmpty());

        assertTrue(c1.fail(new Exception()));
        assertTrue(c1.isFailed());
        assertTrue(c1.isTerminated());
        assertNull(coordinator.getCurrentCoordination());

        assertFalse(c1.fail(new Exception()));
        try
        {
            c1.end();
            fail("Expected IllegalStateException on end() after fail()");
        }
        catch (IllegalStateException ise)
        {
            // expected
        }

        final Coordination c2 = coordinator.create(name);
        assertNotNull(c2);
        assertEquals(name, c2.getName());
        assertNull(coordinator.getCurrentCoordination());
        assertFalse(c2.isFailed());
        assertFalse(c2.isTerminated());
        assertTrue(c2.getParticipants().isEmpty());

        assertEquals(Coordination.OK, c2.end());
        assertFalse(c2.isFailed());
        assertTrue(c2.isTerminated());
        assertNull(coordinator.getCurrentCoordination());

        assertFalse(c2.fail(new Exception()));
        try
        {
            c2.end();
            fail("Expected IllegalStateException on second end()");
        }
        catch (IllegalStateException ise)
        {
            // expected
        }
    }

    public void test_beginCoordination()
    {
        final String name = "test";
        final Coordination c1 = coordinator.begin(name);
        assertNotNull(c1);
        assertEquals(name, c1.getName());

        assertEquals(c1, coordinator.getCurrentCoordination());
        assertEquals(c1, coordinator.pop());

        assertNull(coordinator.getCurrentCoordination());
        coordinator.push(c1);
        assertEquals(c1, coordinator.getCurrentCoordination());

        c1.end();
        assertNull(coordinator.getCurrentCoordination());

        final Coordination c2 = coordinator.begin(name);
        assertNotNull(c2);
        assertEquals(name, c2.getName());
        assertEquals(c2, coordinator.getCurrentCoordination());
        c2.fail(null);
        assertNull(coordinator.getCurrentCoordination());
    }

    public void test_beginCoordination_stack()
    {
        final String name = "test";

        final Coordination c1 = coordinator.begin(name);
        assertNotNull(c1);
        assertEquals(name, c1.getName());
        assertEquals(c1, coordinator.getCurrentCoordination());

        final Coordination c2 = coordinator.begin(name);
        assertNotNull(c2);
        assertEquals(name, c2.getName());
        assertEquals(c2, coordinator.getCurrentCoordination());

        c2.end();
        assertEquals(c1, coordinator.getCurrentCoordination());

        c1.end();
        assertNull(coordinator.getCurrentCoordination());
    }

    public void test_beginCoordination_stack2()
    {
        final String name = "test";

        final Coordination c1 = coordinator.begin(name);
        assertNotNull(c1);
        assertEquals(name, c1.getName());
        assertEquals(c1, coordinator.getCurrentCoordination());

        final Coordination c2 = coordinator.begin(name);
        assertNotNull(c2);
        assertEquals(name, c2.getName());
        assertEquals(c2, coordinator.getCurrentCoordination());

        c1.end();
        assertEquals(c2, coordinator.getCurrentCoordination());

        c2.end();
        assertNull(coordinator.getCurrentCoordination());
    }

    public void test_participate_with_ended()
    {
        final String name = "test";
        final Coordination c1 = coordinator.create(name);

        final MockParticipant p1 = new MockParticipant();
        c1.participate(p1);
        assertTrue(c1.getParticipants().contains(p1));
        assertEquals(1, c1.getParticipants().size());

        c1.end();
        assertTrue(p1.ended);
        assertFalse(p1.failed);
        assertEquals(c1, p1.c);

        // assert order of call
        final Coordination c2 = coordinator.create(name);
        final MockParticipant p21 = new MockParticipant();
        final MockParticipant p22 = new MockParticipant();
        c2.participate(p21);
        c2.participate(p22);
        assertTrue(c2.getParticipants().contains(p21));
        assertTrue(c2.getParticipants().contains(p22));
        assertEquals(2, c2.getParticipants().size());

        c2.end();
        assertTrue(p21.ended);
        assertEquals(c2, p21.c);
        assertTrue(p22.ended);
        assertEquals(c2, p22.c);
        assertTrue("p21 must be called before p22", p21.time < p22.time);

        // assert order of call with two registrations
        final Coordination c3 = coordinator.create(name);
        final MockParticipant p31 = new MockParticipant();
        final MockParticipant p32 = new MockParticipant();
        c3.participate(p31);
        c3.participate(p32);
        c3.participate(p31); // should be "ignored"
        assertTrue(c3.getParticipants().contains(p31));
        assertTrue(c3.getParticipants().contains(p32));
        assertEquals(2, c3.getParticipants().size());

        c3.end();
        assertTrue(p31.ended);
        assertEquals(c3, p31.c);
        assertTrue(p32.ended);
        assertEquals(c3, p32.c);
        assertTrue("p21 must be called before p22", p31.time < p32.time);
    }

    public void test_participate_with_failed()
    {
        final String name = "test";
        final Coordination c1 = coordinator.create(name);

        final MockParticipant p1 = new MockParticipant();
        c1.participate(p1);
        assertTrue(c1.getParticipants().contains(p1));
        assertEquals(1, c1.getParticipants().size());

        c1.fail(null);
        assertFalse(p1.ended);
        assertTrue(p1.failed);
        assertEquals(c1, p1.c);

        // assert order of call
        final Coordination c2 = coordinator.create(name);
        final MockParticipant p21 = new MockParticipant();
        final MockParticipant p22 = new MockParticipant();
        c2.participate(p21);
        c2.participate(p22);
        assertTrue(c2.getParticipants().contains(p21));
        assertTrue(c2.getParticipants().contains(p22));
        assertEquals(2, c2.getParticipants().size());

        c2.fail(null);
        assertTrue(p21.failed);
        assertEquals(c2, p21.c);
        assertTrue(p22.failed);
        assertEquals(c2, p22.c);
        assertTrue("p21 must be called before p22", p21.time < p22.time);

        // assert order of call with two registrations
        final Coordination c3 = coordinator.create(name);
        final MockParticipant p31 = new MockParticipant();
        final MockParticipant p32 = new MockParticipant();
        c3.participate(p31);
        c3.participate(p32);
        c3.participate(p31); // should be "ignored"
        assertTrue(c3.getParticipants().contains(p31));
        assertTrue(c3.getParticipants().contains(p32));
        assertEquals(2, c3.getParticipants().size());

        c3.fail(null);
        assertTrue(p31.failed);
        assertEquals(c3, p31.c);
        assertTrue(p32.failed);
        assertEquals(c3, p32.c);
        assertTrue("p21 must be called before p22", p31.time < p32.time);
    }

    public void test_Coordination_timeout() throws InterruptedException
    {
        final String name = "test";
        final Coordination c1 = coordinator.create(name);
        final MockParticipant p1 = new MockParticipant();
        c1.participate(p1);
        assertTrue(c1.getParticipants().contains(p1));
        assertEquals(1, c1.getParticipants().size());

        // set a short timeout and wait for it to pass
        c1.addTimeout(100);
        Thread.sleep(150);

        // expect coordination to have terminated
        assertTrue(c1.isTerminated());
        assertTrue(c1.isFailed());

        // expect Participant.failed() being called
        assertTrue(p1.failed);
        assertEquals(c1, p1.c);
    }

    public void test_Coordination_participate_timeout() throws InterruptedException
    {
        final String name1 = "test1";
        final String name2 = "test2";
        final MockParticipant p1 = new MockParticipant();

        // ensure short timeout for participation
        mgr.configure(60000, 200);

        final Coordination c1 = coordinator.create(name1);
        c1.participate(p1);
        assertTrue(c1.getParticipants().contains(p1));
        assertEquals(1, c1.getParticipants().size());

        // preset p1PartFailure to be be sure the participation actually starts
        p1.participateFailure(new Exception("Not Started yet"));

        Thread c2Thread = new Thread()
        {
            public void run()
            {
                final Coordination c2 = coordinator.create(name2);
                try
                {
                    p1.participateFailure(null);
                    c2.participate(p1);
                }
                catch (Throwable t)
                {
                    p1.participateFailure(t);
                }
                finally
                {
                    c2.terminate();
                }
            }
        };
        c2Thread.start();

        // wait at most 2 seconds for the second thread to terminate
        // we expect this if the participation properly times out
        c2Thread.join(2000);
        assertFalse("Thread for second Coordination did not terminate....", c2Thread.isAlive());

        Throwable p1PartFailure = p1.participateFailure;
        if (p1PartFailure == null)
        {
            fail("Expecting CoordinationException/TIMEOUT for second participation");
        }
        else if (p1PartFailure instanceof CoordinationException)
        {
            assertEquals(CoordinationException.TIMEOUT, ((CoordinationException) p1PartFailure).getReason());
        }
        else
        {
            fail("Unexpected Throwable while trying to participate: " + p1PartFailure);
        }

        c1.terminate();

        // make sure c2Thread has terminated
        if (c2Thread.isAlive())
        {
            c2Thread.interrupt();
            c2Thread.join(1000);
            assertFalse("Thread for second Coordination did not terminate....", c2Thread.isAlive());
        }
    }

    static final class MockParticipant implements Participant
    {

        long time;

        Coordination c;

        boolean failed;

        boolean ended;

        Throwable participateFailure;

        public void failed(Coordination c) throws Exception
        {
            this.failed = true;
            this.c = c;
            this.time = System.nanoTime();
        }

        public void ended(Coordination c) throws Exception
        {
            this.ended = true;
            this.c = c;
            this.time = System.nanoTime();
        }

        void participateFailure(Throwable t)
        {
            this.participateFailure = t;
        }
    }
}
