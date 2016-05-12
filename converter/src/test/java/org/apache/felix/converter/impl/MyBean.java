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
package org.apache.felix.converter.impl;

public class MyBean {
    String me;
    boolean enabled;
    Boolean f;
    int[] numbers;

    public String get() {
        return "Not a bean accessor because no camel casing";
    }
    public String gettisburgh() {
        return "Not a bean accessor because no camel casing";
    }
    public int issue() {
        return -1; // not a bean accessor as no camel casing
    }
    public void sets(String s) {
        throw new RuntimeException("Not a bean accessor because no camel casing");
    }
    public String getMe() {
        return me;
    }
    public void setMe(String me) {
        this.me = me;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public Boolean getF() {
        return f;
    }
    public void setF(Boolean f) {
        this.f = f;
    }
    public int[] getNumbers() {
        return numbers;
    }
    public void setNumbers(int[] numbers) {
        this.numbers = numbers;
    }
}
