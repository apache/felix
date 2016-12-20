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
 *
 * <p>
 * Copyright (c) 2006 John Reilly (www.inconspicuous.org) This work is a
 * translation from C to Java of jsmin.c published by Douglas Crockford.
 * Permission is hereby granted to use the Java version under the same
 * conditions as the jsmin.c on which it is based.
 * <p>
 * http://www.crockford.com/javascript/jsmin.html
 */
package org.apache.felix.configurator.impl.json;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;

public class JSMin {

    private static final int EOF = -1;

    private final PushbackReader in;

    private final Writer out;

    private int theA;

    private int theB;

    private int theLookahead = EOF;

    private int theX = EOF;

    private int theY = EOF;

    public JSMin(final Reader in, final Writer out) {
        this.in = new PushbackReader(in);
        this.out = out;
    }

    /**
     * isAlphanum -- return true if the character is a letter, digit, underscore,
     * dollar sign, or non-ASCII character.
     */
    private boolean isAlphanum(final int c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
            || (c >= 'A' && c <= 'Z') || c == '_' || c == '$' || c == '\\' || c > 126);
    }

    /**
     * get -- return the next character from stdin. Watch out for lookahead. If
     * the character is a control character, translate it to a space or
     * linefeed.
     */
    private int get() throws IOException {
        int c = theLookahead;
        theLookahead = EOF;
        if ( c == EOF ) {
            c = in.read();
        }

        if (c >= ' ' || c == '\n' || c == EOF) {
            return c;
        }

        if (c == '\r') {
            return '\n';
        }

        return ' ';
    }

    /**
     * peek -- get the next character without getting it.
     */
    private int peek() throws IOException {
        theLookahead = get();
        return theLookahead;
    }

    /**
     * next -- get the next character, excluding comments. peek() is used to see
     * if a '/' is followed by a '/' or '*'.
     */
    private int next() throws IOException {
        int c = get();
        if (c == '/') {
            switch (peek()) {
            case '/':
                for (;;) {
                    c = get();
                    if (c <= '\n') {
                        break;
                    }
                }
                break;
            case '*':
                get();
                while (c != ' ') {
                    switch (get()) {
                    case '*':
                        if (peek() == '/') {
                            get();
                            c = ' ';
                        }
                        break;
                    case EOF:
                        throw new IOException("Unterminated comment.");
                    }
                }
                break;
            }

        }
        theY = theX;
        theX = c;
        return c;
    }

    /**
     * action -- do something! What you do is determined by the argument:
     * <ul>
     *   <li>1 Output A. Copy B to A. Get the next B.</li>
     *   <li>2 Copy B to A. Get the next B. (Delete A).</li>
     *   <li>3 Get the next B. (Delete B).</li>
     * </ul>
     * action treats a string as a single character. Wow!<br/>
     * action recognizes a regular expression if it is preceded by ( or , or =.
     */
    void action(final int d) throws IOException {
        switch (d) {
        case 1:
            out.write(theA);
            if ((theY == '\n' || theY == ' ') &&
                (theA == '+' || theA == '-' || theA == '*' || theA == '/') &&
                (theB == '+' || theB == '-' || theB == '*' || theB == '/')) {
                out.write(theY);
            }
        case 2:
            theA = theB;

            if (theA == '\'' || theA == '"' || theA == '`') {
                for (;;) {
                    out.write(theA);
                    theA = get();
                    if (theA == theB) {
                        break;
                    }
                    if (theA == '\\') {
                        out.write(theA);
                        theA = get();
                    }
                    if ( theA == EOF) {
                        throw new IOException("Unterminated string literal.");
                    }
                }
            }

        case 3:
            theB = next();
            if (theB == '/'
                    && (theA == '(' || theA == ',' || theA == '=' || theA == ':'
                    || theA == '[' || theA == '!' || theA == '&' || theA == '|'
                    || theA == '?' || theA == '+' || theA == '-' || theA == '~'
                    || theA == '*' || theA == '/' || theA == '{' || theA == '\n')) {
                out.write(theA);
                if (theA == '/' || theA == '*') {
                    out.write(' ');
                }
                out.write(theB);
                for (;;) {
                    theA = get();
                    if (theA == '[') {
                        for (;;) {
                            out.write(theA);
                            theA = get();
                            if (theA == ']') {
                                break;
                            }
                            if (theA == '\\') {
                                out.write(theA);
                                theA = get();
                            }
                            if (theA == EOF) {
                                throw new IOException("Unterminated set in Regular Expression literal.");
                            }
                        }
                    } else if (theA == '/') {
                        switch (peek()) {
                        case '/':
                        case '*':
                            throw new IOException("Unterminated set in Regular Expression literal.");
                        }
                        break;
                    } else if (theA == '\\') {
                        out.write(theA);
                        theA = get();
                    } else if (theA == EOF) {
                        throw new IOException("Unterminated Regular Expression literal.");
                    }
                    out.write(theA);
                }
                theB = next();
            }
        }
    }

    /**
     * jsmin -- Copy the input to the output, deleting the characters which are
     * insignificant to JavaScript. Comments will be removed. Tabs will be
     * replaced with spaces. Carriage returns will be replaced with linefeeds.
     * Most spaces and linefeeds will be removed.
     */
    public void jsmin() throws IOException {
        if (peek() == 0xEF) {
            get();
            get();
            get();
        }
        theA = '\n';
        action(3);
        while (theA != EOF) {
            switch (theA) {
            case ' ':
                action(isAlphanum(theB) ? 1: 2);
                break;
            case '\n':
                switch (theB) {
                case '{':
                case '[':
                case '(':
                case '+':
                case '-':
                case '!':
                case '~':
                    action(1);
                    break;
                case ' ':
                    action(3);
                    break;
                default:
                    action(isAlphanum(theB) ? 1: 2);
                }
                break;
            default:
                switch (theB) {
                case ' ':
                    action(isAlphanum(theB) ? 1: 3);
                    break;
                case '\n':
                    switch (theA) {
                    case '}':
                    case ']':
                    case ')':
                    case '+':
                    case '-':
                    case '"':
                    case '\'':
                    case '`':
                        action(1);
                        break;
                    default:
                        action(isAlphanum(theB) ? 1: 3);
                    }
                    break;
                default:
                    action(1);
                    break;
                }
            }
        }
        out.flush();
    }
}