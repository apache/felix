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
package org.apache.felix.useradmin.impl.role;

import java.util.Dictionary;
import java.util.Properties;

/**
 * Provides an stricter variant of the {@link ObservableDictionary} that only 
 * permits string keys and values of either String or byte[]. 
 * <p>
 * This class is <b>not</b> guaranteed to be thread-safe!
 * </p>
 */
final class ObservableProperties extends ObservableDictionary {

    private static final long serialVersionUID = -2513082903921734796L;

    /**
     * Creates a new, empty, {@link ObservableProperties} instance.
     */
    public ObservableProperties(String getAction, String changeAction) {
        this(getAction, changeAction, new Properties());
    }

    /**
     * Creates a new {@link ObservableProperties} instance with the given dictionary as defaults.
     * 
     * @param dictionary the defaults to set for this properties, cannot be <code>null</code>.
     */
    public ObservableProperties(String getAction, String changeAction, Dictionary dictionary) {
        super(getAction, changeAction, dictionary);
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object key) {
        if (!(key instanceof String)) {
            throw new IllegalArgumentException("Key must be of type String!");
        }

        return super.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object put(Object key, Object value) {
        if (!(key instanceof String)) {
            throw new IllegalArgumentException("Key must be of type String!");
        }
        if (!(value instanceof String) && !(value instanceof byte[])) {
            throw new IllegalArgumentException("Value must be of type String or byte[]!");
        }

        return super.put(key, value);
    }
    
    /**
     * {@inheritDoc}
     */
    public Object remove(Object key) {
        if (!(key instanceof String)) {
            throw new IllegalArgumentException("Key must be of type String!");
        }

        return super.remove(key);
    }
}
