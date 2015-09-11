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

public class NativeLibraryClauseTest extends TestCase {
    public void testNormalizeOSName() {
        assertEquals("win32", NativeLibraryClause.normalizeOSName("win 32"));
        assertEquals("win32", NativeLibraryClause.normalizeOSName("Win*"));
        assertEquals("windows95", NativeLibraryClause.normalizeOSName("Windows 95"));
        assertEquals("windows98", NativeLibraryClause.normalizeOSName("Windows 98"));
        assertEquals("windowsnt", NativeLibraryClause.normalizeOSName("WinNT"));
        assertEquals("windows2000", NativeLibraryClause.normalizeOSName("Win2000"));
        assertEquals("windows2003", NativeLibraryClause.normalizeOSName("Win2003"));
        assertEquals("windowsserver2008", NativeLibraryClause.normalizeOSName("Windows Server 2008"));
        assertEquals("windowsserver2012", NativeLibraryClause.normalizeOSName("Windows Server 2012"));
        assertEquals("windowsxp", NativeLibraryClause.normalizeOSName("WinXP"));
        assertEquals("windowsce", NativeLibraryClause.normalizeOSName("WinCE"));
        assertEquals("windowsvista", NativeLibraryClause.normalizeOSName("WinVista"));
        assertEquals("windows7", NativeLibraryClause.normalizeOSName("Windows 7"));
        assertEquals("windows8", NativeLibraryClause.normalizeOSName("Win8"));
        assertEquals("windows10", NativeLibraryClause.normalizeOSName("Windows 10"));
        assertEquals("linux", NativeLibraryClause.normalizeOSName("Linux1.2.3"));
        assertEquals("aix", NativeLibraryClause.normalizeOSName("AIX-4.5.6"));
        assertEquals("digitalunix", NativeLibraryClause.normalizeOSName("digitalunix_blah"));
        assertEquals("hpux", NativeLibraryClause.normalizeOSName("HPUX-999"));
        assertEquals("irix", NativeLibraryClause.normalizeOSName("Irixxxx"));
        assertEquals("macosx", NativeLibraryClause.normalizeOSName("mac OS X"));
        assertEquals("netware", NativeLibraryClause.normalizeOSName("Netware"));
        assertEquals("openbsd", NativeLibraryClause.normalizeOSName("OpenBSD-0000"));
        assertEquals("netbsd", NativeLibraryClause.normalizeOSName("netbsd "));
        assertEquals("os2", NativeLibraryClause.normalizeOSName("os/2"));
        assertEquals("qnx", NativeLibraryClause.normalizeOSName("procnto"));
        assertEquals("solaris", NativeLibraryClause.normalizeOSName("Solaris 9"));
        assertEquals("sunos", NativeLibraryClause.normalizeOSName("SunOS8"));
        assertEquals("vxworks", NativeLibraryClause.normalizeOSName("VxWorks"));

        // Try all the already normalized names
        assertEquals("aix", NativeLibraryClause.normalizeOSName("aix"));
        assertEquals("digitalunix", NativeLibraryClause.normalizeOSName("digitalunix"));
        assertEquals("hpux", NativeLibraryClause.normalizeOSName("hpux"));
        assertEquals("irix", NativeLibraryClause.normalizeOSName("irix"));
        assertEquals("linux", NativeLibraryClause.normalizeOSName("linux"));
        assertEquals("macos", NativeLibraryClause.normalizeOSName("macos"));
        assertEquals("netbsd", NativeLibraryClause.normalizeOSName("netbsd"));
        assertEquals("netware", NativeLibraryClause.normalizeOSName("netware"));
        assertEquals("openbsd", NativeLibraryClause.normalizeOSName("openbsd"));
        assertEquals("os2", NativeLibraryClause.normalizeOSName("os2"));
        assertEquals("qnx", NativeLibraryClause.normalizeOSName("qnx"));
        assertEquals("solaris", NativeLibraryClause.normalizeOSName("solaris"));
        assertEquals("sunos", NativeLibraryClause.normalizeOSName("sunos"));
        assertEquals("vxworks", NativeLibraryClause.normalizeOSName("vxworks"));
        assertEquals("windows2000", NativeLibraryClause.normalizeOSName("windows2000"));
        assertEquals("windows2003", NativeLibraryClause.normalizeOSName("windows2003"));
        assertEquals("windows7", NativeLibraryClause.normalizeOSName("windows7"));
        assertEquals("windows8", NativeLibraryClause.normalizeOSName("windows8"));
        assertEquals("windows9", NativeLibraryClause.normalizeOSName("windows9"));
        assertEquals("windows10", NativeLibraryClause.normalizeOSName("windows10"));
        assertEquals("windows95", NativeLibraryClause.normalizeOSName("windows95"));
        assertEquals("windows98", NativeLibraryClause.normalizeOSName("windows98"));
        assertEquals("windowsce", NativeLibraryClause.normalizeOSName("windowsce"));
        assertEquals("windowsnt", NativeLibraryClause.normalizeOSName("windowsnt"));
        assertEquals("windowsserver2008", NativeLibraryClause.normalizeOSName("windowsserver2008"));
        assertEquals("windowsserver2012", NativeLibraryClause.normalizeOSName("windowsserver2012"));
        assertEquals("windowsvista", NativeLibraryClause.normalizeOSName("windowsvista"));
        assertEquals("windowsxp", NativeLibraryClause.normalizeOSName("windowsxp"));
        assertEquals("win32", NativeLibraryClause.normalizeOSName("win32"));
    }
    
    public void testgetOsNameWithAliases() {
        assertTrue(NativeLibraryClause.getOsNameWithAliases("win 32").contains("win32"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Win*").contains("win32"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Windows 95").contains("windows95"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Windows 98").contains("windows98"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("WinNT").contains("windowsnt"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Win2000").contains("windows2000"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Win2003").contains("windows2003"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Windows Server 2008").contains("windowsserver2008"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Windows Server 2012").contains("windowsserver2012"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("WinXP").contains("windowsxp"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("WinCE").contains("windowsce"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("WinVista").contains("windowsvista"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Windows 7").contains("windows7"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Windows7").contains("windows7"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Win8").contains("windows8"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Windows 10").contains("windows10"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Linux1.2.3").contains("linux"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("AIX-4.5.6").contains("aix"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("digitalunix_blah").contains("digitalunix"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("HPUX-999").contains("hpux"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Irixxxx").contains("irix"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("mac OS X").contains("mac os x"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Netware").contains("netware"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("OpenBSD-0000").contains("openbsd"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("netbsd ").contains("netbsd"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("os/2").contains("os2"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("procnto").contains("qnx"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("Solaris 9").contains("solaris"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("SunOS8").contains("sunos"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("VxWorks").contains("vxworks"));

        // Try all the already normalized names
        assertTrue(NativeLibraryClause.getOsNameWithAliases("aix").contains("aix"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("digitalunix").contains("digitalunix"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("hpux").contains("hpux"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("irix").contains("irix"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("linux").contains("linux"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("mac os").contains("mac os"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("netbsd").contains("netbsd"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("netware").contains("netware"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("openbsd").contains("openbsd"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("os2").contains("os2"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("qnx").contains("qnx"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("solaris").contains("solaris"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("sunos").contains("sunos"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("vxworks").contains("vxworks"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windows2000").contains("windows2000"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windows2003").contains("windows2003"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windows7").contains("windows7"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windows8").contains("windows8"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windows9").contains("windows9"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windows10").contains("windows10"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windows95").contains("windows95"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windows98").contains("windows98"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windowsce").contains("windowsce"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windowsnt").contains("windowsnt"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windowsserver2008").contains("windowsserver2008"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windowsserver2012").contains("windowsserver2012"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windowsvista").contains("windowsvista"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("windowsxp").contains("windowsxp"));
        assertTrue(NativeLibraryClause.getOsNameWithAliases("win32").contains("win32"));
    }
}
