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
package org.apache.felix.scr.impl.logger;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * This is a logger based on the LogService.
 * It supports both R6 and R7 logging
 *
 */
class LogServiceSupport
{
    private final boolean r7Enabled;

    private final LogService logService;

    private final Bundle bundle;

    public LogServiceSupport(final Bundle bundle, final Object logService)
    {
        this.logService = (LogService) logService;
        this.bundle = bundle;
        this.r7Enabled = "org.osgi.service.log.LoggerFactory".equals(this.logService.getClass().getSuperclass().getName());
    }

    InternalLogger getLogger()
    {
        if ( r7Enabled )
        {
            return new R7LogServiceLogger(this.bundle, this.logService, null);
        }
        return new R6LogServiceLogger(this.logService);
    }

    InternalLogger getLogger(final String className)
    {
        if ( r7Enabled )
        {
            return new R7LogServiceLogger(this.bundle, this.logService, className);
        }
        return new R6LogServiceLogger(this.logService);
    }
}