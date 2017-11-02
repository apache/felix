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

import java.io.PrintStream;

import org.osgi.service.log.LogService;

/**
 * This logger logs to std out / err
 */
class StdOutLogger implements InternalLogger
{
    @Override
    public void log(final int level, final String message, final Throwable ex)
    {
        if ( isLogEnabled(level) )
        {
            // output depending on level
            final PrintStream out = ( level == LogService.LOG_ERROR )? System.err: System.out;

            // level as a string
            final StringBuilder buf = new StringBuilder();
            switch (level)
            {
                case ( LogService.LOG_DEBUG ):
                    buf.append( "DEBUG: " );
                    break;
                case ( LogService.LOG_INFO ):
                    buf.append( "INFO : " );
                    break;
                case ( LogService.LOG_WARNING ):
                    buf.append( "WARN : " );
                    break;
                case ( LogService.LOG_ERROR ):
                    buf.append( "ERROR: " );
                    break;
                default:
                    buf.append( "UNK  : " );
                    break;
            }

            buf.append(message);

            final String msg = buf.toString();

            if ( ex == null )
            {
                out.println(msg);
            }
            else
            {
                // keep the message and the stacktrace together
                synchronized ( out )
                {
                    out.println( msg );
                    ex.printStackTrace( out );
                }
            }
        }
    }

    @Override
    public boolean isLogEnabled(final int level)
    {
        return true;
    }
}
