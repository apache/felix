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

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractHandler
{
    private final static AtomicInteger ID = new AtomicInteger();

    private final int id;
    private final String baseName;
    private final ExtServletContext context;
    private final Map<String, String> initParams;

    public AbstractHandler(ExtServletContext context, String baseName)
    {
        this.context = context;
        this.baseName = baseName;
        this.id = ID.incrementAndGet();
        this.initParams = new HashMap<String, String>();
    }

    public abstract void destroy();

    public final Map<String, String> getInitParams()
    {
        return this.initParams;
    }

    public final String getName()
    {
        String name = this.baseName;
        if (name == null)
        {
            name = String.format("%s_%d", getSubject().getClass(), this.id);
        }
        return name;
    }

    public abstract void init() throws ServletException;

    public final void setInitParams(Dictionary map)
    {
        this.initParams.clear();
        if (map == null)
        {
            return;
        }

        Enumeration e = map.keys();
        while (e.hasMoreElements())
        {
            Object key = e.nextElement();
            Object value = map.get(key);

            if ((key instanceof String) && (value instanceof String))
            {
                this.initParams.put((String) key, (String) value);
            }
        }
    }

    protected final ExtServletContext getContext()
    {
        return this.context;
    }

    /**
     * @return a unique ID for this handler, &gt; 0.
     */
    protected final int getId()
    {
        return id;
    }

    /**
     * @return the {@link Servlet} or {@link Filter} this handler handles.
     */
    protected abstract Object getSubject();
}
