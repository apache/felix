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
package org.apache.felix.cm.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurationHandlerTest {
    
    private static final String SERVICE_PID = "service.pid";

    private static final String PAR_1 = "mongouri";
    private static final String VAL_1 = "127.0.0.1:27017";
    private static final String PAR_2 = "customBlobStore";
    private static final String VAL_2 = "true";

    private static final String CONFIG =
        "#mongodb URI\n" +
        PAR_1 + "=\"" + VAL_1 + "\"\n" +
        "\n" +
        "  # custom datastore\n" +
        PAR_2 + "=B\"" + VAL_2 + "\"\n";

    @Test
    public void testComments() throws IOException
    {
        final Dictionary dict = ConfigurationHandler.read(new ByteArrayInputStream(CONFIG.getBytes("UTF-8")));
        Assert.assertEquals(2, dict.size());
        Assert.assertEquals(VAL_1, dict.get(PAR_1));
        Assert.assertEquals(VAL_2, dict.get(PAR_2).toString());
    }
 
    
    @Test
    public void test_writeArray() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Object> properties = new Hashtable< String, Object>();
        properties.put(SERVICE_PID , new String [] {"foo", "bar"});
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=[ \\\r\n  \"foo\", \\\r\n  \"bar\", \\\r\n  ]\r\n", entry);
    }
    
    @Test
    public void test_writeEmptyCollection() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Object> properties = new Hashtable< String, Object>();
        properties.put(SERVICE_PID , new ArrayList());
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=( \\\r\n)\r\n", entry);
    }
    
    @Test
    public void test_writeCollection() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Object> properties = new Hashtable< String, Object>();
        List list = new ArrayList<String>(){{
            add("foo");
            add("bar");
        }};
        
        properties.put(SERVICE_PID , list);
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=( \\\r\n  \"foo\", \\\r\n  \"bar\", \\\r\n)\r\n", entry);
    }
    
    @Test
    public void test_writeSimpleString() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, String> properties = new Hashtable< String, String>();
        properties.put(SERVICE_PID, "com.adobe.granite.foo.Bar");
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=\"com.adobe.granite.foo.Bar\"\r\n", entry);
    }
    
    @Test
    public void test_writeInteger() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Integer> properties = new Hashtable< String, Integer>();
        properties.put(SERVICE_PID, 1000);
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=I\"1000\"\r\n", entry);
    }
    
    @Test
    public void test_writeLong() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Long> properties = new Hashtable< String, Long>();
        properties.put(SERVICE_PID, 1000L);
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=L\"1000\"\r\n", entry);
    }
    
    @Test
    public void test_writeFloat() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Float> properties = new Hashtable< String, Float>();
        properties.put(SERVICE_PID, 3.6f);
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=F\"1080452710\"\r\n", entry);
    }
    
    @Test
    public void test_writeDouble() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Double> properties = new Hashtable< String, Double>();
        properties.put(SERVICE_PID, 3.6d);
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=D\"4615288898129284301\"\r\n", entry);
    }
    
    @Test
    public void test_writeByte() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Byte> properties = new Hashtable< String, Byte>();
        properties.put(SERVICE_PID, new Byte("10"));
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=X\"10\"\r\n", entry);
    }
    
    @Test
    public void test_writeShort() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Short> properties = new Hashtable< String, Short>();
        properties.put(SERVICE_PID, (short)10);
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=S\"10\"\r\n", entry);
    }
    
    @Test
    public void test_writeChar() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Character> properties = new Hashtable< String, Character>();
        properties.put(SERVICE_PID, 'c');
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=C\"c\"\r\n", entry);
    }
    
    @Test
    public void test_writeBoolean() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, Boolean> properties = new Hashtable< String, Boolean>();
        properties.put(SERVICE_PID, true);
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("service.pid=B\"true\"\r\n", entry);
    }
    
    @Test
    public void test_writeSimpleStringWithError() throws IOException {
        OutputStream out = new ByteArrayOutputStream();
        Dictionary< String, String> properties = new Hashtable< String, String>();
        properties.put("foo.bar", "com.adobe.granite.foo.Bar");
        ConfigurationHandler.write(out, properties);
        String entry = new String(((ByteArrayOutputStream)out).toByteArray(),"UTF-8");
        Assert.assertEquals("foo.bar=\"com.adobe.granite.foo.Bar\"\r\n", entry);
    }
       
    @Test
    public void test_readArray() throws IOException {
        String entry = "service.pid=[ \\\r\n  \"foo\", \\\r\n  \"bar\", \\\r\n  ]\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertArrayEquals(new String [] {"foo", "bar"}, (String [])dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readEmptyCollection() throws IOException {
        String entry = "service.pid=( \\\r\n)\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals(new ArrayList(), (ArrayList)dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readCollection() throws IOException {
        String entry = "service.pid=( \\\r\n  \"foo\", \\\r\n  \"bar\", \\\r\n)\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        List list = new ArrayList<String>(){{
            add("foo");
            add("bar");
        }};
        Assert.assertEquals(list, (ArrayList)dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readSimpleString() throws IOException {
        String entry = "service.pid=\"com.adobe.granite.foo.Bar\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals( "com.adobe.granite.foo.Bar", dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readSimpleStrings() throws IOException {
        String entry = "service.pid=\"com.adobe.granite.foo.Bar\"\r\nfoo.bar=\"com.adobe.granite.foo.Baz\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(2, dictionary.size());
        Assert.assertEquals( "com.adobe.granite.foo.Bar", dictionary.get(SERVICE_PID));
        Assert.assertNotNull(dictionary.get("foo.bar"));
    }
    
    @Test
    public void test_readInteger() throws IOException {
        String entry = "service.pid=I\"1000\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals( 1000, dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readLong() throws IOException {
        String entry = "service.pid=L\"1000\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals( 1000L, dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readFloat() throws IOException {
        String entry = "service.pid=F\"1080452710\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals( 3.6f, dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readDouble() throws IOException {
        String entry = "service.pid=D\"4615288898129284301\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals( 3.6d, dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readByte() throws IOException {
        String entry = "service.pid=X\"10\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals((byte)10 , dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readShort() throws IOException {
        String entry = "service.pid=S\"10\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals((short)10 , dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readChar() throws IOException {
        String entry = "service.pid=C\"c\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals('c' , dictionary.get(SERVICE_PID));
    }
    
    @Test
    public void test_readBoolean() throws IOException {
        String entry = "service.pid=B\"true\"\r\n";
        InputStream stream = new ByteArrayInputStream(entry.getBytes(StandardCharsets.UTF_8));
        Dictionary dictionary = ConfigurationHandler.read(stream);
        Assert.assertEquals(1, dictionary.size());
        Assert.assertEquals(true , dictionary.get(SERVICE_PID));
    }
}
  
