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

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EquinoxDMCommand extends DMCommand implements CommandProvider {
    public EquinoxDMCommand(BundleContext context) {
        super(context);
    }
    
    public void _dm(CommandInterpreter ci) {
        StringBuffer line = new StringBuffer("dm ");
        String arg = ci.nextArgument();
        while (arg != null) {
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(arg);
            arg = ci.nextArgument();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        PrintStream err = new PrintStream(errorBytes);
        super.execute(line.toString(), out, err);
        if (bytes.size() > 0) {
            ci.print(new String(bytes.toByteArray()));
        }
        if (errorBytes.size() > 0) {
            ci.print("Error:\n");
            ci.print(new String(errorBytes.toByteArray()));
        }
    }

    public String getHelp() {
        return "\t" + super.getUsage() + " - " + super.getShortDescription() + "\n";
    }
}
