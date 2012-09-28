/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin;

/**
 * Represents a runtime exception for {@link RoleRepositoryBackend}s.
 */
public class BackendException extends RuntimeException {

    private static final long serialVersionUID = 5566633157189079557L;

    /**
     * Creates a new {@link BackendException} instance.
     * 
     * @param cause the originating cause of this exception.
     */
    public BackendException(Exception cause) {
        super(cause);
    }

    /**
     * Creates a new {@link BackendException} instance.
     * 
     * @param message the message of this exception.
     */
    public BackendException(String message) {
        super(message);
    }

    /**
     * Creates a new {@link BackendException} instance.
     * 
     * @param message the message of this exception;
     * @param cause the originating cause of this exception.
     */
    public BackendException(String message, Exception cause) {
        super(message, cause);
    }
}
