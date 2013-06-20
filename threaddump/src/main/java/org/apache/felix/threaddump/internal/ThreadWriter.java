/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.threaddump.internal;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Date;

/**
 *
 */
public final class ThreadWriter
{

    public static final short NEW = 0;

    public static final short RUNNABLE = 1;

    public static final short BLOCKED = 2;

    public static final short WAITING = 3;

    public static final short TIMED_WAITING = 4;

    public static final short TERMINATED = 5;

    private static final String DATE = "{0,date,yyyy-MM-dd HH:mm:ss}";

    private static final String HEADER = "Full thread dump {0} ({1} {2}):";

    // nid is unknown
    private static final String THREAD = "\"{0}\" {1}prio={2} tid=0x{3} nid=0x{4} {5,choice,0#new|1#runnable|2#waiting for monitor entry|3#in Object.wait()|4#timed_waiting|5#terminated}";

    private static final String THREAD_STATUS = "   java.lang.Thread.State: {0,choice,0#NEW|1#RUNNABLE|2#BLOCKED|3#WAITING (on object monitor)|4#TIMED_WAITING|5#TERMINATED}";

    private static final String STACKTRACE_ELEMENT = "\tat {0}";

    private final PrintWriter writer;

    public ThreadWriter(PrintWriter writer)
    {
        this.writer = writer;
    }

    /**
     * Full thread dump identifier
     */
    public void printHeader()
    {
        println(DATE, new Object[]
            { new Date() });
        println(HEADER, getSystemProperties(new String[]
            { "java.vm.name", "java.runtime.version", "java.vm.info" }));
        printEmptyLine();
    }

    public void printThread(String name, boolean isDaemon, long priority, long id, short status)
    {
        String daemon = isDaemon ? "daemon " : "";
        println(THREAD, new Object[]
            { name, daemon, String.valueOf(priority), Long.toHexString(id), Integer.toHexString(-1), // nid
                new Short(status) });
        println(THREAD_STATUS, new Object[]
            { new Short(status) });
    }

    public void printStackTrace(StackTraceElement[] stackTrace)
    {
        if (stackTrace != null)
        {
            for (int i = 0; i < stackTrace.length; i++)
            {
                printStackTraceElement(stackTrace[i]);
            }
        }
    }

    public void printStackTraceElement(StackTraceElement element)
    {
        println(STACKTRACE_ELEMENT, new Object[]
            { element });
    }

    public void printEmptyLine()
    {
        writer.println();
    }

    public void println(String message)
    {
        writer.println(message);
    }

    public void println(String pattern, Object[] arguments)
    {
        String result = MessageFormat.format(pattern, arguments);
        writer.println(result);
    }

    private static Object[] getSystemProperties(String[] keys)
    {
        Object[] values = new Object[keys.length];

        for (int i = 0; i < keys.length; i++)
        {
            values[i] = System.getProperty(keys[i]);
        }

        return values;
    }

}
