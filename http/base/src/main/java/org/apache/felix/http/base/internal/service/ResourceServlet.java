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
package org.apache.felix.http.base.internal.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

<<<<<<< HEAD
public final class ResourceServlet extends HttpServlet
{
    private final String path;

    public ResourceServlet(String path)
    {
        this.path = path;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        String target = req.getPathInfo();
        if (target == null)
        {
            target = "";
        }

        if (!target.startsWith("/"))
        {
            target += "/" + target;
        }

        String resName = this.path + target;
        URL url = getServletContext().getResource(resName);
=======
/**
 * The resource servlet
 */
public final class ResourceServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    /** The path of the resource registration. */
    private final String prefix;

    public ResourceServlet(final String prefix)
    {
        this.prefix = prefix;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
            throws ServletException, IOException
    {
        final String target = req.getPathInfo();
        final String resName = (target == null ? this.prefix : this.prefix + target);

        final URL url = getServletContext().getResource(resName);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        if (url == null)
        {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        else
        {
            handle(req, res, url, resName);
        }
    }

<<<<<<< HEAD
    private void handle(HttpServletRequest req, HttpServletResponse res, URL url, String resName) throws IOException
    {
        String contentType = getServletContext().getMimeType(resName);
=======
    private void handle(final HttpServletRequest req,
            final HttpServletResponse res, final URL url, final String resName)
    throws IOException
    {
        final String contentType = getServletContext().getMimeType(resName);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if (contentType != null)
        {
            res.setContentType(contentType);
        }

<<<<<<< HEAD
        long lastModified = getLastModified(url);
=======
        final long lastModified = getLastModified(url);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if (lastModified != 0)
        {
            res.setDateHeader("Last-Modified", lastModified);
        }

        if (!resourceModified(lastModified, req.getDateHeader("If-Modified-Since")))
        {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
        else
        {
            copyResource(url, res);
        }
    }

<<<<<<< HEAD
    private long getLastModified(URL url)
=======
    private long getLastModified(final URL url)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        long lastModified = 0;

        try
        {
<<<<<<< HEAD
            URLConnection conn = url.openConnection();
            lastModified = conn.getLastModified();
        }
        catch (Exception e)
=======
            final URLConnection conn = url.openConnection();
            lastModified = conn.getLastModified();
        }
        catch (final Exception e)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            // Do nothing
        }

        if (lastModified == 0)
        {
<<<<<<< HEAD
            String filepath = url.getPath();
            if (filepath != null)
            {
                File f = new File(filepath);
=======
            final String filepath = url.getPath();
            if (filepath != null)
            {
                final File f = new File(filepath);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                if (f.exists())
                {
                    lastModified = f.lastModified();
                }
            }
        }

        return lastModified;
    }

    private boolean resourceModified(long resTimestamp, long modSince)
    {
        modSince /= 1000;
        resTimestamp /= 1000;

        return resTimestamp == 0 || modSince == -1 || resTimestamp > modSince;
    }

<<<<<<< HEAD
    private void copyResource(URL url, HttpServletResponse res) throws IOException
=======
    private void copyResource(final URL url, final HttpServletResponse res) throws IOException
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        URLConnection conn = null;
        OutputStream os = null;
        InputStream is = null;

        try
        {
            conn = url.openConnection();

            is = conn.getInputStream();
            os = res.getOutputStream();
<<<<<<< HEAD
            // FELIX-3987 content length should be set *before* any streaming is done 
=======
            // FELIX-3987 content length should be set *before* any streaming is done
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            // as headers should be written before the content is actually written...
            int len = getContentLength(conn);
            if (len >= 0)
            {
                res.setContentLength(len);
            }

            byte[] buf = new byte[1024];
            int n;

            while ((n = is.read(buf, 0, buf.length)) >= 0)
            {
                os.write(buf, 0, n);
            }
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }

            if (os != null)
            {
                os.close();
            }
        }
    }

<<<<<<< HEAD
    private int getContentLength(URLConnection conn)
=======
    private int getContentLength(final URLConnection conn)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        int length = -1;

        length = conn.getContentLength();
        if (length < 0)
        {
<<<<<<< HEAD
            // Unknown, try whether it is a file, and if so, use the file 
=======
            // Unknown, try whether it is a file, and if so, use the file
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            // API to get the length of the content...
            String path = conn.getURL().getPath();
            if (path != null)
            {
                File f = new File(path);
<<<<<<< HEAD
                // In case more than 2GB is streamed 
=======
                // In case more than 2GB is streamed
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                if (f.length() < Integer.MAX_VALUE)
                {
                    length = (int) f.length();
                }
            }
        }
        return length;
    }
}
