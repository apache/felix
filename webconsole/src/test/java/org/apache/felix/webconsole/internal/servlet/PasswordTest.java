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
package org.apache.felix.webconsole.internal.servlet;

import org.junit.Assert;
import org.junit.Test;

public class PasswordTest {
    static final String PASSWORD_HASHED = "{sha-256}jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg=";
    
    private Password password;
    
    @Test
    public void test_matches() throws NoSuchFieldException, SecurityException {
        password = new Password("{sha-256}ffe2c845abea5d1c-1000-fn7QYN5RT4T2wM2+WJAnPHoZERW9dheoxUam1KW3oEA=");
        Assert.assertTrue(password.matches("admin".getBytes()));
        //test backward compaibility
        password = new Password("password");
        Assert.assertTrue(password.matches("password".getBytes()));
        password = new Password("{sha-256}jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg=");
        Assert.assertTrue(password.matches("admin".getBytes()));
       
    }
    
    @Test
    public void test_isPasswordHashed() {
        Assert.assertTrue(Password.isPasswordHashed(PASSWORD_HASHED));
        Assert.assertFalse(Password.isPasswordHashed("foo"));
    }
    
    @Test
    public void test_hashPassword() {
        String pwd1 = Password.hashPassword("admin");
        String pwd2 = Password.hashPassword("admin");
        Assert.assertFalse(pwd1.equals(pwd2));
    }

}
