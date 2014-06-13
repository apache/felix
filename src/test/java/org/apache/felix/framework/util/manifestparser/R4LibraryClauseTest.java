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

import junit.framework.TestCase;

public class R4LibraryClauseTest extends TestCase {
    public void testNormalizeOSName() {
        assertEquals("win32", R4LibraryClause.normalizeOSName("win 32"));
        assertEquals("win32", R4LibraryClause.normalizeOSName("Win*"));
        assertEquals("windows95", R4LibraryClause.normalizeOSName("Windows 95"));
        assertEquals("windows98", R4LibraryClause.normalizeOSName("Windows 98"));
        assertEquals("windowsnt", R4LibraryClause.normalizeOSName("WinNT"));
        assertEquals("windows2000", R4LibraryClause.normalizeOSName("Win2000"));
        assertEquals("windows2003", R4LibraryClause.normalizeOSName("Win2003"));
        assertEquals("windowsserver2008", R4LibraryClause.normalizeOSName("Windows Server 2008"));
        assertEquals("windowsserver2012", R4LibraryClause.normalizeOSName("Windows Server 2012"));
        assertEquals("windowsxp", R4LibraryClause.normalizeOSName("WinXP"));
        assertEquals("windowsce", R4LibraryClause.normalizeOSName("WinCE"));
        assertEquals("windowsvista", R4LibraryClause.normalizeOSName("WinVista"));
        assertEquals("windows7", R4LibraryClause.normalizeOSName("Windows 7"));
        assertEquals("windows8", R4LibraryClause.normalizeOSName("Win8"));
        assertEquals("linux", R4LibraryClause.normalizeOSName("Linux1.2.3"));
        assertEquals("aix", R4LibraryClause.normalizeOSName("AIX-4.5.6"));
        assertEquals("digitalunix", R4LibraryClause.normalizeOSName("digitalunix_blah"));
        assertEquals("hpux", R4LibraryClause.normalizeOSName("HPUX-999"));
        assertEquals("irix", R4LibraryClause.normalizeOSName("Irixxxx"));
        assertEquals("macos", R4LibraryClause.normalizeOSName("mac OS X"));
        assertEquals("netware", R4LibraryClause.normalizeOSName("Netware"));
        assertEquals("openbsd", R4LibraryClause.normalizeOSName("OpenBSD-0000"));
        assertEquals("netbsd", R4LibraryClause.normalizeOSName("netbsd "));
        assertEquals("os2", R4LibraryClause.normalizeOSName("os/2"));
        assertEquals("qnx", R4LibraryClause.normalizeOSName("procnto"));
        assertEquals("solaris", R4LibraryClause.normalizeOSName("Solaris 9"));
        assertEquals("sunos", R4LibraryClause.normalizeOSName("SunOS8"));
        assertEquals("vxworks", R4LibraryClause.normalizeOSName("VxWorks"));

        // Try all the already normalized names
        assertEquals("aix", R4LibraryClause.normalizeOSName("aix"));
        assertEquals("digitalunix", R4LibraryClause.normalizeOSName("digitalunix"));
        assertEquals("hpux", R4LibraryClause.normalizeOSName("hpux"));
        assertEquals("irix", R4LibraryClause.normalizeOSName("irix"));
        assertEquals("linux", R4LibraryClause.normalizeOSName("linux"));
        assertEquals("macos", R4LibraryClause.normalizeOSName("macos"));
        assertEquals("netbsd", R4LibraryClause.normalizeOSName("netbsd"));
        assertEquals("netware", R4LibraryClause.normalizeOSName("netware"));
        assertEquals("openbsd", R4LibraryClause.normalizeOSName("openbsd"));
        assertEquals("os2", R4LibraryClause.normalizeOSName("os2"));
        assertEquals("qnx", R4LibraryClause.normalizeOSName("qnx"));
        assertEquals("solaris", R4LibraryClause.normalizeOSName("solaris"));
        assertEquals("sunos", R4LibraryClause.normalizeOSName("sunos"));
        assertEquals("vxworks", R4LibraryClause.normalizeOSName("vxworks"));
        assertEquals("windows2000", R4LibraryClause.normalizeOSName("windows2000"));
        assertEquals("windows2003", R4LibraryClause.normalizeOSName("windows2003"));
        assertEquals("windows7", R4LibraryClause.normalizeOSName("windows7"));
        assertEquals("windows8", R4LibraryClause.normalizeOSName("windows8"));
        assertEquals("windows9", R4LibraryClause.normalizeOSName("windows9"));
        assertEquals("windows95", R4LibraryClause.normalizeOSName("windows95"));
        assertEquals("windows98", R4LibraryClause.normalizeOSName("windows98"));
        assertEquals("windowsce", R4LibraryClause.normalizeOSName("windowsce"));
        assertEquals("windowsnt", R4LibraryClause.normalizeOSName("windowsnt"));
        assertEquals("windowsserver2008", R4LibraryClause.normalizeOSName("windowsserver2008"));
        assertEquals("windowsserver2012", R4LibraryClause.normalizeOSName("windowsserver2012"));
        assertEquals("windowsvista", R4LibraryClause.normalizeOSName("windowsvista"));
        assertEquals("windowsxp", R4LibraryClause.normalizeOSName("windowsxp"));
        assertEquals("win32", R4LibraryClause.normalizeOSName("win32"));
    }
}
