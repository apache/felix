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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.ServerBuilder;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.jline.builtins.Options;

public class Ssh {

    public static final String[] functions = {"sshd"};

    private static final int defaultPort = 2022;

    private final CommandProcessor processor;
    private SshServer server;
    private Object context;
    private int port;
    private String ip;

    public Ssh(CommandProcessor processor) {
        this.processor = processor;
    }

    public void sshd(CommandSession session, String[] argv) throws IOException {
        final String[] usage = {"sshd - start an ssh server",
                "Usage: sshd [-i ip] [-p port] start | stop | status",
                "  -i --ip=INTERFACE        listen interface (default=127.0.0.1)",
                "  -p --port=PORT           listen port (default=" + defaultPort + ")",
                "  -? --help                show help"};

        Options opt = Options.compile(usage).parse(argv);
        List<String> args = opt.args();

        if (opt.isSet("help") || args.isEmpty()) {
            opt.usage(System.err);
            return;
        }

        String command = args.get(0);

        if ("start".equals(command)) {
            if (server != null) {
                throw new IllegalStateException("sshd is already running on port " + port);
            }
            ip = opt.get("ip");
            port = opt.getNumber("port");
            context = session.get(org.apache.felix.gogo.runtime.activator.Activator.CONTEXT);
            start();
            status();
        } else if ("stop".equals(command)) {
            if (server == null) {
                throw new IllegalStateException("sshd is not running.");
            }
            stop();
        } else if ("status".equals(command)) {
            status();
        } else {
            throw opt.usageError("bad command: " + command);
        }

    }

    private void status() {
        if (server != null) {
            System.out.println("sshd is running on " + ip + ":" + port);
        } else {
            System.out.println("sshd is not running.");
        }
    }

    private void start() throws IOException {
        server = ServerBuilder.builder().build();
        server.setPort(port);
        server.setHost(ip);
        server.setShellFactory(new ShellFactoryImpl(processor));
        server.setCommandFactory(new ScpCommandFactory.Builder().withDelegate(new ShellCommandFactory(processor)).build());
        server.setSubsystemFactories(Collections.<NamedFactory<Command>>singletonList(
                new SftpSubsystemFactory.Builder().build()
        ));
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        server.start();
    }

    private void stop() throws IOException {
        server.stop();
    }
}
