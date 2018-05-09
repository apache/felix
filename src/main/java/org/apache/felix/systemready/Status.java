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
package org.apache.felix.systemready;

import static java.util.stream.Collectors.minBy;

import java.util.stream.Stream;

public class Status {
    // Be aware that the order of the enum declarations matters for the Comparator
    public enum State { RED, YELLOW, GREEN;
        /**
         * returns {{GREEN}} for {{true}} and {{YELLOW}} for {{false}}
         */
        public static State fromBoolean(boolean ready) {
            return (ready) ? State.GREEN : State.YELLOW;
        }
        
        public static State worstOf(Stream<State> states) {
            return states.collect(minBy(State::compareTo)).orElse(State.GREEN);
        }
    }
    
    private State state;

    private String details;
    
    public Status(State state, String details) {
        this.state = state;
        this.details = details;
    }
    
    public State getState() {
        return state;
    }
    
    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "CheckStatus{" +
                "state=" + state +
                ", details='" + details + '\'' +
                '}';
    }
    
    
}
