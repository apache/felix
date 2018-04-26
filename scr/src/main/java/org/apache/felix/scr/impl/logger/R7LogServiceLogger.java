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
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

/**
 * This is a logger based on the R7 LogService/LoggerFactory
 */
class R7LogServiceLogger implements InternalLogger
{
    private final Logger logger;

    public R7LogServiceLogger(final Bundle bundle, final LogService loggerFactory, final String name)
    {
        this.logger = ((LoggerFactory)loggerFactory).getLogger(bundle, name, Logger.class);
    }

    @Override
    public boolean isLogEnabled(final int level)
    {
        switch ( level )
        {
            case 1 : return logger.isErrorEnabled();
            case 2 : return logger.isWarnEnabled();
            case 3 : return logger.isInfoEnabled();
            default : return logger.isDebugEnabled();
        }
    }

    @Override
    public void log(final int level, final String message, final Throwable ex)
    {
        if ( ex == null )
        {
            switch ( level )
            {
                case 1 : logger.error(message); break;
                case 2 : logger.warn(message); break;
                case 3 : logger.info(message); break;
                default : logger.debug(message);
            }
        }
        else
        {
            switch ( level )
            {
                case 1 : logger.error(message, ex); break;
                case 2 : logger.warn(message, ex); break;
                case 3 : logger.info(message, ex); break;
                default : logger.debug(message, ex);
            }
        }
    }
}