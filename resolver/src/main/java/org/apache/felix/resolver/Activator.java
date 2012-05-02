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
package org.apache.felix.resolver;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.resolver.Resolver;

public class Activator implements BundleActivator
{
    public static final String LOG_LEVEL = "felix.resolver.log.level";

    public void start(BundleContext bc) throws Exception
    {
        int logLevel = 4;
        if (bc.getProperty(LOG_LEVEL) != null)
        {
            try
            {
                logLevel = Integer.parseInt(bc.getProperty(LOG_LEVEL));
            }
            catch (NumberFormatException ex)
            {
                // Use default log level.
            }
        }
        bc.registerService(
            Resolver.class,
            new ResolverImpl(new Logger(logLevel)),
            null);
    }

    public void stop(BundleContext bc) throws Exception
    {
    }
}