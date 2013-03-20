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

import org.apache.felix.ipojo.extender.internal.BundleProcessor;
import org.osgi.framework.Bundle;

import java.util.LinkedList;

/**
 * A bundle processor delegating a wrapped bundle processor. On stop, the bundles are processed in reverse.
 */
public class ReverseBundleProcessor extends ForwardingBundleProcessor {

    /**
     * The wrapped bundle processor.
     */
    private final BundleProcessor m_delegate;

    /**
     * A list of bundle to process.
     */
    private LinkedList<Bundle> m_bundles = new LinkedList<Bundle>();

    /**
     * Creates the processor.
     *
     * @param delegate the processor on which call are delegated
     */
    public ReverseBundleProcessor(BundleProcessor delegate) {
        m_delegate = delegate;
    }

    /**
     * @return the wrapped processor.
     */
    @Override
    protected BundleProcessor delegate() {
        return m_delegate;
    }

    /**
     * A bundle is starting.
     * The bundle is added to the list, and the processing delegated to the wrapped processor.
     *
     * @param bundle the bundle
     */
    @Override
    public void activate(Bundle bundle) {
        m_bundles.addLast(bundle);
        super.activate(bundle);
    }

    /**
     * A bundle is stopping.
     * The bundle is removed from the list and the processing delegated to the wrapped processor.
     *
     * @param bundle the bundle
     */
    @Override
    public void deactivate(Bundle bundle) {
        m_bundles.remove(bundle);
        super.deactivate(bundle);
    }

    /**
     * iPOJO is stopping.
     * The bundle that are still in the list are processed in the <strong>reverse</strong> order by the wrapped
     * processor.
     */
    @Override
    public void stop() {
        // deactivate in reverse order
        while (!m_bundles.isEmpty()) {
            super.deactivate(m_bundles.removeLast());
        }
        super.stop();
    }
}
