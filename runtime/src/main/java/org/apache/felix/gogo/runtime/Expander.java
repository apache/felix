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
                    Object val = expandVar();

                    if (EOT == ch && buf.length() == 0)
                    {
                        return val;
                    }

                    if (null != val)
                    {
                        buf.append(val);
                    }

                    continue; // expandVar() has already read next char

                case '\\':
                    ch = (inQuote && ("u$\\\n\"".indexOf(peek()) == -1)) ? '\\'
                            : escape();

                    if (ch != '\0') // ignore line continuation
                    {
                        buf.append(ch);
                    }

                    break;

                case '"':
                    skipQuote();
                    value = text.subSequence(start, index - 1);
                    Object expand = expand(value, evaluate, true);
                    if (eot() && buf.length() == 0 && value == expand)
                    {
                        // FELIX-2468 avoid returning CharSequence implementation
                        return value.toString();
                    }
                    if (null != expand)
                    {
                        buf.append(expand.toString());
                    }
                    break;

                case '\'':
                    if (!inQuote)
                    {
                        skipQuote();
                        value = text.subSequence(start, index - 1);

                        if (eot() && buf.length() == 0)
                        {
                            return value.toString();
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
        Object val;

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

                    default:
                        throw new SyntaxError(sLine, sCol, "bad substitution: ${" + group + "}");
                }
            }
            getch();
        }

        return val;
    }

    private boolean isName(char ch)
    {
        return Character.isJavaIdentifierPart(ch) && (ch != '$') || ('.' == ch);
    }

}
