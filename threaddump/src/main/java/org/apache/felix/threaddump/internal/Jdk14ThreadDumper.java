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

/**
 * {@link ThreadDumper} implementation which relies on regular Java API.
 */
final class Jdk14ThreadDumper implements ThreadDumper
{

    /**
     * {@inheritDoc}
     */
    public void printThreads(ThreadWriter threadWriter)
    {
        // first get the root thread group
        ThreadGroup rootGroup = getRootThreadGroup();

        printThreadGroup(threadWriter, rootGroup);

        int numGroups = rootGroup.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[2 * numGroups];
        rootGroup.enumerate(groups);
        for (int i = 0; i < groups.length; i++)
        {
            printThreadGroup(threadWriter, groups[i]);
        }
    }

    private static ThreadGroup getRootThreadGroup()
    {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null)
        {
            rootGroup = rootGroup.getParent();
        }
        return rootGroup;
    }

    private static void printThreadGroup(ThreadWriter threadWriter, ThreadGroup group)
    {
        if (group != null)
        {
            int numThreads = group.activeCount();
            Thread[] threads = new Thread[numThreads * 2];
            group.enumerate(threads, false);
            for (int i = 0; i < threads.length; i++)
            {
                printThread(threadWriter, threads[i]);
            }
        }
    }

    private static void printThread(ThreadWriter threadWriter, Thread thread)
    {
        if (thread != null)
        {
            short status = ThreadWriter.NEW;
            if (thread.isAlive())
            {
                status = ThreadWriter.RUNNABLE;
            }
            else if (thread.isInterrupted())
            {
                status = ThreadWriter.TERMINATED;
            }
            // TODO there are missing cases here!

            threadWriter.printThread(thread.getName(), thread.isDaemon(), thread.getPriority(), -1, status);
            threadWriter.printEmptyLine();
        }
    }

}
