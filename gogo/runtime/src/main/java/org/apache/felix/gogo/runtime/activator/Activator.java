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
package org.apache.felix.gogo.runtime.activator;

import java.util.ArrayList;
<<<<<<< HEAD
import java.util.List;

import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.gogo.api.CommandSessionListener;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
=======
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.command.CommandSessionListener;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.threadio.ThreadIO;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
<<<<<<< HEAD
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator
{
    protected CommandProcessorImpl processor;
    private ThreadIOImpl threadio;
    private ServiceTracker commandTracker;
    private ServiceTracker converterTracker;
    private ServiceTracker listenerTracker;
    private ServiceRegistration processorRegistration;
    private ServiceRegistration threadioRegistration;
    
    public static final String CONTEXT = ".context";

    protected ServiceRegistration newProcessor(ThreadIO tio, BundleContext context)
    {
        processor = new CommandProcessorImpl(tio);
        try
        {
            processor.addListener(new EventAdminListener(context));
        }
        catch (NoClassDefFoundError error)
        {
            // Ignore the listener if EventAdmin package isn't present
        }

        // Setup the variables and commands exposed in an OSGi environment.
        processor.addConstant(CONTEXT, context);
        processor.addCommand("osgi", processor, "addCommand");
        processor.addCommand("osgi", processor, "removeCommand");
        processor.addCommand("osgi", processor, "eval");

        return context.registerService(CommandProcessor.class.getName(), processor, null);
    }

    public void start(final BundleContext context) throws Exception
    {
        threadio = new ThreadIOImpl();
        threadio.start();
<<<<<<< HEAD
        threadioRegistration = context.registerService(ThreadIO.class.getName(),
            threadio, null);
=======
        threadioRegistration = context.registerService(ThreadIO.class.getName(), threadio, null);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        processorRegistration = newProcessor(threadio, context);
        
        commandTracker = trackOSGiCommands(context);
        commandTracker.open();

<<<<<<< HEAD
        converterTracker = new ServiceTracker(context, Converter.class.getName(), null)
        {
            @Override
            public Object addingService(ServiceReference reference)
            {
                Converter converter = (Converter) super.addingService(reference);
=======
        converterTracker = new ServiceTracker<Converter, Converter>(context, Converter.class, null)
        {
            @Override
            public Converter addingService(ServiceReference<Converter> reference)
            {
                Converter converter = super.addingService(reference);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                processor.addConverter(converter);
                return converter;
            }

            @Override
<<<<<<< HEAD
            public void removedService(ServiceReference reference, Object service)
            {
                processor.removeConverter((Converter) service);
=======
            public void removedService(ServiceReference<Converter> reference, Converter service)
            {
                processor.removeConverter(service);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                super.removedService(reference, service);
            }
        };
        converterTracker.open();

<<<<<<< HEAD
        listenerTracker = new ServiceTracker(context, CommandSessionListener.class.getName(), null)
        {
            @Override
            public Object addingService(ServiceReference reference) {
                CommandSessionListener listener = (CommandSessionListener) super.addingService(reference);
=======
        listenerTracker = new ServiceTracker<CommandSessionListener, CommandSessionListener>(context, CommandSessionListener.class.getName(), null)
        {
            @Override
            public CommandSessionListener addingService(ServiceReference<CommandSessionListener> reference)
            {
                CommandSessionListener listener = super.addingService(reference);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                processor.addListener(listener);
                return listener;
            }

            @Override
<<<<<<< HEAD
            public void removedService(ServiceReference reference, Object service) {
                processor.removeListener((CommandSessionListener) service);
=======
            public void removedService(ServiceReference<CommandSessionListener> reference, CommandSessionListener service)
            {
                processor.removeListener(service);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                super.removedService(reference, service);
            }
        };
        listenerTracker.open();
    }

<<<<<<< HEAD
    public void stop(BundleContext context) throws Exception
    {
=======
    public void stop(BundleContext context) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        processorRegistration.unregister();
        threadioRegistration.unregister();
        commandTracker.close();
        converterTracker.close();
        listenerTracker.close();
        threadio.stop();
        processor.stop();
    }

    private ServiceTracker trackOSGiCommands(final BundleContext context)
        throws InvalidSyntaxException
    {
        Filter filter = context.createFilter(String.format("(&(%s=*)(%s=*))",
            CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION));

<<<<<<< HEAD
        return new ServiceTracker(context, filter, null)
        {
            @Override
            public Object addingService(ServiceReference reference)
            {
                Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
                Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
                List<Object> commands = new ArrayList<Object>();

                if (scope != null && function != null)
                {
=======
        return new ServiceTracker<Object, List<Object>>(context, filter, null)
        {
            private final ConcurrentMap<ServiceReference<Object>, Map<String, CommandProxy>> proxies
                    = new ConcurrentHashMap<>();

            @Override
            public List<Object> addingService(ServiceReference<Object> reference)
            {
                Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
                Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
                Object ranking = reference.getProperty(Constants.SERVICE_RANKING);
                List<Object> commands = new ArrayList<>();

                int rank = 0;
                if (ranking != null)
                {
                    try
                    {
                        rank = Integer.parseInt(ranking.toString());
                    }
                    catch (NumberFormatException e)
                    {
                        // Ignore
                    }
                }
                if (scope != null && function != null)
                {
                    Map<String, CommandProxy> proxyMap = new HashMap<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    if (function.getClass().isArray())
                    {
                        for (Object f : ((Object[]) function))
                        {
<<<<<<< HEAD
                            Function target = new CommandProxy(context, reference,
                                f.toString());
                            processor.addCommand(scope.toString(), target, f.toString());
=======
                            CommandProxy target = new CommandProxy(context, reference, f.toString());
                            proxyMap.put(f.toString(), target);
                            processor.addCommand(scope.toString(), target, f.toString(), rank);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                            commands.add(target);
                        }
                    }
                    else
                    {
<<<<<<< HEAD
                        Function target = new CommandProxy(context, reference,
                            function.toString());
                        processor.addCommand(scope.toString(), target, function.toString());
                        commands.add(target);
                    }
=======
                        CommandProxy target = new CommandProxy(context, reference, function.toString());
                        proxyMap.put(function.toString(), target);
                        processor.addCommand(scope.toString(), target, function.toString(), rank);
                        commands.add(target);
                    }
                    proxies.put(reference, proxyMap);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    return commands;
                }
                return null;
            }

            @Override
<<<<<<< HEAD
            public void removedService(ServiceReference reference, Object service)
=======
            public void removedService(ServiceReference<Object> reference, List<Object> service)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            {
                Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
                Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);

                if (scope != null && function != null)
                {
<<<<<<< HEAD
                    if (!function.getClass().isArray())
                    {
                        processor.removeCommand(scope.toString(), function.toString());
                    }
                    else
                    {
                        for (Object func : (Object[]) function)
                        {
                            processor.removeCommand(scope.toString(), func.toString());
                        }
=======
                    Map<String, CommandProxy> proxyMap = proxies.remove(reference);
                    for (Map.Entry<String, CommandProxy> entry : proxyMap.entrySet())
                    {
                        processor.removeCommand(scope.toString(), entry.getKey(), entry.getValue());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    }
                }

                super.removedService(reference, service);
            }
        };
    }

}
