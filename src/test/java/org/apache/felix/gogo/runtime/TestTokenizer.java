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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTokenizer
{
    private final Map<String, Object> vars = new HashMap<>();
    private final Evaluate evaluate;

    public TestTokenizer()
    {
        evaluate = new Evaluate()
        {
            public Object eval(Token t) throws Exception
            {
                throw new UnsupportedOperationException("eval not implemented.");
            }

            public Object get(String key)
            {
                return vars.get(key);
            }

            public Object put(String key, Object value)
            {
                return vars.put(key, value);
            }

            public Object expr(Token t) {
                throw new UnsupportedOperationException("expr not implemented.");
            }
        };
    }

    @Test
    public void testHello() throws Exception
    {
        testHello("hello world\n");
        testHello("hello world\n\n"); // multiple \n reduced to single Token.NL
        testHello("hello world\r\n"); // \r\n -> \n

        // escapes

        testHello("hello \\\nworld\n");
        try
        {
            testHello("hello\\u20world\n");
            fail("bad unicode accepted");
        }
        catch (SyntaxError e)
        {
            // expected
        }

        // whitespace and comments

        testHello(" hello  world    \n ");
        testHello("hello world // comment\n\n");
        testHello("hello world #\\ comment\n\n");
        testHello("// comment\nhello world\n");
        testHello("// comment ?\\ \nhello world\n");
        testHello("hello /*\n * comment\n */ world\n");
    }

    // hello world
    private void testHello(CharSequence text) throws Exception
    {
        Tokenizer t = new Tokenizer(text);
        assertEquals("hello", t.next().toString());
        assertEquals("world", t.next().toString());
        assertEquals("\n", t.next().toString());
        assertNull(t.next());
    }
    
    @Test
    public void testString() throws Exception
    {
        testString("'single $quote' \"double $quote\"\n");
    }

    // 'single quote' "double quote"
    private void testString(CharSequence text) throws Exception
    {
        Tokenizer t = new Tokenizer(text);
        assertEquals("'single $quote'", t.next().toString());
        assertEquals("\"double $quote\"", t.next().toString());
        assertEquals("\n", t.next().toString());
        assertNull(t.next());
    }

    @Test
    public void testClosure() throws Exception
    {
        testClosure2("x = { echo '}' $args //comment's\n}\n");
        testClosure2("x={ echo '}' $args //comment's\n}\n");
        token1("{ echo \\{ $args \n}");
        token1("{ echo \\} $args \n}");
    }

    //
    // x = {echo $args};
    //
    private void testClosure2(CharSequence text) throws Exception
    {
        Tokenizer t = new Tokenizer(text);
        assertEquals("x", t.next().toString());
        assertEquals("=", t.next().toString());
        assertEquals("{", t.next().toString());
        assertEquals("echo", t.next().toString());
        assertEquals("'}'", t.next().toString());
        assertEquals("$args", t.next().toString());
        assertEquals("\n", t.next().toString());
        assertEquals("}", t.next().toString());
        assertEquals("\n", t.next().toString());
        assertEquals(null, t.next());
    }

    private void token1(CharSequence text) throws Exception
    {
        Tokenizer t = new Tokenizer(text);
        assertEquals("{", t.next().toString());
        assertEquals("echo", t.next().toString());
        t.next();
        assertEquals("$args", t.next().toString());
        assertEquals("\n", t.next().toString());
        assertEquals("}", t.next().toString());
        assertNull(t.next());
    }

    @Test
    public void testSubscripts() throws Exception
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a1", "baz");
        map.put("a2", "bar");
        map.put("b1", "foo");

        vars.clear();
        vars.put("key", "a1");
        vars.put("map", map);

        assertEquals("baz", expand("${map[a1]}"));
        assertEquals("baz", expand("${map[$key]}"));
        assertEquals("az", expand("${map[a1][1,3]}"));
        assertEquals("AZ", expand("${(U)map[a1][1,3]}"));
        assertEquals(map, expand("${map}"));
        assertEquals("baz bar foo", expand("\"${map}\""));
        assertEquals(Arrays.asList("baz", "bar", "foo"), expand("\"${map[@]}\""));
        assertEquals(Arrays.asList("a1", "a2", "b1"), expand("\"${(k)map[@]}\""));
        assertEquals(Arrays.asList("a1", "baz", "a2", "bar", "b1", "foo"), expand("\"${(kv)map[@]}\""));
        assertEquals(Arrays.asList("a2", "bar"), expand("${${(kv)map[@]}[2,4]}"));
        assertEquals(Arrays.asList("a2", "bar"), expand("${${(kv)=map}[2,4]}"));
    }

    @Test
    public void testExpand() throws Exception
    {
        final URI home = new URI("/home/derek");
        final File pwd = new File("/tmp");
        final String user = "derek";

        vars.clear();
        vars.put("HOME", home);
        vars.put("PWD", pwd);
        vars.put("USER", user);
        vars.put(user, "Derek Baum");

        // quote removal
        assertEquals("hello", expand("hello").toString());
        assertEquals("hello", expand("'hello'"));
        assertEquals("\"hello\"", expand("'\"hello\"'"));
        assertEquals("hello", expand("\"hello\""));
        assertEquals("'hello'", expand("\"'hello'\""));

        // escapes
        assertEquals("hello\\w", expand("hello\\\\w"));
        assertEquals("hellow", expand("hello\\w"));
        assertEquals("hello\\w", expand("\"hello\\\\w\""));
        assertEquals("hello\\w", expand("\"hello\\w\""));
        assertEquals("hello\\\\w", expand("'hello\\\\w'"));
        assertEquals("hello", expand("he\\\nllo"));
        assertEquals("he\\llo", expand("'he\\llo'"));
        assertEquals("he'llo", expand("'he'\\''llo'"));
        assertEquals("he\"llo", expand("\"he\\\"llo\""));
        assertEquals("he'llo", expand("he\\'llo"));
        assertEquals("he$llo", expand("\"he\\$llo\""));
        assertEquals("he\\'llo", expand("\"he\\'llo\""));
        assertEquals("hello\\w", expand("\"hello\\w\""));

        // unicode

        // Note: we could use literal Unicode pound '£' instead of \u00a3 in next test.
        // if above is not UK currency symbol, then your locale is not configured for UTF-8.
        // Java on Macs cannot handle UTF-8 unless you explicitly set '-Dfile.encoding=UTF-8'.
        assertEquals("pound\u00a3cent\u00a2", expand("pound\\u00a3cent\\u00a2"));
        assertEquals("euro\\u20ac", expand("'euro\\u20ac'"));
        try
        {
            expand("eot\\u20a");
            fail("EOT in unicode");
        }
        catch (SyntaxError e)
        {
            // expected
        }
        try
        {
            expand("bad\\u20ag");
            fail("bad unicode");
        }
        catch (SyntaxError e)
        {
            // expected
        }

        // simple variable expansion - quoting or concatenation converts result to String
        assertEquals(user, expand("$USER"));
        assertEquals(home, expand("$HOME"));
        assertEquals(home.toString(), expand("$HOME$W"));
        assertEquals(pwd, expand("$PWD"));
        assertEquals("$PWD", expand("'$PWD'"));
        assertEquals("$PWD", expand("\\$PWD"));
        assertEquals(pwd.toString(), expand("\"$PWD\""));
        assertEquals("W" + pwd, expand("W$PWD"));
        assertEquals(pwd + user, expand("$PWD$USER"));

        // variable substitution  ${NAME:-WORD} etc
        assertNull(expand("$JAVA_HOME"));
        assertEquals(user, expand("${USER}"));
        assertEquals(user + "W", expand("${USER}W"));
        assertEquals("java_home", expand("${JAVA_HOME:-java_home}"));
        assertEquals(pwd, expand("${NOTSET:-$PWD}"));
        assertNull(vars.get("JAVA_HOME"));
        assertEquals("java_home", expand("${JAVA_HOME:=java_home}"));
        assertEquals("java_home", vars.get("JAVA_HOME"));
        assertEquals("java_home", expand("$JAVA_HOME"));
        assertEquals("yes", expand("${JAVA_HOME:+yes}"));
        assertNull(expand("${NOTSET:+yes}"));
        assertEquals("", expand("\"${NOTSET:+yes}\""));
        try
        {
            expand("${NOTSET:?}");
            fail("expected 'not set' exception");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        // bad variable names
        assertEquals("$ W", expand("$ W"));
        assertEquals("$ {W}", expand("$ {W}"));
        try
        {
            expand("${W }");
            fail("expected syntax error");
        }
        catch (SyntaxError e)
        {
            // expected
        }

        try {
            expand("${USER\\\n:?}");
        }
        catch (SyntaxError e)
        {
            // expected
        }
        assertEquals(user, expand("${US\\u0045R:?}"));

        // bash doesn't supported nested expansions
        // gogo only supports them in the ${} syntax
        assertEquals("Derek Baum", expand("${(P)$USER}"));
        assertEquals("Derek Baum", expand("${${(P)USER}:-Derek Baum}"));
        assertEquals("Derek Baum", expand("${${(P)USR}:-$derek}"));
        assertEquals("derek", expand("${${USER}}"));
        assertEquals("derek", expand("${${USER:-d}}"));
        assertEquals("x", expand("${$USR:-x}"));
        assertEquals("$" + user, expand("$$USER"));
    }

    private Object expand(CharSequence word) throws Exception
    {
        return Expander.expand(word, evaluate);
    }

    @Test
    public void testParser() throws Exception
    {
        new Parser("// comment\n" + "a=\"who's there?\"; ps -ef;\n" + "ls | \n grep y\n").program();
        String p1 = "a=1 \\$b=2 c={closure}\n";
        new Parser(p1).program();
        new Parser("[" + p1 + "]").program();
    }

    //
    // FELIX-4679 / FELIX-4671.
    //
    @Test
    public void testScriptFelix4679() throws Exception
    {
        String script = "addcommand system (((${.context} bundles) 0) loadclass java.lang.System)";

        PrintStream sout = new PrintStream(System.out) {
            @Override
            public void close() {
            }
        };
        PrintStream serr = new PrintStream(System.err) {
            @Override
            public void close() {
            }
        };
        ThreadIOImpl tio = new ThreadIOImpl();
        tio.start();

        try
        {
            BundleContext bc = createMockContext();

            CommandProcessorImpl processor = new CommandProcessorImpl(tio);
            processor.addCommand("gogo", processor, "addcommand");
            processor.addConstant(".context", bc);

            CommandSessionImpl session = new CommandSessionImpl(processor, new ByteArrayInputStream(script.getBytes()), sout, serr);

            Closure c = new Closure(session, null, script);
            assertNull(c.execute(session, null));
        }
        finally
        {
            tio.stop();
        }
    }

    private BundleContext createMockContext() throws ClassNotFoundException
    {
        Bundle systemBundle = mock(Bundle.class);
        when(systemBundle.loadClass(eq("java.lang.System"))).thenReturn(System.class);

        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundles()).thenReturn(new Bundle[] { systemBundle });
        return bc;
    }
}
