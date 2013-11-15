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
package org.apache.felix.http.sslfilter.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

class SslFilterRequest extends HttpServletRequestWrapper
{
    // The HTTPS scheme name
    private static final String HTTPS_SCHEME = "https";

    // The HTTP scheme prefix in an URL
    private static final String HTTP_SCHEME_PREFIX = "http://";

    // pattern to convert the header to a PEM certificate for parsing
    // by replacing spaces with line breaks
    private static Pattern HEADER_TO_CERT = Pattern.compile("(?! CERTIFICATE)(?= ) ");

    // character encoding for the client certificate header
    private static final String UTF_8 = "UTF-8";

    /**
     * If there is an SSL certificate associated with the request, it must be
     * exposed by the servlet container to the servlet programmer as an array of
     * objects of type java.security.cert.X509Certificate and accessible via a
     * ServletRequest attribute of javax.servlet.request.X509Certificate.
     * <p>
     * The order of this array is defined as being in ascending order of trust.
     * The first certificate in the chain is the one set by the client, the next
     * is the one used to authenticate the first, and so on.
     */
    protected static final String ATTR_SSL_CERTIFICATE = "javax.servlet.request.X509Certificate";

    private String requestURL;

    @SuppressWarnings("unchecked")
    SslFilterRequest(HttpServletRequest request, String clientCertHeader) throws CertificateException
    {
        super(request);

        if (clientCertHeader != null && clientCertHeader.length() > 0)
        {
            final String clientCert = HEADER_TO_CERT.matcher(clientCertHeader).replaceAll("\n");

            try
            {
                CertificateFactory fac = CertificateFactory.getInstance("X.509");

                InputStream instream = new ByteArrayInputStream(clientCert.getBytes(UTF_8));

                Collection<X509Certificate> certs = (Collection<X509Certificate>) fac.generateCertificates(instream);
                request.setAttribute(ATTR_SSL_CERTIFICATE, certs.toArray(new X509Certificate[certs.size()]));
            }
            catch (UnsupportedEncodingException e)
            {
                // Any JRE should support UTF-8...
                throw new InternalError("UTF-8 not supported?!");
            }
        }
    }

    void done()
    {
        getRequest().removeAttribute(ATTR_SSL_CERTIFICATE);
    }

    public String getScheme()
    {
        return HTTPS_SCHEME;
    }

    public boolean isSecure()
    {
        return true;
    }

    public StringBuffer getRequestURL()
    {
        if (this.requestURL == null)
        {
            StringBuffer tmp = super.getRequestURL();
            if (tmp.indexOf(HTTP_SCHEME_PREFIX) == 0)
            {
                this.requestURL = HTTPS_SCHEME.concat(tmp.substring(4));
            }
            else
            {
                this.requestURL = tmp.toString();
            }
        }

        return new StringBuffer(this.requestURL);
    }
}
