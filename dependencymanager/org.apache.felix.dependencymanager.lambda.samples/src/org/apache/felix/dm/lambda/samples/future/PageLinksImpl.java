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
package org.apache.felix.dm.lambda.samples.future;

import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.felix.dm.Component;
import org.osgi.service.log.LogService;

/**
 * Provides all hrefs found from a given web page.
 */
public class PageLinksImpl implements PageLinks {
	private LogService m_log;
	private final static String HREF_PATTERN = "<a\\s+href\\s*=\\s*(\"[^\"]*\"|[^\\s>]*)\\s*>";
	private List<String> m_links; // web page hrefs (links).
    private String m_url;

	PageLinksImpl(String url) {
	    m_url = url;
	}
	
	void bind(LogService log) {
		m_log = log;
	}
	
	void init(Component c) {
	    // asynchronously download the content of the URL specified in the constructor.
	    CompletableFuture<List<String>> futureLinks = CompletableFuture.supplyAsync(() -> download(m_url)) 
	        .thenApply(this::parseLinks);	       

	    // Add the future dependency so we'll be started once the CompletableFuture "futureLinks" has completed.
	    component(c, comp -> comp.withFuture(futureLinks, future -> future.complete(this::setLinks)));
	}
	
	// Called when our future has completed.
    void setLinks(List<String> links) {
        m_links = links;
    }
    
	// once our future has completed, our component is started.
	void start() {
		m_log.log(LogService.LOG_WARNING, "Service starting: number of links found from Felix web site: " + m_links.size());
	}
	
	@Override
	public List<String> getLinks() {
		return m_links;
	}

	public static String download(String url) { 
	    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) { 
	        return buffer.lines().collect(Collectors.joining("\n"));
	    } catch (IOException ex) {
	        throw new RuntimeException(ex);
	    }
	}
	
	private List<String> parseLinks(String content) {		 
		Pattern pattern = Pattern.compile(HREF_PATTERN, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(content);
		List<String> result = new ArrayList<>();
		while (matcher.find())
			result.add(matcher.group(1));
		return result;
	}
}
