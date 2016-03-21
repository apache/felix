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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTokenizer
{
    private final Map<String, Object> vars = new HashMap<>();
    private final Evaluate evaluate;
    private Path currentDir = null;

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

            public Path currentDir() {
                return currentDir;
            }
        };
    }

    @Before
    public void setUp() {
        currentDir = null;
        vars.clear();
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
    public void testJavaArray() throws Exception
    {
        vars.put("a", new Object[] { "b", "c"});

        assertEquals(Arrays.asList("B", "C"), expand("${(U)a}"));
    }

    @Test
    public void testRawVariable() throws Exception
    {
        vars.put("a1", Arrays.asList("a", 1));

        assertSame(vars.get("a1"), expand("$a1"));
        assertNotSame(vars.get("a1"), expand("${a1}"));
        assertEquals(vars.get("a1"), expand("${a1}"));
    }

    @Test
    public void testSubscripts() throws Exception
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a1", "baz");
        map.put("a2", "bar");
        map.put("b1", "foo");

        vars.put("key", "a1");
        vars.put("map", map);

        assertEquals("baz", expand("${map[a1]}"));
        assertEquals("baz", expand("${map[$key]}"));
        assertEquals("az", expand("${map[a1][1,-0]}"));
        assertEquals("AZ", expand("${(U)map[a1][1,3]}"));
        assertEquals(map, expand("${map}"));
        assertEquals("baz bar foo", expand("\"${map}\""));
        assertEquals("baz bar foo", expand("\"${map[*]}\""));
        assertEquals(Arrays.asList("baz", "bar", "foo"), expand("\"${map[@]}\""));
        assertEquals(Arrays.asList("a1", "a2", "b1"), expand("\"${(k)map[@]}\""));
        assertEquals(Arrays.asList("a1", "baz", "a2", "bar", "b1", "foo"), expand("\"${(kv)map[@]}\""));
        assertEquals(Arrays.asList("a2", "bar"), expand("${${(kv)map[@]}[2,4]}"));
        assertEquals(Arrays.asList("a2", "bar"), expand("${${(kv)=map}[2,4]}"));

        // TODO: test subscripts on array resulting in a single element
    }

    @Test
    public void testBraces() throws Exception {
        assertEquals(Arrays.asList("1", "3"), expand("{1..3..2}"));
        assertEquals(Arrays.asList("3", "1"), expand("{1..3..-2}"));
        assertEquals(Arrays.asList("1", "3"), expand("{3..1..-2}"));
        assertEquals(Arrays.asList("3", "1"), expand("{3..1..2}"));

        assertEquals(Arrays.asList("1", "2", "3"), expand("{1..3}"));
        assertEquals(Arrays.asList("a1b", "a2b", "a3b"), expand("a{1..3}b"));

        assertEquals(Arrays.asList("a1b", "a2b", "a3b"), expand("a{1,2,3}b"));

        assertEquals(Arrays.asList("a1b", "a2b", "a3b"), expand("a{1,2,3}b"));
        assertEquals(Arrays.asList("a1b", "a,b", "a3b"), expand("a{1,',',3}b"));

        currentDir = Paths.get(".");
        try {
            expand("a{1,*,3}b");
            fail("Expected exception");
        } catch (Exception e) {
            assertEquals("no matches found: a*b", e.getMessage());
        } finally {
            currentDir = null;
        }

        vars.put("a", "1");
        assertEquals(Arrays.asList("a1b", "a\nb", "a3b"), expand("a{$a,$'\\n',3}b"));
        assertEquals(Arrays.asList("ab1*z", "ab2*z", "arz"), expand("a{b{1..2}'*',r}z"));

    }

    @Test
    public void testJoinSplit() throws Exception {
        vars.put("array", Arrays.asList("a", "b", "c"));
        vars.put("string", "a\n\nb\nc");
        vars.put("str", "such a bad bad trip");

        assertEquals("a:b:c", expand("${(j.:.)array}"));
        assertEquals("a\nb\nc", expand("${(pj.\\n.)array}"));
        assertEquals("a\nb\nc", expand("${(F)array}"));

        assertEquals(Arrays.asList("a", "b", "c"), expand("${(f)string}"));
        assertEquals(Arrays.asList("a\n\n", "\nc"), expand("${(s:b:)string}"));

        assertEquals("a:b:c", expand("${(Fj':')array}"));

        assertEquals("a bad such trip", expand("${(j' ')${(s' 'uo)str}}"));
    }

    @Test
    public void testParamFlag() throws Exception {
        vars.put("foo", "bar");
        vars.put("bar", "baz");

        assertEquals("bar", expand("${${foo}}"));
        assertEquals("baz", expand("${(P)foo}"));
        assertEquals("baz", expand("${(P)${foo}}"));
    }

    @Test
    public void testCaseFlags() throws Exception {
        vars.put("foo", "bAr");

        assertEquals("bAr", expand("${foo}"));
        assertEquals("bar", expand("${(L)foo}"));
        assertEquals("BAR", expand("${(U)${foo}}"));
        assertEquals("Bar", expand("${(C)${foo}}"));
    }

    @Test
    public void testQuotes() throws Exception {
        vars.put("foo", "\"{a'}\\b");
        vars.put("q1", "\"foo\"");

        assertEquals("\\\"\\{a\\'\\}\\\\b", expand("${(q)foo}"));
        assertEquals("'\"{a'\\''}\\b'", expand("${(qq)foo}"));
        assertEquals("\"\\\"{a'}\\\\b\"", expand("${(qqq)foo}"));
        assertEquals("$'\"{a\\'}\\b'", expand("${(qqqq)foo}"));
        assertEquals("'\"{a'\\''}\\b'", expand("${(q-)foo}"));

        assertEquals("foo", expand("${(Q)q1}"));
    }

    @Test
    public void testChars() throws Exception {
        List<Integer> array = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            array.add(i);
        }
        vars.put("array", array);

        assertEquals(Arrays.asList("^@", "^A", "^B", "^C", "^D", "^E", "^F", "^G",
                "^H", "^I", "^J", "^K", "^L", "^M", "^N", "^O",
                "^P", "^Q", "^R", "^S", "^T", "^U", "^V", "^W",
                "^X", "^Y", "^Z", "^\\[", "^\\\\", "^\\]", "^^", "^_",
                "\\ ", "\\!", "\\\"", "\\#", "\\$", "\\%", "\\&", "\\'",
                "\\(", "\\)", "\\*", "+", ",", "-", ".", "/",
                "0", "1", "2", "3", "4", "5", "6", "7",
                "8", "9", ":", "\\;", "\\<", "\\=", "\\>", "\\?"), expand("${(qV#)array}"));
    }

    @Test
    public void testSorting() throws Exception {
        vars.put("array", Arrays.asList("foo1", "foo02", "foo2", "fOo3", "Foo20", "foo23"));

        assertEquals(Arrays.asList("Foo20", "fOo3", "foo02", "foo1", "foo2", "foo23"), expand("${(o)array}"));
        assertEquals(Arrays.asList("foo23", "foo2", "foo1", "foo02", "fOo3", "Foo20"), expand("${(O)array}"));
        assertEquals(Arrays.asList("foo02", "foo1", "foo2", "Foo20", "foo23", "fOo3"), expand("${(oi)array}"));
        assertEquals(Arrays.asList("fOo3", "foo23", "Foo20", "foo2", "foo1", "foo02"), expand("${(Oi)array}"));
        assertEquals(Arrays.asList("foo1", "foo02", "foo2", "fOo3", "Foo20", "foo23"), expand("${(oa)array}"));
        assertEquals(Arrays.asList("foo23", "Foo20", "fOo3", "foo2", "foo02", "foo1"), expand("${(Oa)array}"));
        assertEquals(Arrays.asList("foo1", "foo02", "foo2", "fOo3", "Foo20", "foo23"), expand("${(oia)array}"));
        assertEquals(Arrays.asList("foo23", "Foo20", "fOo3", "foo2", "foo02", "foo1"), expand("${(Oia)array}"));
        assertEquals(Arrays.asList("Foo20", "fOo3", "foo1", "foo02", "foo2", "foo23"), expand("${(on)array}"));
        assertEquals(Arrays.asList("foo23", "foo2", "foo02", "foo1", "fOo3", "Foo20"), expand("${(On)array}"));
        assertEquals(Arrays.asList("foo1", "foo02", "foo2", "fOo3", "Foo20", "foo23"), expand("${(oin)array}"));
        assertEquals(Arrays.asList("foo23", "Foo20", "fOo3", "foo2", "foo02", "foo1"), expand("${(Oin)array}"));
    }

    @Test
    public void testPatterns() throws Exception {
        vars.put("foo", "twinkle twinkle little star");
        vars.put("sub", "t*e");
        vars.put("sb", "*e");
        vars.put("rep", "spy");

        assertEquals("spynkle spynkle little star", expand("${(G)foo/'twi'/$rep}"));

        assertEquals("twinkle twinkle little star", expand("${foo/${sub}/${rep}}"));
        assertEquals("spy twinkle little star", expand("${foo/${~sub}/${rep}}"));
        assertEquals("spy star", expand("${foo//${~sub}/${rep}}"));
        assertEquals("spy spy lispy star", expand("${(G)foo/${~sub}/${rep}}"));
        assertEquals("spy star", expand("${(G)foo//${~sub}/${rep}}"));
        assertEquals("spy twinkle little star", expand("${foo/t${~sb}/${rep}}"));
    }

    @Test
    public void testExpand() throws Exception
    {
        final URI home = new URI("/home/derek");
        final File pwd = new File("/tmp");
        final String user = "derek";

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
//CHANGE        assertEquals("hello", expand("he\\\nllo"));
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
        assertEquals("pound\u00a3cent\u00a2", expand("$'pound\\u00a3cent\\u00a2'"));
        assertEquals("euro\\u20ac", expand("$'euro\\\\u20ac'"));
        assertEquals("euro\u20ac", expand("$'euro\\u20ac'"));
        assertEquals("euro\u020a", expand("$'euro\\u20a'"));
        assertEquals("euro\u020ag", expand("$'euro\\u20ag'"));

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
//CHANGE        assertEquals(user, expand("${US\\u0045R:?}"));

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
