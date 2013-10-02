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

package org.apache.felix.ipojo.extender.internal.processor;

import static java.lang.String.format;

import org.apache.felix.ipojo.extender.internal.BundleProcessor;
import org.apache.felix.ipojo.extender.internal.DefaultJob;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.Bundle;

/**
 * A bundle processor submitting the activating job to the queue service.
 * The submitted job relies on a delegated bundle processor.
 */
public class QueuingActivationProcessor extends ForwardingBundleProcessor {

    /**
     * Identify the kind of job submitted to the QueueService.
     */
    public static final String BUNDLE_ACTIVATION_JOB_TYPE = "bundle.activation";

    /**
     * The wrapped bundle processor used by the job.
     */
    private final BundleProcessor m_delegate;

    /**
     * The queue service.
     */
    private final QueueService m_queueService;

    /**
     * Creates an instance of the queuing bundle processor
     *
     * @param delegate     the bundle processor used by the submitted job
     * @param queueService the used queue service
     */
    public QueuingActivationProcessor(BundleProcessor delegate, QueueService queueService) {
        m_delegate = delegate;
        m_queueService = queueService;
    }

    @Override
    protected BundleProcessor delegate() {
        return m_delegate;
    }

    /**
     * A bundle is starting.
     * The processing of the bundle is wrapped in a job submitted to the queue service.
     *
     * @param bundle the bundle
     */
    public void activate(final Bundle bundle) {
        m_queueService.submit(
                new DefaultJob<Boolean>(bundle, BUNDLE_ACTIVATION_JOB_TYPE) {
                    public Boolean call() throws Exception {
                        QueuingActivationProcessor.super.activate(bundle);
                        return true;
                    }
                },
                format("Bundle %d being activated", bundle.getBundleId()));
    }

}
