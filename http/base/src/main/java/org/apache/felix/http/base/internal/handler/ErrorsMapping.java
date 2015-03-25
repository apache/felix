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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.runtime.dto.ErrorPageRuntime;

public final class ErrorsMapping
{
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\d{3}");

    private final Map<Integer, ServletHandler> errorCodesMap;
    private final Map<String, ServletHandler> exceptionsMap;

    // inverted indexes

    private final Map<Servlet, Collection<Integer>> invertedErrorCodesMap;
    private final Map<Servlet, Collection<String>> invertedExceptionsMap;

    public ErrorsMapping()
    {
        this.errorCodesMap = new HashMap<Integer, ServletHandler>();
        this.exceptionsMap = new HashMap<String, ServletHandler>();
        this.invertedErrorCodesMap = new HashMap<Servlet, Collection<Integer>>();
        this.invertedExceptionsMap = new HashMap<Servlet, Collection<String>>();
    }

    void addErrorServlet(String errorPage, ServletHandler handler) throws ServletException
    {
        // TODO Handle special values 4xx and 5xx
        if (ERROR_CODE_PATTERN.matcher(errorPage).matches())
        {
            Integer errorCode = Integer.valueOf(errorPage);
            addErrorServlet(handler, errorCode, this.errorCodesMap, this.invertedErrorCodesMap);
        }
        else
        {
            addErrorServlet(handler, errorPage, this.exceptionsMap, this.invertedExceptionsMap);
        }
    }

    private static <E> void addErrorServlet(ServletHandler handler, E error, Map<E, ServletHandler> index, Map<Servlet, Collection<E>> invertedIndex) throws ServletException
    {
        if (!index.containsKey(error))
        {
            index.put(error, handler);
        }
        else
        {
            throw new ServletException("Handler for error " + error + " already registered");
        }

        Servlet servlet = handler.getServlet();
        Collection<E> values = invertedIndex.get(servlet);

        if (values == null)
        {
            values = new LinkedList<E>();
            invertedIndex.put(servlet, values);
        }

        values.add(error);
    }

    public ServletHandler get(int errorCode)
    {
        return this.errorCodesMap.get(errorCode);
    }

    public ServletHandler get(String exception)
    {
        return this.exceptionsMap.get(exception);
    }

    void removeServlet(Servlet servlet)
    {
        removeMapping(servlet, this.errorCodesMap, this.invertedErrorCodesMap);
        removeMapping(servlet, this.exceptionsMap, this.invertedExceptionsMap);
    }

    private static <E> void removeMapping(Servlet servlet, Map<E, ServletHandler> index, Map<Servlet, Collection<E>> invertedIndex)
    {
        Collection<E> keys = invertedIndex.remove(servlet);
        if (keys != null && !keys.isEmpty())
        {
            for (E key : keys)
            {
                index.remove(key);
            }
        }
    }

    void clear()
    {
        this.errorCodesMap.clear();
        this.exceptionsMap.clear();
    }

    @SuppressWarnings("unchecked")
    public Collection<ServletHandler> getMappedHandlers()
    {
        return sortedUnion(errorCodesMap.values(), exceptionsMap.values());
    }

    public ErrorPageRuntime getErrorPage(ServletHandler servletHandler)
    {
        Collection<Integer> errorCodes = getCopy(servletHandler, invertedErrorCodesMap);
        Collection<String> exceptions = getCopy(servletHandler, invertedExceptionsMap);
        return new ErrorPageRuntime(servletHandler, errorCodes, exceptions);
    }

    private static <T> List<T> getCopy(ServletHandler key, Map<Servlet, Collection<T>> map)
    {
        Collection<T> result = map.get(key.getServlet());
        if (result != null)
        {
            return new ArrayList<T>(result);
        }

        return Collections.emptyList();
    }
}
