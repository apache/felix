/*
<<<<<<< HEAD
 * Copyright (c) OSGi Alliance (2002, 2012). All Rights Reserved.
=======
 * Copyright (c) OSGi Alliance (2002, 2013). All Rights Reserved.
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.url;

<<<<<<< HEAD
import java.net.*;
=======
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.osgi.annotation.versioning.ConsumerType;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

/**
 * Abstract implementation of the {@code URLStreamHandlerService} interface. All
 * the methods simply invoke the corresponding methods on
 * {@code java.net.URLStreamHandler} except for {@code parseURL} and
 * {@code setURL}, which use the {@code URLStreamHandlerSetter} parameter.
 * Subclasses of this abstract class should not need to override the
 * {@code setURL} and {@code parseURL(URLStreamHandlerSetter,...)} methods.
 * 
 * @ThreadSafe
<<<<<<< HEAD
 * @version $Id: b86572a4f13b7bb4a343ac4d6b6fb3487e01bd31 $
 */
=======
 * @author $Id: 79cfc45d97b037436d50bda26111109bd5d42a37 $
 */
@ConsumerType
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
public abstract class AbstractURLStreamHandlerService extends URLStreamHandler implements URLStreamHandlerService {
	/**
	 * @see "java.net.URLStreamHandler.openConnection"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	public abstract URLConnection openConnection(URL u) throws java.io.IOException;

	/**
	 * The {@code URLStreamHandlerSetter} object passed to the parseURL method.
	 */
	protected volatile URLStreamHandlerSetter	realHandler;

	/**
	 * Parse a URL using the {@code URLStreamHandlerSetter} object. This method
	 * sets the {@code realHandler} field with the specified
	 * {@code URLStreamHandlerSetter} object and then calls
	 * {@code parseURL(URL,String,int,int)}.
	 * 
	 * @param realHandler The object on which the {@code setURL} method must be
	 *        invoked for the specified URL.
	 * @see "java.net.URLStreamHandler.parseURL"
	 */
<<<<<<< HEAD
	public void parseURL(URLStreamHandlerSetter realHandler, URL u, String spec, int start, int limit) {
=======
	public void parseURL(@SuppressWarnings("hiding") URLStreamHandlerSetter realHandler, URL u, String spec, int start, int limit) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
		this.realHandler = realHandler;
		parseURL(u, spec, start, limit);
	}

	/**
	 * This method calls {@code super.toExternalForm}.
	 * 
	 * @see "java.net.URLStreamHandler.toExternalForm"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	public String toExternalForm(URL u) {
		return super.toExternalForm(u);
	}

	/**
	 * This method calls {@code super.equals(URL,URL)}.
	 * 
	 * @see "java.net.URLStreamHandler.equals(URL,URL)"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	public boolean equals(URL u1, URL u2) {
		return super.equals(u1, u2);
	}

	/**
	 * This method calls {@code super.getDefaultPort}.
	 * 
	 * @see "java.net.URLStreamHandler.getDefaultPort"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	public int getDefaultPort() {
		return super.getDefaultPort();
	}

	/**
	 * This method calls {@code super.getHostAddress}.
	 * 
	 * @see "java.net.URLStreamHandler.getHostAddress"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	public InetAddress getHostAddress(URL u) {
		return super.getHostAddress(u);
	}

	/**
	 * This method calls {@code super.hashCode(URL)}.
	 * 
	 * @see "java.net.URLStreamHandler.hashCode(URL)"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	public int hashCode(URL u) {
		return super.hashCode(u);
	}

	/**
	 * This method calls {@code super.hostsEqual}.
	 * 
	 * @see "java.net.URLStreamHandler.hostsEqual"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	public boolean hostsEqual(URL u1, URL u2) {
		return super.hostsEqual(u1, u2);
	}

	/**
	 * This method calls {@code super.sameFile}.
	 * 
	 * @see "java.net.URLStreamHandler.sameFile"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	public boolean sameFile(URL u1, URL u2) {
		return super.sameFile(u1, u2);
	}

	/**
	 * This method calls
	 * {@code realHandler.setURL(URL,String,String,int,String,String)}.
	 * 
	 * @see "java.net.URLStreamHandler.setURL(URL,String,String,int,String,String)"
	 * @deprecated This method is only for compatibility with handlers written
	 *             for JDK 1.1.
	 */
<<<<<<< HEAD
=======
	@SuppressWarnings("javadoc")
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	protected void setURL(URL u, String proto, String host, int port, String file, String ref) {
		realHandler.setURL(u, proto, host, port, file, ref);
	}

	/**
	 * This method calls
	 * {@code realHandler.setURL(URL,String,String,int,String,String,String,String)}
	 * .
	 * 
	 * @see "java.net.URLStreamHandler.setURL(URL,String,String,int,String,String,String,String)"
	 */
<<<<<<< HEAD
=======
	@Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
	protected void setURL(URL u, String proto, String host, int port, String auth, String user, String path, String query, String ref) {
		realHandler.setURL(u, proto, host, port, auth, user, path, query, ref);
	}
}
