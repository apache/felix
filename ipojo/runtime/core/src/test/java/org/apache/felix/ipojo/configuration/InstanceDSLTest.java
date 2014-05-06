/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.configuration;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.extender.internal.processor.ConfigurationProcessor;

import static org.apache.felix.ipojo.configuration.Instance.*;

/**
 * Check the instance DSL.
 */
public class InstanceDSLTest extends TestCase {


    public void testInstance() {
        instance().of("my.factory")
                .with("simple").setto("simple");

        instance()
                .of("my.factory")
                .with("simple").setto("simple")

                .with("list").setto(list(1, 2, 3))
                .with("list2").setto(list().with(1).with(2).with(3))

                .with("map").setto(map().with(pair("entry", "value")))
                .with("map").setto(map().with(pair("entry2", list("aaa", "bbb"))))
                .with("map").setto(map().with(pair("key", 1), pair("key2", 2)))
                .with("map").setto(map().with(entry("key", 1), entry("key2", 2)));
    }

    public void testClassnameExtraction() {
        String cn = ConfigurationProcessor.getClassNameFromResource("/org/apache/felix/ipojo/Pojo.class");
        Assert.assertEquals(cn, "org.apache.felix.ipojo.Pojo");
    }

    /**
     * Test for FELIX-4490.
     */
    public void testInstanceNameNotNullOrEmpty() {
        try {
            instance().named(null);
            Assert.fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // OK
        }

        try {
            instance().named("");
            Assert.fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // OK
        }

        try {
            instance().nameIfUnnamed(null);
            Assert.fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // OK
        }

        try {
            instance().nameIfUnnamed("");
            Assert.fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // OK
        }

        try {
            instance().with("instance.name").setto(null);
            Assert.fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // OK
        }

        try {
            instance().with("instance.name").setto("");
            Assert.fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}
