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
package org.apache.felix.gogo.jline;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.jline.Shell.Context;
import org.apache.felix.gogo.jline.telnet.Telnet;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
    private BundleContext context;
    private ServiceTracker commandProcessorTracker;
    private Set<ServiceRegistration> regs;

    private ExecutorService executor;

    public Activator() {
        regs = new HashSet<ServiceRegistration>();
    }

    public void start(BundleContext context) throws Exception {
        this.context = context;
        this.commandProcessorTracker = createCommandProcessorTracker();
        this.commandProcessorTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        Iterator<ServiceRegistration> iterator = regs.iterator();
        while (iterator.hasNext()) {
            ServiceRegistration reg = iterator.next();
            reg.unregister();
            iterator.remove();
        }

        stopShell();

        this.commandProcessorTracker.close();
    }

    private ServiceTracker createCommandProcessorTracker() {
        return new ServiceTracker(context, CommandProcessor.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                CommandProcessor processor = (CommandProcessor) super.addingService(reference);
                startShell(context, processor);
                return processor;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                stopShell();
                super.removedService(reference, service);
            }
        };
    }

    private void startShell(final BundleContext context, CommandProcessor processor) {
        Dictionary<String, Object> dict = new Hashtable<String, Object>();
        dict.put(CommandProcessor.COMMAND_SCOPE, "gogo");

        // register converters
        regs.add(context.registerService(Converter.class.getName(), new Converters(context.getBundle(0).getBundleContext()), null));

        // register commands

        dict.put(CommandProcessor.COMMAND_FUNCTION, Builtin.functions);
        regs.add(context.registerService(Builtin.class.getName(), new Builtin(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Procedural.functions);
        regs.add(context.registerService(Procedural.class.getName(), new Procedural(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Posix.functions);
        regs.add(context.registerService(Posix.class.getName(), new Posix(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Telnet.functions);
        regs.add(context.registerService(Telnet.class.getName(), new Telnet(processor), dict));

        Shell shell = new Shell(new ShellContext(), processor, null);
        dict.put(CommandProcessor.COMMAND_FUNCTION, Shell.functions);
        regs.add(context.registerService(Shell.class.getName(), shell, dict));

        // start shell on a separate thread...
        executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "Gogo shell");
            }
        });
        executor.submit(new StartShellJob(context, processor));
    }

    private void stopShell() {
        if (executor != null && !(executor.isShutdown() || executor.isTerminated())) {
            executor.shutdownNow();

            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("!!! FAILED TO STOP EXECUTOR !!!");
                }
            } catch (InterruptedException e) {
                // Restore administration...
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    private static class StartShellJob implements Runnable {
        private final BundleContext context;
        private final CommandProcessor processor;

        public StartShellJob(BundleContext context, CommandProcessor processor) {
            this.context = context;
            this.processor = processor;
        }

        public void run() {
            CommandSession session = processor.createSession(System.in, System.out, System.err);
            try {
                // wait for gosh command to be registered
                for (int i = 0; (i < 100) && session.get("gogo:gosh") == null; ++i) {
                    TimeUnit.MILLISECONDS.sleep(10);
                }

                String args = context.getProperty("gosh.args");
                args = (args == null) ? "" : args;
                session.execute("gosh --login " + args);
            } catch (Exception e) {
                Object loc = session.get(".location");
                if (null == loc || !loc.toString().contains(":")) {
                    loc = "gogo";
                }

                System.err.println(loc + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                session.close();
            }
        }
    }

    private class ShellContext implements Context {
        public String getProperty(String name) {
            return context.getProperty(name);
        }

        public void exit() throws Exception {
            context.getBundle(0).stop();
        }
    }
}