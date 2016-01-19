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
package org.apache.felix.gogo.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;

public class Console implements Runnable
{
    private final CommandSession session;
    private final InputStream in;
    private final PrintStream out;
    private final History history;
    private volatile boolean quit;

    public Console(CommandSession session, History history)
    {
        this.session = session;
        in = session.getKeyboard();
        out = session.getConsole();
        this.history = history;
    }

    public void run()
    {
        try
        {
            while (!Thread.currentThread().isInterrupted() && !quit)
            {
                CharSequence line = getLine(getPrompt());

                if (line == null)
                {
                    break;
                }

                try
                {
                    if (line.charAt(0) == '!' || line.charAt(0) == '^')
                    {
                        line = history.evaluate(line);
                        System.out.println(line);
                    }

                    Object result = session.execute(line);
                    session.put("_", result); // set $_ to last result

                    if (result != null
                        && !Boolean.FALSE.equals(session.get(".Gogo.format")))
                    {
                        out.println(session.format(result, Converter.INSPECT));
                    }
                }
                catch (Throwable e)
                {
                    final String SESSION_CLOSED = "session is closed";
                    if ((e instanceof IllegalStateException) && SESSION_CLOSED.equals(e.getMessage()))
                    {
                        // FIXME: we assume IllegalStateException is because the session is closed;
                        // but it may be for another reason, so we also check the message (yuk).
                        // It would be better if the RFC-147 API threw a unique exception, such as
                        // org.osgi.service.command.SessionClosedException
                        out.println("gosh: " + e);
                        quit = true;
                    }

                    if (!quit)
                    {
                        session.put("exception", e);
                        Object loc = session.get(".location");

                        if (null == loc || !loc.toString().contains(":"))
                        {
                            loc = "gogo";
                        }

                        out.println(loc + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                }
                finally
                {
                    this.history.append(line);
                }
            }
        }
        catch (Exception e)
        {
            if (!quit)
            {
                e.printStackTrace();
            }
        }
    }

    private String getPrompt()
    {
        Object prompt = session.get("prompt");
        if (prompt instanceof Function)
        {
            try
            {
                prompt = ((Function) prompt).execute(session, null);
            }
            catch (Exception e)
            {
                out.println(prompt + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                prompt = null;
            }
        }

        if (prompt == null)
        {
            prompt = "g! ";
        }

        return prompt.toString();
    }

    private CharSequence getLine(String prompt) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        out.print(prompt);

        while (!quit)
        {
            out.flush();

            int c = 0;
            try
            {
                c = in.read();
            }
            catch (IOException e)
            {
                if ("Stream closed".equals(e.getMessage())) {
                    quit = true;
                } else {
                    throw e;
                }
            }

            switch (c)
            {
                case -1:
                case 4: // EOT, ^D from telnet
                    quit = true;
                    break;

                case '\r':
                    break;

                case '\n':
                    if (sb.length() > 0)
                    {
                        return sb;
                    }
                    out.print(prompt);
                    break;

                case '\b':
                    if (sb.length() > 0)
                    {
                        out.print("\b \b");
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    break;

                default:
                    sb.append((char) c);
                    break;
            }
        }

        return null;
    }

}
