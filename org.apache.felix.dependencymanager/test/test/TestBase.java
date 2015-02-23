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

import static java.lang.System.out;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Base class for all tests.
 * For now, this class provides logging support.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TestBase {
    final int WARN = 1;
    final int INFO = 2;
    final int DEBUG = 3;
    
    // Set the enabled log level.
    final int m_level = WARN;
    
    @SuppressWarnings("unused")
    void debug(String format, Object ... params) {
        if (m_level >= DEBUG) {
            out.println(Thread.currentThread().getName() + " - " + String.format(format, params));
        }
    }
    
    void warn(String format, Object ... params) {
        warn(format, null, params);
    }
    
    @SuppressWarnings("unused")
    void info(String format, Object ... params) {
        if (m_level >= INFO) {
            out.println(Thread.currentThread().getName() + " - " + String.format(format, params));
        }
    }

    void warn(String format, Throwable t, Object ... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(Thread.currentThread().getName()).append(" - ").append(String.format(format, params));
        if (t != null) {
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            t.printStackTrace(pw);
            sb.append(System.getProperty("line.separator"));
            sb.append(buffer.toString());
        }
        System.out.println(sb.toString());
    }
}
