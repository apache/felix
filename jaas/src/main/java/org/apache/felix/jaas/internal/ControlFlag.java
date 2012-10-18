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

package org.apache.felix.jaas.internal;

import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

/**
 * User: chetanm
 * Date: 7/9/12
 * Time: 9:55 PM
 */
@SuppressWarnings("UnusedDeclaration")
enum ControlFlag
{

    REQUIRED(LoginModuleControlFlag.REQUIRED), REQUISITE(LoginModuleControlFlag.REQUISITE), SUFFICIENT(
        LoginModuleControlFlag.SUFFICIENT), OPTIONAL(LoginModuleControlFlag.OPTIONAL), ;

    private final LoginModuleControlFlag flag;

    private ControlFlag(LoginModuleControlFlag flag)
    {
        this.flag = flag;
    }

    public LoginModuleControlFlag flag()
    {
        return flag;
    }

    public static ControlFlag from(String val)
    {
        val = Util.trimToNull(val);
        if (val == null)
        {
            return REQUIRED;
        }

        val = val.toUpperCase();
        return ControlFlag.valueOf(val);
    }

    public static String toString(LoginModuleControlFlag flag)
    {
        if (flag == LoginModuleControlFlag.REQUIRED)
        {
            return "REQUIRED";
        }
        else if (flag == LoginModuleControlFlag.REQUISITE)
        {
            return "REQUISITE";
        }
        else if (flag == LoginModuleControlFlag.SUFFICIENT)
        {
            return "SUFFICIENT";
        }
        else if (flag == LoginModuleControlFlag.OPTIONAL)
        {
            return "OPTIONAL";
        }
        throw new IllegalArgumentException("Unknown flag " + flag);
    }
}
