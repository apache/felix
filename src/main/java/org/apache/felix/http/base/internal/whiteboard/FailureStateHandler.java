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

import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SERVLET_CONTEXT_FAILURE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_UNKNOWN;
import static org.osgi.service.http.runtime.dto.DTOConstants.FAILURE_REASON_VALIDATION_FAILED;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.http.base.internal.logger.SystemLogger;
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

    public void addFailure(final AbstractInfo<?> info, final int reason, final Exception ex)
    {
        this.addFailure(info, 0, reason, ex);
    }

    public void addFailure(final AbstractInfo<?> info, final int reason)
    {
        this.addFailure(info, 0, reason);
    }

    public void addFailure(final AbstractInfo<?> info, final long contextId, final int reason)
    {
        this.addFailure(info, contextId, reason, null);
    }

    public void addFailure(final AbstractInfo<?> info, final long contextId, final int reason, final Exception ex)
    {
        final String type = info.getClass().getSimpleName().substring(0, info.getClass().getSimpleName().length() - 4);
        final String serviceInfo;
        if ( info.getServiceReference() == null ) {
            serviceInfo = "with id " + info.getServiceId();
        } else {
            serviceInfo = String.valueOf(info.getServiceId()) +
                    " (bundle " + info.getServiceReference().getBundle().getSymbolicName()
                    + " reference " + info.getServiceReference() + ")";
        }
        if ( reason == FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING )
        {
            SystemLogger.debug("Ignoring unmatching " + type + " service " + serviceInfo);
        }
        else if ( reason == FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE )
        {
            SystemLogger.debug("Ignoring shadowed " + type + " service " + serviceInfo);
        }
        else if ( reason == FAILURE_REASON_SERVICE_NOT_GETTABLE )
        {
            SystemLogger.error("Ignoring ungettable " + type + " service " + serviceInfo, ex);
        }
        else if ( reason == FAILURE_REASON_VALIDATION_FAILED )
        {
            SystemLogger.debug("Ignoring invalid " + type + " service " + serviceInfo);
        }
        else if ( reason == FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING )
        {
            SystemLogger.debug("Ignoring unmatched " + type + " service " + serviceInfo);
        }
        else if ( reason == FAILURE_REASON_SERVLET_CONTEXT_FAILURE )
        {
            SystemLogger.debug("Servlet context " + String.valueOf(contextId) + " failure: Ignoring " + type + " service " + serviceInfo);
        }
        else if ( reason == FAILURE_REASON_UNKNOWN)
        {
            SystemLogger.error("Exception while registering " + type + " service " + serviceInfo, ex);
        }

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
