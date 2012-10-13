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

import org.apache.felix.scr.ScrService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

public class Main implements Runnable
{
    public static volatile CountDownLatch _enabledLatch;
    public static volatile CountDownLatch _disabledLatch;

    private volatile ComponentContext _ctx;
    private volatile AtomicInteger _counter = new AtomicInteger();
    private volatile Random _rnd = new Random();
    private volatile LogService _logService;
    private ScrService _scr;
    private volatile Thread _thread;
    private volatile boolean _running;

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
                            //_logService.log(LogService.LOG_INFO, "enabling component " + _componentNames[i]);
                            _ctx.enableComponent(_componentNames[i]);
                        }
                        else
                        {
                            //_logService.log(LogService.LOG_INFO, "disabling component " + _componentNames[i]);
                            _ctx.disableComponent(_componentNames[i]);
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

    void bindSCR(ScrService scr)
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
                    new EnableManager(new String[] { "B", "C", "D", "E", "F", "G", "H", "I", "J", "K" });
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
        dumpState(sw, "A");
        dumpState(sw, "B");
        dumpState(sw, "C");
        dumpState(sw, "D");
        dumpState(sw, "E");
        dumpState(sw, "F");
        dumpState(sw, "G");
        dumpState(sw, "H");
        dumpState(sw, "I");
        dumpState(sw, "J");
        dumpState(sw, "K");
        _logService.log(LogService.LOG_WARNING, sw.toString());
    }

    private void dumpState(StringWriter sw, String name)
    {
        org.apache.felix.scr.Component a = _scr.getComponents(name)[0];
        sw.append(name).append("[").append(getState(a)).append("] ");
    }

    private CharSequence getState(org.apache.felix.scr.Component c)
    {
        switch (c.getState()) {
        case org.apache.felix.scr.Component.STATE_ACTIVATING:
            return "activating";
        case org.apache.felix.scr.Component.STATE_ACTIVE:
            return "active";
        case org.apache.felix.scr.Component.STATE_DEACTIVATING:
            return "deactivating";
        case org.apache.felix.scr.Component.STATE_DISABLED:
            return "disabled";
        case org.apache.felix.scr.Component.STATE_DISABLING:
            return "disabling";
        case org.apache.felix.scr.Component.STATE_DISPOSED:
            return "disposed";
        case org.apache.felix.scr.Component.STATE_DISPOSING:
            return "disposing";
        case org.apache.felix.scr.Component.STATE_ENABLED:
            return "enabled";
        case org.apache.felix.scr.Component.STATE_ENABLING:
            return "enabling";
        case org.apache.felix.scr.Component.STATE_FACTORY:
            return "factory";
        case org.apache.felix.scr.Component.STATE_REGISTERED:
            return "registered";
        case org.apache.felix.scr.Component.STATE_UNSATISFIED:
            return "unsatisfied";
        default:
            return "?";
        }
    }
}
