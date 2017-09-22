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
package org.apache.felix.webconsole.plugins.ds.internal;

import java.io.IOException;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigurationAdminSupport
{

    public boolean check(final Object obj, final String pid)
    {
        final ConfigurationAdmin ca = (ConfigurationAdmin)obj;
        try
        {
            // we use listConfigurations to not create configuration
            // objects persistently without the user providing actual
            // configuration
            String filter = '(' + Constants.SERVICE_PID + '=' + pid + ')';
            Configuration[] configs = ca.listConfigurations(filter);
            if (configs != null && configs.length > 0)
            {
                return true;
            }
        }
        catch (InvalidSyntaxException ise)
        {
            // should print message
        }
        catch (IOException ioe)
        {
            // should print message
        }
        return false;
    }
}
