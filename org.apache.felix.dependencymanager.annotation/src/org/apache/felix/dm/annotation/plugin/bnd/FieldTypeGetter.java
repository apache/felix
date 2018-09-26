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
package org.apache.felix.dm.annotation.plugin.bnd;

import aQute.bnd.osgi.Clazz.FieldDef;

/**
 * Class extracted from bndtools (aQute.bnd.component.AnnotationScanner), which
 * allows to determine class field collection type.
 * For example, given "private Collection<MyService> services;", this class allows to determine the
 * "MyService" type. 
 */
public class FieldTypeGetter {
	public enum FieldCollectionType {
		service, properties, reference, serviceobjects, tuple
	}

	static String determineFieldType(Logger log, FieldDef member) {
		String field = member.getName();
		String sig = member.getSignature();
		if (sig == null)
			// no generics, the descriptor will be the class name.
			sig = member.getDescriptor().toString();
		String[] sigs = sig.split("[<;>]");
		int sigLength = sigs.length;
		int index = 0;
		boolean isCollection = false;

		if ("Ljava/lang/Iterable".equals(sigs[index]) || "Ljava/util/Collection".equals(sigs[index]) || "Ljava/util/List".equals(sigs[index])) {
			index++;
			isCollection = true;
		}
		// Along with determining the FieldCollectionType, the following
		// code positions index to read the service type.
		FieldCollectionType fieldCollectionType = null;
		if (sufficientGenerics(index, sigLength, sig)) {
			if ("Lorg/osgi/framework/ServiceReference".equals(sigs[index])) {
				if (sufficientGenerics(index++, sigLength, sig)) {
					fieldCollectionType = FieldCollectionType.reference;
				}
			} else if ("Lorg/osgi/service/component/ComponentServiceObjects".equals(sigs[index])) {
				if (sufficientGenerics(index++, sigLength, sig)) {
					fieldCollectionType = FieldCollectionType.serviceobjects;
				}
			} else if ("Ljava/util/Map".equals(sigs[index])) {
				if (sufficientGenerics(index++, sigLength, sig)) {
					fieldCollectionType = FieldCollectionType.properties;
				}
			} else if ("Ljava/util/Map$Entry".equals(sigs[index]) && sufficientGenerics(index++ + 5, sigLength, sig)) {
				if ("Ljava/util/Map".equals(sigs[index++]) && "Ljava/lang/String".equals(sigs[index++])) {
					if ("Ljava/lang/Object".equals(sigs[index]) || "+Ljava/lang/Object".equals(sigs[index])) {
						fieldCollectionType = FieldCollectionType.tuple;
						index += 3; // ;>;
					} else if ("*".equals(sigs[index])) {
						fieldCollectionType = FieldCollectionType.tuple;
						index += 2; // >;
					} else {
						index = sigLength;// no idea what service might
											// be.
					}
				}
			} else {
				fieldCollectionType = FieldCollectionType.service;
			}
		}
		if (isCollection) {
			// def.fieldCollectionType = fieldCollectionType;
		}
		String annoService = null;
		if (annoService == null && index < sigs.length) {
			annoService = sigs[index].substring(1).replace('/', '.');
		}
		return annoService;
	}

	private static boolean sufficientGenerics(int index, int sigLength, String sig) {
		if (index + 1 > sigLength) {
			// analyzer.error(
			// "In component %s, method %s, signature: %s does not have sufficient generic
			// type information",
			// component.effectiveName(), def.name, sig);
			return false;
		}
		return true;
	}
}
