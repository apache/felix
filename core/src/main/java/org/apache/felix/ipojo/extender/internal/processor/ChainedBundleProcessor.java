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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A bundle processor chaining others processor.
 * It composes the <em>extender</em> mechanism.
 * <p/>
 * Instance must be created using the {@link #create(org.apache.felix.ipojo.extender.internal.BundleProcessor...)}
 * method.
 */
public class ChainedBundleProcessor implements BundleProcessor {

    /**
     * The list of processors.
     * Be aware that the order if important, as processors will be called in the insertion order.
     * <p/>
     * Once the chained bundle processor is created, this list cannot be modified.
     */
    private final List<BundleProcessor> m_processors = new ArrayList<BundleProcessor>();

    private ChainedBundleProcessor() {
        // Just here to avoid direct creation.
    }

    /**
     * Creates a new chained bundle processor.
     *
     * @param processors the set of processor to chain. Cannot be <code>null</code> or empty.
     * @return the created bundle processor
     * @throws IllegalArgumentException if the given processor list is <code>null</code> or empty.
     */
    public static ChainedBundleProcessor create(BundleProcessor... processors) {
        if (processors == null || processors.length == 0) {
            throw new IllegalArgumentException("Chained processor cannot be created without processors");
        }
        ChainedBundleProcessor chain = new ChainedBundleProcessor();
        Collections.addAll(chain.m_processors, processors);
        return chain;
    }

    /**
     * Gets the list of processors.
     * This method returns a copy of the list of processor.
     *
     * @return a copy of the processor list
     */
    public List<BundleProcessor> getProcessors() {
        List<BundleProcessor> list = new ArrayList<BundleProcessor>();
        list.addAll(m_processors);
        return list;
    }

    /**
     * A bundle is starting.
     * Call the {@link BundleProcessor#activate(org.osgi.framework.Bundle)} method on all chained processors.
     *
     * @param bundle the bundle
     */
    public void activate(Bundle bundle) {
        for (BundleProcessor processor : m_processors) {
            processor.activate(bundle);
        }
    }

    /**
     * A bundle is stopping.
     * Call the {@link BundleProcessor#deactivate(org.osgi.framework.Bundle)} method on all chained processors.
     *
     * @param bundle the bundle
     */
    public void deactivate(Bundle bundle) {
        List<BundleProcessor> reverse = new ArrayList<BundleProcessor>(m_processors);
        Collections.reverse(reverse);
        for (BundleProcessor processor : reverse) {
            processor.deactivate(bundle);
        }
    }

    /**
     * The iPOJO bundle is starting.
     * Call the {@link org.apache.felix.ipojo.extender.internal.BundleProcessor#start()}  method on all chained
     * processors.
     */
    public void start() {
        for (BundleProcessor processor : m_processors) {
            processor.start();
        }
    }

    /**
     * The iPOJO bundle is stopping.
     * Call the {@link org.apache.felix.ipojo.extender.internal.BundleProcessor#stop()} method on all chained
     * processors.
     */
    public void stop() {
        List<BundleProcessor> reverse = new ArrayList<BundleProcessor>(m_processors);
        Collections.reverse(reverse);
        for (BundleProcessor processor : reverse) {
            processor.stop();
        }
    }
}
