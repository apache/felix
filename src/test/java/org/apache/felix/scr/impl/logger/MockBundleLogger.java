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


import java.text.MessageFormat;

import org.apache.felix.scr.impl.MockBundle;
import org.apache.felix.scr.impl.MockBundleContext;


public class MockBundleLogger extends BundleLogger
{
    public MockBundleLogger()
    {
        super(new MockBundleContext(new MockBundle()), new MockScrLogger());
    }


    private String lastMessage;


    @Override
    public boolean isLogEnabled( final int level )
    {
        return level == 1;
    }

    @Override
    public void log( final int level, final String pattern, final Throwable ex, final Object...arguments )
    {
        if ( isLogEnabled( level ) )
        {
            log( level,  MessageFormat.format( pattern, arguments ), ex );
        }
    }


    @Override
    public void log( final int level, final String message, final Throwable ex )
    {
        lastMessage = message;
    }


    public boolean messageContains( final String value )
    {
        return lastMessage != null && lastMessage.indexOf( value ) >= 0;
    }
}