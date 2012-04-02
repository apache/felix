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
import java.util.Arrays;
import java.util.Comparator;

import org.apache.felix.webconsole.internal.AbstractConfigurationPrinter;

public class ThreadPrinter extends AbstractConfigurationPrinter
{

    private static final String TITLE = "Threads";

    private static final String LABEL = "_threads";

    public String getTitle()
    {
        return TITLE;
    }

    public void printConfiguration(PrintWriter pw)
    {
        // first get the root thread group
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null)
        {
            rootGroup = rootGroup.getParent();
        }

        printThreadGroup(pw, rootGroup);

        int numGroups = rootGroup.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[2 * numGroups];
        rootGroup.enumerate(groups);
        Arrays.sort(groups, ThreadGroupComparator.getInstance());
        for (int i = 0; i < groups.length; i++)
        {
            printThreadGroup(pw, groups[i]);
        }
    }

    private static final void printThreadGroup(PrintWriter pw, ThreadGroup group)
    {
        if (group != null)
        {
            StringBuffer info = new StringBuffer();
            info.append("ThreadGroup ").append(group.getName());
            info.append(" [");
            info.append("maxprio=").append(group.getMaxPriority());

            info.append(", parent=");
            if (group.getParent() != null)
            {
                info.append(group.getParent().getName());
            }
            else
            {
                info.append('-');
            }

            info.append(", isDaemon=").append(group.isDaemon());
            info.append(", isDestroyed=").append(group.isDestroyed());
            info.append(']');

            ConfigurationRender.infoLine(pw, null, null, info.toString());

            int numThreads = group.activeCount();
            Thread[] threads = new Thread[numThreads * 2];
            group.enumerate(threads, false);
            Arrays.sort(threads, ThreadComparator.getInstance());
            for (int i = 0; i < threads.length; i++)
            {
                printThread(pw, threads[i]);
            }

            pw.println();
        }
    }

    private static final void printThread(PrintWriter pw, Thread thread)
    {
        if (thread != null)
        {
            StringBuffer info = new StringBuffer();
            info.append("Thread ").append(thread.getName());
            info.append(" [");
            info.append("priority=").append(thread.getPriority());
            info.append(", alive=").append(thread.isAlive());
            info.append(", daemon=").append(thread.isDaemon());
            info.append(", interrupted=").append(thread.isInterrupted());
            info.append(", loader=").append(thread.getContextClassLoader());
            info.append(']');

            ConfigurationRender.infoLine(pw, "  ", null, info.toString());
        }
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
        if (null == thread1 || null == thread2) return -1;
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

        return t1.compareTo(t2);
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
        if (null == thread1 || null == thread2) return -1;
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

        return t1.compareTo(t2);
    }

}