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
package org.apache.felix.http.base.internal.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.holder.ServletHolder;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.ErrorPageRuntime;
import org.apache.felix.http.base.internal.runtime.dto.ServletRuntime;
import org.osgi.service.http.runtime.dto.DTOConstants;

public final class ErrorPageRegistry
{
    private static final String CLIENT_ERROR = "4xx";
    private static final String SERVER_ERROR = "5xx";
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\d{3}");

    private static final List<Long> CLIENT_ERROR_CODES = hundredOf(400);
    private static final List<Long> SERVER_ERROR_CODES = hundredOf(500);

    private static List<Long> hundredOf(final int start)
    {
        List<Long> result = new ArrayList<Long>();
        for (long i = start; i < start + 100; i++)
        {
            result.add(i);
        }
        return Collections.unmodifiableList(result);
    }

    private static String parseErrorCodes(final List<Long> codes, final String string)
    {
        if (CLIENT_ERROR.equalsIgnoreCase(string))
        {
            codes.addAll(CLIENT_ERROR_CODES);
        }
        else if (SERVER_ERROR.equalsIgnoreCase(string))
        {
            codes.addAll(SERVER_ERROR_CODES);
        }
        else if (ERROR_CODE_PATTERN.matcher(string).matches())
        {
            codes.add(Long.parseLong(string));
        }
        else
        {
            return string;
        }
        return null;
    }

    private final Map<Long, List<ServletHolder>> errorCodesMap = new ConcurrentHashMap<Long, List<ServletHolder>>();
    private final Map<String, List<ServletHolder>> exceptionsMap = new ConcurrentHashMap<String, List<ServletHolder>>();

    private final Map<ServletInfo, ErrorRegistrationStatus> statusMapping = new ConcurrentHashMap<ServletInfo, ErrorRegistrationStatus>();

    private static final class ErrorRegistration {
        final List<Long> errorCodes = new ArrayList<Long>();
        final Set<String> exceptions = new HashSet<String>();
    }

    private static final class ErrorRegistrationStatus {
        ServletHolder holder;
        final Map<Long, Integer> errorCodeMapping = new ConcurrentHashMap<Long, Integer>();
        final Map<String, Integer> exceptionMapping = new ConcurrentHashMap<String, Integer>();
    }

    private ErrorRegistration getErrorRegistration(@Nonnull final ServletInfo info)
    {
        if ( info.getErrorPage() != null )
        {
            final ErrorRegistration reg = new ErrorRegistration();
            for(final String val : info.getErrorPage())
            {
                final String exception = parseErrorCodes(reg.errorCodes, val);
                if ( exception != null )
                {
                    reg.exceptions.add(exception);
                }
            }
            return reg;
        }
        return null;
    }

    /**
     * Add the servlet for error handling
     * @param holder The servlet holder.
     */
    public void addServlet(@Nonnull final ServletHolder holder)
    {
        final ErrorRegistration reg = getErrorRegistration(holder.getServletInfo());
        if ( reg != null )
        {
            final ErrorRegistrationStatus status = new ErrorRegistrationStatus();
            status.holder = holder;
            for(final long code : reg.errorCodes)
            {
                List<ServletHolder> list = errorCodesMap.get(code);
                if ( list == null )
                {
                    // activate
                    if ( tryToActivate(code, holder, status) )
                    {
                        final List<ServletHolder> newList = new ArrayList<ServletHolder>(1);
                        newList.add(holder);
                        errorCodesMap.put(code, newList);
                    }
                }
                else
                {
                    final List<ServletHolder> newList = new ArrayList<ServletHolder>(list);
                    newList.add(holder);
                    Collections.sort(newList);

                    if ( newList.get(0) == holder )
                    {
                        // activate and reactive
                        if ( tryToActivate(code, holder, status) )
                        {
                            final ServletHolder old = list.get(0);
                            old.destroy();
                            errorCodesMap.put(code, newList);
                        }
                    }
                    else
                    {
                        // failure
                        status.errorCodeMapping.put(code, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                        errorCodesMap.put(code, newList);
                    }
                }
            }
            for(final String exception : reg.exceptions)
            {
                List<ServletHolder> list = exceptionsMap.get(exception);
                if ( list == null )
                {
                    // activate
                    if ( tryToActivate(exception, holder, status) )
                    {
                        final List<ServletHolder> newList = new ArrayList<ServletHolder>(1);
                        newList.add(holder);
                        exceptionsMap.put(exception, newList);
                    }
                }
                else
                {
                    final List<ServletHolder> newList = new ArrayList<ServletHolder>(list);
                    newList.add(holder);
                    Collections.sort(newList);

                    if ( newList.get(0) == holder )
                    {
                        // activate and reactive
                        if ( tryToActivate(exception, holder, status) )
                        {
                            final ServletHolder old = list.get(0);
                            old.destroy();
                            exceptionsMap.put(exception, newList);
                        }
                    }
                    else
                    {
                        // failure
                        status.exceptionMapping.put(exception, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                        exceptionsMap.put(exception, newList);
                    }
                }
            }
            this.statusMapping.put(holder.getServletInfo(), status);
        }
    }

    /**
     * Remove the servlet from error handling
     * @param info The servlet info.
     */
    public void removeServlet(@Nonnull final ServletInfo info, final boolean destroy)
    {
        final ErrorRegistration reg = getErrorRegistration(info);
        if ( reg != null )
        {
            this.statusMapping.remove(info);
            for(final long code : reg.errorCodes)
            {
                final List<ServletHolder> list = errorCodesMap.get(code);
                if ( list != null )
                {
                    int index = 0;
                    final Iterator<ServletHolder> i = list.iterator();
                    while ( i.hasNext() )
                    {
                        final ServletHolder holder = i.next();
                        if ( holder.getServletInfo().equals(info) )
                        {
                            holder.destroy();

                            final List<ServletHolder> newList = new ArrayList<ServletHolder>(list);
                            newList.remove(holder);

                            if ( index == 0 )
                            {
                                index++;
                                while ( index < list.size() )
                                {
                                    final ServletHolder next = list.get(index);
                                    if ( tryToActivate(code, next, statusMapping.get(next.getServletInfo())) )
                                    {
                                        break;
                                    }
                                    else
                                    {
                                        newList.remove(next);
                                    }
                                }
                            }
                            if ( newList.isEmpty() )
                            {
                                errorCodesMap.remove(code);
                            }
                            else
                            {
                                errorCodesMap.put(code, newList);
                            }

                            break;
                        }
                        index++;
                    }
                }
            }
            for(final String exception : reg.exceptions)
            {
                final List<ServletHolder> list = exceptionsMap.get(exception);
                if ( list != null )
                {
                    int index = 0;
                    final Iterator<ServletHolder> i = list.iterator();
                    while ( i.hasNext() )
                    {
                        final ServletHolder holder = i.next();
                        if ( holder.getServletInfo().equals(info) )
                        {
                            holder.destroy();

                            final List<ServletHolder> newList = new ArrayList<ServletHolder>(list);
                            newList.remove(holder);

                            if ( index == 0 )
                            {
                                index++;
                                while ( index < list.size() )
                                {
                                    final ServletHolder next = list.get(index);
                                    if ( tryToActivate(exception, next, statusMapping.get(next.getServletInfo())) )
                                    {
                                        break;
                                    }
                                    else
                                    {
                                        newList.remove(next);
                                    }
                                }
                            }
                            if ( newList.isEmpty() )
                            {
                                exceptionsMap.remove(exception);
                            }
                            else
                            {
                                exceptionsMap.put(exception, newList);
                            }

                            break;
                        }
                        index++;
                    }
                }
            }
        }
    }

    /**
     * Get the servlet handling the error
     * @param exception Optional exception
     * @param errorCode Error code
     * @return The servlet handling the error or {@code null}
     */
    public ServletHolder get(final Throwable exception, final int errorCode)
    {
        ServletHolder errorHandler = this.get(exception);
        if (errorHandler != null)
        {
            return errorHandler;
        }

        return get(errorCode);
    }

    private ServletHolder get(final int errorCode)
    {
        final List<ServletHolder> list = this.errorCodesMap.get(errorCode);
        if ( list != null )
        {
            return list.get(0);
        }
        return null;
    }

    private ServletHolder get(final Throwable exception)
    {
        if (exception == null)
        {
            return null;
        }

        ServletHolder servletHandler = null;
        Class<?> throwableClass = exception.getClass();
        while ( servletHandler == null && throwableClass != null )
        {
            final List<ServletHolder> list = this.errorCodesMap.get(throwableClass.getName());
            if ( list != null )
            {
                servletHandler = list.get(0);
            }
            if ( servletHandler == null )
            {
                throwableClass = throwableClass.getSuperclass();
                if ( !Throwable.class.isAssignableFrom(throwableClass) )
                {
                    throwableClass = null;
                }
            }

        }
        return servletHandler;
    }

    private boolean tryToActivate(final Long code, final ServletHolder holder, final ErrorRegistrationStatus status)
    {
        // add to active
        final int result = holder.init();
        status.errorCodeMapping.put(code, result);

        return result == -1;
    }

    private boolean tryToActivate(final String exception, final ServletHolder holder, final ErrorRegistrationStatus status)
    {
        // add to active
        final int result = holder.init();
        status.exceptionMapping.put(exception, result);

        return result == -1;
    }

    public Collection<ErrorPageRuntime> getErrorPageRuntimes()
    {
        final Collection<ErrorPageRuntime> errorPages = new TreeSet<ErrorPageRuntime>(ServletRuntime.COMPARATOR);

        for(final ErrorRegistrationStatus status : this.statusMapping.values())
        {
            // TODO - we could do this calculation already when generating the status object
            final Set<Long> activeCodes = new HashSet<Long>();
            final Set<String> activeExceptions = new HashSet<String>();
            final Set<Long> inactiveCodes = new HashSet<Long>();
            final Set<String> inactiveExceptions = new HashSet<String>();

            for(Map.Entry<Long, Integer> codeEntry : status.errorCodeMapping.entrySet() )
            {
                if ( codeEntry.getValue() == -1 )
                {
                    activeCodes.add(codeEntry.getKey());
                }
                else
                {
                    inactiveCodes.add(codeEntry.getKey());
                }
            }
            for(Map.Entry<String, Integer> codeEntry : status.exceptionMapping.entrySet() )
            {
                if ( codeEntry.getValue() == -1 )
                {
                    activeExceptions.add(codeEntry.getKey());
                }
                else
                {
                    inactiveExceptions.add(codeEntry.getKey());
                }
            }
            if ( !activeCodes.isEmpty() || !activeExceptions.isEmpty() )
            {
                errorPages.add(new ErrorPageRuntime(status.holder, activeCodes, activeExceptions));
            }
            if ( !inactiveCodes.isEmpty() || !inactiveExceptions.isEmpty() )
            {
                // add failure
            }
        }

        return errorPages;
    }
}
