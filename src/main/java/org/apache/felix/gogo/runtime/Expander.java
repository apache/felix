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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("fallthrough")
public class Expander extends BaseTokenizer
{

    /**
     * expand variables, quotes and escapes in word.
     */
    public static Object expand(CharSequence word, Evaluate eval) throws Exception
    {
        return expand(word, eval, false, true, false, true, false);
    }

    private static Object expand(CharSequence word, Evaluate eval, boolean inQuote) throws Exception
    {
        return new Expander(word, eval, inQuote, true, false, false, false).expand();
    }

    private static Object expand(CharSequence word, Evaluate eval,
                                 boolean inQuote,
                                 boolean generateFileNames,
                                 boolean semanticJoin,
                                 boolean unquote,
                                 boolean asPattern) throws Exception
    {
        return new Expander(word, eval, inQuote, generateFileNames, semanticJoin, unquote, asPattern).expand();
    }

    private final Evaluate evaluate;
    private boolean inQuote;
    private boolean generateFileNames;
    private boolean semanticJoin;
    private boolean unquote;
    private boolean asPattern;
    private boolean rawVariable;

    public Expander(CharSequence text, Evaluate evaluate,
                    boolean inQuote,
                    boolean generateFileNames,
                    boolean semanticJoin,
                    boolean unquote,
                    boolean asPattern)
    {
        super(text);
        this.evaluate = evaluate;
        this.inQuote = inQuote;
        this.generateFileNames = generateFileNames;
        this.semanticJoin = semanticJoin;
        this.unquote = unquote;
        this.asPattern = asPattern;
    }

    public Object expand(CharSequence word) throws Exception
    {
        return expand(word, evaluate, inQuote, true, false, false, false);
    }

    public Object expand(CharSequence word,
                         boolean generateFileNames,
                         boolean semanticJoin,
                         boolean unquote) throws Exception
    {
        return expand(word, evaluate, inQuote, generateFileNames, semanticJoin, unquote, false);
    }

    public Object expandPattern(CharSequence word) throws Exception
    {
        return expand(word, evaluate, inQuote, false, false, false, true);
    }

    private Object expand() throws Exception
    {
        Object expanded = doExpand();
        if (rawVariable)
        {
            return expanded;
        }
        Stream<Object> stream = expanded instanceof Collection
                ? asCollection(expanded).stream()
                : Stream.of(expanded);
        List<Object> args = stream
                .flatMap(uncheck(o -> o instanceof CharSequence ? expandBraces((CharSequence) o).stream() : Stream.of(o)))
                .flatMap(uncheck(o -> generateFileNames && o instanceof CharSequence ? generateFileNames((CharSequence) o).stream() : Stream.of(o)))
                .map(o -> unquote && o instanceof CharSequence ? unquote((CharSequence) o) : o)
                .collect(Collectors.toList());
        if (args.size() == 1)
        {
            return args.get(0);
        }
        if (expanded instanceof ArgList)
        {
            return new ArgList(args);
        }
        return args;
    }

    private CharSequence unquote(CharSequence arg)
    {
        if (inQuote)
        {
            return arg;
        }
        boolean hasEscape = false;
        for (int i = 0; i < arg.length(); i++)
        {
            int c = arg.charAt(i);
            if (c == '\\' || c == '"' || c == '\'')
            {
                hasEscape = true;
                break;
            }
        }
        if (!hasEscape)
        {
            return arg;
        }
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;
        StringBuilder buf = new StringBuilder(arg.length());
        for (int i = 0; i < arg.length(); i++)
        {
            char c = arg.charAt(i);
            if (doubleQuoted && escaped)
            {
                if (c != '"' && c != '\\' && c != '$' && c != '%')
                {
                    buf.append('\\');
                }
                buf.append(c);
                escaped = false;
            }
            else if (escaped)
            {
                buf.append(c);
                escaped = false;
            }
            else if (singleQuoted)
            {
                if (c == '\'')
                {
                    singleQuoted = false;
                }
                else
                {
                    buf.append(c);
                }
            }
            else if (doubleQuoted)
            {
                if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == '\"')
                {
                    doubleQuoted = false;
                }
                else
                {
                    buf.append(c);
                }
            }
            else if (c == '\\')
            {
                escaped = true;
            }
            else if (c == '\'')
            {
                singleQuoted = true;
            }
            else if (c == '"')
            {
                doubleQuoted = true;
            }
            else
            {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    protected List<? extends CharSequence> expandBraces(CharSequence arg) throws Exception
    {
        int braces = 0;
        boolean escaped = false;
        boolean doubleQuoted = false;
        boolean singleQuoted = false;
        List<CharSequence> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < arg.length(); i++)
        {
            char c = arg.charAt(i);
            if (doubleQuoted && escaped)
            {
                escaped = false;
            }
            else if (escaped)
            {
                escaped = false;
            }
            else if (singleQuoted)
            {
                if (c == '\'')
                {
                    singleQuoted = false;
                }
            }
            else if (doubleQuoted)
            {
                if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == '\"')
                {
                    doubleQuoted = false;
                }
            }
            else if (c == '\\')
            {
                escaped = true;
            }
            else if (c == '\'')
            {
                singleQuoted = true;
            }
            else if (c == '"')
            {
                doubleQuoted = true;
            }
            else
            {
                if (c == '{')
                {
                    if (braces++ == 0)
                    {
                        if (i > start)
                        {
                            parts.add(arg.subSequence(start, i));
                        }
                        start = i;
                    }
                }
                else if (c == '}')
                {
                    if (--braces == 0)
                    {
                        parts.add(arg.subSequence(start, i + 1));
                        start = i + 1;
                    }
                }
            }
        }
        if (start < arg.length())
        {
            parts.add(arg.subSequence(start, arg.length()));
        }
        if (start == 0)
        {
            return Collections.singletonList(arg);
        }
        List<CharSequence> generated = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "\\{(((?<intstart>\\-?[0-9]+)\\.\\.(?<intend>\\-?[0-9]+)(\\.\\.(?<intinc>\\-?0*[1-9][0-9]*))?)" +
                "|((?<charstart>\\S)\\.\\.(?<charend>\\S)))\\}");
        for (CharSequence part : parts)
        {
            List<CharSequence> generators = new ArrayList<>();
            Matcher matcher = pattern.matcher(part);
            if (matcher.matches())
            {
                if (matcher.group("intstart") != null)
                {
                    int intstart = Integer.parseInt(matcher.group("intstart"));
                    int intend = Integer.parseInt(matcher.group("intend"));
                    int intinc = matcher.group("intinc") != null ? Integer.parseInt(matcher.group("intinc")) : 1;
                    if (intstart > intend)
                    {
                        if (intinc < 0)
                        {
                            int k = intstart;
                            intstart = intend;
                            intend = k;
                        }
                        intinc = -intinc;
                    }
                    else
                    {
                        if (intinc < 0)
                        {
                            int k = intstart;
                            intstart = intend;
                            intend = k;
                        }
                    }
                    if (intinc > 0)
                    {
                        for (int k = intstart; k <= intend; k += intinc)
                        {
                            generators.add(Integer.toString(k));
                        }
                    }
                    else
                    {
                        for (int k = intstart; k >= intend; k += intinc)
                        {
                            generators.add(Integer.toString(k));
                        }
                    }
                }
                else
                {
                    char charstart = matcher.group("charstart").charAt(0);
                    char charend = matcher.group("charend").charAt(0);
                    if (charstart < charend)
                    {
                        for (char c = charstart; c <= charend; c++)
                        {
                            generators.add(Character.toString(c));
                        }
                    }
                    else
                    {
                        for (char c = charstart; c >= charend; c--)
                        {
                            generators.add(Character.toString(c));
                        }
                    }
                }
            }
            else if (part.charAt(0) == '{' && part.charAt(part.length() - 1) == '}')
            {
                // Split on commas
                braces = 0;
                escaped = false;
                doubleQuoted = false;
                singleQuoted = false;
                start = 1;
                for (int i = 1; i < part.length() - 1; i++)
                {
                    char c = part.charAt(i);
                    if (doubleQuoted && escaped)
                    {
                        escaped = false;
                    }
                    else if (escaped)
                    {
                        escaped = false;
                    }
                    else if (singleQuoted)
                    {
                        if (c == '\'')
                        {
                            singleQuoted = false;
                        }
                    }
                    else if (doubleQuoted)
                    {
                        if (c == '\\')
                        {
                            escaped = true;
                        }
                        else if (c == '\"')
                        {
                            doubleQuoted = false;
                        }
                    }
                    else if (c == '\\')
                    {
                        escaped = true;
                    }
                    else if (c == '\'')
                    {
                        singleQuoted = true;
                    }
                    else if (c == '"')
                    {
                        doubleQuoted = true;
                    }
                    else
                    {
                        if (c == '}')
                        {
                            braces--;
                        }
                        else if (c == '{')
                        {
                            braces++;
                        }
                        else if (c == ',' && braces == 0)
                        {
                            generators.add(part.subSequence(start, i));
                            start = i + 1;
                        }
                    }
                }
                if (start < part.length() - 1)
                {
                    generators.add(part.subSequence(start, part.length() - 1));
                }
                generators = generators.stream()
                        .map(uncheck(cs -> expand(cs, false, false, false)))
                        .flatMap(o -> o instanceof Collection ? asCollection(o).stream() : Stream.of(o))
                        .map(String::valueOf)
                        .collect(Collectors.toList());

                // If there's no splitting comma, expand with the braces
                if (generators.size() < 2)
                {
                    generators = Collections.singletonList(part.toString());
                }
            }
            else
            {
                generators.add(part.toString());
            }
            if (generated.isEmpty())
            {
                generated.addAll(generators);
            }
            else
            {
                List<CharSequence> prevGenerated = generated;
                generated = generators.stream()
                        .flatMap(s -> prevGenerated.stream().map(cs -> String.valueOf(cs) + s))
                        .collect(Collectors.toList());
            }
        }
        return generated;
    }

    public interface FunctionExc<T, R>
    {
        R apply(T t) throws Exception;
    }

    public static <T, R> Function<T, R> uncheck(FunctionExc<T, R> func)
    {
        return t -> {
            try
            {
                return func.apply(t);
            }
            catch (Exception e)
            {
                return sneakyThrow(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T sneakyThrow(Throwable t) throws E
    {
        throw (E) t;
    }

    protected List<? extends CharSequence> generateFileNames(CharSequence arg) throws IOException
    {
        // Disable if currentDir is not set
        Path currentDir = evaluate.currentDir();
        if (currentDir == null || inQuote)
        {
            return Collections.singletonList(arg);
        }
        // Search for unquoted escapes
        boolean hasUnescapedReserved = false;
        boolean escaped = false;
        boolean doubleQuoted = false;
        boolean singleQuoted = false;
        StringBuilder buf = new StringBuilder(arg.length());
        String pfx = "";
        for (int i = 0; i < arg.length(); i++)
        {
            char c = arg.charAt(i);
            if (doubleQuoted && escaped)
            {
                if (c != '"' && c != '\\' && c != '$' && c != '%')
                {
                    buf.append('\\');
                }
                buf.append(c);
                escaped = false;
            }
            else if (escaped)
            {
                buf.append(c);
                escaped = false;
            }
            else if (singleQuoted)
            {
                if (c == '\'')
                {
                    singleQuoted = false;
                }
                else
                {
                    buf.append(c);
                }
            }
            else if (doubleQuoted)
            {
                if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == '\"')
                {
                    doubleQuoted = false;
                }
                else
                {
                    buf.append(c);
                }
            }
            else if (c == '\\')
            {
                escaped = true;
            }
            else if (c == '\'')
            {
                singleQuoted = true;
            }
            else if (c == '"')
            {
                doubleQuoted = true;
            }
            else
            {
                if ("*(|<[?".indexOf(c) >= 0 && !hasUnescapedReserved)
                {
                    hasUnescapedReserved = true;
                    pfx = buf.toString();
                }
                buf.append(c);
            }
        }
        if (!hasUnescapedReserved)
        {
            return Collections.singletonList(arg);
        }

        String org = buf.toString();
        List<String> expanded = new ArrayList<>();
        Path dir;
        String prefix;
        if (pfx.indexOf('/') >= 0)
        {
            pfx = pfx.substring(0, pfx.lastIndexOf('/'));
            arg = org.substring(pfx.length() + 1);
            dir = currentDir.resolve(pfx).normalize();
            prefix = pfx + "/";
        }
        else
        {
            dir = currentDir;
            prefix = "";
        }
        PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:" + arg);
        Files.walkFileTree(dir,
                EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE,
                new FileVisitor<Path>()
                {
                    @Override
                    public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException
                    {
                        if (file.equals(dir))
                        {
                            return FileVisitResult.CONTINUE;
                        }
                        if (Files.isHidden(file))
                        {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        Path r = dir.relativize(file);
                        if (matcher.matches(r))
                        {
                            expanded.add(prefix + r.toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                    {
                        if (!Files.isHidden(file))
                        {
                            Path r = dir.relativize(file);
                            if (matcher.matches(r))
                            {
                                expanded.add(prefix + r.toString());
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
                    {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
                    {
                        return FileVisitResult.CONTINUE;
                    }
                });
        Collections.sort(expanded);
        if (expanded.isEmpty())
        {
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
                    break;

                case '$':
                    // Posix quote
                    if (peek() == '\'')
                    {
                        getch();
                        skipQuote();
                        value = text.subSequence(start + 1, index - 1);
                        getch();
                        buf.append("\'");
                        buf.append(ansiEscape(value));
                        buf.append("\'");
                    }
                    // Parameter expansion
                    else
                    {
                        Object val = expandVar(true);
                        if (EOT == ch && buf.length() == 0)
                        {
                            return val;
                        }
                        rawVariable = false;
                        if (null != val)
                        {
                            buf.append(val);
                        }
                    }
                    break;

                case '\\':
                    buf.append(ch);
                    if (peek() != EOT)
                    {
                        getch();
                        buf.append(ch);
                    }
                    getch();
                    break;

                case '"':
                    skipQuote();
                    value = text.subSequence(start, index - 1);
                    getch();
                    Object expand = expand(value, evaluate, true);
                    if (eot() && buf.length() == 0)
                    {
                        if (expand instanceof ArgList)
                        {
                            return new ArgList((ArgList) expand).stream()
                                    .map(String::valueOf)
                                    .map(s -> "\"" + s + "\"").collect(Collectors.toList());
                        }
                        else if (expand instanceof Collection)
                        {
                            return asCollection(expand).stream().map(String::valueOf).collect(Collectors.joining(" ", "\"", "\""));
                        }
                        else if (expand != null)
                        {
                            return "\"" + expand.toString() + "\"";
                        }
                        else
                        {
                            return "";
                        }
                    }
                    if (expand instanceof Collection)
                    {
                        boolean first = true;
                        buf.append("\"");
                        for (Object o : ((Collection) expand))
                        {
                            if (!first)
                            {
                                buf.append(" ");
                            }
                            first = false;
                            buf.append(o);
                        }
                        buf.append("\"");
                    }
                    else if (expand != null)
                    {
                        buf.append("\"");
                        buf.append(expand.toString());
                        buf.append("\"");
                    }
                    break;

                case '\'':
                    skipQuote();
                    value = text.subSequence(start - 1, index);
                    getch();
                    if (eot() && buf.length() == 0)
                    {
                        return value;
                    }
                    buf.append(value);
                    break;

                default:
                    buf.append(ch);
                    getch();
                    break;
            }

        }

        return buf.toString();
    }

    private CharSequence ansiEscape(CharSequence arg)
    {
        StringBuilder buf = new StringBuilder(arg.length());
        for (int i = 0; i < arg.length(); i++)
        {
            int c = arg.charAt(i);
            int ch;
            if (c == '\\')
            {
                c = i < arg.length() - 1 ? arg.charAt(++i) : '\\';
                switch (c)
                {
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
                        for (int j = 0; j < 3; j++)
                        {
                            c = i < arg.length() - 1 ? arg.charAt(++i) : -1;
                            if (c >= 0)
                            {
                                ch = ch * 8 + (c - '0');
                            }
                        }
                        buf.append((char) ch);
                        break;
                    case 'u':
                        ch = 0;
                        for (int j = 0; j < 4; j++)
                        {
                            c = i < arg.length() - 1 ? arg.charAt(++i) : -1;
                            if (c >= 0)
                            {
                                if (c >= 'A' && c <= 'F')
                                {
                                    ch = ch * 16 + (c - 'A' + 10);
                                }
                                else if (c >= 'a' && c <= 'f')
                                {
                                    ch = ch * 16 + (c - 'a' + 10);
                                }
                                else if (c >= '0' && c <= '9')
                                {
                                    ch = ch * 16 + (c - '0');
                                }
                                else
                                {
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
            } else
            {
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
        return expandVar(false);
    }

    private Object expandVar(boolean rawVariable) throws Exception
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
                    this.rawVariable = rawVariable;
                }
            }
        }
        else
        {
            getch();

            // unique flag
            boolean flagu = false;
            // sort flags
            boolean flago = false;
            boolean flagO = false;
            boolean flaga = false;
            boolean flagi = false;
            boolean flagn = false;
            // map flags
            boolean flagk = false;
            boolean flagv = false;
            // param flags
            boolean flagP = false;
            // case transformation flags
            boolean flagC = false;
            boolean flagL = false;
            boolean flagU = false;
            // pattern flags
            boolean flagG = false;
            // expand flag
            boolean flagExpand = false;
            // visible chars
            boolean flagV = false;
            // codepoint
            boolean flagSharp = false;
            // quoting
            int flagq = 0;
            boolean flagQ = false;
            // split / join
            String flags = null;
            String flagj = null;
            // pattern
            boolean flagPattern = false;
            // length
            boolean computeLength = false;
            // Parse flags
            if (ch == '(') {
                getch();
                boolean flagp = false;
                while (ch != EOT && ch != ')')
                {
                    switch (ch)
                    {
                        case 'u':
                            flagu = true;
                            break;
                        case 'p':
                            flagp = true;
                            break;
                        case 'f':
                            flags = "\n";
                            break;
                        case 'F':
                            flagj = "\n";
                            break;
                        case 's':
                        case 'j': {
                            char opt = ch;
                            char c = getch();
                            if (c == EOT)
                            {
                                throw new IllegalArgumentException("error in flags");
                            }
                            int start = index;
                            while (true)
                            {
                                char n = getch();
                                if (n == EOT)
                                {
                                    throw new IllegalArgumentException("error in flags");
                                }
                                else if (n == c)
                                {
                                    break;
                                }
                            }
                            String s = text.subSequence(start, index - 1).toString();
                            if (flagp)
                            {
                                s = ansiEscape(s).toString();
                            }
                            if (opt == 's')
                            {
                                flags = s;
                            }
                            else if (opt == 'j')
                            {
                                flagj = s;
                            }
                            else
                            {
                                throw new IllegalArgumentException("error in flags");
                            }
                            flagp = false;
                            break;
                        }
                        case 'q':
                            if (flagq != 0)
                            {
                                throw new IllegalArgumentException("error in flags");
                            }
                            flagq = 1;
                            if (peek() == '-')
                            {
                                flagq = -1;
                                getch();
                            }
                            else
                            {
                                while (peek() == 'q')
                                {
                                    getch();
                                    flagq++;
                                }
                                if (peek() == '-')
                                {
                                    throw new IllegalArgumentException("error in flags");
                                }
                            }
                            break;
                        case 'Q':
                            flagQ = true;
                            break;
                        case '#':
                            flagSharp = true;
                            break;
                        case 'V':
                            flagV = true;
                            break;
                        case 'o':
                            flago = true;
                            break;
                        case 'O':
                            flagO = true;
                            break;
                        case 'a':
                            flaga = true;
                            break;
                        case 'i':
                            flagi = true;
                            break;
                        case 'n':
                            flagn = true;
                            break;
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

            // Map to List conversion
            boolean _flagk = flagk;
            boolean _flagv = flagv;
            Function<Object, Object> toCollection = v -> v instanceof Map
                    ? toList(asMap(v), _flagk, _flagv)
                    : v != null && v.getClass().isArray()
                        ? Arrays.asList((Object[]) v)
                        : v;

            //
            // String transformations
            //
            BiFunction<Function<String, String>, Object, Object> stringApplyer = (func, v) -> {
                v = toCollection.apply(v);
                if (v instanceof Collection)
                {
                    return asCollection(v).stream()
                            .map(String::valueOf)
                            .map(func)
                            .collect(Collectors.toList());
                }
                else if (v != null)
                {
                    return func.apply(v.toString());
                }
                else
                    return null;
            };

            if (ch == '+')
            {
                getch();
                val = getAndEvaluateName();
            }
            else
            {
                while (true)
                {
                    if (ch == '#')
                    {
                        computeLength = true;
                        getch();
                    }
                    else if (ch == '=')
                    {
                        if (flags == null) {
                            flags = "\\s";
                        }
                        getch();
                    }
                    else if (ch == '~')
                    {
                        flagPattern = true;
                        getch();
                    }
                    else
                    {
                        break;
                    }
                }

                Object val1 = getName('}');

                if (ch == '}' || ch == '[')
                {
                    val = val1 instanceof Token ? evaluate.get(expand((Token) val1).toString()) : val1;
                }
                else
                {
                    int start = index - 1;
                    while (ch != EOT && ch != '}' && ":-+=?#%/".indexOf(ch) >= 0)
                    {
                        getch();
                    }
                    Token op = text.subSequence(start, index - 1);
                    if (Token.eq("-", op) || Token.eq(":-", op))
                    {
                        val1 = val1 instanceof Token ? evaluate.get(expand((Token) val1).toString()) : val1;
                        Object val2 = getValue();
                        val = val1 == null ? val2 : val1;
                    }
                    else if (Token.eq("+", op) || Token.eq(":+", op))
                    {
                        val1 = val1 instanceof Token ? evaluate.get(expand((Token) val1).toString()) : val1;
                        Object val2 = getValue();
                        val = val1 != null ? val2 : null;
                    }
                    else if (Token.eq("=", op) || Token.eq(":=", op) || Token.eq("::=", op))
                    {
                        if (!(val1 instanceof Token))
                        {
                            throw new SyntaxError(line, column, "not an identifier");
                        }
                        String name = expand((Token) val1).toString();
                        val1 = evaluate.get(name);
                        val = getValue();
                        if (Token.eq("::=", op) || val1 == null)
                        {
                            evaluate.put(name, val);
                        }
                    }
                    else if (Token.eq("?", op) || Token.eq(":?", op))
                    {
                        String name;
                        if (val1 instanceof Token)
                        {
                            name = expand((Token) val1).toString();
                            val = evaluate.get(name);
                        }
                        else
                        {
                            name = "";
                            val = val1;
                        }
                        if (val == null || val.toString().length() == 0)
                        {
                            throw new IllegalArgumentException(name + ": parameter not set");
                        }
                    }
                    else if (Token.eq("#", op) || Token.eq("##", op)
                            || Token.eq("%", op) || Token.eq("%%", op)
                            || Token.eq("/", op) || Token.eq("//", op))
                    {
                        val1 = val1 instanceof Token ? evaluate.get(expand((Token) val1).toString()) : val1;
                        String val2 = getPattern(op.charAt(0) == '/' ? "/}" : "}");
                        if (val2 != null)
                        {
                            String p = toRegexPattern(unquoteGlob(val2), op.length() == 1);
                            String r;
                            if (op.charAt(0) == '/')
                            {
                                if (ch == '/')
                                {
                                    getch();
                                    r = getValue().toString();
                                }
                                else
                                {
                                    r = "";
                                }
                            }
                            else
                            {
                                r = "";
                            }
                            String m = op.charAt(0) == '#' ? "^" + p : op.charAt(0) == '%' ? p + "$" : p;
                            val1 = toCollection.apply(val1);
                            if (val1 instanceof Collection)
                            {
                                List<String> l = new ArrayList<>();
                                for (Object o : ((Collection) val1))
                                {
                                    if (flagG)
                                    {
                                        l.add(o.toString().replaceAll(m, r));
                                    }
                                    else
                                    {
                                        l.add(o.toString().replaceFirst(m, r));
                                    }
                                }
                                val = l;
                            }
                            else if (val1 != null)
                            {
                                if (flagG)
                                {
                                    val = val1.toString().replaceAll(m, r);
                                }
                                else
                                {
                                    val = val1.toString().replaceFirst(m, r);
                                }
                            }
                        }
                        else
                        {
                            val = val1;
                        }
                    }
                }
            }

            //
            // Subscripts
            //
            while (ch == '[')
            {
                Object left;
                boolean nLeft = false;
                Object right;
                boolean nRight = false;
                getch();
                if (ch == '*')
                {
                    left = text.subSequence(index - 1, index);
                    getch();
                }
                else if (ch == '@')
                {
                    left = text.subSequence(index - 1, index);
                    flagExpand = true;
                    getch();
                }
                else
                {
                    if (ch == '-')
                    {
                        nLeft = true;
                        getch();
                    }
                    left = getName(']');
                }
                if (ch == ',')
                {
                    getch();
                    if (ch == '-')
                    {
                        nRight = true;
                        getch();
                    }
                    right = getName(']');
                }
                else
                {
                    right = null;
                }
                if (ch != ']')
                {
                    throw new SyntaxError(line, column, "invalid subscript");
                }
                getch();
                if (right == null)
                {
                    left = left instanceof Token ? expand((Token) left) : left;
                    String sLeft = left.toString();
                    if (val instanceof Map)
                    {
                        if (sLeft.equals("@") || sLeft.equals("*"))
                        {
                            val = toList(asMap(val), flagk, flagv);
                        }
                        else
                        {
                            val = ((Map) val).get(sLeft);
                        }
                    }
                    else if (val instanceof List)
                    {
                        if (sLeft.equals("@") || sLeft.equals("*"))
                        {
                            val = new ArgList((List) val);
                        }
                        else
                        {
                            int iLeft = Integer.parseInt(sLeft);
                            List list = (List) val;
                            val = list.get(nLeft ? list.size() - 1 - iLeft : iLeft);
                        }
                    }
                    else if (val != null)
                    {
                        if (sLeft.equals("@") || sLeft.equals("*"))
                        {
                            val = val.toString();
                        }
                        else
                        {
                            int iLeft = Integer.parseInt(sLeft);
                            String str = val.toString();
                            val = str.charAt(nLeft ? str.length() - 1 - iLeft : iLeft);
                        }
                    }
                }
                else
                {
                    if (val instanceof Map)
                    {
                        val = null;
                    }
                    else
                    {
                        left = left instanceof Token ? expand((Token) left) : left;
                        right = right instanceof Token ? expand((Token) right) : right;
                        int iLeft = Integer.parseInt(left.toString());
                        int iRight = Integer.parseInt(right.toString());
                        if (val instanceof List)
                        {
                            List list = (List) val;
                            val = list.subList(nLeft  ? list.size() - iLeft  : iLeft,
                                               nRight ? list.size() - iRight : iRight);
                        }
                        else
                        {
                            String str = val.toString();
                            val = str.substring(nLeft  ? str.length() - iLeft  : iLeft,
                                                nRight ? str.length() - iRight : iRight);
                        }
                    }
                }
            }

            if (ch != '}')
            {
                throw new SyntaxError(sLine, sCol, "bad substitution");
            }

            // Parameter name replacement
            if (flagP)
            {
                val = val != null ? evaluate.get(val.toString()) : null;
            }

            // Double quote joining
            boolean joined = false;
            if (inQuote && !computeLength && !flagExpand)
            {
                val = toCollection.apply(val);
                if (val instanceof Collection)
                {
                    String j = flagj != null ? flagj : " ";
                    val = asCollection(val).stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(j));
                    joined = true;
                }
            }

            // Character evaluation
            if (flagSharp)
            {
                val = stringApplyer.apply(this::sharp, val);
            }

            // Length
            if (computeLength)
            {
                if (val instanceof Collection)
                {
                    val = ((Collection) val).size();
                }
                else if (val instanceof Map)
                {
                    val = ((Map) val).size();
                }
                else if (val != null)
                {
                    val = val.toString().length();
                }
                else
                {
                    val = 0;
                }
            }

            // Forced joining
            if (flagj != null || flags != null && !joined)
            {
                val = toCollection.apply(val);
                if (val instanceof Collection)
                {
                    String j = flagj != null ? flagj : " ";
                    val = asCollection(val).stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(j));
                }
            }

            // Simple word splitting
            if (flags != null)
            {
                String _flags = flags;
                val = toCollection.apply(val);
                if (!(val instanceof Collection))
                {
                    val = Collections.singletonList(val);
                }
                val = asCollection(val).stream()
                        .map(String::valueOf)
                        .flatMap(s -> Arrays.stream(s.split(_flags)))
                        .collect(Collectors.toList());
            }

            // Case modification
            if (flagC)
            {
                val = stringApplyer.apply(this::toCamelCase, val);
            }
            else if (flagL)
            {
                val = stringApplyer.apply(String::toLowerCase, val);
            }
            else if (flagU)
            {
                val = stringApplyer.apply(String::toUpperCase, val);
            }

            // Visibility enhancement
            if (flagV)
            {
                val = stringApplyer.apply(this::visible, val);
            }

            // Quote
            if (flagq != 0)
            {
                int _flagq = flagq;
                val = stringApplyer.apply(s -> quote(s, _flagq), val);
                inQuote = true;
            }
            else if (flagQ) {
                val = stringApplyer.apply(this::unquote, val);
            }

            // Uniqueness
            if (flagu)
            {
                val = toCollection.apply(val);
                if (val instanceof Collection)
                {
                    val = new ArrayList<>(new HashSet<>(asCollection(val)));
                }
            }

            // Ordering
            if (flaga || flagi || flagn || flago || flagO)
            {
                val = toCollection.apply(val);
                if (val instanceof Collection)
                {
                    List<Object> list;
                    if (flagn)
                    {
                        boolean _flagi = flagi;
                        list = asCollection(val).stream()
                                .map(String::valueOf)
                                .sorted((s1, s2) -> numericCompare(s1, s2, _flagi))
                                .collect(Collectors.toList());
                    }
                    else if (flaga)
                    {
                        list = new ArrayList<>(asCollection(val));
                    }
                    else
                    {
                        Comparator<String> comparator = flagi ? String.CASE_INSENSITIVE_ORDER : Comparator.naturalOrder();
                        list = asCollection(val).stream()
                                .map(String::valueOf)
                                .sorted(comparator)
                                .collect(Collectors.toList());
                    }
                    if (flagO)
                    {
                        Collections.reverse(list);
                    }
                    val = list;
                }
            }

            // Semantic joining
            if (semanticJoin)
            {
                val = toCollection.apply(val);
                if (val instanceof Collection)
                {
                    val = asCollection(val).stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(" "));
                }
            }

            // Empty argument removal
            if (val instanceof Collection)
            {
                val = asCollection(val).stream()
                        .filter(o -> !(o instanceof CharSequence) || ((CharSequence) o).length() > 0)
                        .collect(Collectors.toList());
            }

            if (asPattern && !inQuote && !flagPattern)
            {
                val = toCollection.apply(val);
                Stream<Object> stream = val instanceof Collection ? asCollection(val).stream() : Stream.of(val);
                List<String> patterns = stream.map(String::valueOf)
                        .map(s -> quote(s, 2))
                        .collect(Collectors.toList());
                val = patterns.size() == 1 ? patterns.get(0) : patterns;
            }

            if (inQuote)
            {
                val = toCollection.apply(val);
                if (val instanceof Collection)
                {
                    List<Object> l = new ArrayList<>(asCollection(val));
                    if (flagExpand)
                    {
                        val = new ArgList(l);
                    }
                    else
                    {
                        val = l;
                    }
                }
            }
            else
            {
                if (flagExpand && val instanceof List)
                {
                    val = new ArgList((List) val);
                }
            }

            getch();
        }

        return val;
    }

    private String quote(String s, int flagq)
    {
        StringBuilder buf = new StringBuilder();
        // Backslashes
        if (flagq == 1)
        {
            for (int i = 0; i < s.length(); i++)
            {
                char ch = s.charAt(i);
                if (ch < 32 || ch >= 127)
                {
                    buf.append("$'\\").append(Integer.toOctalString(ch)).append("\'");
                }
                else if (" !\"#$&'()*;<=>?[\\]{|}~%".indexOf(ch) >= 0)
                {
                    buf.append("\\").append(ch);
                }
                else
                {
                    buf.append(ch);
                }
            }
        }
        // Single quotes
        else if (flagq == 2)
        {
            buf.append("'");
            for (int i = 0; i < s.length(); i++)
            {
                char ch = s.charAt(i);
                if (ch == '\'')
                {
                    buf.append("'\\''");
                }
                else
                {
                    buf.append(ch);
                }
            }
            buf.append("'");
        }
        // Double quotes
        else if (flagq == 3)
        {
            buf.append("\"");
            for (int i = 0; i < s.length(); i++)
            {
                char ch = s.charAt(i);
                if ("\"\\$%".indexOf(ch) >= 0)
                {
                    buf.append("\\").append(ch);
                }
                else
                {
                    buf.append(ch);
                }
            }
            buf.append("\"");
        }
        // Posix
        else if (flagq == 4)
        {
            buf.append("$'");
            for (int i = 0; i < s.length(); i++)
            {
                char ch = s.charAt(i);
                if (ch < 32 || ch >= 127)
                {
                    buf.append("\\").append(Integer.toOctalString(ch));
                }
                else
                {
                    switch (ch)
                    {
                        case '\n':
                            buf.append("\\n");
                            break;
                        case '\t':
                            buf.append("\\t");
                            break;
                        case '\r':
                            buf.append("\\r");
                            break;
                        case '\'':
                            buf.append("\\'");
                            break;
                        default:
                            buf.append(ch);
                            break;
                    }
                }
            }
            buf.append("'");
        }
        // Readable
        else
        {
            boolean needQuotes = false;
            for (int i = 0; i < s.length(); i++)
            {
                char ch = s.charAt(i);
                if (ch < 32 || ch >= 127 || " !\"#$&'()*;<=>?[\\]{|}~%".indexOf(ch) >= 0)
                {
                    needQuotes = true;
                    break;
                }
            }
            return needQuotes ? quote(s, 2) : s;
        }
        return buf.toString();
    }

    private String unquote(String arg)
    {
        boolean hasEscape = false;
        for (int i = 0; i < arg.length(); i++)
        {
            int c = arg.charAt(i);
            if (c == '\\' || c == '"' || c == '\'')
            {
                hasEscape = true;
                break;
            }
        }
        if (!hasEscape)
        {
            return arg;
        }
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;
        StringBuilder buf = new StringBuilder(arg.length());
        for (int i = 0; i < arg.length(); i++)
        {
            char c = arg.charAt(i);
            if (doubleQuoted && escaped)
            {
                if (c != '"' && c != '\\' && c != '$' && c != '%')
                {
                    buf.append('\\');
                }
                buf.append(c);
                escaped = false;
            }
            else if (escaped)
            {
                buf.append(c);
                escaped = false;
            }
            else if (singleQuoted)
            {
                if (c == '\'')
                {
                    singleQuoted = false;
                }
                else
                {
                    buf.append(c);
                }
            }
            else if (doubleQuoted)
            {
                if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == '\"')
                {
                    doubleQuoted = false;
                }
                else {
                    buf.append(c);
                }
            }
            else if (c == '\\')
            {
                escaped = true;
            }
            else if (c == '\'')
            {
                singleQuoted = true;
            }
            else if (c == '"')
            {
                doubleQuoted = true;
            }
            else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private int numericCompare(String s1, String s2, boolean caseInsensitive)
    {
        int i1s = 0, i2s = 0;
        while (i1s < s1.length() && i2s < s2.length())
        {
            char c1 = s1.charAt(i1s);
            char c2 = s2.charAt(i2s);
            if (caseInsensitive)
            {
                c1 = Character.toLowerCase(c1);
                c2 = Character.toLowerCase(c2);
            }
            if (c1 != c2)
            {
                if (c1 >= '0' && c1 <= '9' && c2 >= '0' && c2 <= '9')
                {
                    break;
                }
                else
                {
                    return c1 < c2 ? -1 : 1;
                }
            }
            i1s++;
            i2s++;
        }
        while (i1s > 0)
        {
            char c1 = s1.charAt(i1s - 1);
            if (c1 < '0' || c1 > '9')
            {
                break;
            }
            i1s--;
        }
        while (i2s > 0)
        {
            char c2 = s2.charAt(i2s - 1);
            if (c2 < '0' || c2 > '9')
            {
                break;
            }
            i2s--;
        }
        int i1e = i1s;
        int i2e = i2s;
        while (i1e < s1.length() - 1)
        {
            char c1 = s1.charAt(i1e + 1);
            if (c1 < '0' || c1 > '9')
            {
                break;
            }
            i1e++;
        }
        while (i2e < s2.length() - 1)
        {
            char c2 = s2.charAt(i2e + 1);
            if (c2 < '0' || c2 > '9')
            {
                break;
            }
            i2e++;
        }
        int i1 = Integer.parseInt(s1.substring(i1s, i1e + 1));
        int i2 = Integer.parseInt(s2.substring(i2s, i2e + 1));
        if (i1 < i2)
        {
            return -1;
        }
        else if (i1 > i2)
        {
            return 1;
        }
        else
        {
            return i1e > i2e ? -1 : 1;
        }
    }

    private String toCamelCase(String s)
    {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String sharp(String s)
    {
        int codepoint = 0;
        try
        {
            codepoint = Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            // Ignore
        }
        return new String(Character.toChars(codepoint));
    }

    private String visible(String s)
    {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            if (ch < 32)
            {
                sb.append('^');
                sb.append((char)(ch + '@'));
            }
            else
            {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> asCollection(Object val)
    {
        return (Collection) val;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> asMap(Object val)
    {
        return (Map) val;
    }

    private List<Object> toList(Map<Object, Object> val1, boolean flagk, boolean flagv)
    {
        List<Object> l = new ArrayList<>();
        if (flagk && flagv)
        {
            for (Map.Entry<Object, Object> entry : val1.entrySet())
            {
                l.add(entry.getKey());
                l.add(entry.getValue());
            }
        }
        else if (flagk)
        {
            l.addAll(val1.keySet());
        }
        else
        {
            l.addAll(val1.values());
        }
        return l;
    }

    private Object getAndEvaluateName() throws Exception
    {
        Object r = getName('}');
        if (r instanceof Token)
        {
            return evaluate.get(expand((Token) r).toString());
        }
        else
        {
            return r;
        }
    }

    private Object getName(char closing) throws Exception
    {
        if (ch == '\"')
        {
            if (peek() != '$')
            {
                throw new IllegalArgumentException("bad substitution");
            }
            boolean oldInQuote = inQuote;
            try
            {
                inQuote = true;
                getch();
                Object val = getName(closing);
                return val;
            }
            finally
            {
                inQuote = oldInQuote;
            }
        }
        else if (ch == '$')
        {
            return expandVar();
        }
        else {
            int start = index - 1;
            while (ch != EOT && ch != closing && isName(ch))
            {
                getch();
                if (ch == '\\')
                {
                    escape();
                }
                else if (ch == '{')
                {
                    findClosing();
                }
            }
            if (ch == EOT)
            {
                throw new EOFError(line, column, "unexpected EOT looking for matching '}'", "compound", Character.toString('}'));
            }
            return text.subSequence(start, index - 1);
        }
    }

    private String getPattern(String closing) throws Exception
    {
        CharSequence sub = findUntil(text, index - 1, closing);
        index += sub.length() - 1;
        getch();
        return expandPattern(sub).toString();
    }

    private CharSequence findUntil(CharSequence text, int start, String closing) throws Exception
    {
        int braces = 0;
        boolean escaped = false;
        boolean doubleQuoted = false;
        boolean singleQuoted = false;
        for (int i = start; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (doubleQuoted && escaped)
            {
                escaped = false;
            }
            else if (escaped)
            {
                escaped = false;
            }
            else if (singleQuoted)
            {
                if (c == '\'')
                {
                    singleQuoted = false;
                }
            }
            else if (doubleQuoted)
            {
                if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == '\"')
                {
                    doubleQuoted = false;
                }
            }
            else if (c == '\\')
            {
                escaped = true;
            }
            else if (c == '\'')
            {
                singleQuoted = true;
            }
            else if (c == '"')
            {
                doubleQuoted = true;
            }
            else {
                if (braces == 0 && closing.indexOf(c) >= 0)
                {
                    return text.subSequence(start, i);
                }
                else if (c == '{')
                {
                    braces++;
                }
                else if (c == '}')
                {
                    braces--;
                }
            }
        }
        return text.subSequence(start, text.length());
    }

    private Object getValue() throws Exception
    {
        if (ch == '$')
        {
            return expandVar();
        }
        else
        {
            int start = index - 1;
            while (ch != EOT && ch != '}')
            {
                if (ch == '\\')
                {
                    escape();
                    getch();
                }
                else if (ch == '{' || ch == '(' || ch == '[')
                {
                    findClosing();
                }
                else
                {
                    getch();
                }
            }
            if (ch == EOT)
            {
                throw new EOFError(line, column, "unexpected EOT looking for matching '}'", "compound", Character.toString('}'));
            }
            Token name = text.subSequence(start, index - 1);
            return expand(name).toString();
        }
    }

    private void findClosing()
    {
        char start = ch;
        while (getch() != EOT)
        {
            if (ch == '(' || ch == '{' || ch == '[')
            {
                findClosing();
            }
            else if (start == '(' && ch == ')'
                    || start == '{' && ch == '}'
                    || start == '[' && ch == ']')
            {
                return;
            }
        }
    }

    private static final char EOL = 0;

    private static boolean isRegexMeta(char ch)
    {
        return ".^$+{[]|()".indexOf(ch) != -1;
    }

    private static boolean isGlobMeta(char ch)
    {
        return "\\*?[{".indexOf(ch) != -1;
    }

    private static char next(String str, int index)
    {
        return index < str.length() ? str.charAt(index) : EOL;
    }

    /**
     * Convert a string containing escape sequences and quotes, representing a glob pattern
     * to the corresponding regexp pattern
     */
    private static String unquoteGlob(String str)
    {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        boolean escaped = false;
        boolean doubleQuoted = false;
        boolean singleQuoted = false;
        while (index < str.length())
        {
            char ch = str.charAt(index++);
            if (escaped)
            {
                if (isGlobMeta(ch))
                {
                    sb.append('\\');
                }
                sb.append(ch);
                escaped = false;
            }
            else if (singleQuoted)
            {
                if (ch == '\'')
                {
                    singleQuoted = false;
                }
                else
                {
                    if (isGlobMeta(ch))
                    {
                        sb.append('\\');
                    }
                    sb.append(ch);
                }
            }
            else if (doubleQuoted)
            {
                if (ch == '\\')
                {
                    escaped = true;
                }
                else if (ch == '\"')
                {
                    doubleQuoted = false;
                }
                else
                {
                    if (isGlobMeta(ch))
                    {
                        sb.append('\\');
                    }
                    sb.append(ch);
                }
            }
            else
            {
                switch (ch)
                {
                    case '\\':
                        escaped = true;
                        break;
                    case '\'':
                        singleQuoted = true;
                        break;
                    case '"':
                        doubleQuoted = true;
                        break;
                    default:
                        sb.append(ch);
                        break;
                }
            }
        }
        return sb.toString();
    }

    private static String toRegexPattern(String str, boolean shortest)
    {
        boolean inGroup = false;
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (index < str.length())
        {
            char ch = str.charAt(index++);
            switch (ch)
            {
                case '*':
                    sb.append(shortest ? ".*?" : ".*");
                    break;
                case ',':
                    if (inGroup)
                    {
                        sb.append(")|(?:");
                    }
                    else
                    {
                        sb.append(',');
                    }
                    break;
                case '?':
                    sb.append(".");
                    break;
                case '[':
                    sb.append("[");
                    if (next(str, index) == '^')
                    {
                        sb.append("\\^");
                        ++index;
                    }
                    else
                    {
                        if (next(str, index) == '!')
                        {
                            sb.append('^');
                            ++index;
                        }
                        if (next(str, index) == '-')
                        {
                            sb.append('-');
                            ++index;
                        }
                    }
                    boolean inLeft = false;
                    char left = 0;
                    while (index < str.length())
                    {
                        ch = str.charAt(index++);
                        if (ch == ']')
                        {
                            break;
                        }
                        if (ch == '\\' || ch == '[' || ch == '&' && next(str, index) == '&')
                        {
                            sb.append('\\');
                        }
                        sb.append(ch);
                        if (ch == '-')
                        {
                            if (!inLeft)
                            {
                                throw new PatternSyntaxException("Invalid range", str, index - 1);
                            }
                            if ((ch = next(str, index++)) == EOL || ch == ']')
                            {
                                break;
                            }
                            if (ch < left)
                            {
                                throw new PatternSyntaxException("Invalid range", str, index - 3);
                            }
                            sb.append(ch);
                            inLeft = false;
                        }
                        else
                        {
                            inLeft = true;
                            left = ch;
                        }
                    }
                    if (ch != ']')
                    {
                        throw new PatternSyntaxException("Missing \']", str, index - 1);
                    }
                    sb.append("]");
                    break;
                case '\\':
                    if (index == str.length())
                    {
                        throw new PatternSyntaxException("No character to escape", str, index - 1);
                    }
                    char ch2 = str.charAt(index++);
                    if (isGlobMeta(ch2) || isRegexMeta(ch2))
                    {
                        sb.append('\\');
                    }
                    sb.append(ch2);
                    break;
                case '{':
                    if (inGroup)
                    {
                        throw new PatternSyntaxException("Cannot nest groups", str, index - 1);
                    }
                    sb.append("(?:(?:");
                    inGroup = true;
                    break;
                case '}':
                    if (inGroup)
                    {
                        sb.append("))");
                        inGroup = false;
                    }
                    else
                    {
                        sb.append('}');
                    }
                    break;
                default:
                    if (isRegexMeta(ch))
                    {
                        sb.append('\\');
                    }
                    sb.append(ch);
                    break;
            }
        }
        if (inGroup)
        {
            throw new PatternSyntaxException("Missing \'}", str, index - 1);
        }
        return sb.toString();
    }


    private boolean isName(char ch)
    {
        return Character.isJavaIdentifierPart(ch) && (ch != '$') || ('.' == ch);
    }

}
