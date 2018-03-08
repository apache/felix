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
package org.apache.felix.resolver.reason;

import java.util.Collection;

import org.osgi.resource.Requirement;
import org.osgi.service.resolver.ResolutionException;

/**
 * An exception that holds the reason for a resolution failure.
 *
 * @see {@link ResolutionException}
 */
public class ReasonException extends ResolutionException {

    /**
     * The reasons for resolution failure.
     */
    public static enum Reason {
        /**
         * Represents an unresolved package referenced by {@code Dynamic-ImportPackage}.
         * <p>
         * {@link ReasonException#getUnresolvedRequirements()} will return a
         * collection containing the single {@code osgi.wiring.package;resolution:=dynamic}
         * requirement which failed to resolve.
         * <p>
         * This reason has no a transitive cause.
         */
        DynamicImport,

        /**
         * Represents the scenario where a fragment matches a host but is not
         * selected because another fragment with the same {@code Bundle-SymbolicName}
         * already matched, usually a fragment of a higher version.
         * <p>
         * {@link ReasonException#getUnresolvedRequirements()} will return
         * a collection containing the single {@code osgi.wiring.host} requirement of
         * the fragment.
         * <p>
         * This reason has no a transitive cause.
         */
        FragmentNotSelected,

        /**
         * Represents the scenario where a requirement could not be resolved.
         * <p>
         * {@link ReasonException#getUnresolvedRequirements()} will return
         * a collection containing a single requirement that didn't resolve.
         * <p>
         * This reason may have a transitive cause.
         */
        MissingRequirement,

        /**
         * Represents a failure in the <em>use constraints</em> of a bundle.
         * <p>
         * {@link ReasonException#getUnresolvedRequirements()} will return
         * a collection containing a single requirement to blame for the use constraint
         * violation.
         * <p>
         * This reason has no a transitive cause.
         */
        UseConstraint
    }

    private static final long serialVersionUID = -5276675175114379539L;

    public ReasonException(Reason reason, String message, Throwable cause, Collection<Requirement> unresolvedRequirements) {
        super(message, cause, unresolvedRequirements);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    private final Reason reason;

}
