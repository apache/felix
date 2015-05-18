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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.state.FailureServletState;
import org.apache.felix.http.base.internal.runtime.dto.state.ServletState;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * TODO - check if add/remove needs syncing
 */
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

    private static String parseErrorCodes(final Set<Long> codes, final String string)
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

    private final Map<Long, List<ServletHandler>> errorCodesMap = new ConcurrentHashMap<Long, List<ServletHandler>>();
    private final Map<String, List<ServletHandler>> exceptionsMap = new ConcurrentHashMap<String, List<ServletHandler>>();

    private final Map<ServletInfo, ErrorRegistrationStatus> statusMapping = new ConcurrentHashMap<ServletInfo, ErrorRegistrationStatus>();

    private static final class ErrorRegistration {
        final Set<Long> errorCodes = new TreeSet<Long>();
        final Set<String> exceptions = new TreeSet<String>();
    }

    private static final class ErrorRegistrationStatus {
        ServletHandler handler;
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
     * @param handler The servlet handler.
     */
    public void addServlet(@Nonnull final ServletHandler handler)
    {
        final ErrorRegistration reg = getErrorRegistration(handler.getServletInfo());
        if ( reg != null )
        {
            final ErrorRegistrationStatus status = new ErrorRegistrationStatus();
            status.handler = handler;
            for(final long code : reg.errorCodes)
            {
                List<ServletHandler> list = errorCodesMap.get(code);
                if ( list == null )
                {
                    // activate
                    if ( tryToActivate(code, handler, status) )
                    {
                        final List<ServletHandler> newList = new ArrayList<ServletHandler>(1);
                        newList.add(handler);
                        errorCodesMap.put(code, newList);
                    }
                }
                else
                {
                    final List<ServletHandler> newList = new ArrayList<ServletHandler>(list);
                    newList.add(handler);
                    Collections.sort(newList);

                    if ( newList.get(0) == handler )
                    {
                        // activate and reactive
                        if ( tryToActivate(code, handler, status) )
                        {
                            final ServletHandler old = list.get(0);
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
                List<ServletHandler> list = exceptionsMap.get(exception);
                if ( list == null )
                {
                    // activate
                    if ( tryToActivate(exception, handler, status) )
                    {
                        final List<ServletHandler> newList = new ArrayList<ServletHandler>(1);
                        newList.add(handler);
                        exceptionsMap.put(exception, newList);
                    }
                }
                else
                {
                    final List<ServletHandler> newList = new ArrayList<ServletHandler>(list);
                    newList.add(handler);
                    Collections.sort(newList);

                    if ( newList.get(0) == handler )
                    {
                        // activate and reactive
                        if ( tryToActivate(exception, handler, status) )
                        {
                            final ServletHandler old = list.get(0);
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
            this.statusMapping.put(handler.getServletInfo(), status);
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
                final List<ServletHandler> list = errorCodesMap.get(code);
                if ( list != null )
                {
                    int index = 0;
                    final Iterator<ServletHandler> i = list.iterator();
                    while ( i.hasNext() )
                    {
                        final ServletHandler handler = i.next();
                        if ( handler.getServletInfo().equals(info) )
                        {
                            handler.destroy();

                            final List<ServletHandler> newList = new ArrayList<ServletHandler>(list);
                            newList.remove(handler);

                            if ( index == 0 )
                            {
                                index++;
                                while ( index < list.size() )
                                {
                                    final ServletHandler next = list.get(index);
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
                final List<ServletHandler> list = exceptionsMap.get(exception);
                if ( list != null )
                {
                    int index = 0;
                    final Iterator<ServletHandler> i = list.iterator();
                    while ( i.hasNext() )
                    {
                        final ServletHandler handler = i.next();
                        if ( handler.getServletInfo().equals(info) )
                        {
                            handler.destroy();

                            final List<ServletHandler> newList = new ArrayList<ServletHandler>(list);
                            newList.remove(handler);

                            if ( index == 0 )
                            {
                                index++;
                                while ( index < list.size() )
                                {
                                    final ServletHandler next = list.get(index);
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
    public ServletHandler get(final Throwable exception, final int errorCode)
    {
        ServletHandler errorHandler = this.get(exception);
        if (errorHandler != null)
        {
            return errorHandler;
        }

        return get(errorCode);
    }

    private ServletHandler get(final int errorCode)
    {
        final List<ServletHandler> list = this.errorCodesMap.get(errorCode);
        if ( list != null )
        {
            return list.get(0);
        }
        return null;
    }

    private ServletHandler get(final Throwable exception)
    {
        if (exception == null)
        {
            return null;
        }

        ServletHandler servletHandler = null;
        Class<?> throwableClass = exception.getClass();
        while ( servletHandler == null && throwableClass != null )
        {
            final List<ServletHandler> list = this.errorCodesMap.get(throwableClass.getName());
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

    private boolean tryToActivate(final Long code, final ServletHandler handler, final ErrorRegistrationStatus status)
    {
        // add to active
        final int result = handler.init();
        status.errorCodeMapping.put(code, result);

        return result == -1;
    }

    private boolean tryToActivate(final String exception, final ServletHandler handler, final ErrorRegistrationStatus status)
    {
        // add to active
        final int result = handler.init();
        status.exceptionMapping.put(exception, result);

        return result == -1;
    }

    public void getRuntimeInfo(final Collection<ServletState> servletStates,
            final Collection<FailureServletState> failureServletStates)
    {
        for(final ErrorRegistrationStatus status : this.statusMapping.values())
        {
            // TODO - we could do this calculation already when generating the status object
            final ErrorRegistration active = new ErrorRegistration();
            final Map<Integer, ErrorRegistration> inactive = new HashMap<Integer, ErrorRegistration>();

            for(Map.Entry<Long, Integer> codeEntry : status.errorCodeMapping.entrySet() )
            {
                if ( codeEntry.getValue() == -1 )
                {
                    active.errorCodes.add(codeEntry.getKey());
                }
                else
                {
                    ErrorRegistration set = inactive.get(codeEntry.getValue());
                    if ( set == null )
                    {
                        set = new ErrorRegistration();
                        inactive.put(codeEntry.getValue(), set);
                    }
                    set.errorCodes.add(codeEntry.getKey());
                }
            }
            for(Map.Entry<String, Integer> codeEntry : status.exceptionMapping.entrySet() )
            {
                if ( codeEntry.getValue() == -1 )
                {
                    active.exceptions.add(codeEntry.getKey());
                }
                else
                {
                    ErrorRegistration set = inactive.get(codeEntry.getValue());
                    if ( set == null )
                    {
                        set = new ErrorRegistration();
                        inactive.put(codeEntry.getValue(), set);
                    }
                    set.exceptions.add(codeEntry.getKey());
                }
            }
            if ( !active.errorCodes.isEmpty() || !active.exceptions.isEmpty() )
            {
                servletStates.add(new ServletState()
                {

                    @Override
                    public Servlet getServlet()
                    {
                        return status.handler.getServlet();
                    }

                    @Override
                    public ServletInfo getServletInfo()
                    {
                        return status.handler.getServletInfo();
                    }

                    @Override
                    public String[] getPatterns()
                    {
                        return null;
                    }

                    @Override
                    public long[] getErrorCodes()
                    {
                        final long[] codes = new long[active.errorCodes.size()];
                        final Iterator<Long> iter = active.errorCodes.iterator();
                        for(int i=0; i<codes.length; i++)
                        {
                            codes[i] = iter.next();
                        }
                        return codes;
                    }

                    @Override
                    public String[] getErrorExceptions()
                    {
                        return active.exceptions.toArray(new String[active.exceptions.size()]);
                    }
                });
            }
            for(final Map.Entry<Integer, ErrorRegistration> entry : inactive.entrySet())
            {
                failureServletStates.add(new FailureServletState()
                {

                    @Override
                    public Servlet getServlet()
                    {
                        return status.handler.getServlet();
                    }

                    @Override
                    public ServletInfo getServletInfo()
                    {
                        return status.handler.getServletInfo();
                    }

                    @Override
                    public String[] getPatterns()
                    {
                        return null;
                    }

                    @Override
                    public long[] getErrorCodes()
                    {
                        final long[] codes = new long[entry.getValue().errorCodes.size()];
                        final Iterator<Long> iter = entry.getValue().errorCodes.iterator();
                        for(int i=0; i<codes.length; i++)
                        {
                            codes[i] = iter.next();
                        }
                        return codes;
                    }

                    @Override
                    public String[] getErrorExceptions()
                    {
                        return entry.getValue().exceptions.toArray(new String[entry.getValue().exceptions.size()]);
                    }

                    @Override
                    public long getReason()
                    {
                        return entry.getKey();
                    }

                });
            }
        }
    }
}
