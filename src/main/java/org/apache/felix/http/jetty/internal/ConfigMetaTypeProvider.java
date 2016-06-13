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
package org.apache.felix.http.jetty.internal;

import java.io.InputStream;
import java.util.ArrayList;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

class ConfigMetaTypeProvider implements MetaTypeProvider
{

    private final Bundle bundle;

    public ConfigMetaTypeProvider(final Bundle bundle)
    {
        this.bundle = bundle;
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    @Override
    public String[] getLocales()
    {
        return null;
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String, java.lang.String)
     */
    @Override
    public ObjectClassDefinition getObjectClassDefinition( String id, String locale )
    {
        if ( !JettyService.PID.equals( id ) )
        {
            return null;
        }

        final ArrayList<AttributeDefinition> adList = new ArrayList<AttributeDefinition>();

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_HOST,
                "Host Name",
                "IP Address or Host Name of the interface to which HTTP and HTTPS bind. The default is " +
                   "\"0.0.0.0\" indicating all interfaces.",
                "0.0.0.0",
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_HOST)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_HTTP_ENABLE,
                "Enable HTTP",
                "Whether or not HTTP is enabled. Defaults to true thus HTTP enabled.",
                true,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_HTTP_ENABLE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.HTTP_PORT,
                "HTTP Port",
                "Port to listen on for HTTP requests. Defaults to 8080.",
                8080,
                bundle.getBundleContext().getProperty(JettyConfig.HTTP_PORT)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.HTTP_TIMEOUT,
                "Connection Timeout",
                "Time limit for reaching an timeout specified in milliseconds. This property applies to both HTTP and HTTP connections. Defaults to 60 seconds.",
                60000,
                bundle.getBundleContext().getProperty(JettyConfig.HTTP_TIMEOUT)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_HTTPS_ENABLE,
                "Enable HTTPS",
                "Whether or not HTTPS is enabled. Defaults to false thus HTTPS disabled.",
                false,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_HTTPS_ENABLE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.HTTPS_PORT,
                "HTTPS Port",
                "Port to listen on for HTTPS requests. Defaults to 433.",
                433,
                bundle.getBundleContext().getProperty(JettyConfig.HTTPS_PORT)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_KEYSTORE,
                "Keystore",
                "Absolute Path to the Keystore to use for HTTPS. Only used if HTTPS is enabled in which case this property is required.",
                null,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_KEYSTORE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_KEYSTORE_PASSWORD,
                "Keystore Password",
                "Password to access the Keystore. Only used if HTTPS is enabled.",
                null,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_KEYSTORE_PASSWORD)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_KEYSTORE_KEY_PASSWORD,
                "Key Password",
                "Password to unlock the secret key from the Keystore. Only used if HTTPS is enabled.",
                null,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_KEYSTORE_KEY_PASSWORD)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_TRUSTSTORE,
                "Truststore",
                "Absolute Path to the Truststore to use for HTTPS. Only used if HTTPS is enabled.",
                null,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_TRUSTSTORE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_TRUSTSTORE_PASSWORD,
                "Truststore Password",
                "Password to access the Truststore. Only used if HTTPS is enabled.",
                null,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_TRUSTSTORE_PASSWORD)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_HTTPS_CLIENT_CERT,
                "Client Certificate",
                "Requirement for the Client to provide a valid certificate. Defaults to none.",
                AttributeDefinition.STRING,
                new String[] {"none"},
                0,
                new String[] {"No Client Certificate", "Client Certificate Wanted", "Client Certificate Needed"},
                new String[] {"none", "wants", "needs"},
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_HTTPS_CLIENT_CERT)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_HTTP_CONTEXT_PATH,
                "Context Path",
                "The Servlet Context Path to use for the Http Service. If this property is not configured it " +
                    "defaults to \"/\". This must be a valid path starting with a slash and not ending with a slash (unless it is the root context).",
                "/",
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_HTTP_CONTEXT_PATH)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_HTTP_MBEANS,
                "Register MBeans",
                "Whether or not to use register JMX MBeans from the servlet container (Jetty). If this is " +
                    "enabled Jetty Request and Connector statistics are also enabled. The default is to not enable JMX.",
                false,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_HTTP_MBEANS)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_SESSION_TIMEOUT,
                "Session Timeout",
                "Default lifetime of an HTTP session specified in a whole number of minutes. If the timeout is 0 or less, sessions will by default never timeout. The default is 0.",
                0,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_SESSION_TIMEOUT)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_THREADPOOL_MAX,
                "Thread Pool Max",
                "Maximum number of jetty threads. Using the default -1 uses Jetty's default (200).",
                -1,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_THREADPOOL_MAX)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_ACCEPTORS,
                "Acceptors",
                "Number of acceptor threads to use, or -1 for a default value. Acceptors accept new TCP/IP connections. If 0, then the selector threads are used to accept connections.",
                -1,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_ACCEPTORS)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SELECTORS,
                "Selectors",
                "Number of selector threads, or <=0 for a default value. Selectors notice and schedule established connection that can make IO progress.",
                -1,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SELECTORS)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_HEADER_BUFFER_SIZE,
                "Header Buffer Size",
                "Size of the buffer for request and response headers. Default is 16KB.",
                16384,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_HEADER_BUFFER_SIZE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_REQUEST_BUFFER_SIZE,
                "Request Buffer Size",
                "Size of the buffer for requests not fitting the header buffer. Default is 8KB.",
                8192,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_REQUEST_BUFFER_SIZE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_RESPONSE_BUFFER_SIZE,
                "Response Buffer Size",
                "Size of the buffer for responses. Default is 24KB.",
                24576,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_RESPONSE_BUFFER_SIZE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_MAX_FORM_SIZE,
                "Maximum Form Size",
                "Size of Body for submitted form content. Default is 200KB.",
                204800,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_MAX_FORM_SIZE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_HTTP_PATH_EXCLUSIONS,
                "Path Exclusions",
                "Contains a list of context path prefixes. If a Web Application Bundle is started with a context path matching any " +
                    "of these prefixes, it will not be deployed in the servlet container.",
                AttributeDefinition.STRING,
                new String[] {"/system"},
                2147483647,
                null, null,
                getStringArray(bundle.getBundleContext().getProperty(JettyConfig.FELIX_HTTP_PATH_EXCLUSIONS))));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_EXCLUDED_SUITES,
                "Excluded Cipher Suites",
                "List of cipher suites that should be excluded. Default is none.",
                AttributeDefinition.STRING,
                null,
                2147483647,
                null, null,
                getStringArray(bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_EXCLUDED_SUITES))));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_INCLUDED_SUITES,
                "Included Cipher Suites",
                "List of cipher suites that should be included. Default is none.",
                AttributeDefinition.STRING,
                null,
                2147483647,
                null, null,
                getStringArray(bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_INCLUDED_SUITES))));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SEND_SERVER_HEADER,
                "Send Server Header",
                "If enabled, the server header is sent.",
                false,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SEND_SERVER_HEADER)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_INCLUDED_PROTOCOLS,
                "Included Protocols",
                "List of SSL protocols to include by default. Protocols may be any supported by the Java " +
                    "platform such as SSLv2Hello, SSLv3, TLSv1, TLSv1.1, or TLSv1.2. Any listed protocol " +
                    "not supported is silently ignored. Default is none assuming to use any protocol enabled " +
                    "and supported on the platform.",
                AttributeDefinition.STRING,
                null,
                2147483647,
                null, null,
                getStringArray(bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_INCLUDED_PROTOCOLS))));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_EXCLUDED_PROTOCOLS,
                "Excluded Protocols",
                "List of SSL protocols to exclude. This property further restricts the enabled protocols by " +
                    "explicitly disabling. Any protocol listed in both this property and the Included " +
                    "protocols property is excluded. Default is none such as to accept all protocols enabled " +
                    "on platform or explicitly listed by the Included protocols property.",
                AttributeDefinition.STRING,
                null,
                2147483647,
                null, null,
                getStringArray(bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_EXCLUDED_PROTOCOLS))));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_PROXY_LOAD_BALANCER_CONNECTION_ENABLE,
                "Enable Proxy/Load Balancer Connection",
                "Whether or not the Proxy/Load Balancer Connection is enabled. Defaults to false thus disabled.",
                false,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_PROXY_LOAD_BALANCER_CONNECTION_ENABLE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_RENEGOTIATION_ALLOWED,
                "Renegotiation allowed",
                "Whether TLS renegotiation is allowed (true by default)",
                false,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_RENEGOTIATION_ALLOWED)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SESSION_COOKIE_HTTP_ONLY,
                "Session Cookie httpOnly",
                "Session Cookie httpOnly (true by default)",
                true,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SESSION_COOKIE_HTTP_ONLY)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SESSION_COOKIE_SECURE,
                "Session Cookie secure",
                "Session Cookie secure (false by default)",
                false,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SESSION_COOKIE_SECURE)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SERVLET_SESSION_ID_PATH_PARAMETER_NAME,
                "Session Id path parameter",
                "Defaults to jsessionid. If set to null or \"none\" no URL rewriting will be done.",
                "jsessionid",
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SERVLET_SESSION_ID_PATH_PARAMETER_NAME)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SERVLET_CHECK_REMOTE_SESSION_ENCODING,
                "Check remote session encoding",
                "If true, Jetty will add JSESSIONID parameter even when encoding external urls with calls to encodeURL() (true by default)",
                true,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SERVLET_CHECK_REMOTE_SESSION_ENCODING)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SERVLET_SESSION_COOKIE_NAME,
                "Session Cookie Name",
                "Session Cookie Name",
                "JSESSIONID",
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SERVLET_SESSION_COOKIE_NAME)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SERVLET_SESSION_DOMAIN,
                "Session Domain",
                "If this property is set, then it is used as the domain for session cookies. If it is not set, then no domain is specified for the session cookie. Default is none.",
                null,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SERVLET_SESSION_DOMAIN)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SERVLET_SESSION_PATH,
                "Session Path",
                "If this property is set, then it is used as the path for the session cookie. Default is context path.",
                null,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SERVLET_SESSION_DOMAIN)));

        adList.add(new AttributeDefinitionImpl(JettyConfig.FELIX_JETTY_SERVLET_SESSION_MAX_AGE,
                "Session Max Age",
                "Max age for the session cookie. Default is -1.",
                -1,
                bundle.getBundleContext().getProperty(JettyConfig.FELIX_JETTY_SERVLET_SESSION_MAX_AGE)));

        return new ObjectClassDefinition()
        {

            private final AttributeDefinition[] attrs = adList
                .toArray(new AttributeDefinition[adList.size()]);

            @Override
            public String getName()
            {
                return "Apache Felix Jetty Based Http Service";
            }

            @Override
            public InputStream getIcon(int arg0)
            {
                return null;
            }

            @Override
            public String getID()
            {
                return JettyService.PID;
            }

            @Override
            public String getDescription()
            {
                return "Configuration for the embedded Jetty Servlet Container.";
            }

            @Override
            public AttributeDefinition[] getAttributeDefinitions(int filter)
            {
                return (filter == OPTIONAL) ? null : attrs;
            }
        };
    }

    private String [] getStringArray(final String value)
    {
        if ( value != null )
        {
            return value.trim().split(",");
        }
        return null;
    }

    private static class AttributeDefinitionImpl implements AttributeDefinition
    {

        private final String id;
        private final String name;
        private final String description;
        private final int type;
        private final String[] defaultValues;
        private final int cardinality;
        private final String[] optionLabels;
        private final String[] optionValues;


        AttributeDefinitionImpl( final String id, final String name, final String description, final String defaultValue, final String overrideValue )
        {
            this( id, name, description, STRING, defaultValue == null ? null : new String[] { defaultValue }, 0, null, null, overrideValue == null ? null : new String[] { overrideValue } );
        }

        AttributeDefinitionImpl( final String id, final String name, final String description, final int defaultValue, final String overrideValue )
        {
            this( id, name, description, INTEGER, new String[]
                { String.valueOf(defaultValue) }, 0, null, null, overrideValue == null ? null : new String[] { overrideValue } );
        }

        AttributeDefinitionImpl( final String id, final String name, final String description, final boolean defaultValue, final String overrideValue )
        {
            this( id, name, description, BOOLEAN, new String[]
                { String.valueOf(defaultValue) }, 0, null, null, overrideValue == null ? null : new String[] { overrideValue } );
        }

        AttributeDefinitionImpl( final String id, final String name, final String description, final int type,
                final String[] defaultValues, final int cardinality, final String[] optionLabels,
                final String[] optionValues,
                final String overrideValue)
        {
            this(id, name, description, type, defaultValues, cardinality, optionLabels, optionValues, overrideValue == null ? null : new String[] { overrideValue });
        }

        AttributeDefinitionImpl( final String id, final String name, final String description, final int type,
            final String[] defaultValues, final int cardinality, final String[] optionLabels,
            final String[] optionValues,
            final String[] overrideValues)
        {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            if ( overrideValues != null )
            {
               this.defaultValues = overrideValues;
            }
            else
            {
                this.defaultValues = defaultValues;
            }
            this.cardinality = cardinality;
            this.optionLabels = optionLabels;
            this.optionValues = optionValues;
        }


        @Override
        public int getCardinality()
        {
            return cardinality;
        }


        @Override
        public String[] getDefaultValue()
        {
            return defaultValues;
        }


        @Override
        public String getDescription()
        {
            return description;
        }


        @Override
        public String getID()
        {
            return id;
        }


        @Override
        public String getName()
        {
            return name;
        }


        @Override
        public String[] getOptionLabels()
        {
            return optionLabels;
        }


        @Override
        public String[] getOptionValues()
        {
            return optionValues;
        }


        @Override
        public int getType()
        {
            return type;
        }


        @Override
        public String validate( String arg0 )
        {
            return null;
        }
    }
}
