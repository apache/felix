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
package org.apache.felix.gogo.runtime;

<<<<<<< HEAD
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.command.CommandSession;
=======
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.threadio.ThreadIO;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

public class Context extends CommandProcessorImpl
{
    public static final String EMPTY = "";
    
<<<<<<< HEAD
    private static final ThreadIOImpl threadio;
    private final CommandSession session;

    static
    {
        threadio = new ThreadIOImpl();
        threadio.start();
    }

    public Context()
=======
    private final CommandSession session;

    public Context(ThreadIO threadio, InputStream in, PrintStream out, PrintStream err)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        super(threadio);
        addCommand("osgi", this, "addCommand");
        addCommand("osgi", this, "removeCommand");
        addCommand("osgi", this, "eval");
<<<<<<< HEAD
        session = (CommandSessionImpl) createSession(System.in, System.out, System.err);
=======
        session = createSession(in, out, err);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public Object execute(CharSequence source) throws Exception
    {
        Object result = new Exception();
        try
        {
            return result = session.execute(source);
        }
        finally
        {
            System.err.println("execute<" + source + "> = ("
                + (null == result ? "Null" : result.getClass().getSimpleName()) + ")("
                + result + ")\n");
        }
    }

    public void addCommand(String function, Object target)
    {
        addCommand("test", target, function);
    }

<<<<<<< HEAD
    public void set(String name, Object value)
    {
        session.put(name, value);
    }

=======
    public Object set(String name, Object value)
    {
        return session.put(name, value);
    }

    public Object get(String name)
    {
        return session.get(name);
    }

    public void currentDir(Path path) {
        session.currentDir(path);
    }

    public Path currentDir() {
        return session.currentDir();
    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
