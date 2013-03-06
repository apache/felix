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
 * The declaration status.
 * A declaration may be fulfilled or not (bound or not).
 * When the declaration is unbound, a message can be given to explain the reason.
 * Implementation are immutable.
 */
public interface Status {
    /**
     * Is the declaration fulfilled ?
     *
     * @return {@literal true} if the declaration is bound, {@literal false} otherwise.
     */
    boolean isBound();

    /**
     * Gets the unbound message if any.
     *
     * @return the unbound message, <code>null</code> if no message.
     */
    String getMessage();

    /**
     * Gets the unbound error if any.
     *
     * @return the unbound error, <code>null</code> if no error were set.
     */
    Throwable getThrowable();
}
