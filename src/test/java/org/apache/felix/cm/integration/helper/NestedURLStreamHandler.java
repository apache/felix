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
package org.apache.felix.cm.integration.helper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

public class NestedURLStreamHandler extends AbstractURLStreamHandlerService implements URLStreamHandlerService {

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        return new NestedURLConnection( u );
    }
    
    public static class NestedURLConnection extends URLConnection {

        protected NestedURLConnection( URL url ) {
            super( url );
        }

        @Override
        public void connect() throws IOException {
            
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream( getURL().getFile() );
        }
    }

    @Override
    public String toExternalForm( final URL u ) {
        // This is necessary, because we want to force a permission check

    	try {
	        String property = System.getProperty("file.separator");
	    	
	    	if(property != null) {
	    		System.out.println( "File Separator is: " + property );
	    	}
    	} catch (SecurityException se) {
    		System.out.println( "Forbidden to check the File Separator." );
    	}

        return super.toExternalForm( u );
    }

}
