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
package org.apache.felix.webconsole.internal.misc;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

/**
 * This is a helper class for dumping thread stacks.
 */
public class ThreadDumper
{

    private final Method getStackTrace;
    private final Method getId;

    /**
     * Base constructor.
     */
    public ThreadDumper()
    {
        Method _getStackTrace = null;
        Method _getId = null;
        final Class[] nullArgs = null;
        try
        {
            _getStackTrace = Thread.class.getMethod("getStackTrace", nullArgs); //$NON-NLS-1$
            _getId = Thread.class.getMethod("getId", nullArgs); //$NON-NLS-1$
        }
        catch (Throwable e)
        {
            /* ignore - stack traces will be unavailable */
        }
        getStackTrace = _getStackTrace;
        getId = _getId;
    }

    /**
     * Prints all available thread groups, threads and a summary of threads availability. 
     * The thread groups and the threads will be sorted alphabetically regardless of the case.
     * 
     * @param pw the writer where to print the threads information
     * @param withStackTrace to include or not the stack traces
     */
    public final void printThreads(PrintWriter pw, boolean withStackTrace)
    {
        // first get the root thread group
        ThreadGroup rootGroup = getRootThreadGroup();

        // enumerate all other threads
        int numGroups = rootGroup.activeGroupCount();
        final ThreadGroup[] groups = new ThreadGroup[2 * numGroups];
        numGroups = rootGroup.enumerate(groups);
        Arrays.sort(groups, ThreadGroupComparator.getInstance());

        printSummary(pw, rootGroup, groups);
        printThreadGroup(pw, rootGroup, withStackTrace);

        // don't use numGroups, but groups.length, otherwise when we get null elements
        // sorted at the beginning, we will skip the real objects
        for (int i = 0; i < groups.length; i++)
        {
            printThreadGroup(pw, groups[i], withStackTrace);
        }

        pw.println();
    }

    /**
     * Prints information for the given thread.
     * 
     * @param pw the writer where to print the threads information
     * @param thread the thread for which to print the information
     * @param withStackTrace to include or not the stack traces
     */
    public final void printThread(PrintWriter pw, Thread thread, boolean withStackTrace)
    {
        if (thread != null)
        {

            pw.print("  Thread ");
            pw.print(getId(thread));
            pw.print('/');
            pw.print(thread.getName());
            pw.print(" ["); //$NON-NLS-1$
            pw.print("priority=");
            pw.print(thread.getPriority());
            pw.print(", alive=");
            pw.print(thread.isAlive());
            pw.print(", daemon=");
            pw.print(thread.isDaemon());
            pw.print(", interrupted=");
            pw.print(thread.isInterrupted());
            pw.print(", loader=");
            pw.print(thread.getContextClassLoader());
            pw.println(']');

            if (withStackTrace)
            {
                printClassLoader(pw, thread.getContextClassLoader());
                printStackTrace(pw, getStackTrace(thread));
                pw.println();
            }
        }
    }

    private final void printThreadGroup(PrintWriter pw, ThreadGroup group,
        boolean withStackTrace)
    {
        if (group != null)
        {
            pw.print("ThreadGroup ");
            pw.print(group.getName());
            pw.print(" ["); //$NON-NLS-1$
            pw.print("maxprio=");
            pw.print(group.getMaxPriority());

            pw.print(", parent=");
            if (group.getParent() != null)
            {
                pw.print(group.getParent().getName());
            }
            else
            {
                pw.print('-');
            }

            pw.print(", isDaemon=");
            pw.print(group.isDaemon());
            pw.print(", isDestroyed=");
            pw.print(group.isDestroyed());
            pw.println(']');

            int numThreads = group.activeCount();
            Thread[] threads = new Thread[numThreads * 2];
            group.enumerate(threads, false);
            Arrays.sort(threads, ThreadComparator.getInstance());
            for (int i = 0; i < threads.length; i++)
            {
                printThread(pw, threads[i], withStackTrace);
            }

            pw.println();
        }
    }

    private final void printClassLoader(PrintWriter pw, ClassLoader classLoader)
    {
        if (classLoader != null)
        {
            pw.print("    ClassLoader=");
            pw.println(classLoader);
            pw.print("      Parent=");
            pw.println(classLoader.getParent());

            if (classLoader instanceof URLClassLoader)
            {
                URLClassLoader loader = (URLClassLoader) classLoader;
                URL[] urls = loader.getURLs();
                if (urls != null && urls.length > 0)
                {
                    for (int i = 0; i < urls.length; i++)
                    {
                        pw.print("      ");
                        pw.print(i);
                        pw.print(" - ");
                        pw.println(urls[i]);
                    }
                }
            }
        }
    }

    private final void printStackTrace(PrintWriter pw, Object stackTrace)
    {
        pw.println("    Stacktrace");
        if (stackTrace == null || Array.getLength(stackTrace) == 0)
        {
            pw.println("      -"); //$NON-NLS-1$
        }
        else
        {
            for (int i = 0, len = Array.getLength(stackTrace); i < len; i++)
            {
                Object/*StackTraceElement*/stackTraceElement = Array.get(stackTrace, i);
                pw.print("      "); //$NON-NLS-1$
                pw.println(stackTraceElement);
            }
        }
    }

    private final void printSummary(PrintWriter pw, ThreadGroup rootGroup,
        ThreadGroup[] groups)
    {
        int alive = 0;
        int daemon = 0;
        int interrupted = 0;

        int threadCount = 0;
        int threadGroupCount = 0;
        int threadGroupDestroyed = 0;

        ArrayList/*<ThreadGroup>*/list = new ArrayList(groups.length + 1);
        list.add(rootGroup);
        list.addAll(Arrays.asList(groups));
        // main group will eventually enumerate ALL threads, so don't
        // count a thread, it if is already processed
        Collection threadSet = new HashSet();
        for (int j = 0; j < list.size(); j++)
        {
            ThreadGroup group = (ThreadGroup) list.get(j);
            if (null == group)
            {
                continue;
            }
            threadGroupCount++;
            if (group.isDestroyed())
            {
                threadGroupDestroyed++;
            }

            Thread[] threads = new Thread[group.activeCount()];
            group.enumerate(threads);
            for (int i = 0, size = threads.length; i < size; i++)
            {
                Thread thread = threads[i];
                if (null != thread && threadSet.add(thread))
                {
                    if (thread.isAlive())
                    {
                        alive++;
                    }
                    if (thread.isDaemon())
                    {
                        daemon++;
                    }
                    if (thread.isInterrupted())
                    {
                        interrupted++;
                    }
                    threadCount++;
                }
            }
        }

        pw.print("Status: ");
        pw.print(threadCount);
        pw.print(" threads (");
        pw.print(alive);
        pw.print(" alive/");
        pw.print(daemon);
        pw.print(" daemon/");
        pw.print(interrupted);
        pw.print(" interrupted) in ");
        pw.print(threadGroupCount);
        pw.print(" groups (");
        pw.print(threadGroupDestroyed);
        pw.print(" destroyed).");
        pw.println();
        pw.println();
    }

    private final String getId(Thread thread)
    {
        String ret = ""; //$NON-NLS-1$
        if (null != getId)
        {
            try
            {
                ret = "#" + getId.invoke(thread, null); //$NON-NLS-1$
            }
            catch (Throwable e)
            {
                /* ignore */
            }
        }
        return ret;
    }

    private final Object/*StackTraceElement[]*/getStackTrace(Thread thread)
    {
        Object/*StackTraceElement[]*/ret = null;
        if (null != getStackTrace)
        {
            try
            {

                ret = getStackTrace.invoke(thread, null);

            }
            catch (Throwable e)
            {
                // ignore - no traces available
            }
        }
        return ret;
    }

    private static final ThreadGroup getRootThreadGroup()
    {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null)
        {
            rootGroup = rootGroup.getParent();
        }
        return rootGroup;
    }
}

final class ThreadComparator implements Comparator
{

    private ThreadComparator()
    {
        // prevent instantiation
    }

    private static final Comparator instance = new ThreadComparator();

    public static final Comparator getInstance()
    {
        return instance;
    }

    public int compare(Object thread1, Object thread2)
    {
        if (null == thread1 || null == thread2)
            return -1;
        String t1 = ((Thread) thread1).getName();
        String t2 = ((Thread) thread2).getName();
        if (null == t1)
        {
            t1 = ""; //$NON-NLS-1$
        }
        if (null == t2)
        {
            t2 = ""; //$NON-NLS-1$
        }

        return t1.toLowerCase().compareTo(t2.toLowerCase());
    }

}

final class ThreadGroupComparator implements Comparator
{

    private ThreadGroupComparator()
    {
        // prevent instantiation
    }

    private static final Comparator instance = new ThreadGroupComparator();

    public static final Comparator getInstance()
    {
        return instance;
    }

    public int compare(Object thread1, Object thread2)
    {
        if (null == thread1 || null == thread2)
            return -1;
        String t1 = ((ThreadGroup) thread1).getName();
        String t2 = ((ThreadGroup) thread2).getName();
        if (null == t1)
        {
            t1 = ""; //$NON-NLS-1$
        }
        if (null == t2)
        {
            t2 = ""; //$NON-NLS-1$
        }

        return t1.toLowerCase().compareTo(t2.toLowerCase());
    }

}
