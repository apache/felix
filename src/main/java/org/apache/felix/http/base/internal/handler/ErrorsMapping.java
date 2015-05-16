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
package org.apache.felix.http.base.internal.handler;

import static org.apache.felix.http.base.internal.util.CollectionUtils.sortedUnion;
import static org.apache.felix.http.base.internal.util.ErrorPageUtil.parseErrorCodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.http.base.internal.whiteboard.RegistrationFailureException;
import org.osgi.service.http.runtime.dto.DTOConstants;

public final class ErrorsMapping
{
    private final Map<Integer, ServletHandler> errorCodesMap;
    private final Map<String, ServletHandler> exceptionsMap;

    public ErrorsMapping()
    {
        this(new HashMap<Integer, ServletHandler>(), new HashMap<String, ServletHandler>());
    }

    public ErrorsMapping(Map<Integer, ServletHandler> errorCodesMap, Map<String, ServletHandler> exceptionsMap)
    {
        this.errorCodesMap = errorCodesMap;
        this.exceptionsMap = exceptionsMap;
    }

    public ErrorsMapping update(Map<String, ServletHandler> add, Map<String, ServletHandler> remove) throws RegistrationFailureException
    {
        Map<Integer, ServletHandler> newErrorCodesMap = new HashMap<Integer, ServletHandler>(this.errorCodesMap);
        Map<String, ServletHandler> newExceptionsMap = new HashMap<String, ServletHandler>(this.exceptionsMap);;

        for (Map.Entry<String, ServletHandler> errorPage : remove.entrySet())
        {
            String errorString = errorPage.getKey();
            List<Integer> parsedErrorCodes = parseErrorCodes(errorString);
            if (parsedErrorCodes != null)
            {
                removeAllMappings(parsedErrorCodes, newErrorCodesMap);
            }
            else
            {
                newExceptionsMap.remove(errorString);
            }
        }

        for (Map.Entry<String, ServletHandler> errorPage : add.entrySet())
        {
            String errorString = errorPage.getKey();
            List<Integer> parsedErrorCodes = parseErrorCodes(errorString);
            if (parsedErrorCodes != null)
            {
                addErrorServlets(parsedErrorCodes, errorPage.getValue(), newErrorCodesMap);
            }
            else
            {
                addErrorServlet(errorString, errorPage.getValue(), newExceptionsMap);
            }
        }

        return new ErrorsMapping(newErrorCodesMap, newExceptionsMap);
    }

    private void removeAllMappings(List<Integer> parsedErrorCodes, Map<Integer, ServletHandler> newErrorCodesMap)
    {
        for (Integer errorCode : parsedErrorCodes)
        {
            newErrorCodesMap.remove(errorCode);
        }
    }

    private <E> void addErrorServlets(List<E> errors, ServletHandler handler, Map<E, ServletHandler> index) throws RegistrationFailureException
    {
        for (E error : errors)
        {
            addErrorServlet(error, handler, index);
        }
    }

    private <E> void addErrorServlet(E error, ServletHandler handler, Map<E, ServletHandler> index) throws RegistrationFailureException
    {
        if (index.containsKey(error))
        {
            throw new RegistrationFailureException(handler.getServletInfo(), DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE,
                "Handler for error " + error + " already registered");
        }
        index.put(error, handler);
    }

    void clear()
    {
        this.errorCodesMap.clear();
        this.exceptionsMap.clear();
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
        return this.errorCodesMap.get(errorCode);
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
            servletHandler = this.exceptionsMap.get(throwableClass.getName());
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


    @SuppressWarnings("unchecked")
    public Collection<ServletHandler> values()
    {
        return sortedUnion(errorCodesMap.values(), exceptionsMap.values());
    }
}
