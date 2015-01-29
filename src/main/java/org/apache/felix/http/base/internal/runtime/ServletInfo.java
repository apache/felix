/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.base.internal.runtime;

import java.util.Map;

import javax.servlet.Servlet;

import org.osgi.dto.DTO;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.runtime.dto.ServletDTO;

import aQute.bnd.annotation.ConsumerType;

/**
 * Provides registration information for a {@link Servlet}, and is used to programmatically register {@link Servlet}s.
 * <p>
 * This class only provides information used at registration time, and as such differs slightly from {@link DTO}s like, {@link ServletDTO}.
 * </p>
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@ConsumerType
public final class ServletInfo
{
    /**
     * The name of the servlet.
     */
    public String name;

    /**
     * The request mappings for the servlet.
     * <p>
     * The specified patterns are used to determine whether a request is mapped to the servlet.
     * </p>
     */
    public String[] patterns;

    /**
     * The error pages and/or codes.
     */
    public String[] errorPage;

    /**
     * Specifies whether the servlet supports asynchronous processing.
     */
    public boolean asyncSupported = false;

    /**
     * The servlet initialization parameters as provided during registration of the servlet.
     */
    public Map<String, String> initParams;

    /**
     * The {@link HttpContext} for the servlet.
     */
    public HttpContext context;

    public int ranking = 0;
    public long serviceId;
}
