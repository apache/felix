/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The <code>ConnectorFactory</code> is a service interface which allows
 * extensions to inject custom Jetty {@code Connector} instances to add
 * to the Jetty server. Example connectors would be a SPDY connector or
 * an SSL capable connector with a custom {@code SslContextFactory}.
 * <p>
 * {@code ConnectorFactory} services are responsible for creating the
 * {@code Connector} instances and providing base configuration. Global
 * configuration such as TCP/IP timeouts or buffer sizes are handled by the
 * Jetty server launcher. Likewise the life cycle of the connectors is managed
 * by the Jetty server and its launcher.
 */
@ConsumerType
public interface ConnectorFactory
{

    /**
     * Creates new Jetty {@code Connector} instances.
     * <p>
     * The instances must be configured. The Jetty server will additionally
     * configure global configuration such as TCP/IP timeouts and buffer
     * settings.
     * <p>
     * Connectors returned from this method are not started yet. Callers must
     * add them to the Jetty server and start them.
     * <p>
     * If the {@code ConnectorFactory} service is stopped any connectors still
     * active in Jetty servers must be stopped and removed from these Jetty
     * servers.
     *
     * @return A configured Jetty {@code Connector} instance.
     */
    Connector createConnector(Server server);
}
