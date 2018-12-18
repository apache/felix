/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.hc.api.execution;

import java.util.Arrays;

import org.osgi.annotation.versioning.ProviderType;

/** Parameter class to pass a set of tags and names to the filter. */
@ProviderType
public final class HealthCheckSelector {

    private String[] tags;
    private String[] names;

    public String[] tags() {
        return tags;
    }

    public String[] names() {
        return names;
    }

    private HealthCheckSelector() {
    }

    /** Copy the specified names into the current tags array.
     * 
     * @param tags the new tags. Specify null to clear the current tag array
     * @return this */
    public HealthCheckSelector withTags(String... tags) {
        if (this.tags == null) {
            this.tags = tags;
        } else if (tags != null) {
            String[] copy = Arrays.copyOf(this.tags, this.tags.length + tags.length);
            System.arraycopy(tags, 0, copy, this.tags.length, tags.length);
            this.tags = copy;
        } else {
            this.tags = null;
        }
        return this;
    }

    /** Copy the specified names into the current names array.
     * 
     * @param names the new names. Specify null to clear the current name array
     * @return this */
    public HealthCheckSelector withNames(String... names) {
        if (this.names == null) {
            this.names = names;
        } else if (names != null) {
            String[] copy = Arrays.copyOf(this.names, this.names.length + names.length);
            System.arraycopy(names, 0, copy, this.names.length, names.length);
            this.names = copy;
        } else {
            this.names = null;
        }
        return this;
    }

    public static HealthCheckSelector empty() {
        return new HealthCheckSelector();
    }

    public static HealthCheckSelector tags(String... tags) {
        HealthCheckSelector selector = new HealthCheckSelector();
        selector.tags = tags;
        return selector;
    }

    public static HealthCheckSelector names(String... names) {
        HealthCheckSelector selector = new HealthCheckSelector();
        selector.names = names;
        return selector;
    }

    @Override
    public String toString() {
        return "HealthCheckSelector{" +
                "tags=" + (tags == null ? "*" : Arrays.toString(tags)) +
                ", names=" + (names == null ? "*" : Arrays.toString(names)) +
                '}';
    }
}
