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

import java.util.Collections;
import java.util.List;

import org.apache.felix.gogo.runtime.EOFError;
import org.apache.felix.gogo.runtime.Parser.Program;
import org.apache.felix.gogo.runtime.Parser.Statement;
import org.apache.felix.gogo.runtime.SyntaxError;
import org.apache.felix.gogo.runtime.Token;
import org.jline.reader.ParsedLine;

public class Parser implements org.jline.reader.Parser {

    public ParsedLine parse(String line, int cursor) throws org.jline.reader.SyntaxError {
        try {
            return doParse(line, cursor);
        } catch (EOFError e) {
            throw new org.jline.reader.EOFError(e.line(), e.column(), e.getMessage(), e.missing());
        } catch (SyntaxError e) {
            throw new org.jline.reader.SyntaxError(e.line(), e.column(), e.getMessage());
        }
    }

    private ParsedLine doParse(CharSequence line, int cursor) throws SyntaxError {
        org.apache.felix.gogo.runtime.Parser parser = new org.apache.felix.gogo.runtime.Parser(line);
        Program program = parser.program();
        List<Statement> statements = parser.statements();
        // Find corresponding statement
        Statement statement = null;
        for (int i = statements.size() - 1; i >= 0; i--) {
            Statement s = statements.get(i);
            if (s.start() <= cursor) {
                boolean isOk = true;
                // check if there are only spaces after the previous statement
                if (s.start() + s.length() < cursor) {
                    for (int j = s.start() + s.length(); isOk && j < cursor; j++) {
                        isOk = Character.isWhitespace(line.charAt(j));
                    }
                }
                statement = s;
                break;
            }
        }
        if (statement != null) {
            return new ParsedLineImpl(program, statement, cursor, statement.tokens());
        } else {
            // TODO:
            return new ParsedLineImpl(program, program, cursor, Collections.<Token>singletonList(program));
        }
    }

}
