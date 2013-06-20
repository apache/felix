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
package org.apache.felix.threaddump.internal.jdk6;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.apache.felix.threaddump.internal.ThreadDumper;
import org.apache.felix.threaddump.internal.ThreadWriter;
import org.apache.felix.threaddump.internal.jdk5.Jdk15ThreadDumper;

/**
 * {@link ThreadDumper} implementation which relies on JMX APIs in JDK1.5.
 */
public final class Jdk16ThreadDumper extends Jdk15ThreadDumper
{

    private static final String LOCKED = "\t- locked <0x{0}> (a {1})";

    private static final String BLOCKED = "\t- waiting to lock <0x{0}> (a {1}) owned by \"{2}\" tid=0x{3}";

    protected ThreadInfo[] getThreadInfo(ThreadMXBean threadMXBean)
    {
        return threadMXBean.dumpAllThreads(true, true);
    }

    protected long[] findDeadlockedThreads(ThreadMXBean threadMXBean)
    {
        return threadMXBean.findDeadlockedThreads();
    }

    protected void printStackTrace(ThreadWriter threadWriter, ThreadInfo info)
    {
        StackTraceElement[] trace = info.getStackTrace();
        if (trace.length > 0)
        {
            MonitorInfo[] locks = info.getLockedMonitors();
            int currentLock = 0;

            for (int idx = 0; idx < trace.length; idx++)
            {
                threadWriter.printStackTraceElement(trace[idx]);

                if (idx == 0)
                {
                    LockInfo locked = info.getLockInfo();
                    if (locked != null)
                    {
                        printLockInfo(threadWriter, BLOCKED, locked, info.getLockOwnerName(), info.getLockOwnerId());
                    }
                }

                if (currentLock < locks.length && locks[currentLock].getLockedStackDepth() == idx)
                {
                    printLockInfo(threadWriter, LOCKED, locks[currentLock]);
                    currentLock++;
                }
            }

            // print synchronizers ...
            LockInfo[] syncs = info.getLockedSynchronizers();
            if (syncs != null && syncs.length > 0)
            {
                threadWriter.printEmptyLine();
                threadWriter.println("   Locked ownable synchronizers:");
                for (int j = 0; j < syncs.length; j++)
                {
                    LockInfo sync = syncs[j];
                    printLockInfo(threadWriter, LOCKED, sync);
                }
            }
        }
    }

    protected void printDeadlockedThreadInfo(ThreadWriter threadWriter, ThreadInfo info)
    {
        threadWriter.println("\"{0}\":", new Object[]
            { info.getThreadName() });
        threadWriter.println("  waiting to lock monitor {0} (object {1}, a {2}),", new Object[]
            { "7f8a5595d180" /* ? */, Integer.toHexString(info.getLockInfo().getIdentityHashCode()),
                info.getLockInfo().getClassName() });
        threadWriter.println("  which is held by \"{0}\"", new Object[]
            { info.getLockOwnerName() });
    }

    private static void printLockInfo(ThreadWriter threadWriter, String pattern, LockInfo lockInfo)
    {
        printLockInfo(threadWriter, pattern, lockInfo, null, -1);
    }

    private static void printLockInfo(ThreadWriter threadWriter, String pattern, LockInfo lockInfo,
        String lockOwnerName, long lockOwnerId)
    {
        threadWriter.println(
            pattern,
            new Object[]
                { Integer.toHexString(lockInfo.getIdentityHashCode()), lockInfo.getClassName(), lockOwnerName,
                    Long.valueOf(lockOwnerId) });
    }
}
