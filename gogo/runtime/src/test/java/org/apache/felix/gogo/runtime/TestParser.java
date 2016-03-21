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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.felix.gogo.api.Process;
import org.apache.felix.gogo.runtime.Parser.Pipeline;
import org.apache.felix.gogo.runtime.Parser.Program;
import org.apache.felix.gogo.runtime.Parser.Sequence;
import org.apache.felix.gogo.runtime.Parser.Statement;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestParser extends AbstractParserTest
{
    int beentheredonethat = 0;

    @Test
    public void testEvaluatation() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("capture", this);

        assertEquals("a", c.execute("echo a | capture"));
        assertEquals("a", c.execute("(echo a) | capture"));
        assertEquals("a", c.execute("((echo a)) | capture"));
    }

    @Test
    public void testUnknownCommand() throws Exception
    {
        Context c = new Context();
        try
        {
            c.execute("echo");
            fail("Execution should have failed due to missing command");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    @Test
    public void testSpecialValues() throws Exception
    {
        Context c = new Context();
        assertEquals(false, c.execute("false"));
        assertEquals(true, c.execute("true"));
        assertEquals(null, c.execute("null"));
    }

    @Test
    public void testQuotes() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.set("c", "a");

        assertEquals("a b", c.execute("echo a b"));
        assertEquals("a b", c.execute("echo 'a b'"));
        assertEquals("a b", c.execute("echo \"a b\""));
        assertEquals("a b", c.execute("echo a  b"));
        assertEquals("a  b", c.execute("echo 'a  b'"));
        assertEquals("a  b", c.execute("echo \"a  b\""));
        assertEquals("a b", c.execute("echo $c  b"));
        assertEquals("$c  b", c.execute("echo '$c  b'"));
        assertEquals("a  b", c.execute("echo \"$c  b\""));
        assertEquals("a b", c.execute("echo ${c}  b"));
        assertEquals("${c}  b", c.execute("echo '${c}  b'"));
        assertEquals("a  b", c.execute("echo \"${c}  b\""));
        assertEquals("aa", c.execute("echo $c$c"));
        assertEquals("a ;a", c.execute("echo a\\ \\;a"));
        assertEquals("baabab", c.execute("echo b${c}${c}b${c}b"));

        c.set("d", "a  b ");
        assertEquals("a  b ", c.execute("echo \"$d\""));
    }

    @Test
    public void testScope() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        assertEquals("$a", c.execute("test:echo \\$a"));
        assertEquals("file://poo", c.execute("test:echo file://poo"));
    }

    @Test
    public void testPipe() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("capture", this);
        c.addCommand("grep", this);
        c.addCommand("echoout", this);
        c.execute("myecho = { echoout $args }");

        // Disable file name generation to avoid escaping 'd.*'
        c.currentDir(null);

        assertEquals("def", c.execute("echo def|grep d.*|capture"));
        assertEquals("def", c.execute("echoout def|grep d.*|capture"));
        assertEquals("def", c.execute("myecho def|grep d.*|capture"));
        assertEquals("def", c.execute("(echoout abc; echoout def; echoout ghi)|grep d.*|capture"));
        assertEquals("", c.execute("echoout def; echoout ghi | grep d.* | capture"));
        assertEquals("hello world", c.execute("echo hello world|capture"));
        assertEquals("defghi", c.execute("(echoout abc; echoout def; echoout ghi)|grep 'def|ghi'|capture"));
    }

    @Test
    public void testAssignment() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("grep", this);
        assertEquals("a", c.execute("a = a; echo ${$a}"));

        assertEquals("hello", c.execute("echo hello"));
        assertEquals("hello", c.execute("a = (echo hello)"));
      //assertEquals("a", c.execute("a = a; echo $(echo a)")); // #p2 - no eval in var expansion
        assertEquals("3", c.execute("a=3; echo $a"));
        assertEquals("3", c.execute("a = 3; echo $a"));
        assertEquals("a", c.execute("a = a; echo ${$a}"));
    }

    @Test
    public void testComment() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        assertEquals("1", c.execute("echo 1 // hello"));
    }

    @Test
    public void testClosure() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("capture", this);

        assertEquals("a", c.execute("e = { echo $1 } ; e a   b"));
        assertEquals("b", c.execute("e = { echo $2 } ; e a   b"));
        assertEquals("b", c.execute("e = { eval $args } ; e echo  b"));
        assertEquals("ca b", c.execute("e = { echo c$args } ; e a  b"));
        assertEquals("c a b", c.execute("e = { echo c $args } ; e a  b"));
        assertEquals("ca  b", c.execute("e = { echo c$args } ; e 'a  b'"));
    }

    @Test
    public void testArray() throws Exception
    {
        Context c = new Context();
        c.set("echo", true);
        assertEquals("http://www.aqute.biz?com=2&biz=1",
            c.execute("['http://www.aqute.biz?com=2&biz=1'] get 0"));
        assertEquals("{a=2, b=3}", c.execute("[a=2 b=3]").toString());
        assertEquals(3L, c.execute("[a=2 b=3] get b"));
        assertEquals("[3, 4]", c.execute("[1 2 [3 4] 5 6] get 2").toString());
        assertEquals(5, c.execute("[1 2 [3 4] 5 6] size"));
    }

    @Test
    public void testParentheses()
    {
        Parser parser = new Parser("(a|b)|(d|f)");
        Program p = parser.program();
        assertEquals("a|b", ((Sequence) ((Statement) ((Pipeline) p.tokens().get(0)).tokens().get(0)).tokens().get(0)).program().toString());

        parser = new Parser("grep (d.*)|grep (d|f)");
        p = parser.program();
        assertEquals("d.*", ((Sequence)((Statement) ((Pipeline) p.tokens().get(0)).tokens().get(0)).tokens().get(1)).program().toString());
    }

    @Test
    public void testEcho() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.execute("echo peter");
    }

    public void grep(String match) throws IOException
    {
        Pattern p = Pattern.compile(match);
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        String s = rdr.readLine();
        while (s != null)
        {
            if (p.matcher(s).find())
            {
                System.out.println(s);
            }
            s = rdr.readLine();
        }
    }

    public String capture() throws IOException
    {
        StringWriter sw = new StringWriter();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        String s = rdr.readLine();
        while (s != null)
        {
            sw.write(s);
            s = rdr.readLine();
        }
        return sw.toString();
    }

    @Test
    public void testVars() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);

        assertEquals("", c.execute("echo ${very.likely.that.this.does.not.exist}"));
        assertNotNull(c.execute("echo ${java.shell.name}"));
        assertEquals("a", c.execute("a = a; echo ${a}"));
    }

    @Test
    public void testFunny() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        assertEquals("a", c.execute("echo a") + "");
        assertEquals("a", c.execute("eval (echo echo) a") + "");
        //assertEquals("a", c.execute("((echo echo) echo) (echo a)") + "");
        assertEquals("3", c.execute("[a=2 (echo b)=(echo 3)] get b").toString());
    }

    public CharSequence echo(Object args[])
    {
        if (args == null)
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Object arg : args)
        {
            if (arg != null)
            {
                if (sb.length() > 0)
                    sb.append(' ');
                sb.append(arg);
            }
        }
        return sb.toString();
    }

    public void echoout(Object args[])
    {
        System.out.println(echo(args));
    }

    @Test
    public void testContext() throws Exception
    {
        Context c = new Context();
        c.addCommand("ls", this);
        beentheredonethat = 0;
        c.execute("ls");
        assertEquals(1, beentheredonethat);

        beentheredonethat = 0;
        c.execute("ls 10");
        assertEquals(10, beentheredonethat);

        beentheredonethat = 0;
        c.execute("ls a b c d e f g h i j");
        assertEquals(10, beentheredonethat);

        beentheredonethat = 0;
        Integer result = (Integer) c.execute("ls (ls 5)");
        assertEquals(10, beentheredonethat);
        assertEquals((Integer) 5, result);
    }

    public void ls()
    {
        beentheredonethat++;
        System.out.println("ls(): Yes!");
    }

    public int ls(int onoff)
    {
        beentheredonethat += onoff;
        System.out.println("ls(int) " + onoff);
        return onoff;
    }

    public void ls(Object args[])
    {
        beentheredonethat = args.length;
        System.out.print("ls(Object[]) [");
        for (Object i : args)
        {
            System.out.print(i + " ");
        }
        System.out.println("]");
    }

    @Test
    public void testProgram()
    {
        Program x = new Parser("abc def|ghi jkl;mno pqr|stu vwx").program();
        Pipeline p0 = (Pipeline) x.tokens().get(0);
        Statement s00 = (Statement) p0.tokens().get(0);
        assertEquals("|", p0.tokens().get(1).toString());
        Statement s01 = (Statement) p0.tokens().get(2);
        assertEquals(";", x.tokens().get(1).toString());
        Pipeline p1 = (Pipeline) x.tokens().get(2);
        Statement s10 = (Statement) p1.tokens().get(0);
        assertEquals("|", p1.tokens().get(1).toString());
        Statement s11 = (Statement) p1.tokens().get(2);
        assertEquals("abc", s00.tokens().get(0).toString());
        assertEquals("def", s00.tokens().get(1).toString());
        assertEquals("ghi", s01.tokens().get(0).toString());
        assertEquals("jkl", s01.tokens().get(1).toString());
        assertEquals("mno", s10.tokens().get(0).toString());
        assertEquals("pqr", s10.tokens().get(1).toString());
        assertEquals("stu", s11.tokens().get(0).toString());
        assertEquals("vwx", s11.tokens().get(1).toString());
    }

    @Test
    public void testStatements()
    {
        Program x = new Parser("abc def|ghi jkl|mno pqr").program();
        Pipeline p0 = (Pipeline) x.tokens().get(0);
        Statement s00 = (Statement) p0.tokens().get(0);
        Statement s01 = (Statement) p0.tokens().get(2);
        Statement s02 = (Statement) p0.tokens().get(4);
        assertEquals("abc", s00.tokens().get(0).toString());
        assertEquals("def", s00.tokens().get(1).toString());
        assertEquals("ghi", s01.tokens().get(0).toString());
        assertEquals("jkl", s01.tokens().get(1).toString());
        assertEquals("mno", s02.tokens().get(0).toString());
        assertEquals("pqr", s02.tokens().get(1).toString());
    }

    @Test
    public void testPipeRedir()
    {
        Program x = new Parser("abc def|&ghi").program();
        Pipeline p0 = (Pipeline) x.tokens().get(0);
        Statement s00 = (Statement) p0.tokens().get(0);
        assertEquals("|&", p0.tokens().get(1).toString());
        Statement s01 = (Statement) p0.tokens().get(2);
        assertEquals("abc", s00.tokens().get(0).toString());
        assertEquals("def", s00.tokens().get(1).toString());
        assertEquals("ghi", s01.tokens().get(0).toString());
    }

    @Test
    public void testPipeAndOr()
    {
        Program x = new Parser("abc|def&&ghi || jkl").program();
        Pipeline p0 = (Pipeline) x.tokens().get(0);
        Statement s00 = (Statement) p0.tokens().get(0);
        assertEquals("|", p0.tokens().get(1).toString());
        Statement s01 = (Statement) p0.tokens().get(2);
        assertEquals("&&", x.tokens().get(1).toString());
        Statement s1 = (Statement) x.tokens().get(2);
        assertEquals("||", x.tokens().get(3).toString());
        Statement s2 = (Statement) x.tokens().get(4);
        assertEquals("abc", s00.tokens().get(0).toString());
        assertEquals("def", s01.tokens().get(0).toString());
        assertEquals("ghi", s1.tokens().get(0).toString());
        assertEquals("jkl", s2.tokens().get(0).toString());
    }

    @Test
    public void testBackground() {
        Program x = new Parser("echo foo&echo bar").program();
        Statement s0 = (Statement) x.tokens().get(0);
        assertEquals("&", x.tokens().get(1).toString());
        Statement s1 = (Statement) x.tokens().get(2);
        assertEquals("echo", s0.tokens().get(0).toString());
        assertEquals("foo", s0.tokens().get(1).toString());
        assertEquals("echo", s1.tokens().get(0).toString());
        assertEquals("bar", s1.tokens().get(1).toString());
    }

    @Test
    public void testRedir() {
        Program x = new Parser("echo foo&>bar").program();
        Statement s0 = (Statement) x.tokens().get(0);
        assertEquals("echo", s0.tokens().get(0).toString());
        assertEquals("foo", s0.tokens().get(1).toString());
        assertEquals("&>", s0.redirections().get(0).toString());
        assertEquals("bar", s0.redirections().get(1).toString());

        x = new Parser("echo foo1>bar").program();
        s0 = (Statement) x.tokens().get(0);
        assertEquals("echo", s0.tokens().get(0).toString());
        assertEquals("foo1", s0.tokens().get(1).toString());
        assertEquals(">", s0.redirections().get(0).toString());
        assertEquals("bar", s0.redirections().get(1).toString());

        x = new Parser("echo foo 1>bar").program();
        s0 = (Statement) x.tokens().get(0);
        assertEquals("echo", s0.tokens().get(0).toString());
        assertEquals("foo", s0.tokens().get(1).toString());
        assertEquals("1>", s0.redirections().get(0).toString());
        assertEquals("bar", s0.redirections().get(1).toString());
    }

    @Test
    public void testSimpleValue()
    {
        Program p = new Parser(
            "abc def.ghi http://www.osgi.org?abc=\\&x=1 [1,2,3] {{{{{{{xyz}}}}}}} (immediate) {'{{{{{'} {\\{} 'abc{}'")
            .program();
        List<Token> x = ((Statement) p.tokens().get(0)).tokens();
        assertEquals("abc", x.get(0).toString());
        assertEquals("def.ghi", x.get(1).toString());
        assertEquals("http://www.osgi.org?abc=\\&x=1", x.get(2).toString());
        assertEquals("[1,2,3]", x.get(3).toString());
        assertEquals("{{{{{{{xyz}}}}}}}", x.get(4).toString());
        assertEquals("(immediate)", x.get(5).toString());
        assertEquals("{'{{{{{'}", x.get(6).toString());
        assertEquals("{\\{}", x.get(7).toString());
        assertEquals("'abc{}'", x.get(8).toString());
    }

    @Test
    public void testIsTty() throws Exception
    {
        Context c = new Context();
        c.addCommand("istty", this);
        c.addCommand("echo", this);
        assertEquals(true, c.execute("istty 1"));
        assertEquals(false, c.execute("$(istty 1)"));
    }

    public boolean istty(CommandSession session, int fd)
    {
        return Process.current().isTty(fd);
    }

    void each(CommandSession session, Collection<Object> list, Function closure)
        throws Exception
    {
        List<Object> args = new ArrayList<Object>();
        args.add(null);
        for (Object x : list)
        {
            args.set(0, x);
            closure.execute(session, args);
        }
    }

}
