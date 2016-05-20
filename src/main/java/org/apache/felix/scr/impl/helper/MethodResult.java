/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl.helper;

import java.util.Map;

/**
 * The <code>MethodResult</code> conveys the return value of one of the
 * activate, modify, and deactivate methods.
 * <p>
 * Note that the method returning <code>null</code> or being defined as
 * <code>void</code> is not the same thing. If the method returns
 * <code>null</code> an instance of this class is returned whose
 * {@link #getResult()} method returns <code>null</code>. If the method is
 * defined as <code>void</code> the special instance {@link #VOID} is returned.
 */
public class MethodResult
{

    /**
     * Predefined instance indicating a successful call to a void method.
     */
    public static final MethodResult VOID = new MethodResult(false, null);

    /**
     * Predefined instance indicating to reactivate the component.
     */
    public static final MethodResult REACTIVATE = new MethodResult(false, null);

    /**
     * The actual result from the method, which may be <code>null</code>.
     */
    private final Map<String, Object> result;

    private final boolean hasResult;

    public MethodResult(final boolean hasResult, final Map<String, Object> result)
    {
        this.hasResult = hasResult;
        this.result = result;
    }

    public boolean hasResult()
    {
        return hasResult;
    }

    public Map<String, Object> getResult()
    {
        return result;
    }
}
