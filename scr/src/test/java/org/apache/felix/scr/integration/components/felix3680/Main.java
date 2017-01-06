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
package org.apache.felix.scr.integration.components.felix3680;

import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.log.LogService;

public class Main implements Runnable
{
    public static volatile CountDownLatch _enabledLatch;
    public static volatile CountDownLatch _disabledLatch;

    public static volatile CountDownLatch _activatedLatch;
    public static volatile CountDownLatch _deactivatedLatch;

    private volatile ComponentContext _ctx;
    private volatile AtomicInteger _counter = new AtomicInteger();
    private volatile Random _rnd = new Random();
    private volatile LogService _logService;
    private volatile Thread _thread;
    private volatile boolean _running;
    private ServiceComponentRuntime _scr;

    /**
     * Helper used to randomly enable or disable a list of components.
     */
    class EnableManager
    {
        String[] _componentNames;

        EnableManager(String[] componentNames)
        {
            _componentNames = componentNames;
        }

        public void enableComponents(Executor exec)
        {
            enableOrDisable(exec, true);
        }

        public void disableComponents(Executor exec)
        {
            enableOrDisable(exec, false);
        }

        private void enableOrDisable(Executor exec, final boolean enable)
        {
            for (final int i : getRandomIndexes(_componentNames.length))
            {
                exec.execute(new Runnable()
                {
                    public void run()
                    {
                        if (enable)
                        {
                            _logService.log(LogService.LOG_INFO, "enabling component " + _componentNames[i]);
                            _ctx.enableComponent(_componentNames[i]);
                            _enabledLatch.countDown();
                        }
                        else
                        {
                            _logService.log(LogService.LOG_INFO, "disabling component " + _componentNames[i]);
                            _ctx.disableComponent(_componentNames[i]);
                            _disabledLatch.countDown();
                        }
                    }
                });
            }
        }

        private Integer[] getRandomIndexes(int max)
        {
            Set<Integer> set = new LinkedHashSet<Integer>();
            for (int i = 0; i < max; i++)
            {
                int n;
                do
                {
                    n = _rnd.nextInt(max);
                } while (set.contains(n));
                set.add(n);
            }
            for (int i = 0; i < max; i++)
            {
                if (!set.contains(i))
                {
                    throw new IllegalStateException("invalid rnd indexes: " + set);
                }
            }
            return set.toArray(new Integer[set.size()]);
        }
    }

    void bindSCR(ServiceComponentRuntime scr)
    {
        _scr = scr;
    }

    void bindLogService(LogService logService)
    {
        _logService = logService;
    }

    void bindA(ServiceReference sr)
    {
        A a = (A) sr.getBundle().getBundleContext().getService(sr);
        if (a == null)
        {
            throw new IllegalStateException("bindA: bundleContext.getService returned null");
        }
        if (_counter.incrementAndGet() != 1)
        {
            throw new IllegalStateException("bindA: invalid counter value: " + _counter);
        }
        _enabledLatch.countDown();
    }

    void unbindA(ServiceReference sr)
    {
        if (_counter.decrementAndGet() != 0)
        {
            throw new IllegalStateException("unbindA: invalid counter value: " + _counter);
        }
        _disabledLatch.countDown();
    }

    void start(ComponentContext ctx)
    {
        _logService.log(LogService.LOG_INFO, "Main.start");
        _ctx = ctx;
        _running = true;
        _thread = new Thread(this);
        _thread.start();
    }
    
    void stop() 
    {
        _running = false;
        _thread.interrupt();
    }

    public void run()
    {
        Executor exec = Executors.newFixedThreadPool(50);
        _logService.log(LogService.LOG_INFO, "Main.run");
        int loop = 0;
        while (_running)
        {
            _enabledLatch = new CountDownLatch(11); // for B,C,D,E,F,G,H,I,J,K and Main.bindA()
            _disabledLatch = new CountDownLatch(11); // for B,C,D,E,F,G,H,I,J,K and Main.unbindA()
            EnableManager manager =
                    new EnableManager(new String[] { 
                        "org.apache.felix.scr.integration.components.felix3680.B", 
                        "org.apache.felix.scr.integration.components.felix3680.C", 
                        "org.apache.felix.scr.integration.components.felix3680.D", 
                        "org.apache.felix.scr.integration.components.felix3680.E", 
                        "org.apache.felix.scr.integration.components.felix3680.F", 
                        "org.apache.felix.scr.integration.components.felix3680.G", 
                        "org.apache.felix.scr.integration.components.felix3680.H", 
                        "org.apache.felix.scr.integration.components.felix3680.I", 
                        "org.apache.felix.scr.integration.components.felix3680.J", 
                        "org.apache.felix.scr.integration.components.felix3680.K" });
            manager.enableComponents(exec);

            try
            {
                if (!_enabledLatch.await(10000, TimeUnit.MILLISECONDS))
                {
                    System.out.println("Did not get A injected timely ... see logs.txt");
                    _logService.log(LogService.LOG_ERROR, "enableLatch TIMEOUT");
                    dumpComponents();
                    return;
                }
            }
            catch (InterruptedException e)
            {
            }

            manager.disableComponents(exec);
            try
            {
                if (!_disabledLatch.await(10000, TimeUnit.MILLISECONDS))
                {
                    System.out.println("Could not disable components timely ... see logs.txt");
                    _logService.log(LogService.LOG_ERROR, "disableLatch TIMEOUT");
                    dumpComponents();
                    return;
                }
            }
            catch (InterruptedException e)
            {
            }
            
            loop ++;
            if ((loop % 500) == 0) {
                _logService.log(LogService.LOG_WARNING, "Performed " + loop + " tests.");
            }
        }
    }

    private void dumpComponents()
    {
        StringWriter sw = new StringWriter();
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.A");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.B");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.C");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.D");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.E");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.F");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.G");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.H");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.I");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.J");
        dumpState(sw, "org.apache.felix.scr.integration.components.felix3680.K");
        _logService.log(LogService.LOG_WARNING, sw.toString());
    }

    private void dumpState(StringWriter sw, String name)
    {
        ComponentDescriptionDTO c = _scr.getComponentDescriptionDTO(_ctx.getBundleContext().getBundle(), name);
        if ( c != null )
        {
            sw.append( name ).append( "[" ).append( _scr.isComponentEnabled(c)? "enabled":"disabled" ).append( "] " );
        }
    }

}
