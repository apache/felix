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
package org.apache.felix.sandbox.scrplugin;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.codehaus.plexus.util.StringUtils;

/**
 * Simple xml writer.
 */
public class XMLWriter extends PrintWriter {

    String indent = "";

    XMLWriter(OutputStream descriptorStream) throws IOException {
        super(new OutputStreamWriter(descriptorStream, "UTF-8"));

        this.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    void indent() {
        this.indent += "  ";
    }

    void outdent() {
        if (this.indent.length() >= 2) {
            this.indent = this.indent.substring(2);
        }
    }

    void printElementStart(String name, boolean hasAttributes) {
        this.print(this.indent);
        this.print("<");
        this.print(name);
        if (!hasAttributes) {
            this.printElementStartClose(false);
        }
    }

    void printElementStartClose(boolean isEmpty) {
        if (isEmpty) {
            this.print(" /");
        }
        this.println('>');
    }

    void printElementEnd(String name) {
        this.print(this.indent);
        this.print("</");
        this.print(name);
        this.println('>');
    }

    void printAttribute(String name, String value) {
        if (!StringUtils.isEmpty(name) && value != null) {
            this.print(' ');
            this.print(name);
            this.print("=\"");
            this.print(value);
            this.print('"');
        }
    }
}
