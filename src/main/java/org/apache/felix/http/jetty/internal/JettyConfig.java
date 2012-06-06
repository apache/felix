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
package org.apache.felix.http.jetty.internal;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;

public final class JettyConfig
{
    /** Felix specific property to set the interface to listen on. Applies to both HTTP and HTTP */
    private static final String FELIX_HOST = "org.apache.felix.http.host";

    /** Standard OSGi port property for HTTP service */
    private static final String HTTP_PORT = "org.osgi.service.http.port";

    /** Standard OSGi port property for HTTPS service */
    private static final String HTTPS_PORT = "org.osgi.service.http.port.secure";

    /** Felix specific property to set http reaching timeout limit */
    public static final String HTTP_TIMEOUT = "org.apache.felix.http.timeout";

    /** Felix specific property to enable debug messages */
    private static final String FELIX_HTTP_DEBUG = "org.apache.felix.http.debug";
    private static final String HTTP_DEBUG = "org.apache.felix.http.jetty.debug";

    /** Felix specific property to override the keystore file location. */
    private static final String FELIX_KEYSTORE = "org.apache.felix.https.keystore";
    private static final String OSCAR_KEYSTORE = "org.ungoverned.osgi.bundle.https.keystore";

    /** Felix specific property to override the keystore password. */
    private static final String FELIX_KEYSTORE_PASSWORD = "org.apache.felix.https.keystore.password";
    private static final String OSCAR_KEYSTORE_PASSWORD = "org.ungoverned.osgi.bundle.https.password";

    /** Felix specific property to override the keystore key password. */
    private static final String FELIX_KEYSTORE_KEY_PASSWORD = "org.apache.felix.https.keystore.key.password";
    private static final String OSCAR_KEYSTORE_KEY_PASSWORD = "org.ungoverned.osgi.bundle.https.key.password";

    /** Felix specific property to control whether to enable HTTPS. */
    private static final String FELIX_HTTPS_ENABLE = "org.apache.felix.https.enable";
    private static final String OSCAR_HTTPS_ENABLE   = "org.ungoverned.osgi.bundle.https.enable";

    /** Felix specific property to control whether to enable HTTP. */
    private static final String FELIX_HTTP_ENABLE = "org.apache.felix.http.enable";

    /** Felix specific property to override the truststore file location. */
    private static final String FELIX_TRUSTSTORE = "org.apache.felix.https.truststore";

    /** Felix specific property to override the truststore password. */
    private static final String FELIX_TRUSTSTORE_PASSWORD = "org.apache.felix.https.truststore.password";

    /** Felix specific property to control whether to want or require HTTPS client certificates. Valid values are "none", "wants", "needs". Default is "none". */
    private static final String FELIX_HTTPS_CLIENT_CERT = "org.apache.felix.https.clientcertificate";

    /** Felix specific property to control whether Jetty uses NIO or not for HTTP. Valid values are "true", "false". Default is true */
    public static final String  FELIX_HTTP_NIO = "org.apache.felix.http.nio";

    /** Felix specific property to control whether Jetty uses NIO or not for HTTPS. Valid values are "true", "false". Default is the value of org.apache.felix.http.nio */
    public static final String  FELIX_HTTPS_NIO = "org.apache.felix.https.nio";

    /** Felix specific property to configure the session timeout in minutes (same session-timout in web.xml). Default is servlet container specific */
    public static final String FELIX_SESSION_TIMEOUT = "org.apache.felix.http.session.timeout";

    /** Felix speicific property to configure the request buffer size. Default is 16KB (instead of Jetty's default of 4KB) */
    public static final String FELIX_JETTY_HEADER_BUFFER_SIZE = "org.apache.felix.http.jetty.headerBufferSize";

    /** Felix speicific property to configure the request buffer size. Default is 8KB */
    public static final String FELIX_JETTY_REQUEST_BUFFER_SIZE = "org.apache.felix.http.jetty.requestBufferSize";

    /** Felix speicific property to configure the request buffer size. Default is 24KB */
    public static final String FELIX_JETTY_RESPONSE_BUFFER_SIZE = "org.apache.felix.http.jetty.responseBufferSize";

    /** Felix specific property to enable Jetty MBeans. Valid values are "true", "false". Default is false */
    public static final String FELIX_HTTP_MBEANS = "org.apache.felix.http.mbeans";

    /** Felix specific property to set the servlet context path of the Http Service */
    public static final String FELIX_HTTP_CONTEXT_PATH = "org.apache.felix.http.context_path";

    private final BundleContext context;
    private boolean debug;
    private String host;
    private int httpPort;
    private int httpsPort;
    private int httpTimeout;
    private String keystore;
    private String password;
    private String keyPassword;
    private boolean useHttps;
    private String truststore;
    private String trustPassword;
    private boolean useHttp;
    private String clientcert;
    private boolean useHttpNio;
    private boolean useHttpsNio;
    private boolean registerMBeans;
    private int sessionTimeout;
    private int requestBufferSize;
    private int responseBufferSize;
    private String contextPath;

    /**
     * Properties from the configuration not matching any of the
     * predefined properties. These properties can be accessed from the
     * getProperty* methods.
     * <p>
     * This map is indexed by String objects (the property names) and
     * the values are just objects as provided by the configuration.
     */
    private Map genericProperties = new HashMap();

    public JettyConfig(BundleContext context)
    {
        this.context = context;
        reset();
    }

    public boolean isDebug()
    {
        return this.debug;
    }

    /**
     * Returns <code>true</code> if HTTP is configured to be used (
     * {@link #FELIX_HTTP_ENABLE}) and
     * the configured HTTP port ({@link #HTTP_PORT}) is higher than zero.
     */
    public boolean isUseHttp()
    {
        return this.useHttp && getHttpPort() > 0;
    }

    public boolean isUseHttpNio()
    {
        return this.useHttpNio;
    }

    /**
     * Returns <code>true</code> if HTTPS is configured to be used (
     * {@link #FELIX_HTTPS_ENABLE}) and
     * the configured HTTP port ({@link #HTTPS_PORT}) is higher than zero.
     */
    public boolean isUseHttps()
    {
        return this.useHttps && getHttpsPort() > 0;
    }

    public boolean isUseHttpsNio()
    {
        return this.useHttpsNio;
    }

    public boolean isRegisterMBeans()
    {
        return this.registerMBeans;
    }

    public String getHost()
    {
        return this.host;
    }

    public int getHttpPort()
    {
        return this.httpPort;
    }

    public int getHttpsPort()
    {
        return this.httpsPort;
    }

    public int getHttpTimeout()
    {
        return this.httpTimeout;
    }

    public String getKeystore()
    {
        return this.keystore;
    }

    public String getPassword()
    {
        return this.password;
    }

    public String getTruststore()
    {
        return this.truststore;
    }

    public String getTrustPassword()
    {
        return this.trustPassword;
    }

    public String getKeyPassword()
    {
        return this.keyPassword;
    }

    public String getClientcert()
    {
        return this.clientcert;
    }

    /**
     * Returns the configured session timeout in minutes or zero if not
     * configured.
     */
    public int getSessionTimeout()
    {
        return this.sessionTimeout;
    }

    public int getRequestBufferSize()
    {
        return this.requestBufferSize;
    }

    public int getResponseBufferSize()
    {
        return this.responseBufferSize;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public void reset()
    {
        update(null);
    }

    public void update(Dictionary props)
    {
        if (props == null) {
            props = new Properties();
        }

        this.debug = getBooleanProperty(props, FELIX_HTTP_DEBUG, getBooleanProperty(props, HTTP_DEBUG, false));
        this.host = getProperty(props, FELIX_HOST, null);
        this.httpPort = getIntProperty(props, HTTP_PORT, 8080);
        this.httpsPort = getIntProperty(props, HTTPS_PORT, 8443);
        this.httpTimeout = getIntProperty(props, HTTP_TIMEOUT, 60000);
        this.keystore = getProperty(props, FELIX_KEYSTORE, this.context.getProperty(OSCAR_KEYSTORE));
        this.password = getProperty(props, FELIX_KEYSTORE_PASSWORD, this.context.getProperty(OSCAR_KEYSTORE_PASSWORD));
        this.keyPassword = getProperty(props, FELIX_KEYSTORE_KEY_PASSWORD, this.context.getProperty(OSCAR_KEYSTORE_KEY_PASSWORD));
        this.useHttps = getBooleanProperty(props, FELIX_HTTPS_ENABLE, getBooleanProperty(props, OSCAR_HTTPS_ENABLE, false));
        this.useHttp = getBooleanProperty(props, FELIX_HTTP_ENABLE, true);
        this.truststore = getProperty(props, FELIX_TRUSTSTORE, null);
        this.trustPassword = getProperty(props, FELIX_TRUSTSTORE_PASSWORD, null);
        this.clientcert = getProperty(props, FELIX_HTTPS_CLIENT_CERT, "none");
        this.useHttpNio = getBooleanProperty(props, FELIX_HTTP_NIO, true);
        this.useHttpsNio = getBooleanProperty(props, FELIX_HTTPS_NIO, this.useHttpNio);
        this.registerMBeans = getBooleanProperty(props, FELIX_HTTP_MBEANS, false);
        this.sessionTimeout = getIntProperty(props, FELIX_SESSION_TIMEOUT, 0);
        this.requestBufferSize = getIntProperty(FELIX_JETTY_REQUEST_BUFFER_SIZE, 8 * 014);
        this.responseBufferSize = getIntProperty(FELIX_JETTY_RESPONSE_BUFFER_SIZE, 24 * 1024);
        this.contextPath = validateContextPath(getProperty(props, FELIX_HTTP_CONTEXT_PATH, null));

        // copy rest of the properties
        Enumeration keys = props.keys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            this.genericProperties.put(key, props.get(key));
        }
    }

    private String getProperty(Dictionary props, String name, String defValue)
    {
        Object value = props.remove(name);
        if (value == null)
        {
            value = this.context.getProperty(name);
        }

        return value != null ? String.valueOf(value) : defValue;
    }

    private boolean getBooleanProperty(Dictionary props, String name, boolean defValue)
    {
        String value = getProperty(props, name, null);
        if (value != null)
        {
            return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        }

        return defValue;
    }

    private int getIntProperty(Dictionary props, String name, int defValue)
    {
        try {
            return Integer.parseInt(getProperty(props, name, null));
        } catch (Exception e) {
            return defValue;
        }
    }

    private static String validateContextPath(String ctxPath)
    {
        // undefined, empty, or root context path
        if (ctxPath == null || ctxPath.length() == 0 || "/".equals(ctxPath))
        {
            return "/";
        }

        // ensure leading but no trailing slash
        if (!ctxPath.startsWith("/"))
        {
            ctxPath = "/".concat(ctxPath);
        }
        while (ctxPath.endsWith("/"))
        {
            ctxPath = ctxPath.substring(0, ctxPath.length() - 1);
        }

        return ctxPath;
    }

    /**
     * Returns the named generic configuration property from the
     * configuration or the bundle context. If neither property is defined
     * return the defValue.
     */
    public String getProperty(String name, String defValue) {
        Object value = this.genericProperties.get(name);
        if (value == null)
        {
            value = this.context.getProperty(name);
        }

        return value != null ? String.valueOf(value) : defValue;
    }

    /**
     * Returns the named generic configuration property from the
     * configuration or the bundle context. If neither property is defined
     * return the defValue.
     */
    public boolean getBooleanProperty(String name, boolean defValue)
    {
        String value = getProperty(name, null);
        if (value != null)
        {
            return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        }

        return defValue;
    }

    /**
     * Returns the named generic configuration property from the
     * configuration or the bundle context. If neither property is defined
     * return the defValue.
     */
    public int getIntProperty(String name, int defValue)
    {
        try {
            return Integer.parseInt(getProperty(name, null));
        } catch (Exception e) {
            return defValue;
        }
    }

    public void setServiceProperties(Hashtable<String, Object> props)
    {
        props.put(HTTP_PORT, String.valueOf(this.httpPort));
        props.put(HTTPS_PORT, String.valueOf(this.httpsPort));
        props.put(FELIX_HTTP_ENABLE, String.valueOf(this.useHttp));
        props.put(FELIX_HTTPS_ENABLE, String.valueOf(this.useHttps));
    }
}
