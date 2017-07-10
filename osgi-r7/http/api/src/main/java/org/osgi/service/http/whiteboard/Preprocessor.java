/*
 * Copyright (c) OSGi Alliance (2000, 2017). All Rights Reserved.
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

package org.osgi.service.http.whiteboard;

import javax.servlet.Filter;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Services registered as a {@code Preprocessor} using a whiteboard pattern are
 * executed for every request before the dispatching is performed.
 * <p>
 * If there are several services of this type, they are run in order of their
 * service ranking, the one with the highest ranking is used first. In the case
 * of a service ranking tie, the service with the lowest service id is processed
 * first.
 * <p>
 * The preprocessor is handled in the same way as filters. When a preprocessor
 * is put into service {@link Filter#init(javax.servlet.FilterConfig)} is
 * called, when it is not used anymore {@link Filter#destroy()} is called. As
 * these preprocessors are run before dispatching and therefore the targeted
 * servlet context is not known yet,
 * {@link javax.servlet.FilterConfig#getServletContext()} returns the servlet
 * context of the backing implementation. The same context is returned by the
 * request object. The context path is the context path of this underlying
 * servlet context. The passed in chain can be used to invoke the next
 * preprocessor in the chain, or if the end of that chain is reached to start
 * dispatching of the request. A preprocessor might decide to terminate the
 * processing and directly generate a response.
 * <p>
 * Service properties with the prefix
 * {@code HttpWhiteboardConstants#HTTP_WHITEBOARD_PREPROCESSOR_INIT_PARAM_PREFIX}
 * are passed as init parameters to this service.
 * 
 * @ThreadSafe
 * @author $Id$
 * @since 1.1
 */
@ConsumerType
public interface Preprocessor extends Filter {

	// this interface is a marker interface
}
