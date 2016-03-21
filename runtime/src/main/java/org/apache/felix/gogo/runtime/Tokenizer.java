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

import java.util.regex.Pattern;

public class Tokenizer extends BaseTokenizer
{

    private final Pattern redir = Pattern.compile("[0-9&]?>|[0-9]?>>|[0-9]?>&|[0-9]?<|[0-9]?<>|<<<|<<\\-?");

    protected boolean inArray;
    protected int word = 0;

    protected Token pushed;
    protected Token last;

    public Tokenizer(CharSequence text)
    {
        super(text);
    }

    public Token text()
    {
        return text;
    }

    public Token next()
    {
        if (pushed != null)
        {
            Token t = pushed;
            pushed = null;
            return t;
        }
        skipSpace(last == null || Token.eq(last, "\n"));
        int start = index - 1;
        Token t, tn;
        while (true)
        {
            switch (ch)
            {
                case EOT:
                    return token(start);
                case '[':
                    word = 0;
                    inArray = true;
                    return token(start);
                case ']':
                    inArray = false;
                    word++;
                    return token(start);
                case '{':
                    if (start == index - 1 && Character.isWhitespace(peek()))
                    {
                        word = 0;
                        return token(start);
                    }
                    else
                    {
                        if (ch == '{')
                        {
                            find('}', '{');
                        }
                        else
                        {
                            find(')', '(');
                        }
                        getch();
                        break;
                    }
                case '(':
                    if (start == index - 1)
                    {
                        word = 0;
                        return token(start);
                    }
                    else
                    {
                        if (ch == '{')
                        {
                            find('}', '{');
                        }
                        else
                        {
                            find(')', '(');
                        }
                        getch();
                        break;
                    }
                case '>':
                case '<':
                    t = text.subSequence(start, index);
                    tn = text.subSequence(start, index + 1);
                    if (redir.matcher(tn).matches())
                    {
                        getch();
                        break;
                    }
                    if (redir.matcher(t).matches() && start < index - 1)
                    {
                        getch();
                    }
                    word = 0;
                    return token(start);
                case '-':
                    t = text.subSequence(start, index);
                    if (redir.matcher(t).matches())
                    {
                        getch();
                        return token(start);
                    }
                    else {
                        getch();
                        break;
                    }
                case '&':
                    // beginning of token
                    if (start == index - 1) {
                        if (peek() == '&' || peek() == '>')
                        {
                            getch();
                            getch();
                        }
                        word = 0;
                        return token(start);
                    }
                    // in the middle of a redirection
                    else if (redir.matcher(text.subSequence(start, index)).matches())
                    {
                        getch();
                        break;
                    }
                    else
                    {
                        word = 0;
                        return token(start);
                    }
                case '|':
                    if (start == index - 1 && (peek() == '|' || peek() == '&'))
                    {
                        getch();
                        getch();
                    }
                    word = 0;
                    return token(start);
                case ';':
                    word = 0;
                    return token(start);
                case '}':
                case ')':
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                    word++;
                    return token(start);
                case '=':
                    if (inArray || word < 1 || index == start + 1)
                    {
                        word++;
                        return token(start);
                    }
                    getch();
                    break;
                case '\\':
                    escape();
                    getch();
                    break;
                case '\'':
                case '"':
                    skipQuote();
                    getch();
                    break;
                default:
                    getch();
                    break;
            }
        }
    }

    private Token token(int start)
    {
        if (start == index - 1)
        {
            if (ch == EOT)
            {
                return null;
            }
            if (ch == '\r' && peek() == '\n')
            {
                getch();
            }
            getch();
            last = text.subSequence(index - 2, index - 1);
        }
        else
        {
            last = text.subSequence(start, index - 1);
        }
        return last;
    }

    public void push(Token token)
    {
        this.pushed = token;
    }

    public Token readHereDoc(boolean ignoreLeadingTabs)
    {
        final short sLine = line;
        final short sCol = column;
        int start;
        int nlIndex;
        boolean nl;
        // Find word
        skipSpace();
        start = index - 1;
        while (ch != '\n' && ch != EOT) {
            getch();
        }
        if (ch == EOT) {
            throw new EOFError(sLine, sCol, "expected here-doc start", "heredoc", "foo");
        }
        Token token = text.subSequence(start, index - 1);
        getch();
        start = index - 1;
        nlIndex = start;
        nl = true;
        // Get heredoc
        while (true)
        {
            if (nl)
            {
                if (ignoreLeadingTabs && ch == '\t')
                {
                    nlIndex++;
                }
                else
                {
                    nl = false;
                }
            }
            if (ch == '\n' || ch == EOT)
            {
                Token s = text.subSequence(nlIndex, index - 1);
                if (Token.eq(s, token))
                {
                    Token hd = text.subSequence(start, s.start());
                    getch();
                    return hd;
                }
                nlIndex = index;
                nl = true;
            }
            if (ch == EOT)
            {
                throw new EOFError(sLine, sCol, "unexpected eof found in here-doc", "heredoc", token.toString());
            }
            getch();
        }
    }
}
