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
package org.apache.felix.dm.benchmark.scenario;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Helper functions used to work around the java.util.function.* functions, which don't support 
 * methods throwing a checked exception.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Unchecked {
    /**
     * Same functional interface as java.util.function.Consumer, except that the accept method may throw an exception.
     */
    @FunctionalInterface
    public static interface CheckedConsumer<T> {
        public void accept(T t) throws Exception;
    }
    
    /**
     * Same interface as Runnable, except that the run method may throw an exception.
     */
    @FunctionalInterface
    public static interface CheckedRunnable {
        public void run() throws Exception;
    }
    
    /**
     * Same interface as Function, except that the accept method may throw an exception.
     */
    @FunctionalInterface
    public static interface CheckedFunction<T,U> {
        public U apply(T t) throws Exception;
    }
    
    /**
     * Wraps a Consumer whose accept method may throw an exception behind a regular java.util.function.Consumer
     */
    public static <T> Consumer<T> consumer(CheckedConsumer<T> c) {
        return (t) -> {
            try {
                c.accept(t);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            catch (Throwable err) {
                throw err;
            }
        };
    }
    
    /**
     * Wraps a Consumer whose accept method may throw an exception behind a regular java.util.function.Consumer
     */
    public static <T,U> Function<T, U> func(CheckedFunction<T, U> f) {
        return (t) -> {
            try {
                return f.apply(t);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            catch (Throwable err) {
                throw err;
            }
        };
    }

    /**
     * Runs a runnable which may throw an exception without having to catch it.
     */
    public static void run(CheckedRunnable r) {
        try {
            r.run();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        catch (Throwable err) {
            throw err;
        }
    }
}
