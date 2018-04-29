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
package org.apache.felix.sigil.gogo.junit;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Function;

public class SigilTestAdapter
{
    public TestCase newTest(final CommandSession session, final String name,
        final Function f, final Object... args)
    {
        return new TestCase(name)
        {
            public void runTest() throws Throwable
            {
                try
                {
                    f.execute(session, Arrays.asList(args));
                }
                catch (Throwable t)
                {
                    if (t instanceof InvocationTargetException)
                        t = t.getCause();

                    // stack trace is no use for identifying the location of
                    // junit:assert methods in scripts.
                    // So add gogo script location to stack trace.
                    Object loc = session.get(".location");
                    if (loc != null)
                    {
                        // file:/path/to/file:line.column
                        String sloc = (String) loc;
                        String fileName = sloc;
                        int lineNumber = 0;

                        if (sloc.matches(".*:[\\d.]+$"))
                        {
                            int colon = sloc.lastIndexOf(':');
                            fileName = sloc.substring(0, colon);
                            String number = sloc.substring(colon + 1);
                            int dot = number.indexOf('.');
                            if (dot > 0)
                                number = number.substring(0, dot);
                            lineNumber = Integer.parseInt(number);
                        }

                        StackTraceElement[] trace = t.getStackTrace();
                        StackTraceElement element = new StackTraceElement(
                            "SigilTestAdaptor", "runTest", fileName, lineNumber);

                        StackTraceElement[] ev = new StackTraceElement[1 + trace.length];
                        int i = 0;
                        ev[i++] = element;
                        for (StackTraceElement e : trace)
                            ev[i++] = e;

                        t.setStackTrace(ev);
                    }

                    throw t;
                }
            }
        };
    }

    public TestSuite newTestSuite(String name, Test... tests)
    {
        TestSuite suite = new TestSuite(name);
        for (Test t : tests)
        {
            suite.addTest(t);
        }
        return suite;
    }
}
