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
package org.apache.felix.webconsole;


import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.io.IOUtils;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;


/**
 * The Web Console can be extended by registering an OSGi service for the interface
 * {@link javax.servlet.Servlet} with the service property
 * <code>felix.webconsole.label</code> set to the label (last segment in the URL)
 * of the page. The respective service is called a Web Console Plugin or a plugin
 * for short.
 *
 * To help rendering the response the Apache Felix Web Console bundle provides two
 * options. One of the options is to extend the AbstractWebConsolePlugin overwriting
 * the {@link #renderContent(HttpServletRequest, HttpServletResponse)} method.
 */
public abstract class AbstractWebConsolePlugin extends HttpServlet
{

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    /** The name of the request attribute containing the map of FileItems from the POST request */
    public static final String ATTR_FILEUPLOAD = "org.apache.felix.webconsole.fileupload"; //$NON-NLS-1$
    
    /** 
     * The name of the request attribute containing a {@link java.io.File} - upload repository path used by
     * {@link org.apache.commons.fileupload.disk.DiskFileItemFactory}.<p>
     * 
     * The Web Console plugin, that utilizes file upload capabilities of the web console SHOULD:
     * <ol>
     * <li>Obtain the file using {@link org.osgi.framework.BundleContext#getDataFile(String)}
     * <li>Set the file as request attribute
     * <li>Use {@link WebConsoleUtil#getParameter(HttpServletRequest, String)} to obtain the file(s)
     * </ol>
     * 
     * Without setting this attribute, your plugin will not work if there is a security manager enabled.
     * It is guaranteed, that your plugin has permissions to read/write/delete files to the location, 
     * provided by the bundle context.
     */
    public static final String ATTR_FILEUPLOAD_REPO = "org.apache.felix.webconsole.fileupload.repo"; //$NON-NLS-1$

    /**
     * Web Console Plugin typically consists of servlet and resources such as images,
     * scripts or style sheets.
     *
     * To load resources, a Resource Provider is used. The resource provider is an object,
     * that provides a method which name is specified by this constants and it is
     * 'getResource'.
     *
     *  @see #getResourceProvider()
     */
    public static final String GET_RESOURCE_METHOD_NAME = "getResource"; //$NON-NLS-1$

    /**
     * The header fragment read from the templates/main_header.html file
     */
    private static String HEADER;

    /**
     * The footer fragment read from the templates/main_footer.html file
     */
    private static String FOOTER;

    /**
     * The reference to the getResource method provided by the
     * {@link #getResourceProvider()}. This is <code>null</code> if there is
     * none or before the first check if there is one.
     *
     * @see #getGetResourceMethod()
     */
    private Method getResourceMethod;

    /**
     * flag indicating whether the getResource method has already been looked
     * up or not. This prevens the {@link #getGetResourceMethod()} method from
     * repeatedly looking up the resource method on plugins which do not have
     * one.
     */
    private boolean getResourceMethodChecked;

    private BundleContext bundleContext;

    private static BrandingPlugin brandingPlugin = DefaultBrandingPlugin.getInstance();

    private static int logLevel;


    //---------- HttpServlet Overwrites ----------------------------------------

    /**
     * Returns the title for this plugin as returned by {@link #getTitle()}
     *
     * @see javax.servlet.GenericServlet#getServletName()
     */
    public String getServletName()
    {
        return getTitle();
    }


    /**
     * This method should return category string which will be used to render
     * the plugin in the navigation menu. Default implementation returns null,
     * which will result in the plugin link rendered as top level menu item.
     * Concrete implementations wishing to be rendered as a sub-menu item under
     * a category should override this method and return a string or define
     * <code>felix.webconsole.category</code> OSGi property. Currently only
     * single level categories are supported. So, this should be a simple
     * String.
     *
     * @return category
     */
    public String getCategory()
    {
        return null;
    }


    /**
     * Renders the web console page for the request. This consist of the
     * following five parts called in order:
     * <ol>
     * <li>Send back a requested resource
     * <li>{@link #startResponse(HttpServletRequest, HttpServletResponse)}</li>
     * <li>{@link #renderTopNavigation(HttpServletRequest, PrintWriter)}</li>
     * <li>{@link #renderContent(HttpServletRequest, HttpServletResponse)}</li>
     * <li>{@link #endResponse(PrintWriter)}</li>
     * </ol>
     * <p>
     * <b>Note</b>: If a resource is sent back for the request only the first
     * step is executed. Otherwise the first step is a null-operation actually
     * and the latter four steps are executed in order.
     * <p>
     * If the {@link #isHtmlRequest(HttpServletRequest)} method returns
     * <code>false</code> only the
     * {@link #renderContent(HttpServletRequest, HttpServletResponse)} method is
     * called.
     *
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        if ( !spoolResource( request, response ) )
        {
            // detect if this is an html request
            if ( isHtmlRequest(request) )
            {
                // start the html response, write the header, open body and main div
                PrintWriter pw = startResponse( request, response );

                // render top navigation
                renderTopNavigation( request, pw );

                // wrap content in a separate div
                pw.println( "<div id='content'>" ); //$NON-NLS-1$
                renderContent( request, response );
                pw.println( "</div>" ); //$NON-NLS-1$

                // close the main div, body, and html
                endResponse( pw );
            }
            else
            {
                renderContent( request, response );
            }
        }
    }

    /**
     * Detects whether this request is intended to have the headers and
     * footers of this plugin be rendered or not. This method always returns
     * <code>true</code> and may be overwritten by plugins to detect
     * from the actual request, whether or not to render the header and
     * footer.
     *
     * @param request the original request passed from the HTTP server
     * @return <code>true</code> if the page should have headers and footers rendered
     */
    protected boolean isHtmlRequest( final HttpServletRequest request )
    {
        return true;
    }


    //---------- AbstractWebConsolePlugin API ----------------------------------

    /**
     * This method is called from the Felix Web Console to ensure the
     * AbstractWebConsolePlugin is correctly setup.
     *
     * It is called right after the Web Console receives notification for
     * plugin registration.
     *
     * @param bundleContext the context of the plugin bundle
     */
    public void activate( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
    }


    /**
     * This method is called, by the Web Console to de-activate the plugin and release
     * all used resources.
     */
    public void deactivate()
    {
        this.bundleContext = null;
    }


    /**
     * This method is used to render the content of the plug-in. It is called internally
     * from the Web Console.
     *
     * @param req the HTTP request send from the user
     * @param res the HTTP response object, where to render the plugin data.
     * @throws IOException if an input or output error is
     *  detected when the servlet handles the request
     * @throws ServletException  if the request for the GET
     *  could not be handled
     */
    protected abstract void renderContent( HttpServletRequest req, HttpServletResponse res ) throws ServletException,
        IOException;

    /**
     * Retrieves the label. This is the last component in the servlet path.
     *
     * This method MUST be overridden, if the {@link #AbstractWebConsolePlugin()}
     * constructor is used.
     *
     * @return the label.
     */
    public abstract String getLabel();

    /**
     * Retrieves the title of the plug-in. It is displayed in the page header
     * and is also included in the title of the HTML document.
     *
     * This method MUST be overridden, if the {@link #AbstractWebConsolePlugin()}
     * constructor is used.
     *
     * @return the plugin title.
     */
    public abstract String getTitle();

    /**
     * Returns a list of CSS reference paths or <code>null</code> if no
     * additional CSS files are provided by the plugin.
     * <p>
     * The result is an array of strings which are used as the value of
     * the <code>href</code> attribute of the <code>&lt;link&gt;</code> elements
     * placed in the head section of the HTML generated. If the reference is
     * a relative path, it is turned into an absolute path by prepending the
     * value of the {@link WebConsoleConstants#ATTR_APP_ROOT} request attribute.
     *
     * @return The list of additional CSS files to reference in the head
     *      section or <code>null</code> if no such CSS files are required.
     */
    protected String[] getCssReferences()
    {
        return null;
    }

    /**
     * Returns the <code>BundleContext</code> with which this plugin has been
     * activated. If the plugin has not be activated by calling the
     * {@link #activate(BundleContext)} method, this method returns
     * <code>null</code>.
     *
     * @return the bundle context or <code>null</code> if the bundle is not activated.
     */
    protected BundleContext getBundleContext()
    {
        return bundleContext;
    }


    /**
     * Returns the <code>Bundle</code> pertaining to the
     * {@link #getBundleContext() bundle context} with which this plugin has
     * been activated. If the plugin has not be activated by calling the
     * {@link #activate(BundleContext)} method, this method returns
     * <code>null</code>.
     *
     * @return the bundle or <code>null</code> if the plugin is not activated.
     */
    public final Bundle getBundle()
    {
        final BundleContext bundleContext = getBundleContext();
        return ( bundleContext != null ) ? bundleContext.getBundle() : null;
    }

    /**
     * Returns the object which might provide resources. The class of this
     * object is used to find the <code>getResource</code> method.
     * <p>
     * This method may be overwritten by extensions. This base class
     * implementation returns this instance.
     *
     * @return The resource provider object or <code>null</code> if no
     *      resources will be provided by this plugin.
     */
    protected Object getResourceProvider()
    {
        return this;
    }


    /**
     * Returns a method which is called on the
     * {@link #getResourceProvider() resource provider} class to return an URL
     * to a resource which may be spooled when requested. The method has the
     * following signature:
     * <pre>
     * [modifier] URL getResource(String path);
     * </pre>
     * Where the <i>[modifier]</i> may be <code>public</code>, <code>protected</code>
     * or <code>private</code> (if the method is declared in the class of the
     * resource provider). It is suggested to use the <code>private</code>
     * modifier if the method is declared in the resource provider class or
     * the <code>protected</code> modifier if the method is declared in a
     * base class of the resource provider.
     *
     * @return The <code>getResource(String)</code> method or <code>null</code>
     *      if the {@link #getResourceProvider() resource provider} is
     *      <code>null</code> or does not provide such a method.
     */
    private final Method getGetResourceMethod()
    {
        // return what we know of the getResourceMethod, if we already checked
        if (getResourceMethodChecked) {
            return getResourceMethod;
        }

        Method tmpGetResourceMethod = null;
        Object resourceProvider = getResourceProvider();
        if ( resourceProvider != null )
        {
            try
            {
                Class cl = resourceProvider.getClass();
                while ( tmpGetResourceMethod == null && cl != Object.class )
                {
                    Method[] methods = cl.getDeclaredMethods();
                    for ( int i = 0; i < methods.length; i++ )
                    {
                        Method m = methods[i];
                        if ( GET_RESOURCE_METHOD_NAME.equals( m.getName() ) && m.getParameterTypes().length == 1
                            && m.getParameterTypes()[0] == String.class && m.getReturnType() == URL.class )
                        {
                            // ensure modifier is protected or public or the private
                            // method is defined in the plugin class itself
                            int mod = m.getModifiers();
                            if ( Modifier.isProtected( mod ) || Modifier.isPublic( mod )
                                || ( Modifier.isPrivate( mod ) && cl == resourceProvider.getClass() ) )
                            {
                                m.setAccessible( true );
                                tmpGetResourceMethod = m;
                                break;
                            }
                        }
                    }
                    cl = cl.getSuperclass();
                }
            }
            catch ( Throwable t )
            {
                tmpGetResourceMethod = null;
            }
        }

        // set what we have found and prevent future lookups
        getResourceMethod = tmpGetResourceMethod;
        getResourceMethodChecked = true;

        // now also return the method
        return getResourceMethod;
    }


    /**
     * Calls the <code>ServletContext.log(String)</code> method if the
     * configured log level is less than or equal to the given <code>level</code>.
     * <p>
     * Note, that the <code>level</code> paramter is only used to decide whether
     * the <code>GenericServlet.log(String)</code> method is called or not. The
     * actual implementation of the <code>GenericServlet.log</code> method is
     * outside of the control of this method.
     * <p>
     * If the servlet has not been initialized yet or has already been destroyed
     * the message is printed to stderr.
     *
     * @param level The log level at which to log the message
     * @param message The message to log
     */
    public void log( int level, String message )
    {
        if ( logLevel >= level )
        {
            ServletConfig config = getServletConfig();
            if ( config != null )
            {
                ServletContext context = config.getServletContext();
                if ( context != null )
                {
                    context.log( message );
                    return;
                }
            }

            System.err.println( message );
        }
    }


    /**
     * Calls the <code>ServletContext.log(String, Throwable)</code> method if
     * the configured log level is less than or equal to the given
     * <code>level</code>.
     * <p>
     * Note, that the <code>level</code> paramter is only used to decide whether
     * the <code>GenericServlet.log(String, Throwable)</code> method is called
     * or not. The actual implementation of the <code>GenericServlet.log</code>
     * method is outside of the control of this method.
     *
     * @param level The log level at which to log the message
     * @param message The message to log
     * @param t The <code>Throwable</code> to log with the message
     */
    public void log( int level, String message, Throwable t )
    {
        if ( logLevel >= level )
        {
            ServletConfig config = getServletConfig();
            if ( config != null )
            {
                ServletContext context = config.getServletContext();
                if ( context != null )
                {
                    context.log( message, t );
                    return;
                }
            }

            System.err.println( message );
            if ( t != null )
            {
                t.printStackTrace( System.err );
            }
        }
    }
    
    /**
     * If the request addresses a resource which may be served by the
     * <code>getResource</code> method of the
     * {@link #getResourceProvider() resource provider}, this method serves it
     * and returns <code>true</code>. Otherwise <code>false</code> is returned.
     * <code>false</code> is also returned if the resource provider has no
     * <code>getResource</code> method.
     * <p>
     * If <code>true</code> is returned, the request is considered complete and
     * request processing terminates. Otherwise request processing continues
     * with normal plugin rendering.
     *
     * @param request The request object
     * @param response The response object
     * @return <code>true</code> if the request causes a resource to be sent back.
     *
     * @throws IOException If an error occurs accessing or spooling the resource.
     */
    private final boolean spoolResource(final HttpServletRequest request, 
        final HttpServletResponse response) throws IOException
    {
        try
        {
            // We need to call spoolResource0 in privileged block because it uses reflection, which
            // requires the following set of permissions:
            // (java.lang.RuntimePermission "getClassLoader")
            // (java.lang.RuntimePermission "accessDeclaredMembers")
            // (java.lang.reflect.ReflectPermission "suppressAccessChecks")
            // See also https://issues.apache.org/jira/browse/FELIX-4652
            final Boolean ret = (Boolean) AccessController.doPrivileged(new PrivilegedExceptionAction()
            {

                public Object run() throws Exception
                {
                    return spoolResource0(request, response) ? Boolean.TRUE : Boolean.FALSE;
                }
            });
            return ret.booleanValue();
        }
        catch (PrivilegedActionException e)
        {
            final Exception x = e.getException();
            throw x instanceof IOException ? (IOException) x : new IOException(
                x.toString());
        }
    }

    final boolean spoolResource0( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // no resource if no resource accessor
        Method getResourceMethod = getGetResourceMethod();
        if ( getResourceMethod == null )
        {
            return false;
        }

        String pi = request.getPathInfo();
        InputStream ins = null;
        try
        {

            // check for a resource, fail if none
            URL url = ( URL ) getResourceMethod.invoke( getResourceProvider(), new Object[]
                { pi } );
            if ( url == null )
            {
                return false;
            }

            // open the connection and the stream (we use the stream to be able
            // to at least hint to close the connection because there is no
            // method to explicitly close the conneciton, unfortunately)
            URLConnection connection = url.openConnection();
            ins = connection.getInputStream();

            // FELIX-2017 Equinox may return an URL for a non-existing
            // resource but then (instead of throwing) return null on
            // getInputStream. We should account for this situation and
            // just assume a non-existing resource in this case.
            if (ins == null) {
                return false;
            }

            // check whether we may return 304/UNMODIFIED
            long lastModified = connection.getLastModified();
            if ( lastModified > 0 )
            {
                long ifModifiedSince = request.getDateHeader( "If-Modified-Since" ); //$NON-NLS-1$
                if ( ifModifiedSince >= ( lastModified / 1000 * 1000 ) )
                {
                    // Round down to the nearest second for a proper compare
                    // A ifModifiedSince of -1 will always be less
                    response.setStatus( HttpServletResponse.SC_NOT_MODIFIED );

                    return true;
                }

                // have to send, so set the last modified header now
                response.setDateHeader( "Last-Modified", lastModified ); //$NON-NLS-1$
            }

            // describe the contents
            response.setContentType( getServletContext().getMimeType( pi ) );
            response.setIntHeader( "Content-Length", connection.getContentLength() ); //$NON-NLS-1$

            // spool the actual contents
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[2048];
            int rd;
            while ( ( rd = ins.read( buf ) ) >= 0 )
            {
                out.write( buf, 0, rd );
            }

            // over and out ...
            return true;
        }
        catch ( IllegalAccessException iae )
        {
            // log or throw ???
        }
        catch ( InvocationTargetException ite )
        {
            // log or throw ???
            // Throwable cause = ite.getTargetException();
        }
      finally
        {
            IOUtils.closeQuietly(ins);
        }

        return false;
    }


    /**
     * This method is responsible for generating the top heading of the page.
     *
     * @param request the HTTP request coming from the user
     * @param response the HTTP response, where data is rendered
     * @return the writer that was used for generating the response.
     * @throws IOException on I/O error
     * @see #endResponse(PrintWriter)
     */
    protected PrintWriter startResponse( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        response.setCharacterEncoding( "utf-8" ); //$NON-NLS-1$
        response.setContentType( "text/html" ); //$NON-NLS-1$

        final PrintWriter pw = response.getWriter();

        final String appRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT );

        // support localization of the plugin title
        String title = getTitle();
        if ( title.startsWith( "%" ) ) //$NON-NLS-1$
        {
            title = "${" + title.substring( 1 ) + "}"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        VariableResolver resolver = WebConsoleUtil.getVariableResolver(request);
        if (resolver instanceof DefaultVariableResolver) {
            DefaultVariableResolver r = (DefaultVariableResolver) resolver;
            r.put("head.title", title); //$NON-NLS-1$
            r.put("head.label", getLabel()); //$NON-NLS-1$
            r.put("head.cssLinks", getCssLinks(appRoot)); //$NON-NLS-1$
            r.put("brand.name", brandingPlugin.getBrandName()); //$NON-NLS-1$
            r.put("brand.product.url", brandingPlugin.getProductURL()); //$NON-NLS-1$
            r.put("brand.product.name", brandingPlugin.getProductName()); //$NON-NLS-1$
            r.put("brand.product.img", toUrl( brandingPlugin.getProductImage(), appRoot )); //$NON-NLS-1$
            r.put("brand.favicon", toUrl( brandingPlugin.getFavIcon(), appRoot )); //$NON-NLS-1$
            r.put("brand.css", toUrl( brandingPlugin.getMainStyleSheet(), appRoot )); //$NON-NLS-1$
        }
        pw.println( getHeader() );

        return pw;
    }


    /**
     * This method is called to generate the top level links with the available plug-ins.
     *
     * @param request the HTTP request coming from the user
     * @param pw the writer, where the HTML data is rendered
     */
    protected void renderTopNavigation( HttpServletRequest request, PrintWriter pw )
    {
        // assume pathInfo to not be null, else this would not be called
        String current = request.getPathInfo();
        int slash = current.indexOf( "/", 1 ); //$NON-NLS-1$
        if ( slash < 0 )
        {
            slash = current.length();
        }
        current = current.substring( 1, slash );

        String appRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT );

        Map menuMap = ( Map ) request.getAttribute( OsgiManager.ATTR_LABEL_MAP_CATEGORIZED );
        this.renderMenu( menuMap, appRoot, pw );

        // render lang-box
        Map langMap = (Map) request.getAttribute(WebConsoleConstants.ATTR_LANG_MAP);
        if (null != langMap && !langMap.isEmpty())
        {
            // determine the currently selected locale from the request and fail-back
            // to the default locale if not set
            // if locale is missing in locale map, the default 'en' locale is used
            Locale reqLocale = request.getLocale();
            String locale = null != reqLocale ? reqLocale.getLanguage()
                : Locale.getDefault().getLanguage();
            if (!langMap.containsKey(locale))
            {
                locale = Locale.getDefault().getLanguage();
            }
            if (!langMap.containsKey(locale))
            {
                locale = "en"; //$NON-NLS-1$
            }

            pw.println("<div id='langSelect'>"); //$NON-NLS-1$
            pw.println(" <span>"); //$NON-NLS-1$
            printLocaleElement(pw, appRoot, locale, langMap.get(locale));
            pw.println(" </span>"); //$NON-NLS-1$
            pw.println(" <span class='flags ui-helper-hidden'>"); //$NON-NLS-1$
            for (Iterator li = langMap.keySet().iterator(); li.hasNext();)
            {
                // <img src="us.gif" alt="en" title="English"/>
                final Object l = li.next();
                if (!l.equals(locale))
                {
                    printLocaleElement(pw, appRoot, l, langMap.get(l));
                }
            }

            pw.println(" </span>"); //$NON-NLS-1$
            pw.println("</div>"); //$NON-NLS-1$
        }
    }


    protected void renderMenu( Map menuMap, String appRoot, PrintWriter pw )
    {
        if ( menuMap != null )
        {
            SortedMap categoryMap = sortMenuCategoryMap( menuMap, appRoot );
            pw.println( "<ul id=\"navmenu\">" );
            renderSubmenu( categoryMap, appRoot, pw, 0 );
            pw.println("<li class=\"logoutButton navMenuItem-0\">");
            pw.println("<a href=\"" + appRoot + "/logout\">${logout}</a>");
            pw.println("</li>");
            pw.println( "</ul>" );
        }
    }


    private void renderMenu( Map menuMap, String appRoot, PrintWriter pw, int level )
    {
        pw.println( "<ul class=\"navMenuLevel-" + level + "\">" );
        renderSubmenu( menuMap, appRoot, pw, level );
        pw.println( "</ul>" );
    }


    private void renderSubmenu( Map menuMap, String appRoot, PrintWriter pw, int level )
    {
        String liStyleClass = " class=\"navMenuItem-" + level + "\"";
        Iterator itr = menuMap.keySet().iterator();
        while ( itr.hasNext() )
        {
            String key = ( String ) itr.next();
            MenuItem menuItem = ( MenuItem ) menuMap.get( key );
            pw.println( "<li" + liStyleClass + ">" + menuItem.getLink() );
            Map subMenu = menuItem.getSubMenu();
            if ( subMenu != null )
            {
                renderMenu( subMenu, appRoot, pw, level + 1 );
            }
            pw.println( "</li>" );
        }
    }


    private static final void printLocaleElement( PrintWriter pw, String appRoot, Object langCode, Object langName )
    {
        pw.print("  <img src='"); //$NON-NLS-1$
        pw.print(appRoot);
        pw.print("/res/flags/"); //$NON-NLS-1$
        pw.print(langCode);
        pw.print(".gif' alt='"); //$NON-NLS-1$
        pw.print(langCode);
        pw.print("' title='"); //$NON-NLS-1$
        pw.print(langName);
        pw.println("'/>"); //$NON-NLS-1$
    }

    /**
     * This method is responsible for generating the footer of the page.
     *
     * @param pw the writer, where the HTML data is rendered
     * @see #startResponse(HttpServletRequest, HttpServletResponse)
     */
    protected void endResponse( PrintWriter pw )
    {
        pw.println(getFooter());
    }


    /**
     * An utility method, that is used to filter out simple parameter from file
     * parameter when multipart transfer encoding is used.
     *
     * This method processes the request and sets a request attribute
     * {@link #ATTR_FILEUPLOAD}. The attribute value is a {@link Map}
     * where the key is a String specifying the field name and the value
     * is a {@link org.apache.commons.fileupload.FileItem}.
     *
     * @param request the HTTP request coming from the user
     * @param name the name of the parameter
     * @return if not multipart transfer encoding is used - the value is the
     *  parameter value or <code>null</code> if not set. If multipart is used,
     *  and the specified parameter is field - then the value of the parameter
     *  is returned.
     * @deprecated use {@link WebConsoleUtil#getParameter(HttpServletRequest, String)}
     */
    public static final String getParameter( HttpServletRequest request, String name )
    {
        return WebConsoleUtil.getParameter(request, name);
    }

    /**
     * Utility method to handle relative redirects.
     * Some application servers like Web Sphere handle relative redirects differently
     * therefore we should make an absolute URL before invoking send redirect.
     *
     * @param request the HTTP request coming from the user
     * @param response the HTTP response, where data is rendered
     * @param redirectUrl the redirect URI.
     * @throws IOException If an input or output exception occurs
     * @throws IllegalStateException   If the response was committed or if a partial
     *  URL is given and cannot be converted into a valid URL
     * @deprecated use {@link WebConsoleUtil#sendRedirect(HttpServletRequest, HttpServletResponse, String)}
     */
    protected void sendRedirect(final HttpServletRequest request,
                                final HttpServletResponse response,
                                String redirectUrl) throws IOException {
        WebConsoleUtil.sendRedirect(request, response, redirectUrl);
    }

    /**
     * Returns the {@link BrandingPlugin} currently used for web console
     * branding.
     *
     * @return the brandingPlugin
     */
    public static BrandingPlugin getBrandingPlugin() {
        return AbstractWebConsolePlugin.brandingPlugin;
    }

    /**
     * Sets the {@link BrandingPlugin} to use globally by all extensions of
     * this class for branding.
     * <p>
     * Note: This method is intended to be used internally by the Web Console
     * to update the branding plugin to use.
     *
     * @param brandingPlugin the brandingPlugin to set
     */
    public static final void setBrandingPlugin(BrandingPlugin brandingPlugin) {
        if(brandingPlugin == null){
            AbstractWebConsolePlugin.brandingPlugin = DefaultBrandingPlugin.getInstance();
        } else {
            AbstractWebConsolePlugin.brandingPlugin = brandingPlugin;
        }
    }


    /**
     * Sets the log level to be applied for calls to the {@link #log(int, String)}
     * and {@link #log(int, String, Throwable)} methods.
     * <p>
     * Note: This method is intended to be used internally by the Web Console
     * to update the log level according to the Web Console configuration.
     *
     * @param logLevel the maximum allowed log level. If message is logged with
     *        lower level it will not be forwarded to the logger.
     */
    public static final void setLogLevel( int logLevel )
    {
        AbstractWebConsolePlugin.logLevel = logLevel;
    }


    private final String getHeader()
    {
        // MessageFormat pattern place holder
        //  0 main title (brand name)
        //  1 console plugin title
        //  2 application root path (ATTR_APP_ROOT)
        //  3 console plugin label (from the URI)
        //  4 branding favourite icon (BrandingPlugin.getFavIcon())
        //  5 branding main style sheet (BrandingPlugin.getMainStyleSheet())
        //  6 branding product URL (BrandingPlugin.getProductURL())
        //  7 branding product name (BrandingPlugin.getProductName())
        //  8 branding product image (BrandingPlugin.getProductImage())
        //  9 additional HTML code to be inserted into the <head> section
        //    (for example plugin provided CSS links)
        if ( HEADER == null )
        {
            HEADER = readTemplateFile( AbstractWebConsolePlugin.class, "/templates/main_header.html" ); //$NON-NLS-1$
        }
        return HEADER;
    }


    private final String getFooter()
    {
        if ( FOOTER == null )
        {
            FOOTER = readTemplateFile( AbstractWebConsolePlugin.class, "/templates/main_footer.html" ); //$NON-NLS-1$
        }
        return FOOTER;
    }

    /**
     * Reads the <code>templateFile</code> as a resource through the class
     * loader of this class converting the binary data into a string using
     * UTF-8 encoding.
     * <p>
     * If the template file cannot read into a string and an exception is
     * caused, the exception is logged and an empty string returned.
     *
     * @param templateFile The absolute path to the template file to read.
     * @return The contents of the template file as a string or and empty
     *      string if the template file fails to be read.
     *
     * @throws NullPointerException if <code>templateFile</code> is
     *      <code>null</code>
     * @throws RuntimeException if an <code>IOException</code> is thrown reading
     *      the template file into a string. The exception provides the
     *      exception thrown as its cause.
     */
    protected final String readTemplateFile( final String templateFile ) {
        return readTemplateFile( getClass(), templateFile );
    }

    private final String readTemplateFile( final Class clazz, final String templateFile)
    {
        InputStream templateStream = clazz.getResourceAsStream( templateFile );
        if ( templateStream != null )
        {
            try
            {
                String str = IOUtils.toString( templateStream, "UTF-8" ); //$NON-NLS-1$
                switch ( str.charAt(0) )
                { // skip BOM
                    case 0xFEFF: // UTF-16/UTF-32, big-endian
                    case 0xFFFE: // UTF-16, little-endian
                    case 0xEFBB: // UTF-8
                        return str.substring(1);
                }
                return str;
            }
            catch ( IOException e )
            {
                // don't use new Exception(message, cause) because cause is 1.4+
                throw new RuntimeException( "readTemplateFile: Error loading " + templateFile + ": " + e ); //$NON-NLS-1$ //$NON-NLS-2$
            }
            finally
            {
                IOUtils.closeQuietly( templateStream );
            }
        }

        // template file does not exist, return an empty string
        log( LogService.LOG_ERROR, "readTemplateFile: File '" + templateFile + "' not found through class " + clazz ); //$NON-NLS-1$ //$NON-NLS-2$
        return ""; //$NON-NLS-1$
    }


    private final String getCssLinks( final String appRoot )
    {
        // get the CSS references and return nothing if there are none
        final String[] cssRefs = getCssReferences();
        if ( cssRefs == null )
        {
            return ""; //$NON-NLS-1$
        }

        // build the CSS links from the references
        final StringBuffer buf = new StringBuffer();
        for ( int i = 0; i < cssRefs.length; i++ )
        {
            buf.append( "<link href='" ); //$NON-NLS-1$
            buf.append( toUrl( cssRefs[i], appRoot ) );
            buf.append( "' rel='stylesheet' type='text/css' />" ); //$NON-NLS-1$
        }

        return buf.toString();
    }


    /**
     * If the <code>url</code> starts with a slash, it is considered an absolute
     * path (relative URL) which must be prefixed with the Web Console
     * application root path. Otherwise the <code>url</code> is assumed to
     * either be a relative path or an absolute URL, both must not be prefixed.
     *
     * @param url The url path to optionally prefix with the application root
     *          path
     * @param appRoot The application root path to optionally put in front of
     *          the url.
     * @throws NullPointerException if <code>url</code> is <code>null</code>.
     */
    private static final String toUrl( final String url, final String appRoot )
    {
        if ( url.startsWith( "/" ) ) //$NON-NLS-1$
        {
            return appRoot + url;
        }
        return url;
    }


    private SortedMap sortMenuCategoryMap( Map map, String appRoot )
    {
        SortedMap sortedMap = new TreeMap( String.CASE_INSENSITIVE_ORDER );
        Iterator keys = map.keySet().iterator();
        while ( keys.hasNext() )
        {
            String key = ( String ) keys.next();
            if ( key.startsWith( "category." ) )
            {
                SortedMap categoryMap = sortMenuCategoryMap( ( Map ) map.get( key ), appRoot );
                String title = key.substring( key.indexOf( '.' ) + 1 );
                if ( sortedMap.containsKey( title ) )
                {
                    ( ( MenuItem ) sortedMap.get( title ) ).setSubMenu( categoryMap );
                }
                else
                {
                    String link = "<a href=\"#\">" + title + "</a>";
                    MenuItem menuItem = new MenuItem( link, categoryMap );
                    sortedMap.put( title, menuItem );
                }
            }
            else
            {
                String title = ( String ) map.get( key );
                String link = "<a href=\"" + appRoot + "/" + key + "\">" + title + "</a>";
                if ( sortedMap.containsKey( title ) )
                {
                    ( ( MenuItem ) sortedMap.get( title ) ).setLink( link );
                }
                else
                {
                    MenuItem menuItem = new MenuItem( link );
                    sortedMap.put( title, menuItem );
                }
            }

        }
        return sortedMap;
    }

    private static class MenuItem
    {
    private String link;
        private Map subMenu;


        public MenuItem( String link )
        {
            this.link = link;
        }


        public MenuItem( String link, Map subMenu )
        {
            super();
            this.link = link;
            this.subMenu = subMenu;
        }


        public String getLink()
        {
            return link;
        }


        public void setLink( String link )
        {
            this.link = link;
        }


        public Map getSubMenu()
        {
            return subMenu;
        }


        public void setSubMenu( Map subMenu )
        {
            this.subMenu = subMenu;
        }
    }
}
