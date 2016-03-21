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
package org.apache.felix.gogo.jline;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.threadio.ThreadIO;

public class Context extends CommandProcessorImpl
{
    public static final String EMPTY = "";
    
    private final CommandSession session;

    public Context(ThreadIO threadio, InputStream in, PrintStream out, PrintStream err)
    {
        super(threadio);
        Shell shell = new Shell(new MyContext(), this);
        addCommand("gogo", this, "addCommand");
        addCommand("gogo", this, "removeCommand");
        addCommand("gogo", this, "eval");
        register(this, new Builtin(), Builtin.functions);
        register(this, new Procedural(), Procedural.functions);
        register(this, new Posix(this), Posix.functions);
        register(this, shell, Shell.functions);
        session = createSession(in, out, err);
    }

    static void register(CommandProcessorImpl processor, Object target, String[] functions) {
        for (String function : functions) {
            processor.addCommand("gogo", target, function);
        }
    }

    private static class MyContext implements Shell.Context {

        public String getProperty(String name) {
            return System.getProperty(name);
        }

        public void exit() throws Exception {
            System.exit(0);
        }
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
}
