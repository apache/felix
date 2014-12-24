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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import junit.framework.TestCase;

import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class ManifestParserTest extends TestCase
{
    public void testIdentityCapabilityMinimal() throws BundleException
    {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "foo.bar");
        ManifestParser mp = new ManifestParser(null, null, null, headers);

        BundleCapability ic = findCapability(mp.getCapabilities(), IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals("foo.bar", ic.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        assertEquals(IdentityNamespace.TYPE_BUNDLE, ic.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
        assertEquals(0, ic.getDirectives().size());
    }

    public void testIdentityCapabilityFull() throws BundleException
    {
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
    
    @SuppressWarnings("unchecked")
	public void testNativeCapability() throws BundleException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.BUNDLE_MANIFESTVERSION,  "2");
        headers.put(Constants.BUNDLE_SYMBOLICNAME, FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME);
        headers.put(Constants.PROVIDE_CAPABILITY, " osgi.native;" +
        		"osgi.native.osname:List<String>=\"Windows7,Windows 7,Win7,Win32\";"+
        		"osgi.native.osversion:Version=\"7.0\";"+
        		"osgi.native.processor:List<String>=\"x86-64,amd64,em64t,x86_64\";"+
        		"osgi.native.language=\"en\"");
        BundleRevision mockBundleRevision = mock(BundleRevision.class);
        when(mockBundleRevision.getSymbolicName()).thenReturn(FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME);
    	
        ManifestParser mp = new ManifestParser(null, null, mockBundleRevision, headers);

        BundleCapability ic = findCapability(mp.getCapabilities(), NativeNamespace.NATIVE_NAMESPACE);
    	
        assertEquals("en", ic.getAttributes().get(NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE));
        List<String> osList = (List<String>) ic.getAttributes().get(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE);
        assertEquals(4, osList.size());
        assertEquals(new Version("7.0"), ic.getAttributes().get(NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE));
        List<String> nativeProcesserList = (List<String>) ic.getAttributes().get(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE);
        assertEquals(4, nativeProcesserList.size());
    
    }
    
    public void testConvertNativeCode() throws InvalidSyntaxException
    {
        List<NativeLibraryClause> nativeLibraryClauses = new ArrayList<NativeLibraryClause>();
        String[] libraryFiles = {"lib/http.dll", "lib/zlib.dll"};
        String[] osNames = {"Windows95", "Windows98", "WindowsNT"};
        String[] processors = {"x86"};
        String[] osVersions = null;
        String[] languages = {"en", "se"};
        String selectionFilter = "(com.acme.windowing=win32)";
        NativeLibraryClause clause = new NativeLibraryClause(libraryFiles, osNames, processors, osVersions, languages, selectionFilter);
        BundleRevision owner = mock(BundleRevision.class);
        nativeLibraryClauses.add(clause);
        
        List<BundleRequirement> nativeBundleReq = ManifestParser.convertNativeCode(owner, nativeLibraryClauses, false);
        
        BundleRequirement ir = findRequirement(nativeBundleReq, NativeNamespace.NATIVE_NAMESPACE);
        
        String filterStr = (String)ir.getDirectives().get(NativeNamespace.REQUIREMENT_FILTER_DIRECTIVE);
        
        Filter actualFilter = FrameworkUtil.createFilter(filterStr);
        
        Filter expectedFilter = FrameworkUtil.createFilter("(&(|" + 
                "(osgi.native.osname~=windows95)(osgi.native.osname~=windows98)(osgi.native.osname~=windowsnt)" +
                ")" + 
                "(osgi.native.processor~=x86)" + 
                "(|(osgi.native.language~=en)" + 
                "(osgi.native.language~=se)" + 
                ")"+
                "(com.acme.windowing=win32))");
        assertEquals("Filter Should contain native requirements", expectedFilter, actualFilter);
        
    }

    private BundleCapability findCapability(Collection<BundleCapability> capabilities, String namespace)
    {
        for (BundleCapability capability : capabilities)
        {
            if (namespace.equals(capability.getNamespace()))
            {
                return capability;
            }
        }
        return null;
    }
    
    private BundleRequirement findRequirement(Collection<BundleRequirement> requirements, String namespace)
    {
        for(BundleRequirement requirement: requirements)
        {
            if(namespace.equals(requirement.getNamespace()))
            {
                return requirement;
            }
        }
        return null;
    }
}
