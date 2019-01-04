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
package org.apache.felix.systemready.impl.servlet;

import java.io.PrintWriter;
import java.util.stream.Collectors;

import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.SystemStatus;

class StatusWriterJson {

    private PrintWriter writer;

    public StatusWriterJson(PrintWriter writer) {
        this.writer = writer;
    }
    
    public void write(SystemStatus systemState) {
        writer.println("{");
        writer.println(String.format("  \"systemStatus\": \"%s\", ", systemState.getState().name()));
        writer.println("  \"checks\": [");
        String states = systemState.getCheckStates().stream()
                .map(this:: getStatus)
                .collect(Collectors.joining(",\n"));
        writer.println(states);
        writer.println("  ]");
        writer.println("}");
    }

    private String getStatus(CheckStatus status) {
        return String.format(
                "    { \"check\": \"%s\", \"status\": \"%s\", \"details\": \"%s\" }", 
                status.getCheckName(),
                status.getState().name(), 
                status.getDetails().replace("\n", "\\n"));
    }

}
