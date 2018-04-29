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
package org.apache.felix.http.base.internal;

<<<<<<< HEAD
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.logger.LogServiceLogger;
=======
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

public abstract class AbstractActivator
    implements BundleActivator
{
<<<<<<< HEAD
    private BundleContext context;
    private LogServiceLogger logger;
=======
    private volatile BundleContext context;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    protected final BundleContext getBundleContext()
    {
        return this.context;
    }

<<<<<<< HEAD
    public final void start(BundleContext context)
        throws Exception
    {
        this.context = context;
        this.logger = new LogServiceLogger(context);
        SystemLogger.setLogService(this.logger);
        doStart();
    }

    public final void stop(BundleContext context)
        throws Exception
    {
        doStop();
        this.logger.close();
=======
    @Override
    public final void start(final BundleContext context)
        throws Exception
    {
        this.context = context;
        SystemLogger.init(context);
        doStart();
    }

    @Override
    public final void stop(final BundleContext context)
        throws Exception
    {
        doStop();
        SystemLogger.destroy();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    protected abstract void doStart()
        throws Exception;

    protected abstract void doStop()
<<<<<<< HEAD
        throws Exception;   
=======
        throws Exception;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
