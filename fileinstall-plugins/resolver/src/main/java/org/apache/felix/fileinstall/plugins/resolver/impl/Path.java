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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Path {

	public static final String SLASH = "/";
	public static final String GO_UP = "..";

	private final List<String> parts;

	private Path(List<String> parts) {
		this.parts = Collections.unmodifiableList(parts);
	}

	public static Path parse(String string) {
		String[] splits = string.split(SLASH);
		List<String> parts = new ArrayList<String>(splits.length);
		for (String split : splits) {
			if (split.length() > 0) {
                parts.add(split);
            }
		}
		return new Path(parts);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String part : this.parts) {
			if (!first) {
                builder.append(SLASH);
            }
			builder.append(part);
			first = false;
		}
		return builder.toString();
	}

	public Path append(String pathString) {
		return append(parse(pathString));
	}

	public Path append(Path path) {
		List<String> newParts = new ArrayList<String>(this.parts.size() + path.parts.size());
		newParts.addAll(this.parts);

		boolean atRoot = newParts.isEmpty();
		for (String part : path.parts) {
			if (GO_UP.equals(part)) {
				if (atRoot) {
					newParts.add(GO_UP);
				} else {
					newParts.remove(newParts.size() - 1);
					atRoot = newParts.isEmpty();
				}
			} else {
				newParts.add(part);
			}
		}

		return new Path(newParts);
	}

	public boolean isEmpty() {
		return this.parts.isEmpty();
	}

	public int count() {
		return this.parts.size();
	}

	public Path parent() {
		ArrayList<String> newParts = new ArrayList<String>(this.parts);
		if (newParts.isEmpty()) {
            newParts.add(GO_UP);
        } else {
            newParts.remove(newParts.size() - 1);
        }
		return new Path(newParts);
	}

	public String head() {
		if (this.parts.isEmpty()) {
            throw new IllegalArgumentException("Cannot get head of empty path");
        }
		return this.parts.get(0);
	}

	public Path tail() {
		if (this.parts.isEmpty()) {
            throw new IllegalArgumentException("Cannot get tail of empty path");
        }

		ArrayList<String> newParts = new ArrayList<String>(this.parts);
		newParts.remove(0);
		return new Path(newParts);
	}

}
