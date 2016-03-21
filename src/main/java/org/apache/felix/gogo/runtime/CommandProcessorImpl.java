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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.gogo.api.CommandSessionListener;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;

public class CommandProcessorImpl implements CommandProcessor
{
    protected final Set<Converter> converters = new CopyOnWriteArraySet<>();
    protected final Set<CommandSessionListener> listeners = new CopyOnWriteArraySet<>();
    protected final ConcurrentMap<String, Map<Object, Integer>> commands = new ConcurrentHashMap<>();
    protected final Map<String, Object> constants = new ConcurrentHashMap<>();
    protected final ThreadIO threadIO;
    protected final WeakHashMap<CommandSession, Object> sessions = new WeakHashMap<>();
    protected boolean stopped;

    public CommandProcessorImpl()
    {
        this(null);
    }

    public CommandProcessorImpl(ThreadIO tio)
    {
        threadIO = tio;
    }

    @Override
    public CommandSessionImpl createSession(CommandSession parent) {
        synchronized (sessions) {
            if (stopped)
            {
                throw new IllegalStateException("CommandProcessor has been stopped");
            }
            if (!sessions.containsKey(parent) || !(parent instanceof CommandSessionImpl)) {
                throw new IllegalArgumentException();
            }
            CommandSessionImpl session = new CommandSessionImpl(this, (CommandSessionImpl) parent);
            sessions.put(session, null);
            return session;
        }
    }

    public CommandSessionImpl createSession(InputStream in, OutputStream out, OutputStream err)
    {
        synchronized (sessions)
        {
            if (stopped)
            {
                throw new IllegalStateException("CommandProcessor has been stopped");
            }
            CommandSessionImpl session = new CommandSessionImpl(this, in, out, err);
            sessions.put(session, null);
            return session;
        }
    }

    void closeSession(CommandSessionImpl session)
    {
        synchronized (sessions)
        {
            sessions.remove(session);
        }
    }

    public void stop()
    {
        synchronized (sessions)
        {
            stopped = true;
            // Create a copy, as calling session.close() will remove the session from the map
            CommandSession[] toClose = this.sessions.keySet().toArray(new CommandSession[this.sessions.size()]);
            for (CommandSession session : toClose)
            {
                session.close();
            }
            // Just in case...
			sessions.clear();
        }
    }

    public void addConverter(Converter c)
    {
        converters.add(c);
    }

    public void removeConverter(Converter c)
    {
        converters.remove(c);
    }

    public void addListener(CommandSessionListener l)
    {
        listeners.add(l);
    }

    public void removeListener(CommandSessionListener l)
    {
        listeners.remove(l);
    }

    public Set<String> getCommands()
    {
        return Collections.unmodifiableSet(commands.keySet());
    }

    protected Function getCommand(String name, final Object path)
    {
        int colon = name.indexOf(':');

        if (colon < 0)
        {
            return null;
        }

        name = name.toLowerCase();
        String cfunction = name.substring(colon);
        boolean anyScope = (colon == 1 && name.charAt(0) == '*');

        Map<Object, Integer> cmdMap = commands.get(name);

        if (null == cmdMap && anyScope)
        {
            String scopePath = (null == path ? "*" : path.toString());

            for (String scope : scopePath.split(":"))
            {
                if (scope.equals("*"))
                {
                    for (Entry<String, Map<Object, Integer>> entry : commands.entrySet())
                    {
                        if (entry.getKey().endsWith(cfunction))
                        {
                            cmdMap = entry.getValue();
                            break;
                        }
                    }
                }
                else
                {
                    cmdMap = commands.get(scope + cfunction);
                    if (cmdMap != null)
                    {
                        break;
                    }
                }
            }
        }

        Object cmd = null;
        if (cmdMap != null && !cmdMap.isEmpty())
        {
            for (Entry<Object, Integer> e : cmdMap.entrySet())
            {
                if (cmd == null || e.getValue() > cmdMap.get(cmd))
                {
                    cmd = e.getKey();
                }
            }
        }
        if ((null == cmd) || (cmd instanceof Function))
        {
            return (Function) cmd;
        }

        return new CommandProxy(cmd, cfunction.substring(1));
    }

    @Descriptor("add commands")
    public void addCommand(@Descriptor("scope") String scope, @Descriptor("target") Object target)
    {
        Class<?> tc = (target instanceof Class<?>) ? (Class<?>) target : target.getClass();
        addCommand(scope, target, tc);
    }

    @Descriptor("add commands")
    public void addCommand(@Descriptor("scope") String scope, @Descriptor("target") Object target, @Descriptor("functions") Class<?> functions)
    {
        addCommand(scope, target, functions, 0);
    }

    public void addCommand(String scope, Object target, Class<?> functions, int ranking)
    {
        if (target == null)
        {
            return;
        }

        String[] names = getFunctions(functions);
        for (String function : names)
        {
            addCommand(scope, target, function, ranking);
        }
    }

    public Object addConstant(String name, Object target)
    {
        return constants.put(name, target);
    }

    public Object removeConstant(String name)
    {
        return constants.remove(name);
    }

    public void addCommand(String scope, Object target, String function)
    {
        addCommand(scope, target, function, 0);
    }

    public void addCommand(String scope, Object target, String function, int ranking)
    {
        String key = (scope + ":" + function).toLowerCase();
        Map<Object, Integer> cmdMap = commands.get(key);
        if (cmdMap == null)
        {
            commands.putIfAbsent(key, new LinkedHashMap<>());
            cmdMap = commands.get(key);
        }
        cmdMap.put(target, ranking);
    }

    public void removeCommand(String scope, String function)
    {
        // TODO: WARNING: this method does remove all mapping for scope:function
        String key = (scope + ":" + function).toLowerCase();
        commands.remove(key);
    }

    public void removeCommand(String scope, String function, Object target)
    {
        // TODO: WARNING: this method does remove all mapping for scope:function
        String key = (scope + ":" + function).toLowerCase();
        Map<Object, Integer> cmdMap = commands.get(key);
        if (cmdMap != null)
        {
            cmdMap.remove(target);
        }
    }

    public void removeCommand(Object target)
    {
        for (Map<Object, Integer> cmdMap : commands.values())
        {
            cmdMap.remove(target);
        }
    }

    private String[] getFunctions(Class<?> target)
    {
        String[] functions;
        Set<String> list = new TreeSet<>();
        Method methods[] = target.getMethods();
        for (Method m : methods)
        {
            if (m.getDeclaringClass().equals(Object.class))
            {
                continue;
            }
            list.add(m.getName());
            if (m.getName().startsWith("get"))
            {
                String s = m.getName().substring(3);
                if (s.length() > 0)
                {
                    list.add(s.substring(0, 1).toLowerCase() + s.substring(1));
                }
            }
        }

        functions = list.toArray(new String[list.size()]);
        return functions;
    }

    public Object convert(CommandSession session, Class<?> desiredType, Object in)
    {
        int[] cost = new int[1];
        Object ret = Reflective.coerce(session, desiredType, in, cost);
        if (ret == Reflective.NO_MATCH)
        {
            throw new IllegalArgumentException(String.format(
                    "Cannot convert %s(%s) to %s", in, in != null ? in.getClass() : "null", desiredType));
        }
        return ret;
    }

    Object doConvert(Class<?> desiredType, Object in)
    {
        for (Converter c : converters)
        {
            try
            {
                Object converted = c.convert(desiredType, in);
                if (converted != null)
                {
                    return converted;
                }
            }
            catch (Exception e)
            {
                // Ignore
                e.getCause();
            }
        }

        return null;
    }

    // eval is needed to force expansions to be treated as commands (FELIX-1473)
    public Object eval(CommandSession session, Object[] argv) throws Exception
    {
        StringBuilder buf = new StringBuilder();

        for (Object arg : argv)
        {
            if (buf.length() > 0)
                buf.append(' ');
            buf.append(arg);
        }

        return session.execute(buf);
    }

    void beforeExecute(CommandSession session, CharSequence commandline)
    {
        for (CommandSessionListener l : listeners)
        {
            try
            {
                l.beforeExecute(session, commandline);
            }
            catch (Throwable t)
            {
                // Ignore
            }
        }
    }

    void afterExecute(CommandSession session, CharSequence commandline, Exception exception)
    {
        for (CommandSessionListener l : listeners)
        {
            try
            {
                l.afterExecute(session, commandline, exception);
            }
            catch (Throwable t)
            {
                // Ignore
            }
        }
    }

    void afterExecute(CommandSession session, CharSequence commandline, Object result)
    {
        for (CommandSessionListener l : listeners)
        {
            try
            {
                l.afterExecute(session, commandline, result);
            }
            catch (Throwable t)
            {
                // Ignore
            }
        }
    }

    public Object expr(CommandSessionImpl session, CharSequence expr)
    {
        return new Expression(expr.toString()).eval(session.variables);
    }
}
