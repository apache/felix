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
package org.apache.felix.gogo.jline.ssh;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

public class ShellCommand implements Command, Runnable, SessionAware {

    public static final String SHELL_INIT_SCRIPT = "karaf.shell.init.script";
    public static final String EXEC_INIT_SCRIPT = "karaf.exec.init.script";

    private static final Logger LOGGER = Logger.getLogger(ShellCommand.class.getName());

    private String command;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private ServerSession session;
    private CommandProcessor processor;
    private Environment env;

    public ShellCommand(CommandProcessor processor, String command) {
        this.processor = processor;
        this.command = command;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void setSession(ServerSession session) {
        this.session = session;
    }

    public void start(final Environment env) throws IOException {
        this.env = env;
        new Thread(this).start();
    }

    public void run() {
        int exitStatus = 0;
        try {
            final CommandSession session = processor.createSession(in, new PrintStream(out), new PrintStream(err));
            for (Map.Entry<String, String> e : env.getEnv().entrySet()) {
                session.put(e.getKey(), e.getValue());
            }
            try {
                String scriptFileName = System.getProperty(EXEC_INIT_SCRIPT);
                if (scriptFileName == null) {
                    scriptFileName = System.getProperty(SHELL_INIT_SCRIPT);
                }
                executeScript(scriptFileName, session);
                session.execute(command);
            } catch (Throwable t) {
                exitStatus = 1;
                t.printStackTrace();
            }
        } catch (Exception e) {
            exitStatus = 1;
            LOGGER.log(Level.SEVERE, "Unable to start shell", e);
        } finally {
            ShellFactoryImpl.close(in, out, err);
            callback.onExit(exitStatus);
        }
    }

    public void destroy() {
    }

    private void executeScript(String scriptFileName, CommandSession session) {
        if (scriptFileName != null) {
            Reader r = null;
            try {
                File scriptFile = new File(scriptFileName);
                r = new InputStreamReader(new FileInputStream(scriptFile));
                CharArrayWriter w = new CharArrayWriter();
                int n;
                char[] buf = new char[8192];
                while ((n = r.read(buf)) > 0) {
                    w.write(buf, 0, n);
                }
                session.execute(new String(w.toCharArray()));
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error in initialization script", e);
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
    }

}
