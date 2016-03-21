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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.jline.Shell.Context;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.jline.builtins.Completers.DirectoriesCompleter;
import org.jline.builtins.Completers.FilesCompleter;
import org.jline.builtins.Options;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.Widget;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

public class JLineCommands {

    public static final String[] functions = {
            "keymap", "setopt", "unsetopt", "complete", "history",
            "less", "watch", "nano", "widget", "tmux",
            "__files", "__directories", "__usage_completion"
    };

    private final CommandProcessor processor;

    private final org.jline.builtins.Commands commands = new org.jline.builtins.Commands();

    public JLineCommands(CommandProcessor processor) {
        this.processor = processor;
    }

    public void tmux(final CommandSession session, String[] argv) throws Exception {
        commands.tmux(Shell.getTerminal(session),
                System.out, System.err,
                () -> session.get(".tmux"),
                t -> session.put(".tmux", t),
                c -> startShell(session, c), argv);
    }

    private void startShell(CommandSession session, Terminal terminal) {
        new Thread(() -> runShell(session, terminal), terminal.getName() + " shell").start();
    }

    private void runShell(CommandSession session, Terminal terminal) {
        InputStream in = terminal.input();
        PrintStream out = new PrintStream(terminal.output());
        CommandSession newSession = processor.createSession(in, out, out);
        newSession.put(Shell.VAR_TERMINAL, terminal);
        newSession.put(".tmux", session.get(".tmux"));
        Context context = new Context() {
            public String getProperty(String name) {
                return System.getProperty(name);
            }
            public void exit() throws Exception {
                terminal.close();
            }
        };
        try {
            new Shell(context, processor, terminal).gosh(newSession, new String[]{"--login"});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                terminal.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void nano(final CommandSession session, String[] argv) throws Exception {
        commands.nano(Shell.getTerminal(session), System.out, System.err, Shell.cwd(session), argv);
    }

    public void watch(final CommandSession session, String[] argv) throws IOException, InterruptedException {
        final String[] usage = {
                "watch - watches & refreshes the output of a command",
                "Usage: watch [OPTIONS] COMMAND",
                "  -? --help                    Show help",
                "  -n --interval                Interval between executions of the command in seconds",
                "  -a --append                  The output should be appended but not clear the console"
        };
        final Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            opt.usage(System.err);
            return;
        }
        List<String> args = opt.args();
        if (args.isEmpty()) {
            System.err.println("Argument expected");
            return;
        }
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final Terminal terminal = Shell.getTerminal(session);
        final CommandProcessor processor = Shell.getProcessor(session);
        try {
            int interval = 1;
            if (opt.isSet("interval")) {
                interval = opt.getNumber("interval");
                if (interval < 1) {
                    interval = 1;
                }
            }
            final String cmd = String.join(" ", args);
            Runnable task = () -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = new ByteArrayInputStream(new byte[0]);
                PrintStream os = new PrintStream(baos);
                if (opt.isSet("append") || !terminal.puts(Capability.clear_screen)) {
                    terminal.writer().println();
                }
                try {
                    CommandSession ns = processor.createSession(is, os, os);
                    Set<String> vars = Shell.getCommands(session);
                    for (String n : vars) {
                        ns.put(n, session.get(n));
                    }
                    ns.execute(cmd);
                } catch (Throwable t) {
                    t.printStackTrace(os);
                }
                os.flush();
                terminal.writer().print(baos.toString());
                terminal.writer().flush();
            };
            executorService.scheduleAtFixedRate(task, 0, interval, TimeUnit.SECONDS);
            Attributes attr = terminal.enterRawMode();
            terminal.reader().read();
            terminal.setAttributes(attr);
        } finally {
            executorService.shutdownNow();
        }
    }

    public void less(CommandSession session, String[] argv) throws IOException, InterruptedException {
        commands.less(Shell.getTerminal(session), System.out, System.err, Shell.cwd(session), argv);
    }

    public void history(CommandSession session, String[] argv) throws IOException {
        commands.history(Shell.getReader(session), System.out, System.err, argv);
    }

    public void complete(CommandSession session, String[] argv) {
        commands.complete(Shell.getReader(session), System.out, System.err, Shell.getCompletions(session), argv);
    }

    public void widget(final CommandSession session, String[] argv) throws Exception {
        java.util.function.Function<String, Widget> creator = func -> () -> {
            try {
                session.execute(func);
            } catch (Exception e) {
                // TODO: log exception ?
                return false;
            }
            return true;
        };
        commands.widget(Shell.getReader(session), System.out, System.err, creator, argv);
    }

    public void keymap(CommandSession session, String[] argv) {
        commands.keymap(Shell.getReader(session), System.out, System.err, argv);
    }

    public void setopt(CommandSession session, String[] argv) {
        commands.setopt(Shell.getReader(session), System.out, System.err, argv);
    }

    public void unsetopt(CommandSession session, String[] argv) {
        commands.unsetopt(Shell.getReader(session), System.out, System.err, argv);
    }

    public List<Candidate> __files(CommandSession session) {
        ParsedLine line = Shell.getParsedLine(session);
        LineReader reader = Shell.getReader(session);
        List<Candidate> candidates = new ArrayList<>();
        new FilesCompleter(new File(Shell.cwd(session))).complete(reader, line, candidates);
        return candidates;
    }

    public List<Candidate> __directories(CommandSession session) {
        ParsedLine line = Shell.getParsedLine(session);
        LineReader reader = Shell.getReader(session);
        List<Candidate> candidates = new ArrayList<>();
        new DirectoriesCompleter(new File(Shell.cwd(session))).complete(reader, line, candidates);
        return candidates;
    }

    public void __usage_completion(CommandSession session, String command) throws Exception {
        Object func = session.get(command.contains(":") ? command : "*:" + command);
        if (func instanceof Function) {
            ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteArrayOutputStream baes = new ByteArrayOutputStream();
            CommandSession ts = ((CommandSessionImpl) session).processor().createSession(bais, new PrintStream(baos), new PrintStream(baes));
            ts.execute(command + " --help");

            String regex = "(?x)\\s*" + "(?:-([^-]))?" +  // 1: short-opt-1
                    "(?:,?\\s*-(\\w))?" +                 // 2: short-opt-2
                    "(?:,?\\s*--(\\w[\\w-]*)(=\\w+)?)?" + // 3: long-opt-1 and 4:arg-1
                    "(?:,?\\s*--(\\w[\\w-]*))?" +         // 5: long-opt-2
                    ".*?(?:\\(default=(.*)\\))?\\s*" +    // 6: default
                    "(.*)";                               // 7: description
            Pattern pattern = Pattern.compile(regex);
            for (String l : baes.toString().split("\n")) {
                Matcher matcher = pattern.matcher(l);
                if (matcher.matches()) {
                    List<String> args = new ArrayList<>();
                    if (matcher.group(1) != null) {
                        args.add("--short-option");
                        args.add(matcher.group(1));
                    }
                    if (matcher.group(3) != null) {
                        args.add("--long-option");
                        args.add(matcher.group(1));
                    }
                    if (matcher.group(4) != null) {
                        args.add("--argument");
                        args.add("");
                    }
                    if (matcher.group(7) != null) {
                        args.add("--description");
                        args.add(matcher.group(7));
                    }
                    complete(session, args.toArray(new String[args.size()]));
                }
            }
        }
    }

}
