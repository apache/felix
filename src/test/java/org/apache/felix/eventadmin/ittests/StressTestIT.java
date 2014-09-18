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
package org.apache.felix.eventadmin.ittests;

import org.junit.Test;


public class StressTestIT extends AbstractTest {

    private static final String PREFIX = "org/apache/felix/eventing/test";
    private static final int THREADS = 15;
    private static final int EVENTS_PER_THREAD = 10000;

    @Override
    protected void sendEvent(int index) {
        final String postFix = String.valueOf(index % 10);
        final String topic = PREFIX + '/' + postFix;
        this.send(topic, null, index, false);
    }

    @Test
    public void testEventing() throws Exception {
        this.addListener(PREFIX + "/0", null);
        this.addListener(PREFIX + "/1", null);
        this.addListener(PREFIX + "/2", null);
        this.addListener(PREFIX + "/3", null);
        this.addListener(PREFIX + "/4", null);
        this.addListener(PREFIX + "/5", null);
        this.addListener(PREFIX + "/6", null);
        this.addListener(PREFIX + "/7", null);
        this.addListener(PREFIX + "/8", null);
        this.addListener(PREFIX + "/9", null);
        this.addListener(PREFIX + "/*", null);
        this.addListener("org/apache/felix/eventing/*", null);
        this.addListener("org/apache/felix/*", null);
        this.addListener("org/apache/*", null);
        this.addListener("org/*", null);
        this.addListener("*", null);

        this.start(PREFIX, THREADS, EVENTS_PER_THREAD, THREADS * EVENTS_PER_THREAD * (1 + 6));
    }
}
