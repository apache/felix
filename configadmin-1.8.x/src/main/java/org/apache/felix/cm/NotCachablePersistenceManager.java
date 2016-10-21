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
package org.apache.felix.cm;


/**
 * <code>NotCachablePersistenceManager</code> is a marker interface which
 * extends {@link PersistenceManager} to inform that no cache should be applied
 * around this persistence manager. This gives the opportunity for the
 * persistence manager to implement it's own caching heuristics.
 * <p>
 * To make implementations of this interface available to the Configuration
 * Admin Service they must be registered as service for interface
 * {@link PersistenceManager}.
 * <p>
 * See also {@link PersistenceManager}
 *
 * @since 1.1
 */
public interface NotCachablePersistenceManager extends PersistenceManager
{
}
