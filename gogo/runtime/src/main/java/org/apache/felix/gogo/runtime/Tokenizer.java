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

public class Tokenizer extends BaseTokenizer
{

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
                case ';':
                case '|':
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

    public void skip(int length)
    {
        while (--length >= 0)
        {
            getch();
        }
    }

}
