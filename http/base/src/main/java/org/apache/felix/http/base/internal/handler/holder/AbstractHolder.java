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
package org.apache.felix.http.base.internal.handler.holder;

import javax.servlet.ServletContext;

import org.osgi.service.http.runtime.dto.DTOConstants;

public abstract class AbstractHolder<T extends AbstractHolder<?>> implements Comparable<T>
{
    private final ServletContext context;

    public AbstractHolder(final ServletContext context)
    {
        this.context = context;
    }

    public ServletContext getContext()
    {
        return this.context;
    }

    /**
     * Initialize the object
     * @return {code -1} on success, a failure reason according to {@link DTOConstants} otherwise.
     */
    public abstract int init();

    public abstract void destroy();
}
