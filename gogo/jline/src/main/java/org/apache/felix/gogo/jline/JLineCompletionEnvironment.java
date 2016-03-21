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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.jline.builtins.Completers.CompletionData;
import org.jline.builtins.Completers.CompletionEnvironment;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class JLineCompletionEnvironment implements CompletionEnvironment {

    private final CommandSession session;

    public JLineCompletionEnvironment(CommandSession session) {
        this.session = session;
    }

    public Map<String, List<CompletionData>> getCompletions() {
        return Shell.getCompletions(session);
    }

    public Set<String> getCommands() {
        return Shell.getCommands(session);
    }

    public String resolveCommand(String command) {
        return Shell.resolve(session, command);
    }

    public String commandName(String command) {
        int idx = command.indexOf(':');
        return idx >= 0 ? command.substring(idx + 1) : command;
    }

    public Object evaluate(LineReader reader, ParsedLine line, String func) throws Exception {
        session.put(Shell.VAR_COMMAND_LINE, line);
        return session.execute(func);
    }
}
