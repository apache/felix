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
import java.util.Arrays;
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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.dto.BuilderConstants;
import org.apache.felix.http.base.internal.runtime.dto.ErrorPageDTOBuilder;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

/**
 * The error page registry keeps tracks of the active/inactive servlets handling
 * error pages (error code and/or exception).
 * This registry is per servlet context.
 */
public final class ErrorPageRegistry
{
    private static final String CLIENT_ERROR = "4xx";
    private static final String SERVER_ERROR = "5xx";
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\d{3}");

    private static final List<Long> CLIENT_ERROR_CODES = hundredOf(400);
    private static final List<Long> SERVER_ERROR_CODES = hundredOf(500);

    private final Map<String, List<ServletHandler>> errorMapping = new ConcurrentHashMap<String, List<ServletHandler>>();

    private volatile List<ErrorRegistrationStatus> status = Collections.emptyList();

    public static final class ErrorRegistration {
        public final long[] errorCodes;
        public final String[] exceptions;

        public ErrorRegistration(final long[] errorCodes, final String[] exceptions)
        {
            this.errorCodes = errorCodes;
            this.exceptions = exceptions;
        }
    }

    static final class ErrorRegistrationStatus implements Comparable<ErrorRegistrationStatus> {

        private final ServletHandler handler;
        public final Map<Integer, ErrorRegistration> reasonMapping = new HashMap<Integer, ErrorRegistration>();

        public final boolean usesClientErrorCodes;
        public final boolean usesServerErrorCodes;
        
        public ErrorRegistrationStatus(final ServletHandler handler)
        {
            this.handler = handler;
            this.usesClientErrorCodes = hasErrorCode(handler, CLIENT_ERROR);
            this.usesServerErrorCodes = hasErrorCode(handler, SERVER_ERROR);
        }

        public ServletHandler getHandler()
        {
            return this.handler;
        }

        @Override
        public int compareTo(final ErrorRegistrationStatus o)
        {
            return this.handler.compareTo(o.getHandler());
        }
    }

    private static boolean hasErrorCode(final ServletHandler handler, final String key) 
    {
    	for(final String val : handler.getServletInfo().getErrorPage())
    	{
    		if ( key.equals(val) ) 
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    private static List<Long> hundredOf(final int start)
    {
        List<Long> result = new ArrayList<Long>();
        for (long i = start; i < start + 100; i++)
        {
            result.add(i);
        }
        return Collections.unmodifiableList(result);
    }

    private static long[] toLongArray(final Set<Long> set)
    {
        long[] codes = BuilderConstants.EMPTY_LONG_ARRAY;
        if ( !set.isEmpty() )
        {
            codes = new long[set.size()];
            int index = 0;
            for(final Long code : set)
            {
                codes[index++] = code;
            }
        }
        return codes;
    }

    private static Set<Long> toLongSet(final long[] codes)
    {
        final Set<Long> set = new TreeSet<Long>();
        for(final long c : codes)
        {
            set.add(c);
        }
        return set;
    }

    private static String[] toStringArray(final Set<String> set)
    {
        String[] array = BuilderConstants.EMPTY_STRING_ARRAY;
        if ( !set.isEmpty() )
        {
            array = set.toArray(new String[set.size()]);
        }
        return array;
    }

    private static Set<String> toStringSet(final String[] array)
    {
        final Set<String> set = new TreeSet<String>();
        for(final String s : array)
        {
            set.add(s);
        }
        return set;
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

    /**
     * Parse the registration properties of the servlet for error handling
     * @param info The servlet info
     * @return An error registration object if the servlet handles errors
     */
    public static @CheckForNull ErrorRegistration getErrorRegistration(@Nonnull final ServletInfo info)
    {
        if ( info.getErrorPage() != null )
        {
            final Set<Long> errorCodes = new TreeSet<Long>();
            final Set<String> exceptions = new TreeSet<String>();

            for(final String val : info.getErrorPage())
            {
                final String exception = parseErrorCodes(errorCodes, val);
                if ( exception != null )
                {
                    exceptions.add(exception);
                }
            }
            final long[] codes = toLongArray(errorCodes);
            final String[] exceptionsArray = toStringArray(exceptions);

            return new ErrorRegistration(codes, exceptionsArray);
        }
        return null;
    }

    /**
     * Add the servlet for error handling
     * @param handler The servlet handler.
     */
    public synchronized void addServlet(@Nonnull final ServletHandler handler)
    {
        final ErrorRegistration reg = getErrorRegistration(handler.getServletInfo());
        if ( reg != null )
        {
            final ErrorRegistrationStatus status = new ErrorRegistrationStatus(handler);
            for(final long code : reg.errorCodes)
            {
                addErrorHandling(handler, status, code, null);
            }
            for(final String exception : reg.exceptions)
            {
                addErrorHandling(handler, status, 0, exception);
            }
            final List<ErrorRegistrationStatus> newList = new ArrayList<ErrorPageRegistry.ErrorRegistrationStatus>(this.status);
            newList.add(status);
            Collections.sort(newList);
            this.status = newList;
        }
    }

    /**
     * Remove the servlet from error handling
     * @param info The servlet info.
     */
    public synchronized void removeServlet(@Nonnull final ServletInfo info, final boolean destroy)
    {
        final ErrorRegistration reg = getErrorRegistration(info);
        if ( reg != null )
        {
            final List<ErrorRegistrationStatus> newList = new ArrayList<ErrorPageRegistry.ErrorRegistrationStatus>(this.status);
            final Iterator<ErrorRegistrationStatus> i = newList.iterator();
            while ( i.hasNext() )
            {
                final ErrorRegistrationStatus status = i.next();
                if ( status.handler.getServletInfo().equals(info) )
                {
                    i.remove();
                    break;
                }
            }
            this.status = newList;

            for(final long code : reg.errorCodes)
            {
                removeErrorHandling(info, code, null);
            }
            for(final String exception : reg.exceptions)
            {
                removeErrorHandling(info, 0, exception);
            }
        }
    }

    public synchronized void cleanup()
    {
        this.errorMapping.clear();
        this.status = Collections.emptyList();
    }

    private void addErrorHandling(final ServletHandler handler, final ErrorRegistrationStatus status, final long code, final String exception)
    {
        final String key = (exception != null ? exception : String.valueOf(code));

        final List<ServletHandler> newList;
        final List<ServletHandler> list = errorMapping.get(key);
        if ( list == null )
        {
            newList = Collections.singletonList(handler);
        }
        else
        {
            newList = new ArrayList<ServletHandler>(list);
            newList.add(handler);
            Collections.sort(newList);
        }
        if ( newList.get(0) == handler )
        {
            // try to activate (and deactivate old handler)
            final int result = handler.init();
            addReason(status, code, exception, result);
            if ( result == -1 )
            {
                if ( list != null )
                {
                    final ServletHandler old = list.get(0);
                    old.destroy();
                    errorMapping.put(key, newList);
                    ErrorRegistrationStatus oldStatus = null;
                    final Iterator<ErrorRegistrationStatus> i = this.status.iterator();
                    while ( oldStatus == null && i.hasNext() )
                    {
                        final ErrorRegistrationStatus current = i.next();
                        if ( current.handler.getServletInfo().equals(old.getServletInfo()) )
                        {
                            oldStatus = current;
                        }
                    }
                    if ( oldStatus != null )
                    {
                        removeReason(oldStatus, code, exception, -1);
                    	boolean addReason = true;
                    	if ( exception == null )
                    	{
                    		if ( code >= 400 && code < 500 && oldStatus.usesClientErrorCodes && !status.usesClientErrorCodes )
                    		{
                    			addReason = false;
                    		} 
                    		else if ( code >= 500 && code < 600 && oldStatus.usesServerErrorCodes && !status.usesServerErrorCodes )
                    		{
                    			addReason = false;
                    		}
                    	}
                    	if ( addReason )
                    	{
                    		addReason(oldStatus, code, exception, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                    	}
                    }
                }
                else
                {
                    errorMapping.put(key, newList);
                }
            }
        }
        else
        {
            // failure
        	boolean addReason = true;
        	if ( exception == null )
        	{
        		if ( code >= 400 && code < 500 && status.usesClientErrorCodes && !hasErrorCode(newList.get(0), CLIENT_ERROR) )
        		{
        			addReason = false;
        		} 
        		else if ( code >= 500 && code < 600 && status.usesServerErrorCodes && !hasErrorCode(newList.get(0), SERVER_ERROR) )
        		{
        			addReason = false;
        		}
        	}
        	if ( addReason )
        	{
        		addReason(status, code, exception, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
        	}
            errorMapping.put(key, newList);
        }
    }

    /**
     * Make an entry in the status object (which are used to create the DTOs)
     * @param status The status object
     * @param code Either the code
     * @param exception or the exception
     * @param reason The code for the failure reason or {@code -1} for success.	
     */
    private void addReason(final ErrorRegistrationStatus status, 
    		final long code, 
    		final String exception, 
    		final int reason)
    {
        ErrorRegistration reg = status.reasonMapping.get(reason);
        if ( reg == null )
        {
            if ( exception != null )
            {
                reg = new ErrorRegistration(BuilderConstants.EMPTY_LONG_ARRAY, new String[] {exception});
            }
            else
            {
                reg = new ErrorRegistration(new long[] {code}, BuilderConstants.EMPTY_STRING_ARRAY);
            }
        }
        else
        {
            long[] codes = reg.errorCodes;
            String[] exceptions = reg.exceptions;
            if ( exception != null )
            {
                final Set<String> set = toStringSet(exceptions);
                set.add(exception);
                exceptions = toStringArray(set);
            }
            else
            {
                final Set<Long> set = toLongSet(codes);
                set.add(code);
                codes = toLongArray(set);
            }

            reg = new ErrorRegistration(codes, exceptions);
        }
        status.reasonMapping.put(reason, reg);
    }

    private void removeReason(final ErrorRegistrationStatus status, final long code, final String exception, final int reason)
    {
        ErrorRegistration reg = status.reasonMapping.get(reason);
        if ( reg != null )
        {
            long[] codes = reg.errorCodes;
            String[] exceptions = reg.exceptions;
            if ( exception != null )
            {
                final Set<String> set = toStringSet(exceptions);
                set.remove(exception);
                exceptions = toStringArray(set);
            }
            else
            {
                final Set<Long> set = toLongSet(codes);
                set.remove(code);
                codes = toLongArray(set);
            }
            if ( codes.length == 0 && exceptions.length == 0 )
            {
                status.reasonMapping.remove(reason);
            }
            else
            {
                status.reasonMapping.put(reason, new ErrorRegistration(codes, exceptions));
            }
        }
    }

    private void removeErrorHandling(final ServletInfo info, final long code, final String exception)
    {
        final String key = (exception != null ? exception : String.valueOf(code));

        final List<ServletHandler> list = errorMapping.get(key);
        if ( list != null )
        {
            int index = 0;
            final Iterator<ServletHandler> i = list.iterator();
            while ( i.hasNext() )
            {
                final ServletHandler handler = i.next();
                if ( handler.getServletInfo().equals(info) )
                {
                    final List<ServletHandler> newList = new ArrayList<ServletHandler>(list);
                    newList.remove(handler);

                    if ( index == 0 )
                    {
                        handler.destroy();

                        index++;
                        while ( index < list.size() )
                        {
                            final ServletHandler next = list.get(index);
                            ErrorRegistrationStatus nextStatus = null;
                            for(final ErrorRegistrationStatus s : this.status)
                            {
                                if ( s.handler.getServletInfo().equals(next.getServletInfo()) )
                                {
                                    nextStatus = s;
                                    break;
                                }
                            }
                            this.removeReason(nextStatus, code, exception, DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
                            final int reason = next.init();
                            this.addReason(nextStatus, code, exception, reason);
                            if ( reason == -1 )
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
                        errorMapping.remove(key);
                    }
                    else
                    {
                        errorMapping.put(key, newList);
                    }

                    break;
                }
                index++;
            }
        }
    }

    /**
     * Get the servlet handling the error (error code or exception).
     * If an exception is provided, a handler for the exception is searched first.
     * If no handler is found (or no exception provided) a handler for the error
     * code is searched.
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

    /**
     * Get the servlet handling the error code
     * @param errorCode Error code
     * @return The servlet handling the error or {@code null}
     */
    private ServletHandler get(final long errorCode)
    {
        final List<ServletHandler> list = this.errorMapping.get(String.valueOf(errorCode));
        if ( list != null )
        {
            return list.get(0);
        }
        return null;
    }

    /**
     * Get the servlet handling the exception
     * @param exception Error exception
     * @return The servlet handling the error or {@code null}
     */
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
            final List<ServletHandler> list = this.errorMapping.get(throwableClass.getName());
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

    /**
     * Get DTOs for error pages.
     * @param dto The servlet context DTO
     * @param failedErrorPageDTOs The failed error page DTOs
     */
    public void getRuntimeInfo(final ServletContextDTO dto,
            final Collection<FailedErrorPageDTO> failedErrorPageDTOs)
    {
        final List<ErrorPageDTO> errorPageDTOs = new ArrayList<ErrorPageDTO>();
        final List<ErrorRegistrationStatus> statusList = this.status;
        for(final ErrorRegistrationStatus status : statusList)
        {
            for(final Map.Entry<Integer, ErrorRegistration> entry : status.reasonMapping.entrySet())
            {
                final ErrorPageDTO state = ErrorPageDTOBuilder.build(status.getHandler(), entry.getKey());
                state.errorCodes = Arrays.copyOf(entry.getValue().errorCodes, entry.getValue().errorCodes.length);
                state.exceptions = Arrays.copyOf(entry.getValue().exceptions, entry.getValue().exceptions.length);

                if ( entry.getKey() == -1 )
                {
                    errorPageDTOs.add(state);
                }
                else
                {
                    failedErrorPageDTOs.add((FailedErrorPageDTO)state);
                }
            }
        }
        if ( !errorPageDTOs.isEmpty() )
        {
            dto.errorPageDTOs = errorPageDTOs.toArray(new ErrorPageDTO[errorPageDTOs.size()]);
        }
    }
}
