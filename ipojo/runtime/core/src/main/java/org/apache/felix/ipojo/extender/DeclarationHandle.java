/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.extender;

/**
 * Handle on the associated {@link org.apache.felix.ipojo.extender.Declaration} service.
 * It can be used to start and/or stop the underlying declaration as well as retrieving its
 * {@linkplain org.apache.felix.ipojo.extender.Status status} (bound or not).
 *
 * @since 1.11.2
 */
public interface DeclarationHandle {

    /**
     * Publish the {@link org.apache.felix.ipojo.extender.Declaration}. If the declaration
     * is already registered, it's a no-op operation.
     */
    void publish();

    /**
     * Retract the {@link org.apache.felix.ipojo.extender.Declaration} service. If the
     * declaration is not registered, it's a no-op operation.
     */
    void retract();

    /**
     * Return the current (instant) status of the declaration. Remember that
     * {@link org.apache.felix.ipojo.extender.Status} is immutable (status does not change over time).
     * If you want an updated status, call again the {@link #getStatus()} method.
     * @return the instant status of the {@link org.apache.felix.ipojo.extender.Declaration}
     */
    Status getStatus();
}
