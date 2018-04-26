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

import org.osgi.service.log.LogService;

/**
 * This is a logger based on the R6 LogService.
 */
class R6LogServiceLogger implements InternalLogger
{
    private final LogService logService;

    public R6LogServiceLogger(final LogService logService)
    {
        this.logService = logService;
    }

    @Override
    public boolean isLogEnabled(final int level)
    {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void log(final int level, final String message, final Throwable ex)
    {
        this.logService.log(level, message, ex);
    }
}