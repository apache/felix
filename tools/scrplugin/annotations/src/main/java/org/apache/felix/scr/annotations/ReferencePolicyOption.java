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
package org.apache.felix.scr.annotations;

/**
 * Options for {@link Reference#policyOption()} property.
 */
public enum ReferencePolicyOption {

    /**
     * The reluctant policy option is the default policy option.
     * When a new target service for a reference becomes available,
     * references having the reluctant policy option for the static
     * policy or the dynamic policy with a unary cardinality will
     * ignore the new target service. References having the dynamic
     * policy with a multiple cardinality will bind the new
     * target service
     */
    RELUCTANT,

    /**
     * When a new target service for a reference becomes available,
     * references having the greedy policy option will bind the new
     * target service.
     */
    GREEDY;
}
