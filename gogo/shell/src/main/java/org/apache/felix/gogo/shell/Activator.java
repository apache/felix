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
package org.apache.felix.gogo.shell;

<<<<<<< HEAD
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
=======
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

<<<<<<< HEAD
public class Activator implements BundleActivator, Runnable
{
    private BundleContext context;
    private ServiceTracker commandProcessorTracker;
    private Set<ServiceRegistration> regs = new HashSet<ServiceRegistration>();
    private CommandSession session;
    private Shell shell;
    private Thread thread;

    public void start(final BundleContext ctxt) throws Exception
    {
        context = ctxt;
        commandProcessorTracker = processorTracker();
    }

    public void stop(BundleContext context) throws Exception
    {
        if (thread != null)
        {
            thread.interrupt();
        }

        commandProcessorTracker.close();
        
        Iterator<ServiceRegistration> iterator = regs.iterator();
        while (iterator.hasNext())
        {
            ServiceRegistration reg = iterator.next();
            reg.unregister();
            iterator.remove();
        }
    }

    public void run()
    {
        try
        {
            // wait for gosh command to be registered
            for (int i = 0; (i < 100) && session.get("gogo:gosh") == null; ++i) {
	            Thread.sleep(10);
            }
            
            String args = context.getProperty("gosh.args");
            args = (args == null) ? "" : args;
            session.execute("gosh --login " + args);
        }
        catch (Exception e)
        {
            Object loc = session.get(".location");
            if (null == loc || !loc.toString().contains(":"))
            {
                loc = "gogo";
            }

            System.err.println(loc + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            session.close();
        }
=======
public class Activator implements BundleActivator
{
    private BundleContext context;
    private ServiceTracker commandProcessorTracker;
    private Set<ServiceRegistration> regs;

    private volatile ExecutorService executor;
    private volatile StartShellJob shellJob;

    public Activator()
    {
        regs = new HashSet<>();
    }

    public void start(BundleContext context) {
        this.context = context;
        this.commandProcessorTracker = createCommandProcessorTracker();
        this.commandProcessorTracker.open();
    }

    public void stop(BundleContext context) {
        Set<ServiceRegistration> currentRegs;
        synchronized (regs)
        {
            currentRegs = new HashSet<>(regs);
            regs.clear();
        }

        for (ServiceRegistration reg : currentRegs)
        {
            reg.unregister();
        }

        this.commandProcessorTracker.close();

        stopShell();
    }

    private ServiceTracker<CommandProcessor, CommandProcessor> createCommandProcessorTracker()
    {
        return new ServiceTracker<CommandProcessor, CommandProcessor>(context, CommandProcessor.class, null)
        {
            @Override
            public CommandProcessor addingService(ServiceReference<CommandProcessor> reference)
            {
                CommandProcessor processor = super.addingService(reference);
                startShell(context, processor);
                return processor;
            }

            @Override
            public void removedService(ServiceReference<CommandProcessor> reference, CommandProcessor service)
            {
                stopShell();
                super.removedService(reference, service);
            }
        };
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    private void startShell(BundleContext context, CommandProcessor processor)
    {
<<<<<<< HEAD
        Dictionary<String, Object> dict = new Hashtable<String, Object>();
        dict.put(CommandProcessor.COMMAND_SCOPE, "gogo");

        // register converters
        regs.add(context.registerService(Converter.class.getName(), new Converters(context), null));
        
        // register commands
        
        dict.put(CommandProcessor.COMMAND_FUNCTION, Builtin.functions);
        regs.add(context.registerService(Builtin.class.getName(), new Builtin(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Procedural.functions);
        regs.add(context.registerService(Procedural.class.getName(), new Procedural(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Posix.functions);
        regs.add(context.registerService(Posix.class.getName(), new Posix(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Telnet.functions);
        regs.add(context.registerService(Telnet.class.getName(), new Telnet(processor), dict));
        
        shell = new Shell(context, processor);
        dict.put(CommandProcessor.COMMAND_FUNCTION, Shell.functions);
        regs.add(context.registerService(Shell.class.getName(), shell, dict));
        
        // start shell
        session = processor.createSession(System.in, System.out, System.err);
        thread = new Thread(this, "Gogo shell");
        thread.start();
    }

    private ServiceTracker processorTracker()
    {
        ServiceTracker t = new ServiceTracker(context, CommandProcessor.class.getName(),
            null)
        {
            @Override
            public Object addingService(ServiceReference reference)
            {
                CommandProcessor processor = (CommandProcessor) super.addingService(reference);
                startShell(context, processor);
                return processor;
            }

            @Override
            public void removedService(ServiceReference reference, Object service)
            {
                if (thread != null)
                {
                    thread.interrupt();
                }
                super.removedService(reference, service);
            }
        };

        t.open();
        return t;
=======
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(CommandProcessor.COMMAND_SCOPE, "gogo");

        Set<ServiceRegistration> currentRegs = new HashSet<>();

        // register converters
        currentRegs.add(context.registerService(Converter.class.getName(), new Converters(context.getBundle(0).getBundleContext()), null));

        // register commands

        dict.put(CommandProcessor.COMMAND_FUNCTION, Builtin.functions);
        currentRegs.add(context.registerService(Builtin.class.getName(), new Builtin(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Procedural.functions);
        currentRegs.add(context.registerService(Procedural.class.getName(), new Procedural(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Posix.functions);
        currentRegs.add(context.registerService(Posix.class.getName(), new Posix(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Telnet.functions);
        currentRegs.add(context.registerService(Telnet.class.getName(), new Telnet(processor), dict));

        Shell shell = new Shell(context, processor);
        dict.put(CommandProcessor.COMMAND_FUNCTION, Shell.functions);
        currentRegs.add(context.registerService(Shell.class.getName(), shell, dict));

        synchronized (regs)
        {
            regs.addAll(currentRegs);
            currentRegs.clear();
        }

        // start shell on a separate thread...
        executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Gogo shell"));
        shellJob = new StartShellJob(context, processor);
        executor.submit(shellJob);
    }

    private void stopShell()
    {
        if (executor != null && !(executor.isShutdown() || executor.isTerminated()))
        {
            if (shellJob != null)
            {
                shellJob.terminate();
            }
            executor.shutdown();

            try
            {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS))
                {
                    System.err.println("!!! FAILED TO STOP EXECUTOR !!!");
                    Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
                    for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet())
                    {
                        Thread t = entry.getKey();
                        System.err.printf("Thread: %s (%s): %s\n", t.getName(), t.getState(), Arrays.toString(entry.getValue()));
                    }
                }
            }
            catch (InterruptedException e)
            {
                // Restore administration...
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    private static class StartShellJob implements Runnable
    {
        private final BundleContext context;
        private final CommandProcessor processor;
        private volatile CommandSession session;

        public StartShellJob(BundleContext context, CommandProcessor processor)
        {
            this.context = context;
            this.processor = processor;
        }

        public void run()
        {

            session = processor.createSession(new FileInputStream(FileDescriptor.in),
                                              new FileOutputStream(FileDescriptor.out),
                                              new FileOutputStream(FileDescriptor.err));
            try
            {
                // wait for gosh command to be registered
                for (int i = 0; (i < 100) && session.get("gogo:gosh") == null; ++i)
                {
                    TimeUnit.MILLISECONDS.sleep(10);
                }

                String args = context.getProperty("gosh.args");
                args = (args == null) ? "" : args;
                session.execute("gosh --login " + args);
            }
            catch (InterruptedException e)
            {
                // Ok, back off...
                Thread.currentThread().interrupt();
            }
            catch (Exception e)
            {
                Object loc = session.get(".location");
                if (null == loc || !loc.toString().contains(":"))
                {
                    loc = "gogo";
                }

                System.err.println(loc + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
            finally
            {
                terminate();
            }
        }

        public void terminate()
        {
            if (session != null)
            {
                session.close();
                session = null;
            }
            Thread.currentThread().interrupt();
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}