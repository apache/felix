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

package org.apache.felix.ipojo.extender;

/**
 * A declaration is a creation instruction of an entity (Component type, Factory, Instance...).
 * All declarations are exposed as services. <em>Processors</em> are tracking the adequate declaration type and create
 * the entity.
 * <p/>
 * Declaration can be <em>bound</em> or <em>unbound</em> whether they are fulfilled. When they are unbound,
 * a message or/and an error can be set.
 */
public interface Declaration {
    /**
     * Gets the declaration status.
     *
     * @return the current status. As Status are immutable, it returns a new object every time.
     */
    Status getStatus();

    /**
     * Marks the declaration bound.
     */
    void bind();

    /**
     * Unbinds the declaration.
     *
     * @param message an explanation
     */
    void unbind(String message);

    /**
     * Unbinds the declaration
     *
     * @param message   an explanation
     * @param throwable an error
     */
    void unbind(String message, Throwable throwable);
}
