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
package org.apache.felix.gogo.runtime.threadio;

<<<<<<< HEAD
import junit.framework.TestCase;

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

<<<<<<< HEAD
public class TestThreadIO extends TestCase
{
=======
import org.junit.Assert;
import org.junit.Test;

public class TestThreadIO {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    /**
     * Test if the threadio works in a nested fashion. We first push
     * ten markers on the stack and print a message for each, capturing
     * the output in a ByteArrayOutputStream. Then we pop them, also printing
     * a message identifying the level. Then we verify the output for each level.
     */
<<<<<<< HEAD
=======
    @Test
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void testNested()
    {
        ThreadIOImpl tio = new ThreadIOImpl();
        tio.start();
<<<<<<< HEAD
        List<ByteArrayOutputStream> list = new ArrayList<ByteArrayOutputStream>();
=======
        List<ByteArrayOutputStream> list = new ArrayList<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        for (int i = 0; i < 10; i++)
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            list.add(out);
            tio.setStreams(System.in, new PrintStream(out), System.err);
            System.out.print("b" + i);
        }
        for (int i = 9; i >= 0; i--)
        {
            System.out.println("e" + i);
            tio.close();
        }
        tio.stop();
        for (int i = 0; i < 10; i++)
        {
            String message = list.get(i).toString().trim();
<<<<<<< HEAD
            assertEquals("b" + i + "e" + i, message);
=======
            Assert.assertEquals("b" + i + "e" + i, message);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    /**
     * Simple test too see if the basics work.
     */
<<<<<<< HEAD
=======
    @Test
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void testSimple()
    {
        ThreadIOImpl tio = new ThreadIOImpl();
        tio.start();
        System.out.println("Hello World");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        tio.setStreams(System.in, new PrintStream(out), new PrintStream(err));
        try
        {
            System.out.println("Simple Normal Message");
            System.err.println("Simple Error Message");
        }
        finally
        {
            tio.close();
        }
        tio.stop();
        String normal = out.toString().trim();
        //String error = err.toString().trim();
<<<<<<< HEAD
        assertEquals("Simple Normal Message", normal);
=======
        Assert.assertEquals("Simple Normal Message", normal);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        //assertEquals("Simple Error Message", error );
        System.out.println("Goodbye World");
    }
}
