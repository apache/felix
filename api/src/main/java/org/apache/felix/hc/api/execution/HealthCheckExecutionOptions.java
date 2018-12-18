/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.api.execution;

/** Options for behavior of health check execution. */
public class HealthCheckExecutionOptions {

    private boolean forceInstantExecution = false;
    private boolean combineTagsWithOr = false;
    private int overrideGlobalTimeout = 0;

    @Override
    public String toString() {
        return "[HealthCheckExecutionOptions forceInstantExecution=" + forceInstantExecution + ", combineTagsWithOr=" + combineTagsWithOr
                + ", overrideGlobalTimeout=" + overrideGlobalTimeout + "]";
    }

    /** If activated, this will ensure that asynchronous checks will be executed immediately.
     * 
     * @param forceInstantExecution boolean flag */
    public void setForceInstantExecution(boolean forceInstantExecution) {
        this.forceInstantExecution = forceInstantExecution;
    }

    /** If activated, the given tags will be combined with a logical "or" instead of "and".
     * 
     * @param combineTagsWithOr boolean flag */
    public void setCombineTagsWithOr(boolean combineTagsWithOr) {
        this.combineTagsWithOr = combineTagsWithOr;
    }

    /** Allows to override the global timeout for this particular execution of the health check.
     * 
     * @param overrideGlobalTimeout timeout in ms to be used for this execution of the execution */
    public void setOverrideGlobalTimeout(int overrideGlobalTimeout) {
        this.overrideGlobalTimeout = overrideGlobalTimeout;
    }

    /** @return true if instant execution is turned on */
    public boolean isForceInstantExecution() {
        return forceInstantExecution;
    }

    /** @return true if combining tags with or is turned on */
    public boolean isCombineTagsWithOr() {
        return combineTagsWithOr;
    }

    /** @return the timeout to be used for this execution (overriding the global timeout) */
    public int getOverrideGlobalTimeout() {
        return overrideGlobalTimeout;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (combineTagsWithOr ? 1231 : 1237);
        result = prime * result + (forceInstantExecution ? 1231 : 1237);
        result = prime * result + overrideGlobalTimeout;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HealthCheckExecutionOptions other = (HealthCheckExecutionOptions) obj;
        if (combineTagsWithOr != other.combineTagsWithOr)
            return false;
        if (forceInstantExecution != other.forceInstantExecution)
            return false;
        if (overrideGlobalTimeout != other.overrideGlobalTimeout)
            return false;
        return true;
    }

}