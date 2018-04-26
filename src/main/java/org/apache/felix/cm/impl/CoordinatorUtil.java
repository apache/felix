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
package org.apache.felix.cm.impl;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;

/**
 * Utility class for coordinations
 */
public class CoordinatorUtil
{

    public static final class Notifier implements Participant
    {
        private final List<Runnable> runnables = new ArrayList<Runnable>();

        private final UpdateThread thread;

        public Notifier(final UpdateThread t)
        {
            this.thread = t;
        }

        private void execute()
        {
            for(final Runnable r : runnables)
            {
                this.thread.schedule(r);
            }
            runnables.clear();
        }

        @Override
        public void ended(Coordination coordination) throws Exception
        {
            execute();
        }

        @Override
        public void failed(Coordination coordination) throws Exception
        {
            execute();
        }

        public void add(final Runnable t)
        {
            runnables.add(t);
        }
    }

    public static boolean addToCoordination(final Object srv, final UpdateThread thread, final Runnable task)
    {
        final Coordinator coordinator = (Coordinator) srv;
        Coordination c = coordinator.peek();
        if ( c != null )
        {
            Notifier n = null;
            for(final Participant p : c.getParticipants())
            {
                if ( p instanceof Notifier )
                {
                    n = (Notifier) p;
                    break;
                }
            }
            if ( n == null )
            {
                n = new Notifier(thread);
                c.addParticipant(n);
            }
            n.add(task);
            return true;
        }
        return false;
    }
}
