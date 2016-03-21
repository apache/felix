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

public class BaseTokenizer
{

    protected static final char EOT = (char) -1;

    protected final Token text;

    protected short line;
    protected short column;
    protected char ch;
    protected int index;

    public BaseTokenizer(CharSequence text)
    {
        this.text = text instanceof Token ? (Token) text : new Token(text);
        getch();
    }

    public Token text()
    {
        return text;
    }

    protected void find(char target, char deeper)
    {
        final short sLine = line;
        final short sCol = column;
        int level = 1;

        while (level != 0)
        {
            if (eot())
            {
                throw new EOFError(sLine, sCol, "unexpected eof found in the middle of a compound for '"
                        + deeper + target + "'", "compound", Character.toString(target));
                // TODO: fill context correctly
            }

            getch();
            if (ch == '\\')
            {
                escape();
                continue;
            }
            if (ch == target)
            {
                level--;
            }
            else
            {
                if (ch == deeper)
                {
                    level++;
                }
                else
                {
                    if (ch == '"' || ch == '\'' || ch == '`')
                    {
                        skipQuote();
                    }
                }
            }
        }
    }

    protected char escape()
    {
        assert '\\' == ch;
        final short sLine = line;
        final short sCol = column;

        switch (getch())
        {
            case 'u':
                getch();
                int nb = 0;
                for (int i = 0; i < 4; i++)
                {
                    char ch = Character.toUpperCase(this.ch);
                    if (ch >= '0' && ch <= '9')
                    {
                        nb = nb * 16 + (ch - '0');
                        getch();
                        continue;
                    }
                    if (ch >= 'A' && ch <= 'F')
                    {
                        nb = nb * 16 + (ch - 'A' + 10);
                        getch();
                        continue;
                    }
                    if (ch == 0) {
                        throw new EOFError(sLine, sCol, "unexpected EOT in \\ escape", "escape", "0");
                    } else {
                        throw new SyntaxError(sLine, sCol, "bad unicode", text);
                    }
                }
                index--;
                return (char) nb;

            case EOT:
                throw new EOFError(sLine, sCol, "unexpected EOT in \\ escape", "escape", " ");

            case '\n':
                return '\0'; // line continuation

            case '\\':
            case '\'':
            case '"':
            case '$':
                return ch;

            default:
                return ch;
        }
    }

    protected void skipQuote()
    {
        assert '\'' == ch || '"' == ch;
        final char quote = ch;
        final short sLine = line;
        final short sCol = column;

        while (getch() != EOT)
        {
            if (quote == ch)
            {
                return;
            }

            if ((quote == '"') && ('\\' == ch))
                escape();
        }

        throw new EOFError(sLine, sCol, "unexpected EOT looking for matching quote: "
                + quote,
                quote == '"' ? "dquote" : "quote",
                Character.toString(quote));
    }

    protected void skipSpace()
    {
        skipSpace(false);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    protected void skipSpace(boolean skipNewLines)
    {
        while (true)
        {
            while (isBlank(ch))
            {
                getch();
            }

            // skip continuation lines, but not other escapes
            if (('\\' == ch) && (peek() == '\n'))
            {
                getch();
                getch();
                continue;
            }

            if (skipNewLines && ('\n' == ch))
            {
                getch();
                continue;
            }

            if (skipNewLines && ('\r' == ch) && (peek() == '\n'))
            {
                getch();
                getch();
                continue;
            }

            // skip comments
            if (('/' == ch) || ('#' == ch))
            {
                if (('#' == ch) || (peek() == '/'))
                {
                    while ((getch() != EOT) && ('\n' != ch))
                    {
                    }
                    continue;
                }
                else if ('*' == peek())
                {
                    short sLine = line;
                    short sCol = column;
                    getch();

                    while ((getch() != EOT) && !(('*' == ch) && (peek() == '/')))
                    {
                    }

                    if (EOT == ch)
                    {
                        throw new EOFError(sLine, sCol,
                                "unexpected EOT looking for closing comment: */",
                                "comment",
                                "*/");
                    }

                    getch();
                    getch();
                    continue;
                }
            }

            break;
        }
    }

    protected boolean isBlank(char ch)
    {
        return ' ' == ch || '\t' == ch;
    }

    protected boolean eot()
    {
        return index >= text.length();
    }

    protected char getch()
    {
        return ch = getch(false);
    }

    protected char peek()
    {
        return getch(true);
    }

    protected char getch(boolean peek)
    {
        if (eot())
        {
            if (!peek)
            {
                ++index;
                ch = EOT;
            }
            return EOT;
        }

        int current = index;
        char c = text.charAt(index++);

//        if (('\r' == c) && !eot() && (text.charAt(index) == '\n'))
//            c = text.charAt(index++);

        if (peek)
        {
            index = current;
        }
        else if ('\n' == c)
        {
            ++line;
            column = 0;
        }
        else
            ++column;

        return c;
    }

}
