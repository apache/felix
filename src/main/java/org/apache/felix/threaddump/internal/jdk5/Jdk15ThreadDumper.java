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
package org.apache.felix.threaddump.internal.jdk5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.threaddump.internal.ThreadDumper;
import org.apache.felix.threaddump.internal.ThreadWriter;

/**
 * {@link ThreadDumper} implementation which relies on JMX APIs in JDK1.5.
 */
public class Jdk15ThreadDumper implements ThreadDumper
{

    private static final String DEADLOCK = "Found {0} {0,choice,1#deadlock|1<deadlocks}.";

    public void printThreads(ThreadWriter threadWriter)
    {
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        // thread Infos
        final ThreadInfo[] infos = getThreadInfo(threadMXBean);

        // map thread ids to infos idx
        final Map/* <long, int> */id2idx = new HashMap();
        for (int i = 0; i < infos.length; i++)
        {
            id2idx.put(Long.valueOf(infos[i].getThreadId()), Integer.valueOf(i));
        }

        // create an array of all Thread objects indexed equivalent to Infos
        final Thread[] threads = getThreads(id2idx);

        // print the thread information
        for (int i = 0; i < infos.length; i++)
        {
            printThreadInfo(threadWriter, threads[i], infos[i]);
            threadWriter.printEmptyLine();
        }

        // dupm deadlock information
        long[] deadlockedThreadsIds = findDeadlockedThreads(threadMXBean);
        if (deadlockedThreadsIds != null)
        {

            List/* <List<int>> */deadlocks = new ArrayList();
            for (int i = 0; i < deadlockedThreadsIds.length; i++)
            {
                Long l = Long.valueOf(deadlockedThreadsIds[i]);
                Integer idx = (Integer) id2idx.remove(l);
                if (idx != null)
                {
                    List/* <int> */idxs = new ArrayList();
                    deadlocks.add(idxs);

                    do
                    {
                        idxs.add(idx);
                        ThreadInfo info = infos[idx.intValue()];
                        if (info != null)
                        {
                            idx = (Integer) id2idx.remove(Long.valueOf(info.getLockOwnerId()));
                        }
                        else
                        {
                            idx = null;
                        }
                    }
                    while (idx != null);
                }
            }

            for (Iterator di = deadlocks.iterator(); di.hasNext();)
            {
                List idxs = (List) di.next();

                threadWriter.printEmptyLine();
                threadWriter.println("Found one Java-level deadlock:");
                threadWriter.println("=============================");

                for (Iterator ii = idxs.iterator(); ii.hasNext();)
                {
                    Integer idx = (Integer) ii.next();
                    ThreadInfo info = infos[idx.intValue()];

                    printDeadlockedThreadInfo(threadWriter, info);
                }

                threadWriter.printEmptyLine();
                threadWriter.println("Java stack information for the threads listed above:");
                threadWriter.println("===================================================");

                for (Iterator ii = idxs.iterator(); ii.hasNext();)
                {
                    int idx = ((Integer) ii.next()).intValue();
                    printThreadInfo(threadWriter, threads[idx], infos[idx]);
                }
            }

            threadWriter.printEmptyLine();
            threadWriter.println(DEADLOCK, new Object[]
                { Integer.valueOf(deadlocks.size()) });
            threadWriter.printEmptyLine();

        }
    }

    private void printThreadInfo(ThreadWriter threadWriter, Thread t, ThreadInfo info)
    {
        if (t == null)
        {
            return;
        }

        short status = ThreadStateConverter.toStatus(t.getState());
        threadWriter.printThread(t.getName(), t.isDaemon(), t.getPriority(), t.getId(), status);

        printStackTrace(threadWriter, info);
    }

    protected ThreadInfo[] getThreadInfo(ThreadMXBean threadMXBean)
    {
        long[] threadIds = threadMXBean.getAllThreadIds();
        return threadMXBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
    }

    protected long[] findDeadlockedThreads(ThreadMXBean threadMXBean)
    {
        return threadMXBean.findMonitorDeadlockedThreads();
    }

    protected void printStackTrace(ThreadWriter threadWriter, ThreadInfo info)
    {
        threadWriter.printStackTrace(info.getStackTrace());
    }

    protected void printDeadlockedThreadInfo(ThreadWriter threadWriter, ThreadInfo info)
    {
        threadWriter.println("\"{0}\":", new Object[]
            { info.getThreadName() });
        threadWriter.println("  waiting to lock monitor,");
        threadWriter.println("  which is held by \"{0}\"", new Object[]
            { info.getLockOwnerName() });
    }

    private static Thread[] getThreads(final Map/* <long, int> */id2idx)
    {
        // find root thread group
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g.getParent() != null)
        {
            g = g.getParent();
        }
        int numThreads = g.activeCount();
        Thread[] threads = new Thread[numThreads * 2];
        int actualThreads = g.enumerate(threads);
        if (threads.length == actualThreads)
        {
            // some threads have been missed !!
        }

        Thread[] result = new Thread[id2idx.size()];
        for (int i = 0; i < threads.length; i++)
        {
            Thread t = threads[i];
            if (t != null)
            {
                Integer idx = (Integer) id2idx.get(Long.valueOf(t.getId()));
                if (idx != null)
                {
                    result[idx.intValue()] = t;
                }
            }
        }

        return result;
    }
}
