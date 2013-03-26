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

package org.apache.felix.example.jaas.jdbc;

import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The code is based on org.apache.karaf.jaas.modules.jdbc.JDBCLoginModule
 */
public class JdbcLoginModule implements LoginModule {
    private static Logger log = LoggerFactory.getLogger(JdbcLoginModule.class);
    private final DataSource dataSource;
    private CallbackHandler callbackHandler;
    private Set<Principal> principals;
    private boolean detailedLoginExcepion;
    private Subject subject;
    private final String passwordQuery;
    private final String roleQuery;

    public JdbcLoginModule(DataSource dataSource, String passwordQuery, String roleQuery) {
        this.dataSource = dataSource;
        this.passwordQuery = passwordQuery;
        this.roleQuery = roleQuery;
    }


    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
        this.subject = subject;

    }

    @Override
    public boolean login() throws LoginException {
        Connection connection = null;

        PreparedStatement passwordStatement = null;
        PreparedStatement roleStatement = null;

        ResultSet passwordResultSet = null;
        ResultSet roleResultSet = null;

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage() + " not available to obtain information from user");
        }

        String user = ((NameCallback) callbacks[0]).getName();

        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }

        String password = new String(tmpPassword);
        principals = new HashSet<Principal>();

        try {
            connection = dataSource.getConnection();

            //Retrieve user credentials from database.
            passwordStatement = connection.prepareStatement(passwordQuery);
            passwordStatement.setString(1, user);
            passwordResultSet = passwordStatement.executeQuery();

            if (!passwordResultSet.next()) {
                if (!this.detailedLoginExcepion) {
                    throw new LoginException("login failed");
                } else {
                    throw new LoginException("Password for " + user + " does not match");
                }
            } else {
                String storedPassword = passwordResultSet.getString(1);

                if (!checkPassword(password, storedPassword)) {
                    if (!this.detailedLoginExcepion) {
                        throw new LoginException("login failed");
                    } else {
                        throw new LoginException("Password for " + user + " does not match");
                    }
                }
                principals.add(new UserPrincipal(user));
            }

            //Retrieve user roles from database
            roleStatement = connection.prepareStatement(roleQuery);
            roleStatement.setString(1, user);
            roleResultSet = roleStatement.executeQuery();
            while (roleResultSet.next()) {
                String role = roleResultSet.getString(1);
                principals.add(new RolePrincipal(role));
            }
        } catch (Exception ex) {
            throw new LoginException("Error has occured while retrieving credentials from database:" + ex.getMessage());
        } finally {
            try {
                if (passwordResultSet != null) {
                    passwordResultSet.close();
                }
                if (passwordStatement != null) {
                    passwordStatement.close();
                }
                if (roleResultSet != null) {
                    roleResultSet.close();
                }
                if (roleStatement != null) {
                    roleStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                log.warn("Failed to clearly close connection to the database:", ex);
            }
        }
        return true;
    }

    private boolean checkPassword(String password, String storedPassword) {
        return password.equals(storedPassword);
    }

    @Override
    public boolean commit() throws LoginException {
        subject.getPrincipals().addAll(principals);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return false;
    }
}
