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

package org.apache.felix.ipojo.extender.internal.queue.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueListener;
import org.apache.felix.ipojo.extender.queue.debug.QueueEventProxy;

/**
 * User: guillaume
 * Date: 02/10/13
 * Time: 10:51
 */
public class ReplayQueueEventProxy implements QueueEventProxy, QueueListener {
    /**
     * Store QueueListeners.
     */
    private final List<QueueListener> m_listeners = new ArrayList<QueueListener>();

    /**
     * Store QueueEvent to be replayed.
     */
    private List<QueueEvent> m_events = new CopyOnWriteArrayList<QueueEvent>();

    public void addQueueListener(final QueueListener listener) {
        m_listeners.add(listener);
        // replay all the existing events to the new listener
        replay(listener);
    }

    public void removeQueueListener(final QueueListener listener) {
        m_listeners.remove(listener);
    }

    public void enlisted(final JobInfo info) {
        EnlistedQueueEvent event = new EnlistedQueueEvent(info);
        m_events.add(event);
        forward(event);
    }

    public void started(final JobInfo info) {
        StartedQueueEvent event = new StartedQueueEvent(info);
        m_events.add(event);
        forward(event);
    }

    public void executed(final JobInfo info, final Object result) {
        ExecutedQueueEvent event = new ExecutedQueueEvent(info, result);
        m_events.add(event);
        forward(event);
    }

    public void failed(final JobInfo info, final Throwable throwable) {
        FailedQueueEvent event = new FailedQueueEvent(info, throwable);
        m_events.add(event);
        forward(event);
    }

    /**
     * Replay all stored events to the given QueueListener.
     */
    private void replay(final QueueListener listener) {
        for (QueueEvent event : m_events) {
            event.replay(listener);
        }
    }

    /**
     * Forward the given QueueEvent to all the registered listeners.
     */
    private void forward(final QueueEvent event) {
        for (QueueListener listener : m_listeners) {
            event.replay(listener);
        }
    }

    /**
     * Encapsulate event forwarding logic.
     */
    private interface QueueEvent {
        void replay(QueueListener listener);
    }

    private class EnlistedQueueEvent implements QueueEvent {
        private final JobInfo m_info;

        public EnlistedQueueEvent(final JobInfo info) {
            m_info = info;
        }

        public void replay(final QueueListener listener) {
            listener.enlisted(m_info);
        }
    }

    private class StartedQueueEvent implements QueueEvent {
        private final JobInfo m_info;

        public StartedQueueEvent(final JobInfo info) {
            m_info = info;
        }

        public void replay(final QueueListener listener) {
            listener.started(m_info);
        }
    }

    private class ExecutedQueueEvent implements QueueEvent {
        private final JobInfo m_info;
        private final Object m_result;

        public ExecutedQueueEvent(final JobInfo info, final Object result) {
            m_info = info;
            m_result = result;
        }

        public void replay(final QueueListener listener) {
            listener.executed(m_info, m_result);
        }
    }

    private class FailedQueueEvent implements QueueEvent {
        private final JobInfo m_info;
        private final Throwable m_throwable;

        public FailedQueueEvent(final JobInfo info, final Throwable throwable) {
            m_info = info;
            m_throwable = throwable;
        }

        public void replay(final QueueListener listener) {
            listener.failed(m_info, m_throwable);
        }
    }

}
