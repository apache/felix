/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.utils.resource;

import org.apache.felix.utils.collections.StringArrayMap;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

import java.util.Map;
import java.util.Objects;

abstract class AbstractCapabilityRequirement {

    /** The resource. Required. */
    protected final Resource resource;

    /** The namespace. Required. */
    protected final String namespace;

    /** Optional attributes. Never null. */
    protected final Map<String, String> directives;

    /** Optional attributes. Never null. */
    protected final Map<String, Object> attributes;

    AbstractCapabilityRequirement(final Resource res, final String ns, final Map<String, String> dirs, final Map<String, Object> attrs) {
        resource = Objects.requireNonNull(res, "Resource must not be null.");
        namespace = Objects.requireNonNull(ns, "Namespace must not be null.");
        directives = StringArrayMap.reduceMemory(dirs);
        attributes = StringArrayMap.reduceMemory(attrs);
    }

    /**
     * Return the namespace.
     * @return The namespace. This is never @{code null}.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Return the attributes.
     * @return The attributes, might be empty.
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Return the directives.
     * @return The directives, might be empty.
     */
    public Map<String, String> getDirectives() {
        return directives;
    }

    /**
     * Return the resource.
     * @return The resource or @{code null}.
     */
    public Resource getResource() {
        return resource;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractCapabilityRequirement that = (AbstractCapabilityRequirement) o;
        return Objects.equals(resource, that.resource) &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(attributes, that.attributes) &&
                Objects.equals(directives, that.directives);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, namespace, attributes, directives);
    }

    @Override
    public String toString() {
        return toString(getResource(), getNamespace(), getAttributes(), getDirectives());
    }

    public static String toString(Resource res, String namespace, Map<String, Object> attrs, Map<String, String> dirs) {
        StringBuilder sb = new StringBuilder();
        if (res != null) {
            sb.append("[").append(res).append("] ");
        }
        sb.append(namespace);
        for (String key : attrs.keySet()) {
            sb.append("; ");
            append(sb, key, attrs.get(key), true);
        }
        for (String key : dirs.keySet()) {
            sb.append("; ");
            append(sb, key, dirs.get(key), false);
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String key, Object val, boolean attribute) {
        sb.append(key);
        if (val instanceof Version) {
            sb.append(":Version=");
            sb.append(val);
        } else if (val instanceof Long) {
            sb.append(":Long=");
            sb.append(val);
        } else if (val instanceof Double) {
            sb.append(":Double=");
            sb.append(val);
        } else if (val instanceof Iterable) {
            Iterable<?> it = (Iterable<?>) val;
            String scalar = null;
            for (Object o : it) {
                String ts;
                if (o instanceof String) {
                    ts = "String";
                } else if (o instanceof Long) {
                    ts = "Long";
                } else if (o instanceof Double) {
                    ts = "Double";
                } else if (o instanceof Version) {
                    ts = "Version";
                } else {
                    throw new IllegalArgumentException("Unsupported scalar type: " + o);
                }
                if (scalar == null) {
                    scalar = ts;
                } else if (!scalar.equals(ts)) {
                    throw new IllegalArgumentException("Unconsistent list type for attribute " + key);
                }
            }
            sb.append(":List<").append(scalar).append(">=");
            sb.append("\"");
            boolean first = true;
            for (Object o : it) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(o.toString().replace("\"", "\\\"").replace(",", "\\,"));
            }
            sb.append("\"");
        } else {
            sb.append(attribute ? "=" : ":=");
            String s = val.toString();
            if (s.matches("[0-9a-zA-Z_\\-.]*")) {
                sb.append(s);
            } else {
                sb.append("\"").append(s.replace("\"", "\\\\")).append("\"");
            }
        }
    }
}
