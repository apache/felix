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

import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.framework.BundleContext;

public final class JettyConfig
{
    /** Felix specific property to set the interface to listen on. Applies to both HTTP and HTTP */
    public static final String FELIX_HOST = "org.apache.felix.http.host";

    /** Standard OSGi port property for HTTP service */
    public static final String HTTP_PORT = "org.osgi.service.http.port";

    /** Standard OSGi port property for HTTPS service */
    public static final String HTTPS_PORT = "org.osgi.service.http.port.secure";

    /** Felix specific property to set http reaching timeout limit */
    public static final String HTTP_TIMEOUT = "org.apache.felix.http.timeout";

    /** Felix specific property to override the keystore file location. */
    public static final String FELIX_KEYSTORE = "org.apache.felix.https.keystore";
    private static final String OSCAR_KEYSTORE = "org.ungoverned.osgi.bundle.https.keystore";

    /** Felix specific property to override the keystore password. */
    public static final String FELIX_KEYSTORE_PASSWORD = "org.apache.felix.https.keystore.password";
    private static final String OSCAR_KEYSTORE_PASSWORD = "org.ungoverned.osgi.bundle.https.password";

    /** Felix specific property to override the keystore key password. */
    public static final String FELIX_KEYSTORE_KEY_PASSWORD = "org.apache.felix.https.keystore.key.password";
    private static final String OSCAR_KEYSTORE_KEY_PASSWORD = "org.ungoverned.osgi.bundle.https.key.password";

    /** Felix specific property to override the type of keystore (JKS). */
    public static final String FELIX_KEYSTORE_TYPE = "org.apache.felix.https.keystore.type";

    /** Felix specific property to control whether to enable HTTPS. */
    public static final String FELIX_HTTPS_ENABLE = "org.apache.felix.https.enable";
    private static final String OSCAR_HTTPS_ENABLE = "org.ungoverned.osgi.bundle.https.enable";

    /** Felix specific property to control whether to enable HTTP. */
    public static final String FELIX_HTTP_ENABLE = "org.apache.felix.http.enable";

    /** Felix specific property to override the truststore file location. */
    public static final String FELIX_TRUSTSTORE = "org.apache.felix.https.truststore";

    /** Felix specific property to override the truststore password. */
    public static final String FELIX_TRUSTSTORE_PASSWORD = "org.apache.felix.https.truststore.password";

    /** Felix specific property to override the type of truststore (JKS). */
    public static final String FELIX_TRUSTSTORE_TYPE = "org.apache.felix.https.truststore.type";

    /** Felix specific property to control whether to want or require HTTPS client certificates. Valid values are "none", "wants", "needs". Default is "none". */
    public static final String FELIX_HTTPS_CLIENT_CERT = "org.apache.felix.https.clientcertificate";

    /** Felix specific property to configure the session timeout in minutes (same session-timout in web.xml). Default is servlet container specific */
    public static final String FELIX_SESSION_TIMEOUT = "org.apache.felix.http.session.timeout";

    /** Felix specific property to control the maximum size of the jetty thread pool */
    public static final String FELIX_JETTY_THREADPOOL_MAX = "org.apache.felix.http.jetty.threadpool.max";

    /** Felix specific property to control the number of jetty acceptor threads */
    public static final String FELIX_JETTY_ACCEPTORS = "org.apache.felix.http.jetty.acceptors";

    /** Felix specific property to control the number of jetty selector threads */
    public static final String FELIX_JETTY_SELECTORS = "org.apache.felix.http.jetty.selectors";

    /** Felix specific property to configure the request buffer size. Default is 16KB (instead of Jetty's default of 4KB) */
    public static final String FELIX_JETTY_HEADER_BUFFER_SIZE = "org.apache.felix.http.jetty.headerBufferSize";

    /** Felix specific property to configure the request buffer size. Default is 8KB */
    public static final String FELIX_JETTY_REQUEST_BUFFER_SIZE = "org.apache.felix.http.jetty.requestBufferSize";

    /** Felix specific property to configure the request buffer size. Default is 24KB */
    public static final String FELIX_JETTY_RESPONSE_BUFFER_SIZE = "org.apache.felix.http.jetty.responseBufferSize";

    /** Felix specific property to configure the max form size. Default is 200KB */
    public static final String FELIX_JETTY_MAX_FORM_SIZE = "org.apache.felix.http.jetty.maxFormSize";

    /** Felix specific property to enable Jetty MBeans. Valid values are "true", "false". Default is false */
    public static final String FELIX_HTTP_MBEANS = "org.apache.felix.http.mbeans";

    /** Felix specific property to set the servlet context path of the Http Service */
    public static final String FELIX_HTTP_CONTEXT_PATH = "org.apache.felix.http.context_path";

    /** Felix specific property to set the list of path exclusions for Web Application Bundles */
    public static final String FELIX_HTTP_PATH_EXCLUSIONS = "org.apache.felix.http.path_exclusions";

    /** Felix specific property to configure the excluded cipher suites. @deprecated use {@link #FELIX_JETTY_EXCLUDED_SUITES} instead. */
    @Deprecated
    public static final String FELIX_JETTY_EXCLUDED_SUITES_OLD = "org.apache.felix.https.jetty.cipersuites.excluded";
    /** Felix specific property to configure the excluded cipher suites */
    public static final String FELIX_JETTY_EXCLUDED_SUITES = "org.apache.felix.https.jetty.ciphersuites.excluded";

    /** Felix specific property to configure the included cipher suites. @deprecated use {@link #FELIX_JETTY_INCLUDED_SUITES} instead. */
    @Deprecated
    public static final String FELIX_JETTY_INCLUDED_SUITES_OLD = "org.apache.felix.https.jetty.cipersuites.included";
    /** Felix specific property to configure the included cipher suites. */
    public static final String FELIX_JETTY_INCLUDED_SUITES = "org.apache.felix.https.jetty.ciphersuites.included";

    /** Felix specific property to specify whether a server header should be sent (defaults to true) */
    public static final String FELIX_JETTY_SEND_SERVER_HEADER = "org.apache.felix.http.jetty.sendServerHeader";

    /** Felix specific property to configure the included protocols */
    public static final String FELIX_JETTY_INCLUDED_PROTOCOLS = "org.apache.felix.https.jetty.protocols.included";

    /** Felix specific property to configure the excluded protocols */
    public static final String FELIX_JETTY_EXCLUDED_PROTOCOLS = "org.apache.felix.https.jetty.protocols.excluded";

    /** Felix specific properties to be able to disable renegotiation protocol for TLSv1 */
    public static final String FELIX_JETTY_RENEGOTIATION_ALLOWED = "org.apache.felix.https.jetty.renegotiateAllowed";

    /** Felix specific property to control whether to enable Proxy/Load Balancer Connection */
    public static final String FELIX_PROXY_LOAD_BALANCER_CONNECTION_ENABLE = "org.apache.felix.proxy.load.balancer.connection.enable";
    
    /** Felix specific property to configure the session cookie httpOnly flag */
    public static final String FELIX_JETTY_SESSION_COOKIE_HTTP_ONLY = "org.apache.felix.https.jetty.session.cookie.httpOnly";
    
    /** Felix specific property to configure the session cookie secure flag */
    public static final String FELIX_JETTY_SESSION_COOKIE_SECURE = "org.apache.felix.https.jetty.session.cookie.secure";
    
    /** Felix specific property to configure session id path parameter*/
    public static final String FELIX_JETTY_SERVLET_SESSION_ID_PATH_PARAMETER_NAME = "org.eclipse.jetty.servlet.SessionIdPathParameterName";

    /** Felix specific property to configure whether JSESSIONID parameter will be added when encoding external URLs */
    public static final String FELIX_JETTY_SERVLET_CHECK_REMOTE_SESSION_ENCODING = "org.eclipse.jetty.servlet.CheckingRemoteSessionIdEncoding";

    /** Felix specific property to configure session cookie name */
    public static final String FELIX_JETTY_SERVLET_SESSION_COOKIE_NAME = "org.eclipse.jetty.servlet.SessionCookie";

    /** Felix specific property to configure session domain */
    public static final String FELIX_JETTY_SERVLET_SESSION_DOMAIN = "org.eclipse.jetty.servlet.SessionDomain";

    /**  Felix specific property to configure session path */
    public static final String FELIX_JETTY_SERVLET_SESSION_PATH = "org.eclipse.jetty.servlet.SessionPath";

    /**  Felix specific property to configure session max age */
    public static final String FELIX_JETTY_SERVLET_SESSION_MAX_AGE = "org.eclipse.jetty.servlet.MaxAge";

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

    private final BundleContext context;

    /**
     * Properties from the configuration not matching any of the
     * predefined properties. These properties can be accessed from the
     * getProperty* methods.
     * <p>
     * This map is indexed by String objects (the property names) and
     * the values are just objects as provided by the configuration.
     */
    private volatile Dictionary config;

    public JettyConfig(BundleContext context)
    {
        this.context = context;
        reset();
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
            return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
        }

        return defValue;
    }

    public String getClientcert()
    {
        return getProperty(FELIX_HTTPS_CLIENT_CERT, "none");
    }

    public String getContextPath()
    {
        return validateContextPath(getProperty(FELIX_HTTP_CONTEXT_PATH, null));
    }

    public String[] getExcludedCipherSuites()
    {
        return getStringArrayProperty(FELIX_JETTY_EXCLUDED_SUITES, getStringArrayProperty(FELIX_JETTY_EXCLUDED_SUITES_OLD, null));
    }

    public String[] getIncludedProtocols()
    {
        return getStringArrayProperty(FELIX_JETTY_INCLUDED_PROTOCOLS, null);
    }

    public String[] getExcludedProtocols()
    {
        return getStringArrayProperty(FELIX_JETTY_EXCLUDED_PROTOCOLS, null);
    }

    public int getHeaderSize()
    {
        return getIntProperty(FELIX_JETTY_HEADER_BUFFER_SIZE, 16 * 1024);
    }

    public String getHost()
    {
        return getProperty(FELIX_HOST, null);
    }

    public int getHttpPort()
    {
        return determinePort(String.valueOf(getProperty(HTTP_PORT)), 8080);
    }

    public int getHttpsPort()
    {
        return determinePort(String.valueOf(getProperty(HTTPS_PORT)), 8443);
    }

    public int getHttpTimeout()
    {
        return getIntProperty(HTTP_TIMEOUT, 60000);
    }

    public String[] getIncludedCipherSuites()
    {
        return getStringArrayProperty(FELIX_JETTY_INCLUDED_SUITES, getStringArrayProperty(FELIX_JETTY_INCLUDED_SUITES_OLD, null));
    }

    /**
     * Returns the named generic configuration property from the
     * configuration or the bundle context. If neither property is defined
     * return the defValue.
     */
    public int getIntProperty(String name, int defValue)
    {
        return parseInt(getProperty(name, null), defValue);
    }

    public String getKeyPassword()
    {
        return getProperty(FELIX_KEYSTORE_KEY_PASSWORD, this.context.getProperty(OSCAR_KEYSTORE_KEY_PASSWORD));
    }

    public String getKeystore()
    {
        return getProperty(FELIX_KEYSTORE, this.context.getProperty(OSCAR_KEYSTORE));
    }

    public String getKeystoreType()
    {
        return getProperty(FELIX_KEYSTORE_TYPE, KeyStore.getDefaultType());
    }

    public String getPassword()
    {
        return getProperty(FELIX_KEYSTORE_PASSWORD, this.context.getProperty(OSCAR_KEYSTORE_PASSWORD));
    }

    public String[] getPathExclusions()
    {
        return getStringArrayProperty(FELIX_HTTP_PATH_EXCLUSIONS, new String[] { "/system" });
    }

    /**
     * Returns the named generic configuration property from the
     * configuration or the bundle context. If neither property is defined
     * return the defValue.
     */
    public String getProperty(String name, String defValue)
    {
        Object value = getProperty(name);
        return value != null ? String.valueOf(value) : defValue;
    }

    public int getThreadPoolMax()
    {
        return getIntProperty(FELIX_JETTY_THREADPOOL_MAX, -1);
    }

    public int getAcceptors()
    {
        return getIntProperty(FELIX_JETTY_ACCEPTORS, -1);
    }

    public int getSelectors()
    {
        return getIntProperty(FELIX_JETTY_SELECTORS, -1);
    }

    public int getRequestBufferSize()
    {
        return getIntProperty(FELIX_JETTY_REQUEST_BUFFER_SIZE, 8 * 1024);
    }

    public int getResponseBufferSize()
    {
        return getIntProperty(FELIX_JETTY_RESPONSE_BUFFER_SIZE, 24 * 1024);
    }

    public int getMaxFormSize()
    {
        return getIntProperty(FELIX_JETTY_MAX_FORM_SIZE, 200 * 1024);
    }

    /**
     * Returns the configured session timeout in minutes or zero if not
     * configured.
     */
    public int getSessionTimeout()
    {
        return getIntProperty(FELIX_SESSION_TIMEOUT, 0);
    }

    public String getTrustPassword()
    {
        return getProperty(FELIX_TRUSTSTORE_PASSWORD, null);
    }

    public String getTruststore()
    {
        String value = getProperty(FELIX_TRUSTSTORE, null);
        return value == null || value.trim().length() == 0 ? null : value;
    }

    public String getTruststoreType()
    {
        return getProperty(FELIX_TRUSTSTORE_TYPE, KeyStore.getDefaultType());
    }

    public boolean isRegisterMBeans()
    {
        return getBooleanProperty(FELIX_HTTP_MBEANS, false);
    }

    /**
     * Returns <code>true</code> if HTTP is configured to be used (
     * {@link #FELIX_HTTP_ENABLE}) and
     * the configured HTTP port ({@link #HTTP_PORT}) is higher than zero.
     */
    public boolean isUseHttp()
    {
        boolean useHttp = getBooleanProperty(FELIX_HTTP_ENABLE, true);
        return useHttp && getHttpPort() > 0;
    }

    public boolean isSendServerHeader()
    {
        return getBooleanProperty(FELIX_JETTY_SEND_SERVER_HEADER, false);
    }

    /**
     * Returns <code>true</code> if HTTPS is configured to be used (
     * {@link #FELIX_HTTPS_ENABLE}) and
     * the configured HTTP port ({@link #HTTPS_PORT}) is higher than zero.
     */
    public boolean isUseHttps()
    {
        boolean useHttps = getBooleanProperty(FELIX_HTTPS_ENABLE, getBooleanProperty(OSCAR_HTTPS_ENABLE, false));
        return useHttps && getHttpsPort() > 0;
    }

    public boolean isProxyLoadBalancerConnection()
    {
        return getBooleanProperty(FELIX_PROXY_LOAD_BALANCER_CONNECTION_ENABLE, false);
    }

    public boolean isRenegotiationAllowed() {
        return getBooleanProperty(FELIX_JETTY_RENEGOTIATION_ALLOWED, false);
    }

    public void reset()
    {
        update(null);
    }

    public void setServiceProperties(Hashtable<String, Object> props)
    {
        props.put(HTTP_PORT, Integer.toString(getHttpPort()));
        props.put(HTTPS_PORT, Integer.toString(getHttpsPort()));
        props.put(FELIX_HTTP_ENABLE, Boolean.toString(isUseHttp()));
        props.put(FELIX_HTTPS_ENABLE, Boolean.toString(isUseHttps()));
    }

    /**
     * Updates this configuration with the given dictionary.
     *
     * @param props the dictionary with the new configuration values, can be <code>null</code> to reset this configuration to its defaults.
     * @return <code>true</code> if the configuration was updated due to a changed value, or <code>false</code> if no change was found.
     */
    public boolean update(Dictionary props)
    {
        if (props == null)
        {
            props = new Properties();
        }

        // FELIX-4312 Check whether there's something changed in our configuration...
        Dictionary currentConfig = this.config;
        if (currentConfig == null || !props.equals(currentConfig))
        {
            this.config = props;

            return true;
        }

        return false;
    }

    private void closeSilently(ServerSocket resource)
    {
        if (resource != null)
        {
            try
            {
                resource.close();
            }
            catch (IOException e)
            {
                // Ignore...
            }
        }
    }

    /**
     * Determine the appropriate port to use. <code>portProp</code> is based
     * "version range" as described in OSGi Core Spec v4.2 3.2.6. It can use the
     * following forms:
     * <dl>
     * <dd>8000 | 8000</dd>
     * <dd>[8000,9000] | 8000 &lt;= port &lt;= 9000</dd>
     * <dd>[8000,9000) | 8000 &lt;= port &lt; 9000</dd>
     * <dd>(8000,9000] | 8000 &lt; port &lt;= 9000</dd>
     * <dd>(8000,9000) | 8000 &lt; port &lt; 9000</dd>
     * <dd>[,9000) | 1 &lt; port &lt; 9000</dd>
     * <dd>[8000,) | 8000 &lt;= port &lt; 65534</dd>
     * </dl>
     *
     * @param portProp
     *            The port property value to parse.
     * @return The port determined to be usable. -1 if failed to find a port.
     */
    private int determinePort(String portProp, int dflt)
    {
        // Default cases include null/empty range pattern or pattern == *.
        if (portProp == null || "".equals(portProp.trim()))
        {
            return dflt;
        }

        // asking for random port, so let ServerSocket handle it and return the answer
        portProp = portProp.trim();
        if ("*".equals(portProp) || "0".equals(portProp))
        {
            return getSocketPort(0);
        }
        else
        {
            // check that the port property is a version range as described in
            // OSGi Core Spec v4.2 3.2.6.
            // deviations from the spec are limited to:
            // * start, end of interval defaults to 1, 65535, respectively, if missing.
            char startsWith = portProp.charAt(0);
            char endsWith = portProp.charAt(portProp.length() - 1);

            int minPort = 1;
            int maxPort = 65535;

            if (portProp.contains(",") && (startsWith == '[' || startsWith == '(') && (endsWith == ']' || endsWith == ')'))
            {
                String interval = portProp.substring(1, portProp.length() - 1);
                int comma = interval.indexOf(',');

                // check if the comma is first (start port in range is missing)
                int start = (comma == 0) ? minPort : parseInt(interval.substring(0, comma), minPort);
                // check if the comma is last (end port in range is missing)
                int end = (comma == interval.length() - 1) ? maxPort : parseInt(interval.substring(comma + 1), maxPort);
                // check for exclusive notation
                if (startsWith == '(')
                {
                    start++;
                }
                if (endsWith == ')')
                {
                    end--;
                }
                // find a port in the requested range
                int port = start - 1;
                for (int i = start; port < start && i <= end; i++)
                {
                    port = getSocketPort(i);
                }

                return (port < start) ? dflt : port;
            }
            else
            {
                // We don't recognize the pattern as special, so try to parse it to an int
                return parseInt(portProp, dflt);
            }
        }
    }

    private int getSocketPort(int i)
    {
        int port = -1;
        ServerSocket ss = null;
        try
        {
            ss = new ServerSocket(i);
            port = ss.getLocalPort();
        }
        catch (IOException e)
        {
            SystemLogger.debug("Unable to bind to port: " + i);
        }
        finally
        {
            closeSilently(ss);
        }
        return port;
    }

    private Object getProperty(String name)
    {
        Dictionary conf = this.config;
        Object value = (conf != null) ? conf.get(name) : null;
        if (value == null)
        {
            value = this.context.getProperty(name);
        }
        return value;
    }

    /**
     * Get the property value as a string array.
     * Empty values are filtered out - if the resulting array is empty
     * the default value is returned.
     */
    private String[] getStringArrayProperty(String name, String[] defValue)
    {
        Object value = getProperty(name);
        if (value instanceof String)
        {
            final String stringVal = ((String) value).trim();
            if (stringVal.length() > 0)
            {
                return stringVal.split(",");
            }
        }
        else if (value instanceof String[])
        {
            final String[] stringArr = (String[]) value;
            final List<String> list = new ArrayList<String>();
            for (final String stringVal : stringArr)
            {
                if (stringVal.trim().length() > 0)
                {
                    list.add(stringVal.trim());
                }
            }
            if (list.size() > 0)
            {
                return list.toArray(new String[list.size()]);
            }
        }
        else if (value instanceof Collection)
        {
            final ArrayList<String> conv = new ArrayList<String>();
            for (Iterator<?> vi = ((Collection<?>) value).iterator(); vi.hasNext();)
            {
                Object object = vi.next();
                if (object != null)
                {
                    conv.add(String.valueOf(object));
                }
            }
            if (conv.size() > 0)
            {
                return conv.toArray(new String[conv.size()]);
            }
        }
        return defValue;
    }

    private int parseInt(String value, int dflt)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return dflt;
        }
    }
}
