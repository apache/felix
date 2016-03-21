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

import org.apache.felix.gogo.runtime.Pipe.Result;

public interface Job {

    enum Status {
        Created,
        Suspended,
        Background,
        Foreground,
        Done
    }

    int id();

    CharSequence command();

    Status status();

    void suspend();

    void background();

    void foreground();

    void interrupt();

    Result result();

    /**
     * Start the job.
     * If the job is started in foreground,
     * waits for the job to finish or to be
     * suspended or moved to background.
     *
     * @param status the desired job status
     * @return <code>null</code> if the job
     *   has been suspended or moved to background,
     *
     */
    Result start(Status status) throws InterruptedException;

}
