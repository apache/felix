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
// DWB8: throw IllegatlStateException if session used after closed (as per rfc132)
// DWB9: there is no API to list all variables: https://www.osgi.org/bugzilla/show_bug.cgi?id=49
// DWB10: add SCOPE support: https://www.osgi.org/bugzilla/show_bug.cgi?id=51
package org.apache.felix.gogo.runtime;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;

public class CommandSessionImpl implements CommandSession, Converter
{
    public static final String SESSION_CLOSED = "session is closed";
    public static final String VARIABLES = ".variables";
    public static final String COMMANDS = ".commands";
    public static final String CONSTANTS = ".constants";
    private static final String COLUMN = "%-20s %s\n";

    // Streams and channels
    protected InputStream in;
    protected OutputStream out;
    protected PrintStream pout;
    protected OutputStream err;
    protected PrintStream perr;
    protected Channel[] channels;

    private final CommandProcessorImpl processor;
    protected final ConcurrentMap<String, Object> variables = new ConcurrentHashMap<>();
    private volatile boolean closed;

    private final ExecutorService executor;

    private Path currentDir;

    protected CommandSessionImpl(CommandProcessorImpl shell, CommandSessionImpl parent)
    {
        this.currentDir = parent.currentDir;
        this.executor = Executors.newCachedThreadPool();
        this.processor = shell;
        this.channels = parent.channels;
        this.in = parent.in;
        this.out = parent.out;
        this.err = parent.err;
        this.pout = parent.pout;
        this.perr = parent.perr;
    }

    protected CommandSessionImpl(CommandProcessorImpl shell, ReadableByteChannel in, WritableByteChannel out, WritableByteChannel err)
    {
        this.currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        this.executor = Executors.newCachedThreadPool();
        this.processor = shell;
        this.channels = new Channel[] { in, out, err };
        this.in = Channels.newInputStream(in);
        this.out = Channels.newOutputStream(out);
        this.err = out == err ? this.out : Channels.newOutputStream(err);
        this.pout = new PrintStream(this.out, true);
        this.perr = out == err ? pout : new PrintStream(this.err, true);
    }

    protected CommandSessionImpl(CommandProcessorImpl shell, InputStream in, OutputStream out, OutputStream err)
    {
        this.currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        this.executor = Executors.newCachedThreadPool();
        this.processor = shell;
        ReadableByteChannel inCh = Channels.newChannel(in);
        WritableByteChannel outCh = Channels.newChannel(out);
        WritableByteChannel errCh = out == err ? outCh : Channels.newChannel(err);
        this.channels = new Channel[] { inCh, outCh, errCh };
        this.in = in;
        this.out = out;
        this.err = err;
        this.pout = out instanceof PrintStream ? (PrintStream) out : new PrintStream(out, true);
        this.perr = out == err ? pout : err instanceof PrintStream ? (PrintStream) err : new PrintStream(err, true);
    }

    ThreadIO threadIO()
    {
        return processor.threadIO;
    }

    public CommandProcessor processor()
    {
        return processor;
    }

    public ConcurrentMap<String, Object> getVariables()
    {
        return variables;
    }

    public Path currentDir() {
        return currentDir;
    }

    public void currentDir(Path path) {
        currentDir = path;
    }

    public void close()
    {
        if (!this.closed)
        {
            this.closed = true;
            this.processor.closeSession(this);
            executor.shutdownNow();
        }
    }

    ExecutorService getExecutor() {
        return executor;
    }

    public Object execute(CharSequence commandline) throws Exception
    {
        assert processor != null;
        assert processor.threadIO != null;

        if (closed)
        {
            throw new IllegalStateException(SESSION_CLOSED);
        }

        processor.beforeExecute(this, commandline);

        try
        {
            Closure impl = new Closure(this, null, commandline);
            Object result = impl.execute(this, null);
            processor.afterExecute(this, commandline, result);
            return result;
        }
        catch (Exception e)
        {
            processor.afterExecute(this, commandline, e);
            throw e;
        }
    }

    public InputStream getKeyboard()
    {
        return in;
    }

    public Object get(String name)
    {
        // there is no API to list all variables, so overload name == null
        if (name == null || VARIABLES.equals(name))
        {
            return Collections.unmodifiableSet(variables.keySet());
        }

        if (COMMANDS.equals(name))
        {
            return processor.getCommands();
        }

        if (CONSTANTS.equals(name))
        {
            return Collections.unmodifiableSet(processor.constants.keySet());
        }

        Object val = processor.constants.get(name);
        if (val != null)
        {
            return val;
        }

        val = variables.get("#" + name);
        if (val instanceof Function)
        {
            try
            {
                val = ((Function) val).execute(this, null);
            }
            catch (Exception e)
            {
                // Ignore
            }
            return val;
        }
        else if (val != null)
        {
            return val;
        }

        val = variables.get(name);
        if (val != null)
        {
            return val;
        }

        return processor.getCommand(name, variables.get("SCOPE"));
    }

    public Object put(String name, Object value)
    {
        if (value != null)
        {
            return variables.put(name, value);
        }
        else
        {
            return variables.remove(name);
        }
    }

    public PrintStream getConsole()
    {
        return pout;
    }

    @SuppressWarnings("unchecked")
    public CharSequence format(Object target, int level, Converter escape) throws Exception
    {
        if (target == null)
        {
            return "null";
        }

        if (target instanceof CharSequence)
        {
            return (CharSequence) target;
        }

        for (Converter c : processor.converters)
        {
            CharSequence s = c.format(target, level, this);
            if (s != null)
            {
                return s;
            }
        }

        if (target.getClass().isArray())
        {
            if (target.getClass().getComponentType().isPrimitive())
            {
                if (target.getClass().getComponentType() == boolean.class)
                {
                    return Arrays.toString((boolean[]) target);
                }
                else
                {
                    if (target.getClass().getComponentType() == byte.class)
                    {
                        return Arrays.toString((byte[]) target);
                    }
                    else
                    {
                        if (target.getClass().getComponentType() == short.class)
                        {
                            return Arrays.toString((short[]) target);
                        }
                        else
                        {
                            if (target.getClass().getComponentType() == int.class)
                            {
                                return Arrays.toString((int[]) target);
                            }
                            else
                            {
                                if (target.getClass().getComponentType() == long.class)
                                {
                                    return Arrays.toString((long[]) target);
                                }
                                else
                                {
                                    if (target.getClass().getComponentType() == float.class)
                                    {
                                        return Arrays.toString((float[]) target);
                                    }
                                    else
                                    {
                                        if (target.getClass().getComponentType() == double.class)
                                        {
                                            return Arrays.toString((double[]) target);
                                        }
                                        else
                                        {
                                            if (target.getClass().getComponentType() == char.class)
                                            {
                                                return Arrays.toString((char[]) target);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            target = Arrays.asList((Object[]) target);
        }
        if (target instanceof Collection)
        {
            if (level == Converter.INSPECT)
            {
                StringBuilder sb = new StringBuilder();
                Collection<?> c = (Collection<?>) target;
                for (Object o : c)
                {
                    sb.append(format(o, level + 1, this));
                    sb.append("\n");
                }
                return sb;
            }
            else
            {
                if (level == Converter.LINE)
                {
                    StringBuilder sb = new StringBuilder();
                    Collection<?> c = (Collection<?>) target;
                    sb.append("[");
                    for (Object o : c)
                    {
                        if (sb.length() > 1)
                        {
                            sb.append(", ");
                        }
                        sb.append(format(o, level + 1, this));
                    }
                    sb.append("]");
                    return sb;
                }
            }
        }
        if (target instanceof Dictionary)
        {
            Map<Object, Object> result = new HashMap<Object, Object>();
            for (Enumeration e = ((Dictionary) target).keys(); e.hasMoreElements();)
            {
                Object key = e.nextElement();
                result.put(key, ((Dictionary) target).get(key));
            }
            target = result;
        }
        if (target instanceof Map)
        {
            if (level == Converter.INSPECT)
            {
                StringBuilder sb = new StringBuilder();
                Map<?, ?> c = (Map<?, ?>) target;
                for (Map.Entry<?, ?> entry : c.entrySet())
                {
                    CharSequence key = format(entry.getKey(), level + 1, this);
                    sb.append(key);
                    for (int i = key.length(); i < 20; i++)
                    {
                        sb.append(' ');
                    }
                    sb.append(format(entry.getValue(), level + 1, this));
                    sb.append("\n");
                }
                return sb;
            }
            else
            {
                if (level == Converter.LINE)
                {
                    StringBuilder sb = new StringBuilder();
                    Map<?, ?> c = (Map<?, ?>) target;
                    sb.append("[");
                    for (Map.Entry<?, ?> entry : c.entrySet())
                    {
                        if (sb.length() > 1)
                        {
                            sb.append(", ");
                        }
                        sb.append(format(entry, level + 1, this));
                    }
                    sb.append("]");
                    return sb;
                }
            }
        }
        if (target instanceof Path)
        {
            return target.toString();
        }
        if (level == Converter.INSPECT)
        {
            return inspect(target);
        }
        else
        {
            return target.toString();
        }
    }

    CharSequence inspect(Object b)
    {
        boolean found = false;
        Formatter f = new Formatter();
        Method methods[] = b.getClass().getMethods();
        for (Method m : methods)
        {
            try
            {
                String name = m.getName();
                if (m.getName().startsWith("get") && !m.getName().equals("getClass") && m.getParameterTypes().length == 0 && Modifier.isPublic(m.getModifiers()))
                {
                    found = true;
                    name = name.substring(3);
                    m.setAccessible(true);
                    Object value = m.invoke(b, (Object[]) null);
                    f.format(COLUMN, name, format(value, Converter.LINE, this));
                }
            }
            catch (IllegalAccessException e)
            {
                // Ignore
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        if (found)
        {
            return (StringBuilder) f.out();
        }
        else
        {
            return b.toString();
        }
    }

    public Object convert(Class<?> desiredType, Object in)
    {
        return processor.convert(this, desiredType, in);
    }

    public Object doConvert(Class<?> desiredType, Object in)
    {
        return processor.doConvert(desiredType, in);
    }

    public CharSequence format(Object result, int inspect)
    {
        try
        {
            return format(result, inspect, this);
        }
        catch (Exception e)
        {
            return "<can not format " + result + ":" + e;
        }
    }

    public Object expr(CharSequence expr)
    {
        return processor.expr(this, expr);
    }

}
