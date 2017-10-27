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
package org.apache.felix.bundleplugin;

import aQute.bnd.osgi.AbstractResource;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JpaPluginTest {

    @Test
    public void testNamedJndi() throws Exception {
        String xmlStr = "<persistence xmlns='http://java.sun.com/xml/ns/persistence'>\n" +
                        "    <persistence-unit name='the-unit' transaction-type='JTA'>\n" +
                        "        <jta-data-source>osgi:service/jdbc/h2DS</jta-data-source>\n" +
                        "    </persistence-unit>\n" +
                        "</persistence>";
        String expectedReqs = "osgi.extender;osgi.extender=aries.jpa,osgi.service;effective:=active;objectClass=javax.transaction.TransactionManager;";
        assertTransformation(xmlStr, expectedReqs);
    }

    @Test
    public void testService() throws Exception {
        String xmlStr = "<persistence xmlns='http://java.sun.com/xml/ns/persistence'>\n" +
                        "    <persistence-unit name='the-unit' transaction-type='JTA'>\n" +
                        "        <jta-data-source>osgi:service/javax.sql.DataSource/(&amp;(db=mydb)(version=3.1))</jta-data-source>\n" +
                        "    </persistence-unit>\n" +
                        "</persistence>";
        String expectedReqs = "osgi.extender;osgi.extender=aries.jpa,osgi.service;effective:=active;objectClass=javax.sql.DataSource;filter:=\"(&(db=mydb)(version=3.1))\",osgi.service;effective:=active;objectClass=javax.transaction.TransactionManager;";
        assertTransformation(xmlStr, expectedReqs);
    }

    private void assertTransformation(final String xmlStr, String expectedReqs) throws Exception {
        Analyzer analyzer = new Analyzer();
        Jar jar = new Jar("the-jar");
        Resource xml = new AbstractResource(0) {
            @Override
            protected byte[] getBytes() throws Exception {
                return xmlStr.getBytes();
            }
        };
        JpaPlugin plugin = new JpaPlugin();

        jar.putResource("the-persistence-xml", xml);
        analyzer.setJar(jar);
        analyzer.setProperty("Meta-Persistence", "the-persistence-xml");

        plugin.analyzeJar(analyzer);

        assertEquals(expectedReqs, analyzer.getProperty("Require-Capability"));
    }
}
