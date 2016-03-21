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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.gogo.jline.Shell.Context;
import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.command.CommandSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Main {

    public static void main(String[] args) throws IOException {
        InputStream sin = System.in;
        PrintStream sout = System.out;
        PrintStream serr = System.err;

        try (Terminal terminal = TerminalBuilder.builder()
                .name("gogo")
                .type(System.getenv("TERM"))
                .system(true)
                .streams(sin, sout)
                .build()) {
            ThreadIOImpl tio = new ThreadIOImpl();
            tio.start();
            try {
                CommandProcessorImpl processor = new CommandProcessorImpl(tio);
                Context context = new MyContext();
                Shell shell = new Shell(context, processor);
                processor.addCommand("gogo", processor, "addCommand");
                processor.addCommand("gogo", processor, "removeCommand");
                processor.addCommand("gogo", processor, "eval");
                register(processor, new Builtin(), Builtin.functions);
                register(processor, new Procedural(), Procedural.functions);
                register(processor, new Posix(processor), Posix.functions);
                register(processor, shell, Shell.functions);
                /*
                try {
                    register(processor, new Telnet(processor), Telnet.functions);
                } catch (Throwable t) {
                    // ignore
                }
                try {
                    register(processor, new Ssh(processor), Ssh.functions);
                } catch (Throwable t) {
                    // ignore
                }
                */
                CommandSession session = processor.createSession(terminal.input(), terminal.output(), terminal.output());
                session.put(Shell.VAR_CONTEXT, context);
                session.put(Shell.VAR_TERMINAL, terminal);
                try {
                    String[] argv = new String[args.length + 1];
                    argv[0] = "--login";
                    System.arraycopy(args, 0, argv, 1, args.length);
                    shell.gosh(session, argv);
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
            } finally {
                tio.stop();
            }
        }
    }

    static void register(CommandProcessorImpl processor, Object target, String[] functions) {
        for (String function : functions) {
            processor.addCommand("gogo", target, function);
        }
    }

    private static class MyContext implements Context {

        public String getProperty(String name) {
            return System.getProperty(name);
        }

        public void exit() throws Exception {
            System.exit(0);
        }
    }
}
