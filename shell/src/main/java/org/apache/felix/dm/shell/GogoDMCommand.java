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
package org.apache.felix.dm.shell;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.osgi.framework.BundleContext;

/**
 * This class provides DependencyManager commands for the Gogo shell.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class GogoDMCommand extends DMCommand {
    public GogoDMCommand(BundleContext context) {
        super(context);
    }

    public void dmhelp() {
        System.out.println(super.getUsage() + " - " + super.getShortDescription());
    }
    
    public void dm(String[] args) {
        execute("dm", args);
    }
        
    private void execute(String line, String[] args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        PrintStream err = new PrintStream(errorBytes);
        for (int i = 0; i < args.length; i++) {
            line += " " + args[i];
        }
        super.execute(line.toString(), out, err);
        if (bytes.size() > 0) {
            System.out.println(new String(bytes.toByteArray()));
        }
        if (errorBytes.size() > 0) {
            System.out.print("Error:\n");
            System.out.println(new String(errorBytes.toByteArray()));
        }
    }
}
