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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.felix.gogo.api.Process;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.jline.builtins.Options;

public class Procedural {

    static final String[] functions = {"each", "if", "not", "throw", "try", "until", "while", "break", "continue"};

    public void _main(CommandSession session, Object[] argv) throws Throwable {
        if (argv == null || argv.length < 1) {
            throw new IllegalArgumentException();
        }
        Process process = Process.current();
        try {
            run(session, process, argv);
        } catch (OptionException e) {
            process.err().println(e.getMessage());
            process.error(2);
        } catch (HelpException e) {
            process.err().println(e.getMessage());
            process.error(0);
        } catch (ThrownException e) {
            process.error(1);
            throw e.getCause();
        }
    }

    protected static class OptionException extends Exception {
        public OptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    protected static class HelpException extends Exception {
        public HelpException(String message) {
            super(message);
        }
    }

    protected static class ThrownException extends Exception {
        public ThrownException(Throwable cause) {
            super(cause);
        }
    }

    protected static class BreakException extends Exception {
    }

    protected static class ContinueException extends Exception {
    }

    protected Options parseOptions(CommandSession session, String[] usage, Object[] argv) throws HelpException, OptionException {
        try {
            Options opt = Options.compile(usage, s -> get(session, s)).parse(argv, true);
            if (opt.isSet("help")) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                opt.usage(new PrintStream(baos));
                throw new HelpException(baos.toString());
            }
            return opt;
        } catch (IllegalArgumentException e) {
            throw new OptionException(e.getMessage(), e);
        }
    }

    protected String get(CommandSession session, String name) {
        Object o = session.get(name);
        return o != null ? o.toString() : null;
    }

    protected Object run(CommandSession session, Process process, Object[] argv) throws Throwable {
        switch (argv[0].toString()) {
            case "each":
                return doEach(session, process, argv);
            case "if":
                return doIf(session, process, argv);
            case "not":
                return doNot(session, process, argv);
            case "throw":
                return doThrow(session, process, argv);
            case "try":
                return doTry(session, process, argv);
            case "until":
                return doUntil(session, process, argv);
            case "while":
                return doWhile(session, process, argv);
            case "break":
                return doBreak(session, process, argv);
            case "continue":
                return doContinue(session, process, argv);
            default:
                throw new UnsupportedOperationException();
        }
    }

    protected List<Object> doEach(CommandSession session,
                                  Process process,
                                  Object[] argv) throws Exception {
        String[] usage = {
                "each -  loop over the elements",
                "Usage: each [-r] elements { closure }",
                "         elements              an array to iterate on",
                "         closure               a closure to call",
                "  -? --help                    Show help",
                "  -r --result                  Return a list containing each iteration result",
        };
        Options opt = parseOptions(session, usage, argv);

        Collection<Object> elements = getElements(opt);
        List<Function> functions = getFunctions(opt);

        if (elements == null || functions == null || functions.size() != 1) {
            process.err().println("usage: each elements { closure }");
            process.err().println("       elements: an array to iterate on");
            process.err().println("       closure: a function or closure to call");
            process.error(2);
            return null;
        }

        List<Object> args = new ArrayList<>();
        List<Object> results = new ArrayList<>();
        args.add(null);

        for (Object x : elements) {
            checkInterrupt();
            args.set(0, x);
            try {
                results.add(functions.get(0).execute(session, args));
            } catch (BreakException b) {
                break;
            } catch (ContinueException c) {
                continue;
            }
        }

        return opt.isSet("result") ? results : null;
    }

    protected Object doIf(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "if -  if / then / else construct",
                "Usage: if {condition} {if-action} ... {else-action}",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        List<Function> functions = getFunctions(opt);
        if (functions == null || functions.size() < 2) {
            process.err().println("usage: if {condition} {if-action} ... {else-action}");
            process.error(2);
            return null;
        }
        for (int i = 0, length = functions.size(); i < length; ++i) {
            if (i == length - 1 || isTrue(session, ((Function) opt.argObjects().get(i++)))) {
                return ((Function) opt.argObjects().get(i)).execute(session, null);
            }
        }
        return null;
    }

    protected Boolean doNot(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "not -  return the opposite condition",
                "Usage: not { condition }",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        List<Function> functions = getFunctions(opt);
        if (functions == null || functions.size() != 1) {
            process.err().println("usage: not { condition }");
            process.error(2);
            return null;
        }
        return !isTrue(session, functions.get(0));

    }

    protected Object doThrow(CommandSession session, Process process, Object[] argv) throws ThrownException, HelpException, OptionException {
        String[] usage = {
                "throw -  throw an exception",
                "Usage: throw [ message [ cause ] ]",
                "       throw exception",
                "       throw",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        if (opt.argObjects().size() == 0) {
            Object exception = session.get("exception");
            if (exception instanceof Throwable)
                throw new ThrownException((Throwable) exception);
            else
                throw new ThrownException(new Exception());
        }
        else if (opt.argObjects().size() == 1 && opt.argObjects().get(0) instanceof Throwable) {
            throw new ThrownException((Throwable) opt.argObjects().get(0));
        }
        else {
            String message = opt.argObjects().get(0).toString();
            Throwable cause = null;
            if (opt.argObjects().size() > 1) {
                if (opt.argObjects().get(1) instanceof Throwable) {
                    cause = (Throwable) opt.argObjects().get(1);
                }
            }
            throw new ThrownException(new Exception(message).initCause(cause));
        }
    }

    protected Object doTry(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "try -  try / catch / finally construct",
                "Usage: try { try-action } [ { catch-action } [ { finally-action } ]  ]",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        List<Function> functions = getFunctions(opt);
        if (functions == null || functions.size() < 1 || functions.size() > 3) {
            process.err().println("usage: try { try-action } [ { catch-action } [ { finally-action } ] ]");
            process.error(2);
            return null;
        }
        try {
            return functions.get(0).execute(session, null);
        } catch (BreakException b) {
            throw b;
        } catch (Exception e) {
            session.put("exception", e);
            if (functions.size() > 1) {
                functions.get(1).execute(session, null);
            }
            return null;
        } finally {
            if (functions.size() > 2) {
                functions.get(2).execute(session, null);
            }
        }
    }

    protected Object doWhile(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "while -  while loop",
                "Usage: while { condition } { action }",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        List<Function> functions = getFunctions(opt);
        if (functions == null || functions.size() != 2) {
            process.err().println("usage: while { condition } { action }");
            process.error(2);
            return null;
        }
        while (isTrue(session, functions.get(0))) {
            try {
                functions.get(1).execute(session, null);
            } catch (BreakException b) {
                break;
            } catch (ContinueException c) {
                continue;
            }
        }
        return null;
    }

    protected Object doUntil(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "until -  until loop",
                "Usage: until { condition } { action }",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        List<Function> functions = new ArrayList<>();
        for (Object o : opt.argObjects()) {
            if (o instanceof Function) {
                functions.add((Function) o);
            }
            else {
                functions = null;
                break;
            }
        }
        int length = opt.argObjects().size();
        if (length != 2 || functions == null) {
            process.err().println("usage: until { condition } { action }");
            process.error(2);
            return null;
        }
        while (!isTrue(session, functions.get(0))) {
            try {
                functions.get(1).execute(session, null);
            } catch (BreakException e) {
                break;
            } catch (ContinueException c) {
                continue;
            }
        }
        return null;
    }

    protected Object doBreak(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "break -  break from loop",
                "Usage: break",
                "  -? --help                    Show help",
        };
        parseOptions(session, usage, argv);
        throw new BreakException();
    }

    protected Object doContinue(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "continue -  continue loop",
                "Usage: continue",
                "  -? --help                    Show help",
        };
        parseOptions(session, usage, argv);
        throw new ContinueException();
    }

    private boolean isTrue(CommandSession session, Function function) throws Exception {
        checkInterrupt();
        return isTrue(function.execute(session, null));
    }

    private boolean isTrue(Object result) throws InterruptedException {
        checkInterrupt();

        if (result == null)
            return false;

        if (result instanceof Boolean)
            return (Boolean) result;

        if (result instanceof Number) {
            if (0 == ((Number) result).intValue())
                return false;
        }

        if ("".equals(result))
            return false;

        if ("0".equals(result))
            return false;

        return true;
    }

    private void checkInterrupt() throws InterruptedException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("interrupted");
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> getElements(Options opt) {
        Collection<Object> elements = null;
        if (opt.argObjects().size() > 0) {
            Object o = opt.argObjects().remove(0);
            if (o instanceof Collection) {
                elements = (Collection<Object>) o;
            } else if (o != null && o.getClass().isArray()) {
                elements = Arrays.asList((Object[]) o);
            }
        }
        return elements;
    }

    private List<Function> getFunctions(Options opt) {
        List<Function> functions = new ArrayList<>();
        for (Object o : opt.argObjects()) {
            if (o instanceof Function) {
                functions.add((Function) o);
            }
            else {
                functions = null;
                break;
            }
        }
        return functions;
    }

}
