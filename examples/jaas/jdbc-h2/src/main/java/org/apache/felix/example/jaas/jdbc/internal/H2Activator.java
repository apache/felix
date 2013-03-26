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

package org.apache.felix.example.jaas.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.sql.DataSource;

import org.h2.Driver;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.server.web.WebServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2Activator implements BundleActivator
{
    private Logger log = LoggerFactory.getLogger(getClass());
    private JdbcDataSource ds;
    private Connection connection;

    @Override
    public void start(BundleContext context) throws Exception
    {
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("dataSourceName", "test");
        context.registerService(DataSource.class.getName(), ds, props);

        loadData(ds);

        //Register the H2 console servlet
        Dictionary<String, String> servletProps = new Hashtable<String, String>();
        servletProps.put("alias", "/h2");
        servletProps.put("init.webAllowOthers", "true");

        context.registerService(Servlet.class.getName(), new WebServlet(), servletProps);
    }

    private void loadData(JdbcDataSource ds) throws SQLException
    {
        //Load the default data of user and roles
        connection = ds.getConnection();
        Statement stmt = connection.createStatement();
        stmt.execute("CREATE TABLE USERS AS SELECT * FROM CSVREAD('classpath:users.csv',null,'lineComment=#')");
        stmt.execute("CREATE TABLE ROLES AS SELECT * FROM CSVREAD('classpath:roles.csv',null,'lineComment=#')");
        stmt.close();
        log.info("Successfully imported default user and roles");
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        if (connection != null)
        {
            Statement stat = connection.createStatement();
            stat.execute("SHUTDOWN");
            stat.close();

            try
            {
                connection.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Driver.unload();
    }
}
