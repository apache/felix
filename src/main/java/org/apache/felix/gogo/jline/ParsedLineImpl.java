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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.runtime.Parser.Program;
import org.apache.felix.gogo.runtime.Token;
import org.jline.reader.ParsedLine;

public class ParsedLineImpl implements ParsedLine {

    private final Program program;
    private final String source;
    private final int cursor;
    private final List<String> tokens;
    private final int wordIndex;
    private final int wordCursor;

    public ParsedLineImpl(Program program, Token line, int cursor, List<Token> tokens) {
        this.program = program;
        this.source = line.toString();
        this.cursor = cursor - line.start();
        this.tokens = new ArrayList<>();
        for (Token token : tokens) {
            this.tokens.add(token.toString());
        }
        int wi = tokens.size();
        int wc = 0;
        if (cursor >= 0) {
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                if (t.start() > cursor) {
                    wi = i;
                    wc = 0;
                    this.tokens.add(i, "");
                    break;
                }
                if (t.start() + t.length() >= cursor) {
                    wi = i;
                    wc = cursor - t.start();
                    break;
                }
            }
        }
        if (wi == tokens.size()) {
            this.tokens.add("");
        }
        wordIndex = wi;
        wordCursor = wc;
    }

    public String word() {
        return tokens.get(wordIndex());
    }

    public int wordCursor() {
        return wordCursor;
    }

    public int wordIndex() {
        return wordIndex;
    }

    public List<String> words() {
        return tokens;
    }

    public String line() {
        return source;
    }

    public int cursor() {
        return cursor;
    }

    public Program program() {
        return program;
    }
}
