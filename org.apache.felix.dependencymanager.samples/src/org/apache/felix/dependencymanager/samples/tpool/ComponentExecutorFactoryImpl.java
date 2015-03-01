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
package org.apache.felix.dependencymanager.samples.tpool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentExecutorFactory;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentExecutorFactoryImpl implements ComponentExecutorFactory {
    final static int SIZE = Runtime.getRuntime().availableProcessors();
    final static Executor m_threadPool = Executors.newFixedThreadPool(SIZE);

    @Override
    public Executor getExecutorFor(Component component) {
        return m_threadPool;
    }
}
