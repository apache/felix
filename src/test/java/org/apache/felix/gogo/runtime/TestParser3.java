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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * Test features of the new parser/tokenizer, many of which are not supported
 * by the original parser.
 */
public class TestParser3 extends AbstractParserTest
{
    @Test
    public void testArithmetic() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);

        assertEquals("10d", c.execute("echo %(2*(3+2))d"));
        assertEquals(3l, c.execute("%(1+2)"));

        c.set("a", 2l);
        assertEquals(3l, c.execute("%(a+=1)"));
        assertEquals(3l, c.get("a"));
    }

    public CharSequence echo(Object args[])
    {
        if (args == null)
        {
            return "null args!";
        }

        StringBuilder sb = new StringBuilder();
        for (Object arg : args)
        {
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(String.valueOf(arg));
        }
        return sb.toString();
    }

}
