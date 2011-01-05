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
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.gogo.api.CommandSessionListener;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;

public class CommandProcessorImpl implements CommandProcessor
{
    protected final Set<Converter> converters = new HashSet<Converter>();
    protected final Set<CommandSessionListener> listeners = new CopyOnWriteArraySet<CommandSessionListener>();
    protected final Map<String, Object> commands = new LinkedHashMap<String, Object>();
    protected final Map<String, Object> constants = new HashMap<String, Object>();
    protected final ThreadIO threadIO;
    protected final WeakHashMap<CommandSession, Object> sessions = new WeakHashMap<CommandSession, Object>();

    public CommandProcessorImpl(ThreadIO tio)
    {
        threadIO = tio;
    }

    public CommandSession createSession(InputStream in, PrintStream out, PrintStream err)
    {
        CommandSessionImpl session = new CommandSessionImpl(this, in, out, err);
        sessions.put(session, null);
        return session;
    }

    public void stop()
    {
        for (CommandSession session : sessions.keySet())
        {
            session.close();
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
        return commands.keySet();
    }

    Function getCommand(String name, final Object path)
    {
        int colon = name.indexOf(':');

        if (colon < 0)
        {
            return null;
        }

        name = name.toLowerCase();
        Object cmd = commands.get(name);
        String cfunction = name.substring(colon);
        boolean anyScope = (colon == 1 && name.charAt(0) == '*');

        if (null == cmd && anyScope)
        {
            String scopePath = (null == path ? "*" : path.toString());

            for (String scope : scopePath.split(":"))
            {
                if (scope.equals("*"))
                {
                    for (Entry<String, Object> entry : commands.entrySet())
                    {
                        if (entry.getKey().endsWith(cfunction))
                        {
                            cmd = entry.getValue();
                            break;
                        }
                    }
                }
                else
                {
                    cmd = commands.get(scope + cfunction);
                }

                if (cmd != null)
                {
                    break;
                }
            }
        }

        if ((null == cmd) || (cmd instanceof Function))
        {
            return (Function) cmd;
        }

        return new CommandProxy(cmd, cfunction.substring(1));
    }

    public void addCommand(String scope, Object target)
    {
        Class<?> tc = (target instanceof Class<?>) ? (Class<?>) target
            : target.getClass();
        addCommand(scope, target, tc);
    }

    public void addCommand(String scope, Object target, Class<?> functions)
    {
        if (target == null)
        {
            return;
        }

        String[] names = getFunctions(functions);
        for (String function : names)
        {
            addCommand(scope, target, function);
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
        commands.put((scope + ":" + function).toLowerCase(), target);
    }

    public void removeCommand(String scope, String function)
    {
        String func = (scope + ":" + function).toLowerCase();
        commands.remove(func);
    }

    public void removeCommand(Object target)
    {
        for (Iterator<Object> i = commands.values().iterator(); i.hasNext();)
        {
            if (i.next() == target)
            {
                i.remove();
            }
        }
    }

    private String[] getFunctions(Class<?> target)
    {
        String[] functions;
        Set<String> list = new TreeSet<String>();
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

    protected void put(String name, Object target)
    {
        commands.put(name, target);
    }

    public Object convert(Class<?> desiredType, Object in)
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
                e.printStackTrace();
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

}
