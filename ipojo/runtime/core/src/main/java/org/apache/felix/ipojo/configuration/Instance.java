/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.configuration;

import org.apache.felix.ipojo.Factory;

import java.util.*;

/**
 * Instance Builder
 */
public class Instance {

    private String factory;
    private String name;
    private List<Property> configuration;

    public static Instance instance() {
        return new Instance();
    }

    public static <T> FluentList<T> list(T... items) {
        return new FluentList<T>(items);
    }

    public static <K, T> FluentMap<K, T> map(Pair<K, T>... pairs) {
        return new FluentMap<K, T>(pairs);
    }

    public static <K, T> Pair<K, T> pair(K k, T v) {
        return new Pair<K, T>(k, v);
    }

    public static <K, T> Pair<K, T> entry(K k, T v) {
        return new Pair<K, T>(k, v);
    }

    public String factory() {
        return factory;
    }

    public String name() {
        return name;
    }

    public Dictionary<String, Object> configuration() {
        Hashtable<String, Object> configuration = new Hashtable<String, Object>();
        if (this.configuration != null) {
            for (Property property : this.configuration) {
                configuration.put(property.name, property.value);
            }
        }

        if (name != null) {
            configuration.put(Factory.INSTANCE_NAME_PROPERTY, name);
        }

        return configuration;
    }

    public Instance of(String factory) {
        this.factory = factory;
        return this;
    }

    public Instance of(Class clazz) {
        this.factory = clazz.getName();
        return this;
    }

    public Instance named(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("The instance name cannot be null or empty");
        }
        this.name = name;
        return this;
    }

    public Instance.Property<Object> with(String property) {
        if (this.configuration == null) {
            this.configuration = new ArrayList<Property>();
        }
        Property<Object> prop = new Property<Object>(property);
        this.configuration.add(prop);
        return prop;
    }

    public Instance nameIfUnnamed(String name) {
        if (this.name == null) {
            named(name);
        }
        return this;
    }

    public static class FluentList<T> extends ArrayList<T> {

        public FluentList() {
            super(new ArrayList<T>());
        }

        public FluentList(T... items) {
            this();
            addAll(Arrays.asList(items));
        }

        public FluentList<T> with(T o) {
            add(o);
            return this;
        }
    }

    public static class FluentMap<K, T> extends LinkedHashMap<K, T> {

        public FluentMap() {
            super(new LinkedHashMap<K, T>());
        }

        public FluentMap(Pair<? extends K, ? extends T>... pairs) {
            this();
            with(pairs);
        }

        public FluentMap<K, T> with(Pair<? extends K, ? extends T>... pairs) {
            for (Pair<? extends K, ? extends T> pair : pairs) {
                this.put(pair.key, pair.value);
            }
            return this;
        }

        public FluentMap<K, T> putAt(K k, T value) {
            this.put(k, value);
            return this;
        }
    }

    public static class Pair<K, V> {
        private final K key;
        private final V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    public class Property<T> {

        private final String name;
        private T value;

        Property(String name) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("The property name cannot be null or empty");
            }
            this.name = name;
        }

        public Instance setto(T value) {
            if ("instance.name".endsWith(name)) {
                if (value == null || value.toString().length() == 0) {
                    throw new IllegalArgumentException("The instance name cannot be null or empty");
                }
            }
            this.value = value;
            return Instance.this;
        }

        public T get() {
            return value;
        }
    }
}
