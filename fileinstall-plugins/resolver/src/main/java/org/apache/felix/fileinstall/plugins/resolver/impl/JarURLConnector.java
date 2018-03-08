/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.resolver.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

import aQute.bnd.service.url.TaggedData;
import aQute.bnd.service.url.URLConnector;

public class JarURLConnector implements URLConnector {

	@Override
	public TaggedData connectTagged(URL url) throws Exception {
		URLConnection connection = url.openConnection();
		if (connection instanceof JarURLConnection) {
            connection.setUseCaches(false);
        }
		return new TaggedData(connection, connection.getInputStream());
	}

	@Override
	public InputStream connect(URL url) throws IOException, Exception {
		return connectTagged(url).getInputStream();
	}

	@Override
	public TaggedData connectTagged(URL url, String tag) throws Exception {
		return connectTagged(url);
	}

}
