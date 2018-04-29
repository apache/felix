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
package org.apache.felix.gogo.runtime;

<<<<<<< HEAD
import java.io.EOFException;
import java.util.AbstractList;
import java.util.ArrayList;
=======
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

<<<<<<< HEAD
import org.apache.felix.gogo.runtime.Tokenizer.Type;
=======
import org.apache.felix.service.command.Job.Status;
import org.apache.felix.gogo.runtime.Parser.Array;
import org.apache.felix.gogo.runtime.Parser.Executable;
import org.apache.felix.gogo.runtime.Parser.Operator;
import org.apache.felix.gogo.runtime.Parser.Pipeline;
import org.apache.felix.gogo.runtime.Parser.Program;
import org.apache.felix.gogo.runtime.Parser.Sequence;
import org.apache.felix.gogo.runtime.Parser.Statement;
import org.apache.felix.gogo.runtime.Pipe.Result;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;

public class Closure implements Function, Evaluate
{
<<<<<<< HEAD
    public static final String LOCATION = ".location";
    private static final String DEFAULT_LOCK = ".defaultLock";

    private static final long serialVersionUID = 1L;
    private static final ThreadLocal<String> location = new ThreadLocal<String>();
=======

    public static final String LOCATION = ".location";
    public static final String PIPE_EXCEPTION = "pipe-exception";
    private static final String DEFAULT_LOCK = ".defaultLock";

    private static final ThreadLocal<String> location = new ThreadLocal<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    private final CommandSessionImpl session;
    private final Closure parent;
    private final CharSequence source;
<<<<<<< HEAD
    private final List<List<List<Token>>> program;
=======
    private final Program program;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    private final Object script;

    private Token errTok;
    private Token errTok2;
    private List<Object> parms = null;
    private List<Object> parmv = null;

    public Closure(CommandSessionImpl session, Closure parent, CharSequence source) throws Exception
    {
        this.session = session;
        this.parent = parent;
        this.source = source;
<<<<<<< HEAD
        script = session.get("0"); // by convention, $0 is script name

        try
        {
            program = new Parser(source).program();
        }
        catch (Exception e)
        {
            throw setLocation(e);
        }
    }

=======
        this.script = session.get("0"); // by convention, $0 is script name

        if (source instanceof Program)
        {
            program = (Program) source;
        }
        else
        {
            try
            {
                this.program = new Parser(source).program();
            }
            catch (Exception e)
            {
                throw setLocation(e);
            }
        }
    }

    public Closure(CommandSessionImpl session, Closure parent, Program program)
    {
        this.session = session;
        this.parent = parent;
        this.source = program;
        this.script = session.get("0"); // by convention, $0 is script name
        this.program = program;
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public CommandSessionImpl session()
    {
        return session;
    }

    private Exception setLocation(Exception e)
    {
        if (session.get(DEFAULT_LOCK) == null)
        {
            String loc = location.get();
            if (null == loc)
            {
                loc = (null == script ? "" : script + ":");

                if (e instanceof SyntaxError)
                {
                    SyntaxError se = (SyntaxError) e;
                    loc += se.line() + "." + se.column();
                }
                else if (null != errTok)
                {
                    loc += errTok.line + "." + errTok.column;
                }

                location.set(loc);
            }
            else if (null != script && !loc.contains(":"))
            {
                location.set(script + ":" + loc);
            }

            session.put(LOCATION, location.get());
        }

        if (e instanceof EOFError)
        { // map to public exception, so interactive clients can provide more input
            EOFException eofe = new EOFException(e.getMessage());
            eofe.initCause(e);
            return eofe;
        }

        return e;
    }

    // implements Function interface
    public Object execute(CommandSession x, List<Object> values) throws Exception
    {
<<<<<<< HEAD
        try
        {
            location.remove();
            session.variables.remove(LOCATION);
            return execute(values);
=======
        return execute(x, values, null);
    }

    public Object execute(CommandSession x, List<Object> values, Channel capturingOutput) throws Exception
    {
        if (x != null && x != session)
        {
            if (x instanceof CommandSessionImpl)
            {
                return new Closure((CommandSessionImpl) x, null, program).execute(x, values, capturingOutput);
            }
            else
            {
                throw new IllegalStateException("The current session is not a Gogo session");
            }
        }
        try
        {
            location.remove();
            session.put(LOCATION, null);
            return execute(values, capturingOutput);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        catch (Exception e)
        {
            throw setLocation(e);
        }
    }

    @SuppressWarnings("unchecked")
<<<<<<< HEAD
    private Object execute(List<Object> values) throws Exception
    {
        if (null != values)
        {
            parmv = values;
=======
    private Object execute(List<Object> values, Channel capturingOutput) throws Exception
    {
        if (null != values)
        {
            parmv = new ArrayList<>(values);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            parms = new ArgList(parmv);
        }
        else if (null != parent)
        {
            // inherit parent closure parameters
<<<<<<< HEAD
            parms = parent.parms;
            parmv = parent.parmv;
=======
            parmv = parent.parmv != null ? new ArrayList<>(parent.parmv) : null;
            parms = parmv != null ? new ArgList(parmv) : null;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        else
        {
            // inherit session parameters
            Object args = session.get("args");
            if (null != args && args instanceof List<?>)
            {
<<<<<<< HEAD
                parmv = (List<Object>) args;
=======
                parmv = new ArrayList<>((List<Object>) args);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                parms = new ArgList(parmv);
            }
        }

<<<<<<< HEAD
        Pipe last = null;
        Object[] mark = Pipe.mark();

        for (List<List<Token>> pipeline : program)
        {
            ArrayList<Pipe> pipes = new ArrayList<Pipe>();

            for (List<Token> statement : pipeline)
            {
                Pipe current = new Pipe(this, statement);

                if (pipes.isEmpty())
                {
                    if (current.out == null)
                    {
                        current.setIn(session.in);
                        current.setOut(session.out);
                        current.setErr(session.err);
                    }
                }
                else
                {
                    Pipe previous = pipes.get(pipes.size() - 1);
                    previous.connect(current);
                }
                pipes.add(current);
            }

            if (pipes.size() == 1)
            {
                pipes.get(0).run();
            }
            else if (pipes.size() > 1)
            {
                for (Pipe pipe : pipes)
                {
                    pipe.start();
                }
                try
                {
                    for (Pipe pipe : pipes)
                    {
                        pipe.join();
                    }
                }
                catch (InterruptedException e)
                {
                    for (Pipe pipe : pipes)
                    {
                        pipe.interrupt();
                    }
                    throw e;
                }
            }

            last = pipes.remove(pipes.size() - 1);

            for (Pipe pipe : pipes)
            {
                if (pipe.exception != null)
                {
                    // can't throw exception, as result is defined by last pipe
                    Object oloc = session.get(LOCATION);
                    String loc = (String.valueOf(oloc).contains(":") ? oloc + ": "
                        : "pipe: ");
                    session.err.println(loc + pipe.exception);
                    session.put("pipe-exception", pipe.exception);
                }
            }

            if (last.exception != null)
            {
                Pipe.reset(mark);
                throw last.exception;
            }
        }

        Pipe.reset(mark); // reset IO in case same thread used for new client

        return last == null ? null : last.result;
    }

    private Object eval(Object v)
=======
        Result last = null;
        Operator operator = null;
        for (Iterator<Executable> iterator = program.tokens().iterator(); iterator.hasNext();)
        {
            Operator prevOperator = operator;
            Executable executable = iterator.next();
            if (iterator.hasNext()) {
                operator = (Operator) iterator.next();
            } else {
                operator = null;
            }
            if (prevOperator != null) {
                if (Token.eq("&&", prevOperator)) {
                    if (!last.isSuccess()) {
                        continue;
                    }
                }
                else if (Token.eq("||", prevOperator)) {
                    if (last.isSuccess()) {
                        continue;
                    }
                }
            }

            Channel[] streams;
            boolean[] toclose = new boolean[10];
            if (Pipe.getCurrentPipe() != null) {
                streams = Pipe.getCurrentPipe().streams.clone();
            } else {
                streams = new Channel[10];
                streams[0] = session.channels[0];
                streams[1] = new WritableByteChannelImpl((WritableByteChannel) session.channels[1]);
                streams[2] = session.channels[1] == session.channels[2]
                                ? streams[1]
                                : new WritableByteChannelImpl((WritableByteChannel) session.channels[2]);
            }
            if (capturingOutput != null) {
                streams[1] = capturingOutput;
                toclose[1] = true;
            }

            CommandSessionImpl.JobImpl job;
            if (executable instanceof Pipeline) {
                Pipeline pipeline = (Pipeline) executable;
                List<Executable> exec = pipeline.tokens();
                Token s = exec.get(0);
                Token e = exec.get(exec.size() - 1);
                Token t = program.subSequence(s.start - program.start, e.start + e.length - program.start);
                job = session().createJob(t);
                for (int i = 0; i < exec.size(); i++) {
                    Statement ex = (Statement) exec.get(i);
                    Operator op = i < exec.size() - 1 ? (Operator) exec.get(++i) : null;
                    Channel[] nstreams;
                    boolean[] ntoclose;
                    boolean endOfPipe;
                    if (i == exec.size() - 1) {
                        nstreams = streams;
                        ntoclose = toclose;
                        endOfPipe = true;
                    } else if (Token.eq("|", op)) {
                        PipedInputStream pis = new PipedInputStream();
                        PipedOutputStream pos = new PipedOutputStream(pis);
                        nstreams = streams.clone();
                        nstreams[1] = Channels.newChannel(pos);
                        ntoclose = toclose.clone();
                        ntoclose[1] = true;
                        streams[0] = Channels.newChannel(pis);
                        toclose[0] = true;
                        endOfPipe = false;
                    } else if (Token.eq("|&", op)) {
                        PipedInputStream pis = new PipedInputStream();
                        PipedOutputStream pos = new PipedOutputStream(pis);
                        nstreams = streams.clone();
                        nstreams[1] = nstreams[2] = Channels.newChannel(pos);
                        ntoclose = toclose.clone();
                        ntoclose[1] = ntoclose[2] = true;
                        streams[0] = Channels.newChannel(pis);
                        toclose[0] = true;
                        endOfPipe = false;
                    } else {
                        throw new IllegalStateException("Unrecognized pipe operator: '" + op + "'");
                    }
                    Pipe pipe = new Pipe(this, job, ex, nstreams, ntoclose, endOfPipe);
                    job.addPipe(pipe);
                }
            } else {
                job = session().createJob(executable);
                Pipe pipe = new Pipe(this, job, (Statement) executable, streams, toclose, true);
                job.addPipe(pipe);
            }

            // Start pipe in background
            if (operator != null && Token.eq("&", operator)) {
                job.start(Status.Background);
                last = new Result((Object) null);
            }
            // Start in foreground and wait for results
            else {
                last = job.start(Status.Foreground);
                if (last == null) {
                    last = new Result((Object) null);
                } else if (last.exception != null) {
                    throw last.exception;
                }
            }
        }

        return last == null ? null : last.result;
    }

    private static class WritableByteChannelImpl extends AbstractInterruptibleChannel
            implements WritableByteChannel {
        private final WritableByteChannel out;

        WritableByteChannelImpl(WritableByteChannel out) {
            this.out = out;
        }

        public int write(ByteBuffer src) throws IOException {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            begin();
            try {
                return out.write(src);
            } finally {
                end(true);
            }
       }

        protected void implCloseChannel() {
        }
    }

    static Object eval(Object v)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        String s = v.toString();
        if ("null".equals(s))
        {
            v = null;
        }
        else if ("false".equals(s))
        {
            v = false;
        }
        else if ("true".equals(s))
        {
            v = true;
        }
        else
        {
            try
            {
                v = s;
                v = Double.parseDouble(s);    // if it parses as double
                v = Long.parseLong(s);        // see whether it is integral
            }
            catch (NumberFormatException e)
            {
<<<<<<< HEAD
=======
                // Ignore
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
        return v;
    }

    public Object eval(final Token t) throws Exception
    {
<<<<<<< HEAD
        Object v = null;

        switch (t.type)
        {
            case WORD:
                v = Tokenizer.expand(t, this);

                if (t == v)
                {
                    v = eval(v);
                }
                break;

            case CLOSURE:
                v = new Closure(session, this, t);
                break;

            case EXECUTION:
                v = new Closure(session, this, t).execute(session, parms);
                break;

            case ARRAY:
                v = array(t);
                break;

            case ASSIGN:
                v = t.type;
                break;

            default:
                throw new SyntaxError(t.line, t.column, "unexpected token: " + t.type);
        }

        return v;
=======
        return eval(t, true);
    }

    public Object eval(final Token t, boolean convertNumeric) throws Exception
    {
        if (t instanceof Parser.Closure)
        {
            return new Closure(session, this, ((Parser.Closure) t).program());
        }
        else if (t instanceof Sequence)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Channel out = Channels.newChannel(baos);
            Object result = new Closure(session, this, ((Sequence) t).program())
                    .execute(session, parms, out);
            if (result != null) {
                return result;
            } else {
                String s = baos.toString();
                while (!s.isEmpty() && s.charAt(s.length() - 1) == '\n') {
                    s = s.substring(0, s.length() - 1);
                }
                return s;
            }
        }
        else if (t instanceof Array)
        {
            return array((Array) t);
        }
        else {
            Object v = Expander.expand(t, this);
            if (t == v)
            {
                if (convertNumeric)
                {
                    v = eval(v);
                }
            }
            return v;
        }
    }

    public Object execute(Executable executable) throws Exception
    {
        if (executable instanceof Statement)
        {
            return executeStatement((Statement) executable);
        }
        else if (executable instanceof Sequence)
        {
            return new Closure(session, this, ((Sequence) executable).program()).execute(new ArrayList<>(), null);
        }
        else
        {
            throw new IllegalStateException();
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /*
     * executeStatement handles the following cases:
     *    <string> '=' word // simple assignment
     *    <string> '=' word word.. // complex assignment
     *    <bareword> word.. // command invocation
     *    <object> // value of <object>
     *    <object> word.. // method call
     */
<<<<<<< HEAD
    public Object executeStatement(List<Token> statement) throws Exception
=======
    public Object executeStatement(Statement statement) throws Exception
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        Object echo = session.get("echo");
        String xtrace = null;

        if (echo != null && !"false".equals(echo.toString()))
        {
            // set -x execution trace
<<<<<<< HEAD
            StringBuilder buf = new StringBuilder("+");
            for (Token token : statement)
            {
                buf.append(' ');
                buf.append(token.source());
            }
            xtrace = buf.toString();
            session.err.println(xtrace);
        }

        List<Object> values = new ArrayList<Object>();
        errTok = statement.get(0);

        if ((statement.size() > 3) && Type.ASSIGN.equals(statement.get(1).type))
        {
            errTok2 = statement.get(2);
        }

        for (Token t : statement)
        {
            Object v = eval(t);

            if ((Type.EXECUTION == t.type) && (statement.size() == 1))
            {
                return v;
            }

            if (parms == v && parms != null)
            {
                values.addAll(parms); // explode $args array
=======
            xtrace = "+" + statement;
            session.perr.println(xtrace);
        }

        List<Token> tokens = statement.tokens();
        if (tokens.isEmpty())
        {
            return null;
        }

        List<Object> values = new ArrayList<>();
        errTok = tokens.get(0);

        if ((tokens.size() > 3) && Token.eq("=", tokens.get(1)))
        {
            errTok2 = tokens.get(2);
        }

        for (Token t : tokens)
        {
            Object v = eval(t, values.isEmpty());

//            if ((Token.Type.EXECUTION == t.type) && (tokens.size() == 1)) {
//                return v;
//            }

            if (v instanceof ArgList)
            {
                values.addAll((ArgList) v); // explode $args array
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
            else
            {
                values.add(v);
            }
        }

        Object cmd = values.remove(0);
        if (cmd == null)
        {
            if (values.isEmpty())
            {
                return null;
            }

            throw new RuntimeException("Command name evaluates to null: " + errTok);
        }

        if (cmd instanceof CharSequence && values.size() > 0
<<<<<<< HEAD
            && Type.ASSIGN.equals(values.get(0)))
=======
                && Token.eq("=", tokens.get(1)))
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            values.remove(0);
            String scmd = cmd.toString();
            Object value;

            if (values.size() == 0)
            {
<<<<<<< HEAD
                return session.variables.remove(scmd);
=======
                return session.put(scmd, null);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }

            if (values.size() == 1)
            {
                value = values.get(0);
            }
            else
            {
                cmd = values.remove(0);
                if (null == cmd)
                {
                    throw new RuntimeException("Command name evaluates to null: "
<<<<<<< HEAD
                        + errTok2);
=======
                            + errTok2);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }

                trace2(xtrace, cmd, values);

<<<<<<< HEAD
                value = bareword(statement.get(2)) ? executeCmd(cmd.toString(), values)
=======
                value = bareword(tokens.get(2), cmd) ? executeCmd(cmd.toString(), values)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    : executeMethod(cmd, values);
            }

            return assignment(scmd, value);
        }

        trace2(xtrace, cmd, values);

<<<<<<< HEAD
        return bareword(statement.get(0)) ? executeCmd(cmd.toString(), values)
=======
        return bareword(tokens.get(0), cmd) ? executeCmd(cmd.toString(), values)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            : executeMethod(cmd, values);
    }

    // second level expanded execution trace
    private void trace2(String trace1, Object cmd, List<Object> values)
    {
        if ("verbose".equals(session.get("echo")))
        {
            StringBuilder buf = new StringBuilder("+ " + cmd);

            for (Object value : values)
            {
                buf.append(' ');
                buf.append(value);
            }

            String trace2 = buf.toString();

            if (!trace2.equals(trace1))
            {
<<<<<<< HEAD
                session.err.println("+" + trace2);
=======
                session.perr.println("+" + trace2);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }

<<<<<<< HEAD
    private boolean bareword(Token t) throws Exception
    {
        return ((t.type == Type.WORD) && (t == Tokenizer.expand(t, this)) && (eval((Object) t) instanceof String));
=======
    private boolean bareword(Token t, Object v) {
        return v instanceof CharSequence && Token.eq(t, (CharSequence) v);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    private Object executeCmd(String scmd, List<Object> values) throws Exception
    {
        String scopedFunction = scmd;
        Object x = get(scmd);

        if (!(x instanceof Function))
        {
            if (scmd.indexOf(':') < 0)
            {
                scopedFunction = "*:" + scmd;
            }

            x = get(scopedFunction);

            if (x == null || !(x instanceof Function))
            {
                // try default command handler
                if (session.get(DEFAULT_LOCK) == null)
                {
                    x = get("default");
                    if (x == null)
                    {
                        x = get("*:default");
                    }

                    if (x instanceof Function)
                    {
                        try
                        {
                            session.put(DEFAULT_LOCK, true);
                            values.add(0, scmd);
                            return ((Function) x).execute(session, values);
                        }
                        finally
                        {
<<<<<<< HEAD
                            session.variables.remove(DEFAULT_LOCK);
=======
                            session.put(DEFAULT_LOCK, null);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        }
                    }
                }

                throw new CommandNotFoundException(scmd);
            }
        }
        return ((Function) x).execute(session, values);
    }

    private Object executeMethod(Object cmd, List<Object> values) throws Exception
    {
        if (values.isEmpty())
        {
            return cmd;
        }

        boolean dot = values.size() > 1 && ".".equals(String.valueOf(values.get(0)));

        // FELIX-1473 - allow method chaining using dot pseudo-operator, e.g.
        //  (bundle 0) . loadClass java.net.InetAddress . localhost . hostname
        //  (((bundle 0) loadClass java.net.InetAddress ) localhost ) hostname
        if (dot)
        {
            Object target = cmd;
<<<<<<< HEAD
            ArrayList<Object> args = new ArrayList<Object>();
=======
            ArrayList<Object> args = new ArrayList<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            values.remove(0);

            for (Object arg : values)
            {
                if (".".equals(arg))
                {
<<<<<<< HEAD
                    target = Reflective.invoke(session, target,
                        args.remove(0).toString(), args);
=======
                    target = invoke(target, args.remove(0).toString(), args);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    args.clear();
                }
                else
                {
                    args.add(arg);
                }
            }

            if (args.size() == 0)
            {
                return target;
            }

<<<<<<< HEAD
            return Reflective.invoke(session, target, args.remove(0).toString(), args);
=======
            return invoke(target, args.remove(0).toString(), args);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        else if (cmd.getClass().isArray() && values.size() == 1)
        {
            Object[] cmdv = (Object[]) cmd;
            String index = values.get(0).toString();
            return "length".equals(index) ? cmdv.length : cmdv[Integer.parseInt(index)];
        }
        else
        {
<<<<<<< HEAD
            return Reflective.invoke(session, cmd, values.remove(0).toString(), values);
        }
    }

    private Object assignment(String name, Object value)
    {
        session.variables.put(name, value);
        return value;
    }

    private Object array(Token array) throws Exception
    {
        List<Token> list = new ArrayList<Token>();
        Map<Token, Token> map = new LinkedHashMap<Token, Token>();
        (new Parser(array)).array(list, map);

        if (map.isEmpty())
        {
            List<Object> olist = new ArrayList<Object>();
=======
            return invoke(cmd, values.remove(0).toString(), values);
        }
    }

    private Object invoke(Object target, String name, List<Object> args) throws Exception
    {
        return session.invoke(target, name, args);
    }

    private Object assignment(String name, Object value)
    {
        session.put(name, value);
        return value;
    }

    public Object expr(Token expr)
    {
        return session.expr(expr);
    }

    private Object array(Array array) throws Exception
    {
        List<Token> list = array.list();
        Map<Token, Token> map = array.map();

        if (list != null)
        {
            List<Object> olist = new ArrayList<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            for (Token t : list)
            {
                Object oval = eval(t);
                if (oval.getClass().isArray())
                {
<<<<<<< HEAD
                    for (Object o : (Object[]) oval)
                    {
                        olist.add(o);
                    }
=======
                    Collections.addAll(olist, (Object[]) oval);
                }
                else if (oval instanceof ArgList)
                {
                    olist.addAll((ArgList) oval);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
                else
                {
                    olist.add(oval);
                }
            }
            return olist;
        }
        else
        {
<<<<<<< HEAD
            Map<Object, Object> omap = new LinkedHashMap<Object, Object>();
=======
            Map<Object, Object> omap = new LinkedHashMap<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            for (Entry<Token, Token> e : map.entrySet())
            {
                Token key = e.getKey();
                Object k = eval(key);
                if (!(k instanceof String))
                {
<<<<<<< HEAD
                    throw new SyntaxError(key.line, key.column,
=======
                    throw new SyntaxError(key.line(), key.column(),
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        "map key null or not String: " + key);
                }
                omap.put(k, eval(e.getValue()));
            }
            return omap;
        }
    }

    public Object get(String name)
    {
        if (parms != null)
        {
            if ("args".equals(name))
            {
                return parms;
            }

            if ("argv".equals(name))
            {
                return parmv;
            }

            if ("it".equals(name))
            {
                return parms.get(0);
            }

            if (name.length() == 1 && Character.isDigit(name.charAt(0)))
            {
                int i = name.charAt(0) - '0';
                if (i > 0)
                {
                    return parms.get(i - 1);
                }
            }
        }

        return session.get(name);
    }

    public Object put(String key, Object value)
    {
<<<<<<< HEAD
        return session.variables.put(key, value);
    }

    @Override
    public String toString()
    {
        return source.toString().trim().replaceAll("\n+", "\n").replaceAll(
            "([^\\\\{(\\[])\n", "\\1;").replaceAll("[ \\\\\t\n]+", " ");
    }

    /**
     * List that overrides toString() for implicit $args expansion.
     * Also checks for index out of bounds, so that $1 evaluates to null
     * rather than throwing IndexOutOfBoundsException.
     * e.g. x = { a$args }; x 1 2 => a1 2 and not a[1, 2]
     */
    class ArgList extends AbstractList<Object>
    {
        private List<Object> list;

        public ArgList(List<Object> args)
        {
            this.list = args;
        }

        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            for (Object o : list)
            {
                if (buf.length() > 0)
                    buf.append(' ');
                buf.append(o);
            }
            return buf.toString();
        }

        @Override
        public Object get(int index)
        {
            return index < list.size() ? list.get(index) : null;
        }

        @Override
        public Object remove(int index)
        {
            return list.remove(index);
        }

        @Override
        public int size()
        {
            return list.size();
        }
=======
        return session.put(key, value);
    }

    @Override
    public Path currentDir() {
        return isSet(CommandSession.OPTION_NO_GLOB, false) ? null : session().currentDir();
    }

    protected boolean isSet(String name, boolean def) {
        Object v = session.get(name);
        if (v instanceof Boolean) {
            return (Boolean) v;
        } else if (v != null) {
            String s = v.toString();
            return s.isEmpty() || s.equalsIgnoreCase("on")
                    || s.equalsIgnoreCase("1") || s.equalsIgnoreCase("true");
        }
        return def;
    }

    @Override
    public String toString()
    {
        return source.toString().trim().replaceAll("\n+", "\n").replaceAll(
            "([^\\\\{}(\\[])[\\s\n]*\n", "$1;").replaceAll("[ \\\\\t\n]+", " ");
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

}
