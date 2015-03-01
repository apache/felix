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
package test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.dm.Logger;
import org.apache.felix.dm.impl.SerialExecutor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Validates SerialExecutor used by DM implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SerialExecutorTest extends TestBase {
    final Random m_rnd = new Random();
    final int TESTS = 100000;
    
    @Test
    public void testSerialExecutor() {
        info("Testing serial executor");
        int cores = Math.max(10, Runtime.getRuntime().availableProcessors());
        ExecutorService threadPool = null;

        try {
            threadPool = Executors.newFixedThreadPool(cores);
            final SerialExecutor serial = new SerialExecutor(new Logger(null));

            long timeStamp = System.currentTimeMillis();
            for (int i = 0; i < TESTS; i++) {
                final CountDownLatch latch = new CountDownLatch(cores * 2 /* each task reexecutes itself one time */);
                final SerialTask task = new SerialTask(serial, latch);
                for (int j = 0; j < cores; j ++) {
                    threadPool.execute(new Runnable() {
                        public void run() {
                            serial.execute(task);
                        }
                    });
                }
                Assert.assertTrue("Test " + i + " did not terminate timely", latch.await(20000, TimeUnit.MILLISECONDS));
            }
            long now = System.currentTimeMillis();
            System.out.println("Performed " + TESTS + " tests in " + (now - timeStamp) + " ms.");
            timeStamp = now;
        }
        catch (Throwable t) {
            t.printStackTrace();
            Assert.fail("Test failed: " + t.getMessage());
        }
        finally {
        	if (threadPool != null) {
        		shutdown(threadPool);
        	}
        }
    }

    void shutdown(ExecutorService exec) {
        exec.shutdown();
        try {
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
        }
    }

    class SerialTask implements Runnable {
        final AtomicReference<Thread> m_executingThread = new AtomicReference<Thread>();
        final CountDownLatch m_latch;
        private boolean m_firstExecution;
        private final SerialExecutor m_exec;          
        
        SerialTask(SerialExecutor exec, CountDownLatch latch) {
            m_latch = latch;
            m_exec = exec;
            m_firstExecution = true;
        }
        
        public void run() {
            Thread self = Thread.currentThread();
            if (m_firstExecution) {
                // The first time we are executed, the previous executing thread stored in our m_executingThread should be null
                if (!m_executingThread.compareAndSet(null, self)) {
                    System.out.println("detected concurrent call to SerialTask: currThread=" + self
                        + ", other executing thread=" + m_executingThread);
                    return;
                }
            } else {
                // The second time we are executed, the previous executing thread stored in our m_executingThread should be 
                // the current running thread.
                if (m_executingThread.get() != self) {
                    System.out.println("expect to execute reentrant tasks in same thread, but current thread=" + self
                        + ", while expected is " + m_executingThread);
                    return;
                }
            }
                        
            if (m_firstExecution) {
                m_firstExecution = false;
                m_exec.execute(this); // Our run method must be called immediately
            } else {
                if (! m_executingThread.compareAndSet(self, null)) {
                    System.out.println("detected concurrent call to SerialTask: currThread=" + self
                        + ", other executing thread=" + m_executingThread);
                    return;                    
                }
                m_firstExecution = true;
            }
            
            m_latch.countDown();
        }
    }
}
