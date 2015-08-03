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

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Participant;

import junit.framework.TestCase;

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
        final Coordination c1 = coordinator.create(name, 0);
        assertNotNull(c1);
        assertEquals(name, c1.getName());
        assertNull(coordinator.peek());
        assertNull(c1.getFailure());
        assertFalse(c1.isTerminated());
        assertTrue(c1.getParticipants().isEmpty());

        Exception cause = new Exception();
        assertTrue(c1.fail(cause));
        assertSame(cause, c1.getFailure());
        assertTrue(c1.isTerminated());
        assertNull(coordinator.peek());

        assertFalse(c1.fail(new Exception()));
        try
        {
            c1.end();
            fail("Expected CoordinationException.FAILED on end() after fail()");
        }
        catch (CoordinationException ce)
        {
            // expected failed
            assertEquals(CoordinationException.FAILED, ce.getType());
        }

        final Coordination c2 = coordinator.create(name, 0);
        assertNotNull(c2);
        assertEquals(name, c2.getName());
        assertNull(coordinator.peek());
        assertNull(c2.getFailure());
        assertFalse(c2.isTerminated());
        assertTrue(c2.getParticipants().isEmpty());

        c2.end();
        assertNull(c2.getFailure());
        assertTrue(c2.isTerminated());
        assertNull(coordinator.peek());

        assertFalse(c2.fail(new Exception()));
        try
        {
            c2.end();
            fail("Expected CoordinationException.ALREADY_ENDED on second end()");
        }
        catch (CoordinationException ce)
        {
            // expected already terminated
            assertEquals(CoordinationException.ALREADY_ENDED, ce.getType());
        }
    }

    public void test_beginCoordination()
    {
        final String name = "test";
        final Coordination c1 = coordinator.begin(name, 0);
        assertNotNull(c1);
        assertEquals(name, c1.getName());

        assertEquals(c1, coordinator.peek());
        assertEquals(c1, coordinator.pop());

        assertNull(coordinator.peek());
        c1.push();
        assertEquals(c1, coordinator.peek());

        c1.end();
        assertNull(coordinator.peek());

        final Coordination c2 = coordinator.begin(name, 0);
        assertNotNull(c2);
        assertEquals(name, c2.getName());
        assertEquals(c2, coordinator.peek());
        c2.fail(new Exception());
        assertNotNull(coordinator.peek());
        try {
            c2.end();
            fail("Exception should be thrown");
        } catch (CoordinationException ce) {
            // ignore
        }
        assertNull(coordinator.peek());
    }

    public void test_beginCoordination_stack()
    {
        final String name = "test";

        final Coordination c1 = coordinator.begin(name, 0);
        assertNotNull(c1);
        assertEquals(name, c1.getName());
        assertEquals(c1, coordinator.peek());

        final Coordination c2 = coordinator.begin(name, 0);
        assertNotNull(c2);
        assertEquals(name, c2.getName());
        assertEquals(c2, coordinator.peek());

        c2.end();
        assertEquals(c1, coordinator.peek());

        c1.end();
        assertNull(coordinator.peek());
    }

    public void test_beginCoordination_stack2()
    {
        final String name = "test";

        final Coordination c1 = coordinator.begin(name, 0);
        assertNotNull(c1);
        assertEquals(name, c1.getName());
        assertEquals(c1, coordinator.peek());

        final Coordination c2 = coordinator.begin(name, 0);
        assertNotNull(c2);
        assertEquals(name, c2.getName());
        assertEquals(c2, coordinator.peek());

        c1.end();
        assertNull(coordinator.peek());

        try
        {
            c2.end();
            fail("c2 is already terminated");
        }
        catch (CoordinationException ce)
        {
            assertEquals(CoordinationException.ALREADY_ENDED, ce.getType());
        }
        assertNull(coordinator.peek());
    }

    public void test_addParticipant_with_ended()
    {
        final String name = "test";
        final Coordination c1 = coordinator.create(name, 0);

        final MockParticipant p1 = new MockParticipant();
        c1.addParticipant(p1);
        assertTrue(c1.getParticipants().contains(p1));
        assertEquals(1, c1.getParticipants().size());

        c1.end();
        assertTrue(p1.ended);
        assertFalse(p1.failed);
        assertEquals(c1, p1.c);

        // assert order of call
        final Coordination c2 = coordinator.create(name, 0);
        final MockParticipant p21 = new MockParticipant();
        final MockParticipant p22 = new MockParticipant();
        c2.addParticipant(p21);
        c2.addParticipant(p22);
        assertTrue(c2.getParticipants().contains(p21));
        assertTrue(c2.getParticipants().contains(p22));
        assertEquals(2, c2.getParticipants().size());

        c2.end();
        assertTrue(p21.ended);
        assertEquals(c2, p21.c);
        assertTrue(p22.ended);
        assertEquals(c2, p22.c);
        assertTrue("p22 must be called before p21", p22.time < p21.time);

        // assert order of call with two registrations
        final Coordination c3 = coordinator.create(name, 0);
        final MockParticipant p31 = new MockParticipant();
        final MockParticipant p32 = new MockParticipant();
        c3.addParticipant(p31);
        c3.addParticipant(p32);
        c3.addParticipant(p31); // should be "ignored"
        assertTrue(c3.getParticipants().contains(p31));
        assertTrue(c3.getParticipants().contains(p32));
        assertEquals(2, c3.getParticipants().size());

        c3.end();
        assertTrue(p31.ended);
        assertEquals(c3, p31.c);
        assertTrue(p32.ended);
        assertEquals(c3, p32.c);
        assertTrue("p32 must be called before p31", p32.time < p31.time);
    }

    public void test_addParticipant_with_failed()
    {
        final String name = "test";
        final Coordination c1 = coordinator.create(name, 0);

        final MockParticipant p1 = new MockParticipant();
        c1.addParticipant(p1);
        assertTrue(c1.getParticipants().contains(p1));
        assertEquals(1, c1.getParticipants().size());

        c1.fail(new Exception());
        assertFalse(p1.ended);
        assertTrue(p1.failed);
        assertEquals(c1, p1.c);

        // assert order of call
        final Coordination c2 = coordinator.create(name, 0);
        final MockParticipant p21 = new MockParticipant();
        final MockParticipant p22 = new MockParticipant();
        c2.addParticipant(p21);
        c2.addParticipant(p22);
        assertTrue(c2.getParticipants().contains(p21));
        assertTrue(c2.getParticipants().contains(p22));
        assertEquals(2, c2.getParticipants().size());

        c2.fail(new Exception());
        assertTrue(p21.failed);
        assertEquals(c2, p21.c);
        assertTrue(p22.failed);
        assertEquals(c2, p22.c);
        assertTrue("p22 must be called before p21", p22.time < p21.time);

        // assert order of call with two registrations
        final Coordination c3 = coordinator.create(name, 0);
        final MockParticipant p31 = new MockParticipant();
        final MockParticipant p32 = new MockParticipant();
        c3.addParticipant(p31);
        c3.addParticipant(p32);
        c3.addParticipant(p31); // should be "ignored"
        assertTrue(c3.getParticipants().contains(p31));
        assertTrue(c3.getParticipants().contains(p32));
        assertEquals(2, c3.getParticipants().size());

        c3.fail(new Exception());
        assertTrue(p31.failed);
        assertEquals(c3, p31.c);
        assertTrue(p32.failed);
        assertEquals(c3, p32.c);
        assertTrue("p31 must be called before p32", p32.time < p31.time);
    }

    public void test_Coordination_timeout() throws InterruptedException
    {
        final String name = "test";
        final Coordination c1 = coordinator.create(name, 200);
        final MockParticipant p1 = new MockParticipant();
        c1.addParticipant(p1);
        assertTrue(c1.getParticipants().contains(p1));
        assertEquals(1, c1.getParticipants().size());

        // wait for the coordination to time out
        Thread.sleep(250);

        // expect coordination to have terminated
        assertTrue(c1.isTerminated());
        assertSame(Coordination.TIMEOUT, c1.getFailure());

        // expect Participant.failed() being called
        assertTrue(p1.failed);
        assertEquals(c1, p1.c);
    }

    public void test_Coordination_addParticipant_timeout() throws InterruptedException
    {
        final String name1 = "test1";
        final String name2 = "test2";
        final MockParticipant p1 = new MockParticipant();

        // ensure short timeout for participation
        mgr.configure(200);

        final Coordination c1 = coordinator.create(name1, 0);
        c1.addParticipant(p1);
        assertTrue(c1.getParticipants().contains(p1));
        assertEquals(1, c1.getParticipants().size());

        // preset p1PartFailure to be be sure the participation actually starts
        p1.addParticipantFailure(new Exception("Not Started yet"));

        Thread c2Thread = new Thread()
        {
            @Override
            public void run()
            {
                final Coordination c2 = coordinator.create(name2, 0);
                try
                {
                    p1.addParticipantFailure(null);
                    c2.addParticipant(p1);
                }
                catch (Throwable t)
                {
                    p1.addParticipantFailure(t);
                }
                finally
                {
                    c2.end();
                }
            }
        };
        c2Thread.start();

        // wait at most 2 seconds for the second thread to terminate
        // we expect this if the participation properly times out
        c2Thread.join(2000);
        assertFalse("Thread for second Coordination did not terminate....", c2Thread.isAlive());

        Throwable p1PartFailure = p1.addParticipantFailure;
        if (p1PartFailure == null)
        {
            fail("Expecting CoordinationException/FAILED for second participation");
        }
        else if (p1PartFailure instanceof CoordinationException)
        {
            assertEquals(CoordinationException.FAILED, ((CoordinationException) p1PartFailure).getType());
        }
        else
        {
            fail("Unexpected Throwable while trying to addParticipant: " + p1PartFailure);
        }

        c1.end();

        // make sure c2Thread has terminated
        if (c2Thread.isAlive())
        {
            c2Thread.interrupt();
            c2Thread.join(1000);
            assertFalse("Thread for second Coordination did still not terminate....", c2Thread.isAlive());
        }
    }

    static final class MockParticipant implements Participant
    {

        long time;

        Coordination c;

        boolean failed;

        boolean ended;

        Throwable addParticipantFailure;

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

        void addParticipantFailure(Throwable t)
        {
            this.addParticipantFailure = t;
        }
    }
}
