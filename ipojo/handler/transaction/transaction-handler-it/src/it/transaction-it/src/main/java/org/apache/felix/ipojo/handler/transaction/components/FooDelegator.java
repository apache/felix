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

package org.apache.felix.ipojo.handler.transaction.components;

import org.apache.felix.ipojo.handler.transaction.services.CheckService;
import org.apache.felix.ipojo.handler.transaction.services.Foo;

import javax.transaction.Transaction;

public class FooDelegator implements CheckService {

    Transaction transaction;
    Transaction lastCommitted;
    Transaction lastRolledback;
    Transaction current;
    private Foo foo;
    private int commit;
    private int rollback;

    public void onCommit(String method) {
        commit++;
    }

    public void onRollback(String method, Exception e) {
        rollback++;
    }

    public void doSomethingBad() throws NullPointerException {
        current = transaction;
        foo.doSomethingBad();
    }

    public void doSomethingBad2() throws UnsupportedOperationException {
        current = transaction;
        foo.doSomethingBad2();

    }

    public void doSomethingGood() {
        current = transaction;
        foo.doSomethingGood();
    }

    public void doSomethingLong() {
        current = transaction;
        foo.doSomethingLong();
    }

    public Transaction getCurrentTransaction() {
        return current;
    }

    public int getNumberOfCommit() {
        return commit;
    }

    public int getNumberOfRollback() {
        return rollback;
    }

    public Transaction getLastRolledBack() {
        return lastRolledback;
    }

    public Transaction getLastCommitted() {
        return lastCommitted;
    }

    public void onRollback(Transaction t) {
        lastRolledback = t;
        rollback++;
    }

    public void onCommit(Transaction t) {
        lastCommitted = t;
        commit++;
    }

}
