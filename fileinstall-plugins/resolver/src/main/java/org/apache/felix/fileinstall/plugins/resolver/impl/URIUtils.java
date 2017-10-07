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

import java.net.URI;

public final class URIUtils {

	private static final String JAR_URI_SCHEME = "jar";

	private URIUtils() {}

	/**
	 * Resolve a URI relative to a base URI.
	 *
	 * @param baseUri
	 *            The base URI which must be absolute.
	 * @param uri
	 *            The relative URI to resolve against the specified base. Note
	 *            that if this URI is absolute, it will be simply returned
	 *            unchanged.
	 * @return An absolute, resolved URI.
	 * @throws {@link IllegalArgumentException} If the {@code baseUri} parameter
	 *         is null/relative/opaque or if the URI otherwise cannot be
	 *         resolved.
	 */
	public static URI resolve(URI baseUri, URI uri) throws IllegalArgumentException {
		URI resolved;
		if (uri.isAbsolute()) {
            resolved = uri;
        } else {
			URI relative;
			String innerPath;

			// Work out if the relative URI contains a bang (!), meaning to navigate into a JAR
			String uriPath = uri.getPath();
			int bangIndex = uriPath.indexOf('!');
			if (bangIndex >= 0) {
				relative = URI.create(uriPath.substring(0, bangIndex));
				innerPath = uriPath.substring(bangIndex + 1);
			} else {
				relative = uri;
				innerPath = null;
			}

			// Do the normal resolution
			if (baseUri == null) {
                throw new IllegalArgumentException(String.format("Cannot resolve relative URI (%s): base URI is null/unavailable.", uri));
            }
			if (!baseUri.isAbsolute()) {
                throw new IllegalArgumentException(String.format("Cannot resolve relative URI (%s): base URI is also relative (%s).", uri, baseUri));
            }
			if (baseUri.isOpaque()) {
				// Handle "jar:" URIs as a special case
				if (JAR_URI_SCHEME.equals(baseUri.getScheme())) {
                    resolved = resolveJarUri(baseUri, relative);
                } else {
                    throw new IllegalArgumentException(String.format("Cannot resolve relative URI (%s): base URI is opaque (%s).", uri, baseUri));
                }
			} else {
				resolved = baseUri.resolve(relative);
			}

			// If an inner path was indicated, create a jar: URI
			if (innerPath != null) {
				resolved = URI.create("jar:" + resolved.toString() + "!" + innerPath);
			}
		}
		return resolved;
	}

	private static URI resolveJarUri(URI baseUri, URI uri) throws IllegalArgumentException {
		assert JAR_URI_SCHEME.equals(baseUri.getScheme()) : "not a jar: URI";

		String ssp = baseUri.getSchemeSpecificPart();
		int bangIndex = ssp.lastIndexOf('!');
		if (bangIndex == -1) {
            throw new IllegalArgumentException("Invalid URI in the jar: scheme (missing ! separator): " + baseUri);
        }
		URI jarUri = URI.create(ssp.substring(0, bangIndex));
		String pathStr = ssp.substring(bangIndex + 1);

		String query = baseUri.getQuery();
		if (query == null) {
			int qmIndex = pathStr.lastIndexOf('?');
			if (qmIndex >= 0) {
				query = pathStr.substring(qmIndex + 1);
				pathStr = pathStr.substring(0, qmIndex);
			} else {
				query = "";
			}
		}
		boolean nonavigate = query.indexOf("navigate=false") >= 0;

		Path basePath = Path.parse(pathStr);
		Path baseDir = basePath.parent();
		Path resolvedPath = baseDir.append(uri.getPath());

		URI result;
		if (resolvedPath.isEmpty()) {
			result = jarUri;
		} else if (Path.GO_UP.equals(resolvedPath.head())) {
			if (nonavigate) {
                throw new IllegalArgumentException("Cannot navigate outside JAR contents.");
            }
			result = jarUri.resolve(resolvedPath.tail().toString());
		} else {
			result = URI.create(JAR_URI_SCHEME + ":" + jarUri + "!/" + resolvedPath);
		}

		return result;
	}

	/**
	 * Returns the file name of the resource references by the URI. Works with jar: URIs, which normally return
	 * {@code null} if the {@link URI#getPath()} call is used.
	 * @param uri
	 * @return The filename, if it can be calculated.
	 * @throws IllegalArgumentException if the filename cannot be calculated (e.g. if the scheme is not understood).
	 */
	public static String getFileName(URI uri) throws IllegalArgumentException {
		String filename;
		if (JAR_URI_SCHEME.equals(uri.getScheme())) {
			String ssp = uri.getRawSchemeSpecificPart();
			String innerPath;
			int bang = ssp.indexOf('!');
			if (bang < 0) {
                throw new IllegalArgumentException("Invalid jar content URI; missing '!/' path separator: " + uri);
            } else {
                innerPath = ssp.substring(bang + 1);
            }
			filename = getPathLastSegment(innerPath);
		} else {
			String path = uri.getPath();
			if (path == null) {
                throw new IllegalArgumentException("Cannot calculate filename for unknown opaque URI scheme: " + uri);
            }
			filename = getPathLastSegment(path);
		}
		return filename;
	}

	private static String getPathLastSegment(String path) {
		int slash = path.lastIndexOf('/');
		if (slash < 0) {
            return path;
        }

		return path.substring(slash + 1);
	}

}
