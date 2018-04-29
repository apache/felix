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
<<<<<<< HEAD
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
=======
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
<<<<<<< HEAD
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.gogo.api.CommandSessionListener;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
=======
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.service.command.*;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.apache.felix.service.threadio.ThreadIO;

public class CommandProcessorImpl implements CommandProcessor
{
<<<<<<< HEAD
    protected final Set<Converter> converters = new HashSet<Converter>();
    protected final Set<CommandSessionListener> listeners = new CopyOnWriteArraySet<CommandSessionListener>();
    protected final Map<String, Object> commands = new LinkedHashMap<String, Object>();
    protected final Map<String, Object> constants = new HashMap<String, Object>();
    protected final ThreadIO threadIO;
    protected final WeakHashMap<CommandSession, Object> sessions = new WeakHashMap<CommandSession, Object>();
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    public CommandProcessorImpl(ThreadIO tio)
    {
        threadIO = tio;
    }

<<<<<<< HEAD
    public CommandSession createSession(InputStream in, PrintStream out, PrintStream err)
    {
        CommandSessionImpl session = new CommandSessionImpl(this, in, out, err);
        sessions.put(session, null);
        return session;
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public void stop()
    {
<<<<<<< HEAD
        for (CommandSession session : sessions.keySet())
        {
            session.close();
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
    Function getCommand(String name, final Object path)
=======
    protected Function getCommand(String name, final Object path)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        int colon = name.indexOf(':');

        if (colon < 0)
        {
            return null;
        }

        name = name.toLowerCase();
        String cfunction = name.substring(colon);
        boolean anyScope = (colon == 1 && name.charAt(0) == '*');

<<<<<<< HEAD
        Object cmd = commands.get(name);

        if (null == cmd && anyScope)
=======
        Map<Object, Integer> cmdMap = commands.get(name);

        if (null == cmdMap && anyScope)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            String scopePath = (null == path ? "*" : path.toString());

            for (String scope : scopePath.split(":"))
            {
                if (scope.equals("*"))
                {
<<<<<<< HEAD
                    synchronized (commands)
                    {
                        for (Entry<String, Object> entry : commands.entrySet())
                        {
                            if (entry.getKey().endsWith(cfunction))
                            {
                                cmd = entry.getValue();
                                break;
                            }
=======
                    for (Entry<String, Map<Object, Integer>> entry : commands.entrySet())
                    {
                        if (entry.getKey().endsWith(cfunction))
                        {
                            cmdMap = entry.getValue();
                            break;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        }
                    }
                }
                else
                {
<<<<<<< HEAD
                    cmd = commands.get(scope + cfunction);
                }

                if (cmd != null)
                {
                    break;
                }
            }
        }

=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if ((null == cmd) || (cmd instanceof Function))
        {
            return (Function) cmd;
        }

        return new CommandProxy(cmd, cfunction.substring(1));
    }

<<<<<<< HEAD
    public void addCommand(String scope, Object target)
    {
        Class<?> tc = (target instanceof Class<?>) ? (Class<?>) target
            : target.getClass();
        addCommand(scope, target, tc);
    }

    public void addCommand(String scope, Object target, Class<?> functions)
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        if (target == null)
        {
            return;
        }

        String[] names = getFunctions(functions);
        for (String function : names)
        {
<<<<<<< HEAD
            addCommand(scope, target, function);
=======
            addCommand(scope, target, function, ranking);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
        synchronized (commands)
        {
            commands.put((scope + ":" + function).toLowerCase(), target);
        }
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public void removeCommand(String scope, String function)
    {
<<<<<<< HEAD
        String func = (scope + ":" + function).toLowerCase();
        synchronized (commands)
        {
            commands.remove(func);
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    public void removeCommand(Object target)
    {
<<<<<<< HEAD
        synchronized (commands)
        {
            for (Iterator<Object> i = commands.values().iterator(); i.hasNext();)
            {
                if (i.next() == target)
                {
                    i.remove();
                }
            }
=======
        for (Map<Object, Integer> cmdMap : commands.values())
        {
            cmdMap.remove(target);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    private String[] getFunctions(Class<?> target)
    {
        String[] functions;
<<<<<<< HEAD
        Set<String> list = new TreeSet<String>();
=======
        Set<String> list = new TreeSet<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
    protected void put(String name, Object target)
    {
        synchronized (commands)
        {
            commands.put(name, target);
        }
    }

    public Object convert(Class<?> desiredType, Object in)
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
                e.printStackTrace();
            }
        }
=======
                // Ignore
                e.getCause();
            }
        }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
    void afterExecute(CommandSession session, CharSequence commandline,
        Exception exception)
=======
    void afterExecute(CommandSession session, CharSequence commandline, Exception exception)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
=======
    public Object expr(CommandSessionImpl session, CharSequence expr)
    {
        return new Expression(expr.toString()).eval(session.variables);
    }

    public Object invoke(CommandSessionImpl session, Object target, String name, List<Object> args) throws Exception
    {
        return Reflective.invoke(session, target, name, args);
    }

    public Path redirect(CommandSessionImpl session, Path path, int mode)
    {
        return session.currentDir().resolve(path);
    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
