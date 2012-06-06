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
package org.apache.felix.gogo.shell;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class History {

    private static final int SIZE_DEFAULT = 100;

    private LinkedList<String> commands;

    private int limit;

    public History() {
        this.limit = SIZE_DEFAULT;
        this.commands = new LinkedList<String>();
    }

    CharSequence evaluate(final CharSequence commandLine) {

        /*
         * <pre>
         * hist = ( '!' spec ) | ( '^' subst ) .
         * spec = '!' ( '!' | idx | '?' find | string ) [ ':' [ 'a' | 'g' ] 's' regex ] . idx = [ '-' ] { 0..9 } .
         * find = string ( '?' | EOL ) .
         * subst = pat '^' repl '^' EOL .
         * regex = str pat str repl str EOL .
         * </pre>
         */

        final CharacterIterator ci = new StringCharacterIterator(commandLine.toString());

        String event;
        char c = ci.current();
        if (c == '!') {
            c = ci.next();
            if (c == '!') {
                event = this.commands.getLast();
                ci.next();
            } else if ((c >= '0' && c <= '9') || c == '-') {
                event = getCommand(ci);
            } else if (c == '?') {
                event = findContains(ci);
                ci.next();
            } else {
                ci.previous();
                event = findStartsWith(ci);
            }
        } else if (c == '^') {
            event = subst(ci, c, false, this.commands.getLast());
        } else {
            throw new IllegalArgumentException(commandLine + ": Unsupported event");
        }

        if (ci.current() == ':') {
            c = ci.next();
            boolean global = (c == 'a' || c == 'g');
            if (global) {
                c = ci.next();
            }
            if (c == 's') {
                event = subst(ci, ci.next(), global, event);
            }
        }

        return event;
    }

    /**
     * Returns the command history, oldest command first
     */
    Iterator<String> getHistory() {
        return this.commands.iterator();
    }

    void append(final CharSequence commandLine) {
        commands.add(commandLine.toString());
        if (commands.size() > this.limit) {
            commands.removeFirst();
        }
    }

    private String getCommand(final CharacterIterator ci) {
        final StringBuilder s = new StringBuilder();
        char c = ci.current();
        do {
            s.append(c);
            c = ci.next();
        } while (c >= '0' && c <= '9');
        final int n = Integer.parseInt(s.toString());
        final int pos = ((n < 0) ? this.commands.size() : -1) + n;
        if (pos >= 0 && pos < this.commands.size()) {
            return this.commands.get(pos);
        }
        throw new IllegalArgumentException("!" + n + ": event not found");
    }

    private String findContains(final CharacterIterator ci) {
        CharSequence part = findDelimiter(ci, '?');
        final ListIterator<String> iter = this.commands.listIterator(this.commands.size());
        while (iter.hasPrevious()) {
            String value = iter.previous();
            if (value.contains(part)) {
                return value;
            }
        }

        throw new IllegalArgumentException("No command containing '" + part + "' in the history");
    }

    private String findStartsWith(final CharacterIterator ci) {
        String part = findDelimiter(ci, ':').toString();
        final ListIterator<String> iter2 = this.commands.listIterator(this.commands.size());
        while (iter2.hasPrevious()) {
            String value = iter2.previous();
            if (value.startsWith(part)) {
                return value;
            }
        }

        throw new IllegalArgumentException("No command containing '" + part + "' in the history");
    }

    private String subst(final CharacterIterator ci, final char delimiter, final boolean replaceAll, final String event) {
        final String pattern = findDelimiter(ci, delimiter).toString();
        final String repl = findDelimiter(ci, delimiter).toString();
        if (pattern.length() == 0) {
            throw new IllegalArgumentException(":s" + event + ": substitution failed");
        }
        final Pattern regex = Pattern.compile(pattern);
        final Matcher m = regex.matcher(event);
        final StringBuffer res = new StringBuffer();

        if (!m.find()) {
            throw new IllegalArgumentException(":s" + event + ": substitution failed");
        }
        do {
            m.appendReplacement(res, repl);
        } while (replaceAll && m.find());
        m.appendTail(res);
        return res.toString();
    }

    private CharSequence findDelimiter(final CharacterIterator ci, char delimiter) {
        final StringBuilder b = new StringBuilder();
        for (char c = ci.next(); c != CharacterIterator.DONE && c != delimiter; c = ci.next()) {
            if (c == '\\') {
                c = ci.next();
            }
            b.append(c);
        }
        return b;
    }
}
