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
package org.apache.felix.http.base.internal.whiteboard;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.http.base.internal.runtime.AbstractInfo;
import org.apache.felix.http.base.internal.runtime.dto.FailedDTOHolder;

public class FailureStateHandler {

    private static final class FailureStatus
    {
        public final Map<Integer, Set<Long>> reasonToContextsMapping = new ConcurrentHashMap<Integer, Set<Long>>();
    }

    private final Map<AbstractInfo<?>, FailureStatus> serviceFailures = new ConcurrentHashMap<AbstractInfo<?>, FailureStatus>();

    /**
     * Remove all failures.
     */
    public void clear()
    {
        this.serviceFailures.clear();
    }

    public void add(final AbstractInfo<?> info, final int reason)
    {
        this.add(info, 0, reason);
    }

    public void add(final AbstractInfo<?> info, final long contextId, final int reason)
    {
        FailureStatus status = serviceFailures.get(info);
        if ( status == null )
        {
            // we don't need to sync the add operation, that's taken care of by the caller.
            status = new FailureStatus();
            this.serviceFailures.put(info, status);
        }
        Set<Long> contexts = status.reasonToContextsMapping.get(reason);
        if ( contexts == null )
        {
            contexts = new HashSet<Long>();
        }
        else
        {
            contexts = new HashSet<Long>(contexts);
        }
        contexts.add(contextId);
        status.reasonToContextsMapping.put(reason, contexts);
    }

    public boolean remove(final AbstractInfo<?> info)
    {
        return remove(info, 0);
    }

    public boolean removeAll(final AbstractInfo<?> info)
    {
        final boolean result = remove(info, 0);
        this.serviceFailures.remove(info);
        return result;
    }

    public boolean remove(final AbstractInfo<?> info, final long contextId)
    {
        final FailureStatus status = serviceFailures.get(info);
        if ( status != null )
        {
            final Iterator<Map.Entry<Integer, Set<Long>>> i = status.reasonToContextsMapping.entrySet().iterator();
            while ( i.hasNext() )
            {
                final Map.Entry<Integer, Set<Long>> entry = i.next();
                if ( entry.getValue().contains(contextId) )
                {
                    if ( entry.getValue().size() == 1 )
                    {
                        i.remove();
                    }
                    else
                    {
                        final Set<Long> set = new HashSet<Long>(entry.getValue());
                        set.remove(contextId);
                        entry.setValue(set);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public void getRuntimeInfo(final FailedDTOHolder failedDTOHolder) {
        for(final Map.Entry<AbstractInfo<?>, FailureStatus> entry : this.serviceFailures.entrySet() )
        {
            final Iterator<Map.Entry<Integer, Set<Long>>> i = entry.getValue().reasonToContextsMapping.entrySet().iterator();
            while ( i.hasNext() )
            {
                final Map.Entry<Integer, Set<Long>> status = i.next();

                for(final long contextId : status.getValue())
                {
                    failedDTOHolder.add(entry.getKey(), contextId, status.getKey());
                }
            }
        }

    }
}
