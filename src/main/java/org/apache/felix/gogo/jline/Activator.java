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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.jline.Shell.Context;
import org.apache.felix.gogo.jline.SingleServiceTracker.SingleServiceListener;
import org.apache.felix.gogo.runtime.Token;
import org.apache.felix.gogo.runtime.Tokenizer;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator, SingleServiceListener {
    private final Set<ServiceRegistration<?>> regs = new HashSet<>();
    private BundleContext context;
    private SingleServiceTracker<CommandProcessor> commandProcessorTracker;
    private Thread thread;

    public Activator() {
    }

    public void start(BundleContext context) throws Exception {
        this.context = context;
        this.commandProcessorTracker = new SingleServiceTracker<>(context, CommandProcessor.class, this);
        this.commandProcessorTracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        Iterator<ServiceRegistration<?>> iterator = regs.iterator();
        while (iterator.hasNext()) {
            ServiceRegistration reg = iterator.next();
            reg.unregister();
            iterator.remove();
        }
        this.commandProcessorTracker.close();
    }

    @Override
    public void serviceFound() {
        startShell(context, commandProcessorTracker.getService());
    }

    @Override
    public void serviceLost() {
        stopShell();
    }

    @Override
    public void serviceReplaced() {
        serviceLost();
        serviceFound();
    }

    private void startShell(BundleContext context, CommandProcessor processor) {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(CommandProcessor.COMMAND_SCOPE, "gogo");

        // register converters
        regs.add(context.registerService(Converter.class.getName(), new Converters(context.getBundle(0).getBundleContext()), null));

        // register commands

        dict.put(CommandProcessor.COMMAND_FUNCTION, Builtin.functions);
        regs.add(context.registerService(Builtin.class.getName(), new Builtin(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Procedural.functions);
        regs.add(context.registerService(Procedural.class.getName(), new Procedural(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Posix.functions);
        regs.add(context.registerService(Posix.class.getName(), new Posix(processor), dict));

        Shell shell = new Shell(new ShellContext(), processor);
        dict.put(CommandProcessor.COMMAND_FUNCTION, Shell.functions);
        regs.add(context.registerService(Shell.class.getName(), shell, dict));

        // start shell on a separate thread...
        thread = new Thread(() -> doStartShell(processor, shell), "Gogo shell");
        thread.start();
    }

    private void stopShell() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(5000);
                if (thread.isAlive()) {
                    System.err.println("!!! FAILED TO STOP EXECUTOR !!!");
                }
            } catch (InterruptedException e) {
                // Restore administration...
                Thread.currentThread().interrupt();
            }
        }
        while (!regs.isEmpty()) {
            ServiceRegistration<?> reg = regs.iterator().next();
            regs.remove(reg);
            reg.unregister();
        }
    }

    private void doStartShell(CommandProcessor processor, Shell shell) {
        String errorMessage = "gogo: unable to create console";
        try (Terminal terminal = TerminalBuilder.terminal();
             CommandSession session = processor.createSession(terminal.input(), terminal.output(), terminal.output())) {
            session.put(Shell.VAR_TERMINAL, terminal);
            try {
                List<String> args = new ArrayList<>();
                args.add("--login");
                String argstr = shell.getContext().getProperty("gosh.args");
                if (argstr != null) {
                    Tokenizer tokenizer = new Tokenizer(argstr);
                    Token token;
                    while ((token = tokenizer.next()) != null) {
                        args.add(token.toString());
                    }
                }
                shell.gosh(session, args.toArray(new String[args.size()]));
            } catch (Exception e) {
                Object loc = session.get(".location");
                if (null == loc || !loc.toString().contains(":")) {
                    loc = "gogo";
                }
                errorMessage = loc.toString();
                throw e;
            }
        } catch (Exception e) {
            System.err.println(errorMessage + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
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