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
package org.apache.felix.http.base.internal.whiteboard;

import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;

@SuppressWarnings("serial")
public class RegistrationFailureException extends ServletException
{
    private final WhiteboardServiceInfo<?> info;
    private final int errorCode;

    public RegistrationFailureException(WhiteboardServiceInfo<?> info, int errorCode)
    {
        super();
        this.info = info;
        this.errorCode = errorCode;
    }

    public RegistrationFailureException(WhiteboardServiceInfo<?> info, int errorCode, String message)
    {
        super(message);
        this.info = info;
        this.errorCode = errorCode;
    }

    public RegistrationFailureException(WhiteboardServiceInfo<?> info, int errorCode, Throwable exception)
    {
        super(exception);
        this.info = info;
        this.errorCode = errorCode;
    }

    public WhiteboardServiceInfo<?> getInfo()
    {
        return info;
    }

    public int getErrorCode()
    {
        return errorCode;
    }
}