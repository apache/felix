/*
 * Copyright (c) OSGi Alliance (2012, 2017). All Rights Reserved.
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

package org.osgi.service.http.runtime.dto;


/**
 * Represents a {@code javax.servlet.Servlet} currently being used by a servlet
 * context.
 * 
 * @NotThreadSafe
 * @author $Id$
 */
public class ServletDTO extends BaseServletDTO {
	/**
	 * The request mappings for the servlet.
	 * <p>
	 * The specified patterns are used to determine whether a request is mapped
	 * to the servlet. This array is never {@code null}. It might be empty for
	 * named servlets.
	 */
	public String[]				patterns;

	/**
	 * Specifies whether multipart support is enabled.
	 * @since 1.1
	 */
	public boolean				multipartEnabled;

	/**
	 * Specifies the size threshold after which the file will be written to
	 * disk. If multipart is not enabled for this servlet, {@code 0} is
	 * returned.
	 * 
	 * @since 1.1
	 * @see #multipartEnabled
	 */
	public int					multipartFileSizeThreshold;

	/**
	 * Specifies the location where the files can be stored on disk. If
	 * multipart is not enabled for this servlet, {@code null} is returned.
	 * 
	 * @since 1.1
	 * @see #multipartEnabled
	 */
	public String				multipartLocation;

	/**
	 * Specifies the maximum size of a file being uploaded. If multipart is not
	 * enabled for this servlet, {@code 0} is returned.
	 * 
	 * @since 1.1
	 * @see #multipartEnabled
	 */
	public long					multipartMaxFileSize;

	/**
	 * Specifies the maximum request size. If multipart is not enabled for this
	 * servlet, {@code 0} is returned.
	 * 
	 * @since 1.1
	 * @see #multipartEnabled
	 */
	public long					multipartMaxRequestSize;
}
