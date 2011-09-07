/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.plugins.upnp.internal;

class Hex
{

    private Hex()
    {
        // prevent instantiation
    }

    /**
     * Decodes HEX form a <code>String</code>. The new line (\n) and carriage
     * return (\r) symbols in the string are ignored.
     * 
     * The HEX encoded array <pre>0, 15, 33</pre> is encodes as <pre>0-f-21</pre>
     * 
     * @param input the HEX encoded string, that will be decoded
     * @return the decoded binary, byte array data
     * @throws IllegalArgumentException if illegal hex block is detected (e.g. the number of
     *           symbols is more than 2), if illegal character is detected in the
     *           encoded string - a one, that is not a hex digit, dash (-), new
     *           line (\n) or carriage return (\r).
     */
    public static final byte[] decode(String input) throws IllegalArgumentException
    {
        char[] chars = input.toCharArray();
        byte[] tmp = new byte[1 + chars.length / 2];
        int length = 0;
        int current = 0;
        int hexCharatersCount = 0;
        for (int i = 0; i < chars.length; i++)
        {
            char ch = Character.toUpperCase(chars[i]);
            switch (ch)
            {
                case '-':
                    /* add to the array and increase length */
                    tmp[length++] = (byte) current;
                    hexCharatersCount = 0;
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
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                    hexCharatersCount++;
                    if (hexCharatersCount > 2)
                    {
                        throw new IllegalArgumentException("Illegal block detected @ "
                            + i + ", data = " + input);
                    }
                    if (hexCharatersCount == 1)
                    {
                        current = toHex(ch);
                    }
                    else
                    {
                        current = (current << 4) | toHex(ch);
                    }
                    break;
                case '\r':
                case '\n':
                    /* ignored characters */
                    break;
                default:
                    throw new IllegalArgumentException("Illegal character encoding @ "
                        + i + ", char: " + ch);
            }
            if (hexCharatersCount > 0)
            {
                tmp[length] = (byte) current;
            }
        }
        byte[] ret = new byte[length + 1];
        System.arraycopy(tmp, 0, ret, 0, length + 1);
        return ret;
    }

    private static final int toHex(char ch)
    {
        if (ch >= '0' && ch <= '9')
        {
            return ch - '0';
        }
        else if (ch >= 'A' && ch <= 'F')
        {
            return ch - 'A' + 10;
        }
        else
        {
            throw new IllegalArgumentException("Illegal character: " + ch);
        }
    }

}
