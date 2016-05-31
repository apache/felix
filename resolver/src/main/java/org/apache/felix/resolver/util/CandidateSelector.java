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
package org.apache.felix.resolver.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.resource.Capability;

public class CandidateSelector {
    private final AtomicBoolean isUnmodifiable;
    protected final List<Capability> unmodifiable;
    private int currentIndex = 0;

    public CandidateSelector(List<Capability> candidates, AtomicBoolean isUnmodifiable) {
        this.isUnmodifiable = isUnmodifiable;
        this.unmodifiable = new ArrayList<Capability>(candidates);
    }

    protected CandidateSelector(CandidateSelector candidateSelector) {
        this.isUnmodifiable = candidateSelector.isUnmodifiable;
        this.unmodifiable = candidateSelector.unmodifiable;
        this.currentIndex = candidateSelector.currentIndex;
    }

    public CandidateSelector copy() {
        return new CandidateSelector(this);
    }

    public int getRemainingCandidateCount() {
        return unmodifiable.size() - currentIndex;
    }

    public Capability getCurrentCandidate() {
        return currentIndex < unmodifiable.size() ? unmodifiable.get(currentIndex) : null;
    }

    public List<Capability> getRemainingCandidates() {
        return Collections.unmodifiableList(unmodifiable.subList(currentIndex, unmodifiable.size()));
    }

    public boolean isEmpty() {
        return unmodifiable.size() <= currentIndex;
    }

    public Capability removeCurrentCandidate() {
        Capability current = getCurrentCandidate();
        if (current != null) {
            currentIndex += 1;
        }
        return current;
    }

    public String toString() {
        return getRemainingCandidates().toString();
    }

    public int remove(Capability cap) {
        checkModifiable();
        int index = unmodifiable.indexOf(cap);
        if (index != -1) {
            unmodifiable.remove(index);
        }
        return index;
    }

    protected void checkModifiable() {
        if (isUnmodifiable.get()) {
            throw new IllegalStateException("Trying to mutate after candidates have been prepared.");
        }
    }
}
