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
package org.apache.felix.framework.util.manifestparser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;

public class ManifestParserTest extends TestCase {
    public void testIdentityCapabilityMinimal() throws BundleException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "foo.bar");
        ManifestParser mp = new ManifestParser(null, null, null, headers);

        BundleCapability ic = findCapability(mp.getCapabilities(), IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals("foo.bar", ic.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        assertEquals(IdentityNamespace.TYPE_BUNDLE, ic.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
        assertEquals(0, ic.getDirectives().size());
    }

    public void testIdentityCapabilityFull() throws BundleException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "abc;singleton:=true");
        headers.put(Constants.BUNDLE_VERSION, "1.2.3.something");
        String copyright = "(c) 2013 Apache Software Foundation";
        headers.put(Constants.BUNDLE_COPYRIGHT, copyright);
        String description = "A bundle description";
        headers.put(Constants.BUNDLE_DESCRIPTION, description);
        String docurl = "http://felix.apache.org/";
        headers.put(Constants.BUNDLE_DOCURL, docurl);
        String license = "http://www.apache.org/licenses/LICENSE-2.0";
        headers.put("Bundle-License", license);
        ManifestParser mp = new ManifestParser(null, null, null, headers);

        BundleCapability ic = findCapability(mp.getCapabilities(), IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals("abc", ic.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        assertEquals(new Version("1.2.3.something"), ic.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals(IdentityNamespace.TYPE_BUNDLE, ic.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
        assertEquals(copyright, ic.getAttributes().get(IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE));
        assertEquals(description, ic.getAttributes().get(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE));
        assertEquals(docurl, ic.getAttributes().get(IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE));
        assertEquals(license, ic.getAttributes().get(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE));

        assertEquals(1, ic.getDirectives().size());
        assertEquals("true", ic.getDirectives().get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE));
    }

    private BundleCapability findCapability(Collection<BundleCapability> capabilities, String namespace) {
        for (BundleCapability capability : capabilities) {
            if (namespace.equals(capability.getNamespace())) {
                return capability;
            }
        }
        return null;
    }
}
