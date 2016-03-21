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

public class Token implements CharSequence {

    protected final char[] ch;
    protected final int start;
    protected final int length;
    protected final int line;
    protected final int column;

    public Token(CharSequence cs)
    {
        if (cs instanceof Token)
        {
            Token ca = (Token) cs;
            this.ch = ca.ch;
            this.start = ca.start;
            this.length = ca.length;
            this.line = ca.line;
            this.column = ca.column;
        }
        else
        {
            this.ch = cs.toString().toCharArray();
            this.start = 0;
            this.length = ch.length;
            this.line = 0;
            this.column = 0;
        }
    }

    public Token(char[] _ch, int _start, int _length, int _line, int _col)
    {
        this.ch = _ch;
        this.start = _start;
        this.length = _length;
        this.line = _line;
        this.column = _col;
    }

    public int line()
    {
        return line;
    }

    public int column()
    {
        return column;
    }

    public int start()
    {
        return start;
    }

    public int length()
    {
        return this.length;
    }

    public char charAt(int index)
    {
        return this.ch[this.start + index];
    }

    public Token subSequence(int start, int end)
    {
        int line = this.line;
        int col = this.column;
        for (int i = this.start; i < this.start + start; i++)
        {
            if (ch[i] == '\n')
            {
                line++;
                col = 0;
            }
            else
            {
                col++;
            }
        }
        return new Token(this.ch, this.start + start, end - start, line, col);
    }

    public String toString()
    {
        return new String(this.ch, this.start, this.length);
    }

    public static boolean eq(CharSequence cs1, CharSequence cs2)
    {
        if (cs1 == cs2)
        {
            return true;
        }
        int l1 = cs1.length();
        int l2 = cs2.length();
        if (l1 != l2)
        {
            return false;
        }
        for (int i = 0; i < l1; i++)
        {
            if (cs1.charAt(i) != cs2.charAt(i))
            {
                return false;
            }
        }
        return true;
    }

}
