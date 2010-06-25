/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.test.bundle.annotation.composite;

import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

/**
 * The CompositeService is also made up of this Class.
 */
public class Component
{
    // Injected dependency (from CompositeService)
    private Sequencer m_sequencer;

    // Injected dependency (from CompositeService)
    Runnable m_runnable;

    // lifecycle callback (same method as the one from CompositeService)
    void init()
    {
        m_sequencer.step(2);
    }

    // lifecycle callback (same method as the one from CompositeService)
    void start()
    {
        m_sequencer.step(5);
        m_runnable.run(); /* step 6 */
    }

    // lifecycle callback (same method as the one from CompositeService)
    void stop()
    {
        m_sequencer.step(8);
    }

    // lifecycle callback (same method as the one from CompositeService)
    void destroy()
    {
        m_sequencer.step(10);
    }
}
