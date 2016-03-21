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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.service.command.CommandSession;
import org.jline.builtins.Options;

/**
 * Posix-like utilities.
 *
 * @see <a href="http://www.opengroup.org/onlinepubs/009695399/utilities/contents.html">
 * http://www.opengroup.org/onlinepubs/009695399/utilities/contents.html</a>
 */
public class Posix {
    static final String[] functions = {"cat", "echo", "grep", "sort", "sleep", "cd", "pwd", "ls"};

    static final String CWD = "_cwd";

    public static File _pwd(CommandSession session) {
        try {
            File cwd = (File) session.get(CWD);
            if (cwd == null) {
                cwd = new File(".").getCanonicalFile();
                session.put(CWD, cwd);
            }
            return cwd;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sort(CommandSession session, String[] argv) throws IOException {
        final String[] usage = {
                "sort -  writes sorted standard input to standard output.",
                "Usage: sort [OPTIONS] [FILES]",
                "  -? --help                    show help",
                "  -f --ignore-case             fold lower case to upper case characters",
                "  -r --reverse                 reverse the result of comparisons",
                "  -u --unique                  output only the first of an equal run",
                "  -t --field-separator=SEP     use SEP instead of non-blank to blank transition",
                "  -b --ignore-leading-blanks   ignore leading blancks",
                "     --numeric-sort            compare according to string numerical value",
                "  -k --key=KEY                 fields to use for sorting separated by whitespaces"};

        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            opt.usage(System.err);
            return;
        }

        List<String> args = opt.args();

        List<String> lines = new ArrayList<String>();
        if (!args.isEmpty()) {
            for (String filename : args) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        Shell.cwd(session).resolve(filename).toURL().openStream()));
                try {
                    read(reader, lines);
                } finally {
                    reader.close();
                }
            }
        } else {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            read(r, lines);
        }

        String separator = opt.get("field-separator");
        boolean caseInsensitive = opt.isSet("ignore-case");
        boolean reverse = opt.isSet("reverse");
        boolean ignoreBlanks = opt.isSet("ignore-leading-blanks");
        boolean numeric = opt.isSet("numeric-sort");
        boolean unique = opt.isSet("unique");
        List<String> sortFields = opt.getList("key");

        char sep = (separator == null || separator.length() == 0) ? '\0' : separator.charAt(0);
        Collections.sort(lines, new SortComparator(caseInsensitive, reverse, ignoreBlanks, numeric, sep, sortFields));
        String last = null;
        for (String s : lines) {
            if (!unique || last == null || !s.equals(last)) {
                System.out.println(s);
            }
            last = s;
        }
    }

    protected static void read(BufferedReader r, List<String> lines) throws IOException {
        for (String s = r.readLine(); s != null; s = r.readLine()) {
            lines.add(s);
        }
    }

    public static List<String> parseSubstring(String value) {
        List<String> pieces = new ArrayList<String>();
        StringBuilder ss = new StringBuilder();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
        boolean escaped = false;
        loop:
        for (; ; ) {
            if (idx >= value.length()) {
                if (wasStar) {
                    // insert last piece as "" to handle trailing star
                    rightstar = true;
                } else {
                    pieces.add(ss.toString());
                    // accumulate the last piece
                    // note that in the case of
                    // (cn=); this might be
                    // the string "" (!=null)
                }
                ss.setLength(0);
                break loop;
            }

            // Read the next character and account for escapes.
            char c = value.charAt(idx++);
            if (!escaped && ((c == '(') || (c == ')'))) {
                throw new IllegalArgumentException(
                        "Illegal value: " + value);
            } else if (!escaped && (c == '*')) {
                if (wasStar) {
                    // encountered two successive stars;
                    // I assume this is illegal
                    throw new IllegalArgumentException("Invalid filter string: " + value);
                }
                if (ss.length() > 0) {
                    pieces.add(ss.toString()); // accumulate the pieces
                    // between '*' occurrences
                }
                ss.setLength(0);
                // if this is a leading star, then track it
                if (pieces.size() == 0) {
                    leftstar = true;
                }
                wasStar = true;
            } else if (!escaped && (c == '\\')) {
                escaped = true;
            } else {
                escaped = false;
                wasStar = false;
                ss.append(c);
            }
        }
        if (leftstar || rightstar || pieces.size() > 1) {
            // insert leading and/or trailing "" to anchor ends
            if (rightstar) {
                pieces.add("");
            }
            if (leftstar) {
                pieces.add(0, "");
            }
        }
        return pieces;
    }

    public static boolean compareSubstring(List<String> pieces, String s) {
        // Walk the pieces to match the string
        // There are implicit stars between each piece,
        // and the first and last pieces might be "" to anchor the match.
        // assert (pieces.length > 1)
        // minimal case is <string>*<string>

        boolean result = true;
        int len = pieces.size();

        int index = 0;

        loop:
        for (int i = 0; i < len; i++) {
            String piece = pieces.get(i);

            // If this is the first piece, then make sure the
            // string starts with it.
            if (i == 0) {
                if (!s.startsWith(piece)) {
                    result = false;
                    break loop;
                }
            }

            // If this is the last piece, then make sure the
            // string ends with it.
            if (i == len - 1) {
                if (s.endsWith(piece)) {
                    result = true;
                } else {
                    result = false;
                }
                break loop;
            }

            // If this is neither the first or last piece, then
            // make sure the string contains it.
            if ((i > 0) && (i < (len - 1))) {
                index = s.indexOf(piece, index);
                if (index < 0) {
                    result = false;
                    break loop;
                }
            }

            // Move string index beyond the matching piece.
            index += piece.length();
        }

        return result;
    }

    private static void cat(final BufferedReader reader, boolean displayLineNumbers) throws IOException {
        String line;
        int lineno = 1;
        try {
            while ((line = reader.readLine()) != null) {
                if (displayLineNumbers) {
                    System.out.print(String.format("%6d  ", lineno++));
                }
                System.out.println(line);
            }
        } finally {
            reader.close();
        }
    }

    private static <T> void addAll(List<? super T> list, T[] array) {
        if (array != null) {
            Collections.addAll(list, array);
        }
    }

    public File pwd(CommandSession session, String[] argv) throws IOException {
        final String[] usage = {
                "pwd - get current directory",
                "Usage: pwd [OPTIONS]",
                "  -? --help                show help"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            opt.usage(System.err);
            return null;
        }
        if (!opt.args().isEmpty()) {
            System.err.println("usage: pwd");
            return null;
        }
        File cwd = (File) session.get(CWD);
        if (cwd == null) {
            cwd = new File(".").getCanonicalFile();
            session.put(CWD, cwd);
        }
        return cwd;
    }

    public File cd(CommandSession session, String[] argv)
            throws IOException {
        final String[] usage = {
                "cd - get current directory",
                "Usage: cd [OPTIONS] DIRECTORY",
                "  -? --help                show help"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            opt.usage(System.err);
            return null;
        }
        if (opt.args().size() != 1) {
            System.err.println("usage: cd DIRECTORY");
            return null;
        }
        File cwd = pwd(session, new String[0]);
        String dir = opt.args().get(0);

        URI curUri = cwd.toURI();
        URI newUri = curUri.resolve(dir);

        cwd = new File(newUri);
        if (!cwd.exists()) {
            throw new IOException("Directory does not exist");
        } else if (!cwd.isDirectory()) {
            throw new IOException("Target is not a directory");
        }
        session.put(CWD, cwd.getCanonicalFile());
        return cwd;
    }

    public Collection<File> ls(CommandSession session, String[] argv) throws IOException {
        final String[] usage = {
                "ls - list files",
                "Usage: ls [OPTIONS] PATTERNS...",
                "  -? --help                show help"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            opt.usage(System.err);
            return null;
        }
        if (opt.args().isEmpty()) {
            opt.args().add("*");
        }
        opt.args().remove(0);
        List<File> files = new ArrayList<File>();
        for (String pattern : opt.args()) {
            pattern = ((pattern == null) || (pattern.length() == 0)) ? "." : pattern;
            pattern = ((pattern.charAt(0) != File.separatorChar) && (pattern.charAt(0) != '.'))
                    ? "./" + pattern : pattern;
            int idx = pattern.lastIndexOf(File.separatorChar);
            String parent = (idx < 0) ? "." : pattern.substring(0, idx + 1);
            String target = (idx < 0) ? pattern : pattern.substring(idx + 1);

            File actualParent = ((parent.charAt(0) == File.separatorChar)
                    ? new File(parent)
                    : new File(pwd(session, new String[0]), parent)).getCanonicalFile();

            idx = target.indexOf(File.separatorChar, idx);
            boolean isWildcarded = (target.indexOf('*', idx) >= 0);
            if (isWildcarded) {
                if (!actualParent.exists()) {
                    throw new IOException("File does not exist");
                }
                final List<String> pieces = parseSubstring(target);
                addAll(files, actualParent.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        return compareSubstring(pieces, pathname.getName());
                    }
                }));
            } else {
                File actualTarget = new File(actualParent, target).getCanonicalFile();
                if (!actualTarget.exists()) {
                    throw new IOException("File does not exist");
                }
                if (actualTarget.isDirectory()) {
                    addAll(files, actualTarget.listFiles());
                } else {
                    files.add(actualTarget);
                }
            }
        }
        return files;
    }

    public void cat(CommandSession session, String[] argv) throws Exception {
        final String[] usage = {
                "cat - concatenate and print FILES",
                "Usage: cat [OPTIONS] [FILES]",
                "  -? --help                show help",
                "  -n                       number the output lines, starting at 1"
        };

        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            opt.usage(System.err);
            return;
        }

        List<String> args = opt.args();
        if (args.isEmpty()) {
            args = Collections.singletonList("-");
        }

        URI cwd = Shell.cwd(session);
        for (String arg : args) {
            InputStream is;
            if ("-".equals(arg)) {
                is = System.in;

            } else {
                is = cwd.resolve(arg).toURL().openStream();
            }
            cat(new BufferedReader(new InputStreamReader(is)), opt.isSet("n"));
        }
    }

    public void echo(Object[] argv) {
        final String[] usage = {
                "echo - echoes or prints ARGUMENT to standard output",
                "Usage: echo [OPTIONS] [ARGUMENTS]",
                "  -? --help                show help",
                "  -n                       no trailing new line"
        };

        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            opt.usage(System.err);
            return;
        }

        List<String> args = opt.args();
        StringBuilder buf = new StringBuilder();
        if (args != null) {
            for (String arg : args) {
                if (buf.length() > 0)
                    buf.append(' ');
                buf.append(arg);
            }
        }
        if (opt.isSet("n")) {
            System.out.print(buf);
        } else {
            System.out.println(buf);
        }
    }

    public boolean grep(CommandSession session, String[] argv) throws IOException {
        final String[] usage = {
                "grep -  search for PATTERN in each FILE or standard input.",
                "Usage: grep [OPTIONS] PATTERN [FILES]",
                "  -? --help                show help",
                "  -i --ignore-case         ignore case distinctions",
                "  -n --line-number         prefix each line with line number within its input file",
                "  -q --quiet, --silent     suppress all normal output",
                "  -v --invert-match        select non-matching lines"};

        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            opt.usage(System.err);
            return true;
        }

        List<String> args = opt.args();

        if (args.isEmpty()) {
            throw opt.usageError("no pattern supplied.");
        }

        String regex = args.remove(0);
        if (opt.isSet("ignore-case")) {
            regex = "(?i)" + regex;
        }

        if (args.isEmpty()) {
            args.add(null);
        }

        StringBuilder buf = new StringBuilder();

        if (args.size() > 1) {
            buf.append("%1$s:");
        }

        if (opt.isSet("line-number")) {
            buf.append("%2$s:");
        }

        buf.append("%3$s");
        String format = buf.toString();

        Pattern pattern = Pattern.compile(regex);
        boolean status = true;
        boolean match = false;

        for (String arg : args) {
            InputStream in = null;

            try {
                URI cwd = Shell.cwd(session);
                in = (arg == null) ? System.in : cwd.resolve(arg).toURL().openStream();

                BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
                int line = 0;
                String s;
                while ((s = rdr.readLine()) != null) {
                    line++;
                    Matcher matcher = pattern.matcher(s);
                    if (!(matcher.find() ^ !opt.isSet("invert-match"))) {
                        match = true;
                        if (opt.isSet("quiet"))
                            break;

                        System.out.println(String.format(format, arg, line, s));
                    }
                }

                if (match && opt.isSet("quiet")) {
                    break;
                }
            } catch (IOException e) {
                System.err.println("grep: " + e.getMessage());
                status = false;
            } finally {
                if (arg != null && in != null) {
                    in.close();
                }
            }
        }

        return match && status;
    }

    public void sleep(String[] argv) throws InterruptedException {
        final String[] usage = {
                "sleep -  suspend execution for an interval of time",
                "Usage: sleep seconds",
                "  -? --help                    show help"};

        Options opt = Options.compile(usage).parse(argv);

        if (opt.isSet("help")) {
            opt.usage(System.err);
            return;
        }

        List<String> args = opt.args();
        if (args.size() != 1) {
            System.err.println("usage: sleep seconds");
        } else {
            int s = Integer.parseInt(args.get(0));
            Thread.sleep(s * 1000);
        }
    }

    public static class SortComparator implements Comparator<String> {

        private static Pattern fpPattern;

        static {
            final String Digits = "(\\p{Digit}+)";
            final String HexDigits = "(\\p{XDigit}+)";
            final String Exp = "[eE][+-]?" + Digits;
            final String fpRegex = "([\\x00-\\x20]*[+-]?(NaN|Infinity|(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|(\\.(" + Digits + ")(" + Exp + ")?)|(((0[xX]" + HexDigits + "(\\.)?)|(0[xX]" + HexDigits + "?(\\.)" + HexDigits + "))[pP][+-]?" + Digits + "))" + "[fFdD]?))[\\x00-\\x20]*)(.*)";
            fpPattern = Pattern.compile(fpRegex);
        }

        private boolean caseInsensitive;
        private boolean reverse;
        private boolean ignoreBlanks;
        private boolean numeric;
        private char separator;
        private List<Key> sortKeys;

        public SortComparator(boolean caseInsensitive,
                              boolean reverse,
                              boolean ignoreBlanks,
                              boolean numeric,
                              char separator,
                              List<String> sortFields) {
            this.caseInsensitive = caseInsensitive;
            this.reverse = reverse;
            this.separator = separator;
            this.ignoreBlanks = ignoreBlanks;
            this.numeric = numeric;
            if (sortFields == null || sortFields.size() == 0) {
                sortFields = new ArrayList<String>();
                sortFields.add("1");
            }
            sortKeys = new ArrayList<Key>();
            for (String f : sortFields) {
                sortKeys.add(new Key(f));
            }
        }

        public int compare(String o1, String o2) {
            int res = 0;

            List<Integer> fi1 = getFieldIndexes(o1);
            List<Integer> fi2 = getFieldIndexes(o2);
            for (Key key : sortKeys) {
                int[] k1 = getSortKey(o1, fi1, key);
                int[] k2 = getSortKey(o2, fi2, key);
                if (key.numeric) {
                    Double d1 = getDouble(o1, k1[0], k1[1]);
                    Double d2 = getDouble(o2, k2[0], k2[1]);
                    res = d1.compareTo(d2);
                } else {
                    res = compareRegion(o1, k1[0], k1[1], o2, k2[0], k2[1], key.caseInsensitive);
                }
                if (res != 0) {
                    if (key.reverse) {
                        res = -res;
                    }
                    break;
                }
            }
            return res;
        }

        protected Double getDouble(String s, int start, int end) {
            Matcher m = fpPattern.matcher(s.substring(start, end));
            m.find();
            return new Double(s.substring(0, m.end(1)));
        }

        protected int compareRegion(String s1, int start1, int end1, String s2, int start2, int end2, boolean caseInsensitive) {
            int n1 = end1, n2 = end2;
            for (int i1 = start1, i2 = start2; i1 < end1 && i2 < n2; i1++, i2++) {
                char c1 = s1.charAt(i1);
                char c2 = s2.charAt(i2);
                if (c1 != c2) {
                    if (caseInsensitive) {
                        c1 = Character.toUpperCase(c1);
                        c2 = Character.toUpperCase(c2);
                        if (c1 != c2) {
                            c1 = Character.toLowerCase(c1);
                            c2 = Character.toLowerCase(c2);
                            if (c1 != c2) {
                                return c1 - c2;
                            }
                        }
                    } else {
                        return c1 - c2;
                    }
                }
            }
            return n1 - n2;
        }

        protected int[] getSortKey(String str, List<Integer> fields, Key key) {
            int start;
            int end;
            if (key.startField * 2 <= fields.size()) {
                start = fields.get((key.startField - 1) * 2);
                if (key.ignoreBlanksStart) {
                    while (start < fields.get((key.startField - 1) * 2 + 1) && Character.isWhitespace(str.charAt(start))) {
                        start++;
                    }
                }
                if (key.startChar > 0) {
                    start = Math.min(start + key.startChar - 1, fields.get((key.startField - 1) * 2 + 1));
                }
            } else {
                start = 0;
            }
            if (key.endField > 0 && key.endField * 2 <= fields.size()) {
                end = fields.get((key.endField - 1) * 2);
                if (key.ignoreBlanksEnd) {
                    while (end < fields.get((key.endField - 1) * 2 + 1) && Character.isWhitespace(str.charAt(end))) {
                        end++;
                    }
                }
                if (key.endChar > 0) {
                    end = Math.min(end + key.endChar - 1, fields.get((key.endField - 1) * 2 + 1));
                }
            } else {
                end = str.length();
            }
            return new int[]{start, end};
        }

        protected List<Integer> getFieldIndexes(String o) {
            List<Integer> fields = new ArrayList<Integer>();
            if (o.length() > 0) {
                if (separator == '\0') {
                    int i = 0;
                    fields.add(0);
                    for (int idx = 1; idx < o.length(); idx++) {
                        if (Character.isWhitespace(o.charAt(idx)) && !Character.isWhitespace(o.charAt(idx - 1))) {
                            fields.add(idx - 1);
                            fields.add(idx);
                        }
                    }
                    fields.add(o.length() - 1);
                } else {
                    int last = -1;
                    for (int idx = o.indexOf(separator); idx >= 0; idx = o.indexOf(separator, idx + 1)) {
                        if (last >= 0) {
                            fields.add(last);
                            fields.add(idx - 1);
                        } else if (idx > 0) {
                            fields.add(0);
                            fields.add(idx - 1);
                        }
                        last = idx + 1;
                    }
                    if (last < o.length()) {
                        fields.add(last < 0 ? 0 : last);
                        fields.add(o.length() - 1);
                    }
                }
            }
            return fields;
        }

        public class Key {
            int startField;
            int startChar;
            int endField;
            int endChar;
            boolean ignoreBlanksStart;
            boolean ignoreBlanksEnd;
            boolean caseInsensitive;
            boolean reverse;
            boolean numeric;

            public Key(String str) {
                boolean modifiers = false;
                boolean startPart = true;
                boolean inField = true;
                boolean inChar = false;
                for (char c : str.toCharArray()) {
                    switch (c) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            if (!inField && !inChar) {
                                throw new IllegalArgumentException("Bad field syntax: " + str);
                            }
                            if (startPart) {
                                if (inChar) {
                                    startChar = startChar * 10 + (c - '0');
                                } else {
                                    startField = startField * 10 + (c - '0');
                                }
                            } else {
                                if (inChar) {
                                    endChar = endChar * 10 + (c - '0');
                                } else {
                                    endField = endField * 10 + (c - '0');
                                }
                            }
                            break;
                        case '.':
                            if (!inField) {
                                throw new IllegalArgumentException("Bad field syntax: " + str);
                            }
                            inField = false;
                            inChar = true;
                            break;
                        case 'n':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            numeric = true;
                            break;
                        case 'f':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            caseInsensitive = true;
                            break;
                        case 'r':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            reverse = true;
                            break;
                        case 'b':
                            inField = false;
                            inChar = false;
                            modifiers = true;
                            if (startPart) {
                                ignoreBlanksStart = true;
                            } else {
                                ignoreBlanksEnd = true;
                            }
                            break;
                        case ',':
                            inField = true;
                            inChar = false;
                            startPart = false;
                            break;
                        default:
                            throw new IllegalArgumentException("Bad field syntax: " + str);
                    }
                }
                if (!modifiers) {
                    ignoreBlanksStart = ignoreBlanksEnd = SortComparator.this.ignoreBlanks;
                    reverse = SortComparator.this.reverse;
                    caseInsensitive = SortComparator.this.caseInsensitive;
                    numeric = SortComparator.this.numeric;
                }
                if (startField < 1) {
                    throw new IllegalArgumentException("Bad field syntax: " + str);
                }
            }
        }
    }

}
