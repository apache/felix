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
package org.apache.felix.httplite.osgi;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * Represents either a Resource or a Servlet that should be processed 
 * with a valid client connection provided by the server.
 *
 */
public interface ServiceRegistrationHandler
{
    /**
     * Process the request against the registered provider.  
     * 
     * @param closeConnection if true close connection after handling request
     * @throws IOException on I/O error
     * @throws ServletException on Servlet error
     */
    void handle(boolean closeConnection) throws IOException, ServletException;
}
