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
package org.apache.felix.http.base.internal.runtime.dto;

import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

public abstract class BuilderConstants
{

    public static final String[] STRING_ARRAY = new String[0];

    public static final ServletContextDTO[] CONTEXT_DTO_ARRAY = new ServletContextDTO[0];

    public static final ServletDTO[] SERVLET_DTO_ARRAY = new ServletDTO[0];
    public static final ResourceDTO[] RESOURCE_DTO_ARRAY = new ResourceDTO[0];
    public static final FilterDTO[] FILTER_DTO_ARRAY = new FilterDTO[0];
    public static final ErrorPageDTO[] ERROR_PAGE_DTO_ARRAY = new ErrorPageDTO[0];
    public static final ListenerDTO[] LISTENER_DTO_ARRAY = new ListenerDTO[0];
}
