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

import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.ATTR_SSL_CERTIFICATE;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HDR_X_FORWARDED_PORT;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.HTTPS;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.UTF_8;
import static org.apache.felix.http.sslfilter.internal.SslFilterConstants.X_509;

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
    // The HTTP scheme prefix in an URL
    private static final String HTTP_SCHEME_PREFIX = "http://";

    // pattern to convert the header to a PEM certificate for parsing
    // by replacing spaces with line breaks
    private static final Pattern HEADER_TO_CERT = Pattern.compile("(?! CERTIFICATE)(?= ) ");

    @SuppressWarnings("unchecked")
    SslFilterRequest(HttpServletRequest request, String clientCertHeader) throws CertificateException
    {
        super(request);

        // TODO jawi: perhaps we should make this class a little smarter wrt the given request:
        // it now always assumes it should rewrite its URL, while this might not always be the
        // case...

        if (clientCertHeader != null && !"".equals(clientCertHeader.trim()))
        {
            final String clientCert = HEADER_TO_CERT.matcher(clientCertHeader).replaceAll("\n");

            try
            {
                CertificateFactory fac = CertificateFactory.getInstance(X_509);

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

    @Override
    public String getScheme()
    {
        return HTTPS;
    }

    @Override
    public boolean isSecure()
    {
        return true;
    }

    @Override
    public StringBuffer getRequestURL()
    {
        StringBuffer tmp = new StringBuffer(super.getRequestURL());
        // In case the request happened over http, simply insert an additional 's'
        // to make the request appear to be done over https...
        if (tmp.indexOf(HTTP_SCHEME_PREFIX) == 0)
        {
            tmp.insert(4, 's');
        }
        return tmp;
    }

    @Override
    public int getServerPort()
    {
        int port;

        try
        {
            String fwdPort = getHeader(HDR_X_FORWARDED_PORT);
            port = Integer.parseInt(fwdPort);
        }
        catch (Exception e)
        {
            // Use default port for the used protocol...
            port = getRequest().getServerPort();
        }
        return port;
    }
}
