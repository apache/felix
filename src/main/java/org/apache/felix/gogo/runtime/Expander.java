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
package org.apache.felix.gogo.runtime;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@SuppressWarnings("fallthrough")
public class Expander extends BaseTokenizer
{

    /**
     * expand variables, quotes and escapes in word.
     */
    public static Object expand(CharSequence word, Evaluate eval) throws Exception
    {
        return expand(word, eval, false);
    }

    private static Object expand(CharSequence word, Evaluate eval, boolean inQuote) throws Exception
    {
        return new Expander(word, eval, inQuote).expand();
    }

    private final Evaluate evaluate;
    private final boolean inQuote;

    public Expander(CharSequence text, Evaluate evaluate, boolean inQuote)
    {
        super(text);
        this.evaluate = evaluate;
        this.inQuote = inQuote;
    }

    public Object expand(CharSequence word) throws Exception
    {
        return expand(word, evaluate, inQuote);
    }


    private Object expand() throws Exception
    {
        Object expanded = doExpand();
        if (expanded instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object o : ((List) expanded)) {
                if (o instanceof CharSequence) {
                    list.addAll(generateFileNames((CharSequence) o));
                } else {
                    list.add(o);
                }
            }
            List<Object> unquoted = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof CharSequence) {
                    unquoted.add(unquote((CharSequence) o));
                } else {
                    unquoted.add(o);
                }
            }
            if (unquoted.size() == 1) {
                return unquoted.get(0);
            }
            if (expanded instanceof ArgList) {
                return new ArgList(unquoted);
            } else {
                return unquoted;
            }
        } else if (expanded instanceof CharSequence) {
            List<? extends CharSequence> list = generateFileNames((CharSequence) expanded);
            List<CharSequence> unquoted = new ArrayList<>();
            for (CharSequence o : list) {
                unquoted.add(unquote(o));
            }
            if (unquoted.size() == 1) {
                return unquoted.get(0);
            }
            return new ArgList(unquoted);
        }
        return expanded;
    }

    private CharSequence unquote(CharSequence arg) {
        if (inQuote) {
            return arg;
        }
        boolean hasEscape = false;
        for (int i = 0; i < arg.length(); i++) {
            int c = arg.charAt(i);
            if (c == '\\' || c == '"' || c == '\'') {
                hasEscape = true;
                break;
            }
        }
        if (!hasEscape) {
            return arg;
        }
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;
        StringBuilder buf = new StringBuilder(arg.length());
        for (int i = 0; i < arg.length(); i++) {
            char c = arg.charAt(i);
            if (doubleQuoted && escaped) {
                if (c != '"' && c != '\\' && c != '$' && c != '%') {
                    buf.append('\\');
                }
                buf.append(c);
                escaped = false;
            }
            else if (escaped) {
                buf.append(c);
                escaped = false;
            }
            else if (singleQuoted) {
                if (c == '\'') {
                    singleQuoted = false;
                } else {
                    buf.append(c);
                }
            }
            else if (doubleQuoted) {
                if (c == '\\') {
                    escaped = true;
                }
                else if (c == '\"') {
                    doubleQuoted = false;
                }
                else {
                    buf.append(c);
                }
            }
            else if (c == '\\') {
                escaped = true;
            }
            else if (c == '\'') {
                singleQuoted = true;
            }
            else if (c == '"') {
                doubleQuoted = true;
            }
            else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    protected List<? extends CharSequence> generateFileNames(CharSequence arg) throws IOException {
        // Disable if currentDir is not set
        Path currentDir = evaluate.currentDir();
        if (currentDir == null || inQuote) {
            return Collections.singletonList(arg);
        }
        // Search for unquoted escapes
        boolean hasUnescapedReserved = false;
        boolean escaped = false;
        boolean doubleQuoted = false;
        boolean singleQuoted = false;
        StringBuilder buf = new StringBuilder(arg.length());
        String pfx = "";
        for (int i = 0; i < arg.length(); i++) {
            char c = arg.charAt(i);
            if (doubleQuoted && escaped) {
                if (c != '"' && c != '\\' && c != '$' && c != '%') {
                    buf.append('\\');
                }
                buf.append(c);
                escaped = false;
            }
            else if (escaped) {
                buf.append(c);
                escaped = false;
            }
            else if (singleQuoted) {
                if (c == '\'') {
                    singleQuoted = false;
                } else {
                    buf.append(c);
                }
            }
            else if (doubleQuoted) {
                if (c == '\\') {
                    escaped = true;
                }
                else if (c == '\"') {
                    doubleQuoted = false;
                }
                else {
                    buf.append(c);
                }
            }
            else if (c == '\\') {
                escaped = true;
            }
            else if (c == '\'') {
                singleQuoted = true;
            }
            else if (c == '"') {
                doubleQuoted = true;
            }
            else {
                if ("*(|<[?".indexOf(c) >= 0 && !hasUnescapedReserved) {
                    hasUnescapedReserved = true;
                    pfx = buf.toString();
                }
                buf.append(c);
            }
        }
        if (!hasUnescapedReserved) {
            return Collections.singletonList(arg);
        }

        String org = buf.toString();
        List<String> expanded = new ArrayList<>();
        Path dir;
        String prefix;
        if (pfx.indexOf('/') >= 0) {
            pfx = pfx.substring(0, pfx.lastIndexOf('/'));
            arg = org.substring(pfx.length() + 1);
            dir = currentDir.resolve(pfx).normalize();
            prefix = pfx + "/";
        } else {
            dir = currentDir;
            prefix = "";
        }
        PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:" + arg);
        Files.walkFileTree(dir,
                EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE,
                new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.equals(dir)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (Files.isHidden(file)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        Path r = dir.relativize(file);
                        if (matcher.matches(r)) {
                            expanded.add(prefix + r.toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!Files.isHidden(file)) {
                            Path r = dir.relativize(file);
                            if (matcher.matches(r)) {
                                expanded.add(prefix + r.toString());
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
        Collections.sort(expanded);
        if (expanded.isEmpty()) {
            throw new IOException("no matches found: " + org);
        }
        return expanded;
    }


    private Object doExpand() throws Exception
    {
        final String special = "%$\\\"'";
        int i = text.length();
        while ((--i >= 0) && (special.indexOf(text.charAt(i)) == -1));
        // shortcut if word doesn't contain any special characters
        if (i < 0)
            return text;

        StringBuilder buf = new StringBuilder();
        Token value;

        while (ch != EOT)
        {
            int start = index;

            switch (ch)
            {
                case '%':
                    Object exp = expandExp();

                    if (EOT == ch && buf.length() == 0)
                    {
                        return exp;
                    }

                    if (null != exp)
                    {
                        buf.append(exp);
                    }

                    continue; // expandVar() has already read next char

                case '$':
                    // Posix quote
                    if (peek() == '\'') {
                        getch();
                        skipQuote();
                        value = text.subSequence(start + 1, index - 1);
                        getch();
                        buf.append("\'");
                        buf.append(ansiEscape(value));
                        buf.append("\'");
                    }
                    // Parameter expansion
                    else {
                        Object val = expandVar();
                        if (EOT == ch && buf.length() == 0) {
                            return val;
                        }
                        if (null != val) {
                            buf.append(val);
                        }
                    }
                    continue; // expandVar() has already read next char

                case '\\':
                    buf.append(ch);
                    if (peek() != EOT) {
                        getch();
                        buf.append(ch);
                    }
                    break;

                case '"':
                    skipQuote();
                    value = text.subSequence(start, index - 1);
                    getch();
                    Object expand = expand(value, evaluate, true);
                    if (eot() && buf.length() == 0)
                    {
                        if (expand instanceof ArgList) {
                            return new ArgList((ArgList) expand).stream()
                                    .map(String::valueOf)
                                    .map(s -> "\"" + s + "\"").collect(Collectors.toList());
                        } else if (expand instanceof Collection) {
                            return ((Collection) expand).stream().map(String::valueOf).collect(Collectors.joining(" ", "\"", "\""));
                        } else if (expand != null) {
                            return "\"" + expand.toString() + "\"";
                        } else {
                            return "";
                        }
                    }
                    if (expand instanceof Collection) {
                        boolean first = true;
                        buf.append("\"");
                        for (Object o : ((Collection) expand)) {
                            if (!first) {
                                buf.append(" ");
                            }
                            first = false;
                            buf.append(o);
                        }
                        buf.append("\"");
                    }
                    else if (expand != null) {
                        buf.append("\"");
                        buf.append(expand.toString());
                        buf.append("\"");
                    }
                    continue; // has already read next char

                case '\'':
                    if (!inQuote)
                    {
                        skipQuote();
                        value = text.subSequence(start - 1, index);

                        if (eot() && buf.length() == 0)
                        {
                            return value;
                        }

                        buf.append(value);
                        break;
                    }
                    // else fall through
                default:
                    buf.append(ch);
            }

            getch();
        }

        return buf.toString();
    }

    private CharSequence ansiEscape(CharSequence arg) {
        StringBuilder buf = new StringBuilder(arg.length());
        for (int i = 0; i < arg.length(); i++) {
            int c = arg.charAt(i);
            int ch;
            if (c == '\\') {
                c = i < arg.length() - 1 ? arg.charAt(++i) : '\\';
                switch (c) {
                    case 'a':
                        buf.append('\u0007');
                        break;
                    case 'n':
                        buf.append('\n');
                        break;
                    case 't':
                        buf.append('\t');
                        break;
                    case 'r':
                        buf.append('\r');
                        break;
                    case '\\':
                        buf.append('\\');
                        break;
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
                        ch = 0;
                        for (int j = 0; j < 3; j++) {
                            c = i < arg.length() - 1 ? arg.charAt(++i) : -1;
                            if (c >= 0) {
                                ch = ch * 8 + (c - '0');
                            }
                        }
                        buf.append((char) ch);
                        break;
                    case 'u':
                        ch = 0;
                        for (int j = 0; j < 4; j++) {
                            c = i < arg.length() - 1 ? arg.charAt(++i) : -1;
                            if (c >= 0) {
                                if (c >= 'A' && c <= 'F') {
                                    ch = ch * 16 + (c - 'A' + 10);
                                } else if (c >= 'a' && c <= 'f') {
                                    ch = ch * 16 + (c - 'a' + 10);
                                } else if (c >= '0' && c <= '9') {
                                    ch = ch * 16 + (c - '0');
                                } else {
                                    i--;
                                    break;
                                }
                            }
                        }
                        buf.append((char) ch);
                        break;
                    default:
                        buf.append((char) c);
                        break;
                }
            } else {
                buf.append((char) c);
            }
        }
        return buf;
    }

    private Object expandExp()
    {
        assert '%' == ch;
        Object val;

        if (getch() == '(')
        {
            val = evaluate.expr(group());
            getch();
            return val;
        }
        else
        {
            throw new SyntaxError(line, column, "bad expression: " + text);
        }
    }

    private Token group()
    {
        final char push = ch;
        final char pop;

        switch (ch)
        {
            case '{':
                pop = '}';
                break;
            case '(':
                pop = ')';
                break;
            case '[':
                pop = ']';
                break;
            default:
                assert false;
                pop = 0;
        }

        short sLine = line;
        short sCol = column;
        int start = index;
        int depth = 1;

        while (true)
        {
            boolean comment = false;

            switch (ch)
            {
                case '{':
                case '(':
                case '[':
                case '\n':
                    comment = true;
                    break;
            }

            if (getch() == EOT)
            {
                throw new EOFError(sLine, sCol, "unexpected EOT looking for matching '"
                        + pop + "'", "compound", Character.toString(pop));
            }

            // don't recognize comments that start within a word
            if (comment || isBlank(ch))
                skipSpace();

            switch (ch)
            {
                case '"':
                case '\'':
                    skipQuote();
                    break;

                case '\\':
                    ch = escape();
                    break;

                default:
                    if (push == ch)
                        depth++;
                    else if (pop == ch && --depth == 0)
                        return text.subSequence(start, index - 1);
            }
        }

    }

    private Object expandVar() throws Exception
    {
        assert '$' == ch;

        Object val = null;

        short sLine = line;
        short sCol = column;

        if (getch() != '{')
        {
            if ('(' == ch)
            { // support $(...) FELIX-2433
                int start = index - 1;
                find(')', '(');
                Token p = text.subSequence(start, index);
                val = evaluate.eval(new Parser(p).sequence());
                getch();
            }
            else
            {
                int start = index - 1;
                while (isName(ch))
                {
                    getch();
                }

                if (index - 1 == start)
                {
                    val = "$";
                }
                else
                {
                    String name = text.subSequence(start, index - 1).toString();
                    val = evaluate.get(name);
                }
            }
        }
        else
        {
            getch();

            boolean flagk = false;
            boolean flagv = false;
            boolean flagP = false;
            boolean flagC = false;
            boolean flagL = false;
            boolean flagU = false;
            boolean flagG = false;
            boolean flagExpand = false;
            if (ch == '(') {
                getch();
                while (ch != EOT && ch != ')') {
                    switch (ch) {
                        case 'P':
                            flagP = true;
                            break;
                        case '@':
                            flagExpand = true;
                            break;
                        case 'G':
                            flagG = true;
                            break;
                        case 'k':
                            flagk = true;
                            break;
                        case 'v':
                            flagv = true;
                            break;
                        case 'C':
                            flagC = true;
                            flagL = false;
                            flagU = false;
                            break;
                        case 'L':
                            flagC = false;
                            flagL = true;
                            flagU = false;
                            break;
                        case 'U':
                            flagC = false;
                            flagL = false;
                            flagU = true;
                            break;
                        default:
                            throw new SyntaxError(line, column, "unsupported flag: " + ch);
                    }
                    getch();
                }
                getch();
            }

            if (ch == '+') {
                getch();
                val = getAndEvaluateName();
            }
            else {
                boolean computeLength = false;
                boolean wordSplit = false;
                if (ch == '#') {
                    computeLength = true;
                    getch();
                }
                if (ch == '=') {
                    wordSplit = true;
                    getch();
                }

                Object val1 = getName('}');

                if (ch == '}' || ch == '[') {
                    val = val1 instanceof Token ? evaluate.get(expand((Token) val1).toString()) : val1;
                }
                else {
                    int start = index - 1;
                    while (ch != EOT && ch != '}' && ":-+=?#%/".indexOf(ch) >= 0) {
                        getch();
                    }
                    Token op = text.subSequence(start, index - 1);
                    if (Token.eq("-", op) || Token.eq(":-", op)) {
                        val1 = val1 instanceof Token ? evaluate.get(expand((Token) val1).toString()) : val1;
                        Object val2 = getValue();
                        val = val1 == null ? val2 : val1;
                    }
                    else if (Token.eq("+", op) || Token.eq(":+", op)) {
                        val1 = val1 instanceof Token ? evaluate.get(expand((Token) val1).toString()) : val1;
                        Object val2 = getValue();
                        val = val1 != null ? val2 : null;
                    }
                    else if (Token.eq("=", op) || Token.eq(":=", op) || Token.eq("::=", op)) {
                        if (!(val1 instanceof Token)) {
                            throw new SyntaxError(line, column, "not an identifier");
                        }
                        String name = expand((Token) val1).toString();
                        val1 = evaluate.get(name);
                        val = getValue();
                        if (Token.eq("::=", op) || val1 == null) {
                            evaluate.put(name, val);
                        }
                    }
                    else if (Token.eq("?", op) || Token.eq(":?", op)) {
                        String name;
                        if (val1 instanceof Token) {
                            name = expand((Token) val1).toString();
                            val = evaluate.get(name);
                        } else {
                            name = "";
                            val = val1;
                        }
                        if (val == null || val.toString().length() == 0) {
                            throw new IllegalArgumentException(name + ": parameter not set");
                        }
                    }
                    else if (Token.eq("#", op) || Token.eq("##", op)
                            || Token.eq("%", op) || Token.eq("%%", op)
                            || Token.eq("/", op) || Token.eq("//", op)) {
                        val1 = val1 instanceof Token ? evaluate.get(expand((Token) val1).toString()) : val1;
                        Object val2 = getValue();
                        if (val2 != null) {
                            String p = toRegexPattern(val2.toString(), op.length() == 1);
                            String r;
                            if (op.charAt(0) == '/') {
                                if (ch == '/') {
                                    getch();
                                    r = getValue().toString();
                                } else {
                                    r = "";
                                }
                            } else {
                                p = toRegexPattern(val2.toString(), op.length() == 1);
                                r = "";
                            }
                            String m = op.charAt(0) == '#' ? "^" + p : op.charAt(0) == '%' ? p + "$" : p;
                            if (val1 instanceof Map) {
                                val1 = toList((Map) val1, flagk, flagv);
                            }
                            if (val1 instanceof Collection) {
                                List<String> l = new ArrayList<>();
                                for (Object o : ((Collection) val1)) {
                                    if (flagG) {
                                        l.add(o.toString().replaceAll(m, r));
                                    } else {
                                        l.add(o.toString().replaceFirst(m, r));
                                    }
                                }
                                val = l;
                            } else if (val1 != null) {
                                if (flagG) {
                                    val = val1.toString().replaceAll(m, r);
                                } else {
                                    val = val1.toString().replaceFirst(m, r);
                                }
                            }
                        } else {
                            val = val1;
                        }
                    }
                }
                if (computeLength) {
                    if (val instanceof Collection) {
                        val = ((Collection) val).size();
                    }
                    else if (val instanceof Map) {
                        val = ((Map) val).size();
                    }
                    else if (val != null) {
                        val = val.toString().length();
                    }
                    else {
                        val = 0;
                    }
                }
                if (wordSplit) {
                    if (val instanceof Map) {
                        List<Object> c = toList((Map) val, flagk, flagv);
                        val = new ArgList(c);
                    }
                    else if (val instanceof Collection) {
                        if (!(val instanceof ArgList)) {
                            List<Object> l = val instanceof List ? (List) val : new ArrayList<>((Collection<?>) val);
                            val = new ArgList(l);
                        }
                    }
                    else if (val != null) {
                        val = new ArgList(Arrays.asList(val.toString().split("\\s")));
                    }
                }
            }

            while (ch == '[') {
//                Token leftParam;
                Object left;
//                Token rightParam;
                Object right;
                getch();
//                if (ch == '(') {
//                    int start = index;
//                    findClosing();
//                    leftParam = text.subSequence(start, index - 1);
//                    getch();
//                } else {
//                    leftParam = null;
//                }
                if (ch == '@') {
                    left = text.subSequence(index - 1, index);
                    getch();
                } else {
                    left = getName(']');
                }
                if (ch == ',') {
                    getch();
//                    if (ch == '(') {
//                        int start = index;
//                        findClosing();
//                        rightParam = text.subSequence(start, index - 1);
//                        getch();
//                    } else {
//                        rightParam = null;
//                    }
                    right = getName(']');
                } else {
//                    rightParam = null;
                    right = null;
                }
                if (ch != ']') {
                    throw new SyntaxError(line, column, "invalid subscript");
                }
                getch();
                if (right == null) {
                    left = left instanceof Token ? expand((Token) left) : left;
                    if (val instanceof Map) {
                        if (left.toString().equals("@")) {
                            val = new ArgList(toList((Map) val, flagk, flagv));
                        }
                        else {
                            val = ((Map) val).get(left.toString());
                        }
                    }
                    else if (val instanceof List) {
                        if (left.toString().equals("@")) {
                            val = new ArgList((List) val);
                        }
                        else {
                            val = ((List) val).get(Integer.parseInt(left.toString()));
                        }
                    }
                    else if (val != null) {
                        if (left.toString().equals("@")) {
                            val = val.toString();
                        } else {
                            val = val.toString().charAt(Integer.parseInt(left.toString()));
                        }
                    }
                }
                else {
                    left = left instanceof Token ? expand((Token) left) : left;
                    right = right instanceof Token ? expand((Token) right) : right;
                    if (val instanceof Map) {
                        val = null;
                    }
                    else if (val instanceof List) {
                        val = ((List) val).subList(Integer.parseInt(left.toString()), Integer.parseInt(right.toString()));
                    }
                    else {
                        val = val.toString().substring(Integer.parseInt(left.toString()), Integer.parseInt(right.toString()));
                    }
                }
            }

            if (ch != '}') {
                throw new SyntaxError(sLine, sCol, "bad substitution");
            }

            if (flagP) {
                val = val != null ? evaluate.get(val.toString()) : null;
            }
            if (flagC || flagL || flagU) {
                Function<String, String> cnv;
                if (flagC)
                    cnv = s -> s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
                else if (flagL)
                    cnv = String::toLowerCase;
                else
                    cnv = String::toUpperCase;
                if (val instanceof Map) {
                    val = toList((Map) val, flagk, flagv);
                }
                if (val instanceof Collection) {
                    List<String> list = new ArrayList<>();
                    for (Object o : ((Collection) val)) {
                        list.add(o != null ? cnv.apply(o.toString()) : null);
                    }
                    val = list;
                } else if (val != null) {
                    val = cnv.apply(val.toString());
                }
            }

            if (inQuote) {
                if (val instanceof Map) {
                    val = toList((Map) val, flagk, flagv);
                }
                if (val instanceof Collection) {
                    List<Object> l = val instanceof List ? (List) val : new ArrayList<>((Collection) val);
                    if (flagExpand) {
                        val = new ArgList(l);
                    } else {
                        val = l;
                    }
                }
            }
            else {
                if (flagExpand && val instanceof List) {
                    val = new ArgList((List) val);
                }
            }

            getch();

            /*
            Token pre;
            if ("#^~=+".indexOf(ch) >= 0) {
                pre = text.subSequence(index, index + 1);
                getch();
            } else {
                pre = null;
            }

            Token name1;
            Object val1;
            if (ch == '$') {
                name1 = null;
                val1 = expandVar();
            } else {
                int start = index - 1;
                while (ch != EOT && ch != '}' && (isName(ch) || ch == '\\')) {
                    getch();
                    if (ch == '{' || ch == '(' || ch == '[') {
                        findClosing();
                    }
                }
                if (ch == EOT) {
                    throw new EOFError(sLine, sCol, "unexpected EOT looking for matching '}'", "compound", Character.toString('}'));
                }
                name1 = text.subSequence(start, index - 1);
                val1 = null;
            }

            Token op;
            if (ch != '}') {
                int start = index - 1;
                while (ch != EOT && ch != '}' && ":-+=?#%/^|*?".indexOf(ch) >= 0) {
                    getch();
                }
                op = text.subSequence(start, index - 1);
            } else {
                op = null;
            }

            Token name2;
            Object val2;
            if (ch == '}') {
                name2 = null;
                val2 = null;
            }
            else if (ch == '$') {
                name2 = null;
                val2 = expandVar();
            }
            else {
                int start = index - 1;
                while (ch != EOT && ch != '}') {
                    getch();
                    if (ch == '\\') {
                        escape();
                    }
                    else if (ch == '{' || ch == '(' || ch == '[') {
                        findClosing();
                    }
                }
                if (ch == EOT) {
                    throw new EOFError(sLine, sCol, "unexpected EOT looking for matching '}'", "compound", Character.toString('}'));
                }
                name2 = text.subSequence(start, index - 1);
                val2 = null;
            }

            if (ch != '}') {
                throw new SyntaxError(sLine, sCol, "bad substitution");
            }

            if (pre == null && op == null) {
                if (name1 != null) {
                    val1 = evaluate.get(expand(name1).toString());
                }
            }
            else if (pre != null && Token.eq(pre, "+") && op == null) {
                if (name1 != null) {
                    val1 = evaluate.get(expand(name1).toString()) != null;
                } else {
                    throw new SyntaxError(sLine, sCol, "bad substitution");
                }
            }
            else if (pre != null) {
                throw new SyntaxError(sLine, sCol, "bad substitution");
            }
            else if (Token.eq("-", op) || Token.eq(":-", op)) {
                if (name1 != null) {
                    val1 = evaluate.get(expand(name1).toString());
                }
                if (val1 == null) {
                    if (name2 != null) {
                        val2 = expand(expand(name2).toString(), evaluate, inQuote);
                    }
                    val1 = val2;
                }
                if (val1 instanceof Token) {
                    val1 = val1.toString();
                }
            }
            else if (Token.eq("=", op) || Token.eq(":=", op) || Token.eq("::=", op)) {
                assert name1 != null;
                val1 = evaluate.get(expand(name1).toString());
                if (val1 == null && name2 != null) {
                    val1 = expand(name2);
                    if (val1 instanceof Token) {
                        val1 = val1.toString();
                    }
                    evaluate.put(name1.toString(), val1);
                }
            }
            else if (Token.eq("+", op) || Token.eq(":+", op)) {
                assert name1 != null;
                val1 = evaluate.get(expand(name1).toString());
                if (val1 != null && name2 != null) {
                    val1 = expand(name2);
                    if (val1 instanceof Token) {
                        val1 = val1.toString();
                    }
                }
            }
            else if (Token.eq("?", op) || Token.eq(":?", op)) {
                assert name1 != null;
                val1 = evaluate.get(expand(name1).toString());
                if (val1 == null) {
                    if (name2 != null) {
                        val1 = expand(name2);
                        if (val1 instanceof Token) {
                            val1 = val1.toString();
                        }
                    }
                    if (val1 == null || val1.toString().length() == 0) {
                        val1 = "parameter not set";
                    }
                    throw new IllegalArgumentException(name1 + ": " + val1);
                }
            }
            else {

            }
            val = val1;
            if (flagP) {
                val = val != null ? evaluate.get(val.toString()) : null;
            }

            /*
            while (true) {
                if (ch == EOT) {
                    throw new EOFError(sLine, sCol, "unexpected EOT looking for matching '}'", "compound", Character.toString('}'));
                }
                if (ch == '\\') {
                    escape();
                }
                if (ch == '{')
            }
            // ${NAME[[:]-+=?]WORD}
            short sLine = line;
            short sCol = column;
            Token group = group();
            char c;
            int i = 0;

            while (i < group.length())
            {
                switch (group.charAt(i))
                {
                    case ':':
                    case '-':
                    case '+':
                    case '=':
                    case '?':
                    case '#':
                        break;

                    default:
                        ++i;
                        continue;
                }
                break;
            }

            sCol += i;

            String name = String.valueOf(expand(group.subSequence(0, i)));

            for (int j = 0; j < name.length(); ++j)
            {
                if (!isName(name.charAt(j)))
                {
                    throw new SyntaxError(sLine, sCol, "bad name: ${" + group + "}");
                }
            }

            val = evaluate.get(name);

            if (i < group.length())
            {
                c = group.charAt(i++);
                if (':' == c)
                {
                    c = (i < group.length() ? group.charAt(i++) : EOT);
                }

                Token word = group.subSequence(i, group.length());

                switch (c)
                {
                    case '-':
                    case '=':
                        if (null == val)
                        {
                            val = expand(word, evaluate, false);
                            if (val instanceof Token)
                            {
                                val = val.toString();
                            }
                            if ('=' == c)
                            {
                                evaluate.put(name, val);
                            }
                        }
                        break;

                    case '+':
                        if (null != val)
                        {
                            val = expand(word, evaluate, false);
                            if (val instanceof Token)
                            {
                                val = val.toString();
                            }
                        }
                        break;

                    case '?':
                        if (null == val)
                        {
                            val = expand(word, evaluate, false);
                            if (val instanceof Token)
                            {
                                val = val.toString();
                            }
                            if (null == val || val.toString().length() == 0)
                            {
                                val = "parameter not set";
                            }
                            throw new IllegalArgumentException(name + ": " + val);
                        }
                        break;

                    case '#':
                        if (null != val)
                        {

                        }
                        break;

                    default:
                        throw new SyntaxError(sLine, sCol, "bad substitution: ${" + group + "}");
                }
            }
            getch();
            */
        }

        return val;
    }

    private List<Object> toList(Map<Object, Object> val1, boolean flagk, boolean flagv) {
        List<Object> l = new ArrayList<>();
        if (flagk && flagv) {
            for (Map.Entry<Object, Object> entry : val1.entrySet()) {
                l.add(entry.getKey());
                l.add(entry.getValue());
            }
        } else if (flagk) {
            l.addAll(val1.keySet());
        } else {
            l.addAll(val1.values());
        }
        return l;
    }

    private Object getAndEvaluateName() throws Exception {
        Object r = getName('}');
        if (r instanceof Token) {
            return evaluate.get(expand((Token) r).toString());
        } else {
            return r;
        }
    }

    private Object getName(char closing) throws Exception {
        if (ch == '$') {
            return expandVar();
        } else {
            int start = index - 1;
            while (ch != EOT && ch != closing && isName(ch)) {
                getch();
                if (ch == '\\') {
                    escape();
                }
                else if (ch == '{') {
                    findClosing();
                }
            }
            if (ch == EOT) {
                throw new EOFError(line, column, "unexpected EOT looking for matching '}'", "compound", Character.toString('}'));
            }
            return text.subSequence(start, index - 1);
        }
    }

    private Object getValue() throws Exception {
        if (ch == '$') {
            return expandVar();
        } else {
            int start = index - 1;
            while (ch != EOT && ch != '}') {
                if (ch == '\\') {
                    escape();
                }
                else if (ch == '{' || ch == '(' || ch == '[') {
                    findClosing();
                }
                else {
                    getch();
                }
            }
            if (ch == EOT) {
                throw new EOFError(line, column, "unexpected EOT looking for matching '}'", "compound", Character.toString('}'));
            }
            Token name = text.subSequence(start, index - 1);
            return expand(name).toString();
        }
    }

    private void findClosing() {
        char start = ch;
        while (getch() != EOT) {
            if (ch == '(' || ch == '{' || ch == '[') {
                findClosing();
            } else if (start == '(' && ch == ')'
                    || start == '{' && ch == '}'
                    || start == '[' && ch == ']') {
                return;
            }
        }
    }

    private static final char EOL = 0;

    private static boolean isRegexMeta(char ch) {
        return ".^$+{[]|()".indexOf(ch) != -1;
    }

    private static boolean isGlobMeta(char ch) {
        return "\\*?[{".indexOf(ch) != -1;
    }

    private static char next(String str, int index) {
        return index < str.length() ? str.charAt(index) : EOL;
    }

    private static String toRegexPattern(String str, boolean shortest) {
        boolean inGroup = false;
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (true) {
            while (index < str.length()) {
                char ch = str.charAt(index++);
                switch (ch) {
                    case '*':
                        sb.append(shortest ? ".*?" : ".*");
                        break;
                    case ',':
                        if (inGroup) {
                            sb.append(")|(?:");
                        } else {
                            sb.append(',');
                        }
                        break;
                    case '?':
                        sb.append(".");
                        break;
                    case '[':
                        sb.append("[");
                        if (next(str, index) == '^') {
                            sb.append("\\^");
                            ++index;
                        } else {
                            if (next(str, index) == '!') {
                                sb.append('^');
                                ++index;
                            }
                            if (next(str, index) == '-') {
                                sb.append('-');
                                ++index;
                            }
                        }
                        boolean inLeft = false;
                        char left = 0;
                        while (index < str.length()) {
                            ch = str.charAt(index++);
                            if (ch == ']') {
                                break;
                            }
                            if (ch == '\\' || ch == '[' || ch == '&' && next(str, index) == '&') {
                                sb.append('\\');
                            }
                            sb.append(ch);
                            if (ch == '-') {
                                if (!inLeft) {
                                    throw new PatternSyntaxException("Invalid range", str, index - 1);
                                }
                                if ((ch = next(str, index++)) == EOL || ch == ']') {
                                    break;
                                }
                                if (ch < left) {
                                    throw new PatternSyntaxException("Invalid range", str, index - 3);
                                }
                                sb.append(ch);
                                inLeft = false;
                            } else {
                                inLeft = true;
                                left = ch;
                            }
                        }
                        if (ch != ']') {
                            throw new PatternSyntaxException("Missing \']", str, index - 1);
                        }
                        sb.append("]");
                        break;
                    case '\\':
                        if (index == str.length()) {
                            throw new PatternSyntaxException("No character to escape", str, index - 1);
                        }
                        char ch2 = str.charAt(index++);
                        if (isGlobMeta(ch2) || isRegexMeta(ch2)) {
                            sb.append('\\');
                        }
                        sb.append(ch2);
                        break;
                    case '{':
                        if (inGroup) {
                            throw new PatternSyntaxException("Cannot nest groups", str, index - 1);
                        }
                        sb.append("(?:(?:");
                        inGroup = true;
                        break;
                    case '}':
                        if (inGroup) {
                            sb.append("))");
                            inGroup = false;
                        } else {
                            sb.append('}');
                        }
                        break;
                    default:
                        if (isRegexMeta(ch)) {
                            sb.append('\\');
                        }
                        sb.append(ch);
                        break;
                }
            }
            if (inGroup) {
                throw new PatternSyntaxException("Missing \'}", str, index - 1);
            }
            return sb.toString();
        }
    }


    private boolean isName(char ch)
    {
        return Character.isJavaIdentifierPart(ch) && (ch != '$') || ('.' == ch);
    }

}
