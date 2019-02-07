/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.felix.scr.impl.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.ServiceReference;

/**
 * @version $Rev$ $Date$
 */
public class MultiplePrototypeRefPair<S, T> extends AbstractPrototypeRefPair<S, T>
{
    private final ConcurrentMap<ComponentContextImpl<S>, T> instances = new ConcurrentHashMap<>();

    public MultiplePrototypeRefPair( ServiceReference<T> ref )
    {
        super(ref);
    }

    @Override
    public String toString()
    {
        return "[MultiplePrototypeRefPair: ref: [" + getRef() + "] has service: [" + !instances.isEmpty() + "]]";
    }

    @Override
    public T getServiceObject(ComponentContextImpl<S> key) {
        return instances.get(key);
    }

    @Override
    public boolean setServiceObject(ComponentContextImpl<S> key, T serviceObject) {
        return instances.putIfAbsent(key, serviceObject) == null;
    }

    @Override
    protected T remove(ComponentContextImpl<S> key) {
        return instances.remove(key);
    }

    @Override
    protected Collection<Entry<ComponentContextImpl<S>, T>> clearEntries() {
        Collection<Entry<ComponentContextImpl<S>, T>> result = new ArrayList<>(instances.entrySet());
        instances.clear();
        return result;
    }
}
