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

package org.apache.felix.ipojo.extender.internal.queue;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.extender.internal.AbstractService;
import org.apache.felix.ipojo.extender.queue.Callback;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueListener;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.BundleContext;

/**
 * User: guillaume
 * Date: 01/10/13
 * Time: 14:41
 */
public abstract class AbstractQueueService extends AbstractService implements QueueService, QueueNotifier {

    /**
     * Store QueueListeners.
     */
    protected final List<QueueListener> m_listeners = new ArrayList<QueueListener>();

    /**
     * Constructor.
     *
     * @param bundleContext the bundle context
     * @param type          the specification
     */
    protected AbstractQueueService(final BundleContext bundleContext, final Class<?> type) {
        super(bundleContext, type);
    }

    public void addQueueListener(final QueueListener listener) {
        m_listeners.add(listener);
    }

    public void removeQueueListener(final QueueListener listener) {
        m_listeners.remove(listener);
    }

    public void fireEnlistedJobInfo(JobInfo info) {
        for (QueueListener listener : m_listeners) {
            listener.enlisted(info);
        }
    }

    public void fireStartedJobInfo(JobInfo info) {
        for (QueueListener listener : m_listeners) {
            listener.started(info);
        }
    }

    public void fireExecutedJobInfo(JobInfo info, Object result) {
        for (QueueListener listener : m_listeners) {
            listener.executed(info, result);
        }
    }

    public void fireFailedJobInfo(JobInfo info, Throwable throwable) {
        for (QueueListener listener : m_listeners) {
            listener.failed(info, throwable);
        }
    }

}
