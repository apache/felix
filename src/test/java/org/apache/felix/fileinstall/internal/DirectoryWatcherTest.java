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
package org.apache.felix.fileinstall.internal;


import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.felix.fileinstall.ArtifactListener;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.log.LogService;


/**
 * Test class for the DirectoryWatcher
 */
public class DirectoryWatcherTest extends TestCase
{

    private final static String TEST = "test.key";
    Map<String, String> props = new Hashtable<String, String>();
    DirectoryWatcher dw;
    BundleContext mockBundleContext;
    Bundle mockBundle;
    Bundle mockSysBundle;
    FrameworkStartLevel mockStartLevel;


    protected void setUp() throws Exception
    {
        super.setUp();
        IMocksControl ctrl = EasyMock.createControl();
        ctrl.makeThreadSafe(true);
        mockBundleContext = ctrl.createMock(BundleContext.class);
        mockBundle = EasyMock.createNiceMock(Bundle.class);
        mockSysBundle = EasyMock.createNiceMock(Bundle.class);
        mockStartLevel = EasyMock.createMock(FrameworkStartLevel.class);
        props.put( DirectoryWatcher.DIR, new File( "target/load" ).getAbsolutePath() );

        // Might get called, but most of the time it doesn't matter whether they do or don't.
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_DEFAULT))
                        .andStubReturn(null);
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_LEVEL))
                        .andStubReturn(null);
        EasyMock.expect(mockBundleContext.getServiceReference(LogService.class.getName()))
                        .andStubReturn(null);
        EasyMock.expect(mockBundleContext.getBundle()).andReturn(mockBundle).anyTimes();
        EasyMock.expect(mockBundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION)).andReturn(mockSysBundle).anyTimes();
        EasyMock.expect(mockSysBundle.getState()).andReturn(Bundle.ACTIVE).anyTimes();
        EasyMock.expect(mockSysBundle.adapt(FrameworkStartLevel.class)).andReturn(mockStartLevel).anyTimes();
        EasyMock.expect(mockStartLevel.getStartLevel()).andReturn(50).anyTimes();
    }


    public void testGetLongWithNonExistentProperty()
    {
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel);
        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
        assertEquals( "getLong gives the default value for non-existing properties", 100, dw.getLong( props, TEST, 100 ) );
        EasyMock.verify(mockBundleContext);
    }


    public void testGetLongWithExistentProperty()
    {
        props.put( TEST, "33" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel);
        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
        assertEquals( "getLong retrieves the right property value", 33, dw.getLong( props, TEST, 100 ) );
        EasyMock.verify(mockBundleContext);
    }


    public void testGetLongWithIncorrectValue()
    {
        props.put( TEST, "incorrect" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel);
        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
        assertEquals( "getLong retrieves the right property value", 100, dw.getLong( props, TEST, 100 ) );
        EasyMock.verify(mockBundleContext);
    }


    public void testGetBooleanWithNonExistentProperty()
    {
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel); 
        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
        assertEquals( "getBoolean gives the default value for non-existing properties", true, dw.getBoolean( props, TEST, true ) );
        EasyMock.verify(mockBundleContext);
    }


    public void testGetBooleanWithExistentProperty()
    {
        props.put( TEST, "true" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel); 
        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
        assertEquals( "getBoolean retrieves the right property value", true, dw.getBoolean( props, TEST, false ) );
        EasyMock.verify(mockBundleContext);
    }


    public void testGetBooleanWithIncorrectValue()
    {
        props.put( TEST, "incorrect" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel); 
        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
        assertEquals( "getBoolean retrieves the right property value", false, dw.getBoolean( props, TEST, true ) );
        EasyMock.verify(mockBundleContext);
    }


    public void testGetFileWithNonExistentProperty()
    {
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel); 
        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
        assertEquals( "getFile gives the default value for non-existing properties", new File("tmp"), dw.getFile( props, TEST, new File("tmp") ) );
        EasyMock.verify(mockBundleContext);
    }


    public void testGetFileWithExistentProperty()
    {
        props.put( TEST, "test" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel); 
        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
        assertEquals( "getBoolean retrieves the right property value", new File("test"), dw.getFile( props, TEST, new File("tmp") ) );
        EasyMock.verify(mockBundleContext);
    }


    public void testParameterAfterInitialization()
    {
        props.put( DirectoryWatcher.POLL, "500" );
        props.put( DirectoryWatcher.LOG_LEVEL, "1" );
        props.put( DirectoryWatcher.START_NEW_BUNDLES, "false" );
        props.put( DirectoryWatcher.DIR, new File( "src/test/resources" ).getAbsolutePath() );
        props.put( DirectoryWatcher.TMPDIR, new File( "src/test/resources" ).getAbsolutePath() );
        props.put( DirectoryWatcher.FILTER, ".*\\.cfg" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel); 

        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );

        assertEquals( "POLL parameter correctly read", 500l, dw.poll );
        assertEquals( "LOG_LEVEL parameter correctly read", 1, dw.logLevel );
        assertTrue("DIR parameter correctly read", dw.watchedDirectory.getAbsolutePath().endsWith(
                "src" + File.separatorChar + "test" + File.separatorChar + "resources"));
        assertTrue( "TMPDIR parameter correctly read", dw.tmpDir.getAbsolutePath().endsWith(
            "src" + File.separatorChar + "test" + File.separatorChar + "resources" ) );
        assertEquals("START_NEW_BUNDLES parameter correctly read", false, dw.startBundles);
        assertEquals( "FILTER parameter correctly read", ".*\\.cfg", dw.filter );
        EasyMock.verify(mockBundleContext);
    }


    public void testDefaultParametersAreSetAfterEmptyInitialization()
    {
        props.put( DirectoryWatcher.DIR, new File( "src/test/resources" ).getAbsolutePath() );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel); 

        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );

        assertTrue( "DIR parameter correctly read", dw.watchedDirectory.getAbsolutePath().endsWith(
            "src" + File.separatorChar + "test" + File.separatorChar + "resources" ) );
        assertEquals("Default POLL parameter correctly read", 2000l, dw.poll);
        assertEquals( "Default LOG_LEVEL parameter correctly read", 1, dw.logLevel );
        assertTrue("Default TMPDIR parameter correctly read", dw.tmpDir.getAbsolutePath().startsWith(
                new File(System.getProperty("java.io.tmpdir")).getAbsolutePath()));
        assertEquals("Default START_NEW_BUNDLES parameter correctly read", true, dw.startBundles);
        assertEquals( "Default FILTER parameter correctly read", null, dw.filter );
        EasyMock.verify(mockBundleContext);
    }


    public void testIsFragment() throws Exception
    {
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        BundleRevision mockBundleRevision = EasyMock.createNiceMock(BundleRevision.class);
        EasyMock.expect(mockBundle.adapt(BundleRevision.class)).andReturn(mockBundleRevision);
        EasyMock.expect(mockBundleRevision.getTypes())
                .andReturn(BundleRevision.TYPE_FRAGMENT);
       EasyMock.replay(mockBundleContext, mockBundle, mockBundleRevision, mockSysBundle, mockStartLevel);

        dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );

        assertTrue( "Fragment type correctly retrieved from Package Admin service", dw.isFragment( mockBundle ) );

        EasyMock.verify(mockBundleContext);
    }
    
    public void testInvalidTempDir() throws Exception
    {
        String oldTmpDir = System.getProperty("java.io.tmpdir");
        
        try 
        {
            File parent = new File("target/tmp");
            parent.mkdirs();
            parent.setWritable(false, false);
            File tmp = new File(parent, "tmp");
            System.setProperty("java.io.tmpdir", tmp.toString());

            mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
            EasyMock.expect(mockBundleContext.createFilter((String) EasyMock.anyObject()))
                    .andReturn(null);

            BundleRevision mockBundleRevision = EasyMock.createNiceMock(BundleRevision.class);
            EasyMock.expect(mockBundle.adapt(BundleRevision.class)).andReturn(mockBundleRevision);
            EasyMock.expect(mockBundleRevision.getTypes())
                    .andReturn(BundleRevision.TYPE_FRAGMENT);
            EasyMock.replay(mockBundleContext, mockBundle, mockBundleRevision, mockSysBundle, mockStartLevel);
    
            try
            {
                dw = new DirectoryWatcher( new FileInstall(), props, mockBundleContext );
                fail("Expected an IllegalStateException");
            } 
            catch (IllegalStateException e)
            {
                // expected
            }
        }
        finally
        {
            System.setProperty("java.io.tmpdir", oldTmpDir);
        }
    }

    /**
     * Test the {@link DirectoryWatcher#initializeCurrentManagedBundles()} in conjunction with a non opaque Bundle Location.
     * Assert that a new created {@link Artifact} will be added into the {@link DirectoryWatcher#currentManagedArtifacts}.
     * 
     * The {@link DirectoryWatcher#process(java.util.Set)} execution will not be called.
     * This test breaks the execution in {@link Scanner#initialize(java.util.Map)}.
     * @throws URISyntaxException 
     */
    public void testInitializeCurrentManagedBundlesNonOpaqueURIOnBundleLocation() throws URISyntaxException
    {
        final RuntimeException expectedException = new RuntimeException("expected exception to break execution on defined point.");
        final File watchedDirectoryFile = new File("src/test/resources/watched");
        final String watchedDirectoryPath = watchedDirectoryFile.getAbsolutePath();

        final String bundleFileName = "firstjar.jar";
        final File bundleFile = new File(watchedDirectoryPath,bundleFileName);
        final String bundleLocation = "file:"+watchedDirectoryPath+'/'+bundleFileName;

        // break execution
        final Scanner scanner = new Scanner(watchedDirectoryFile)
        {
            public void initialize(Map checksums)
            {
                throw expectedException;
            }
        };

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.expect(mockBundleContext.getBundles()).andReturn(new Bundle[]{mockBundle});
        EasyMock.expect(mockBundleContext.getDataFile((String) EasyMock.anyObject())).andReturn(null).anyTimes();
        EasyMock.expect(mockBundle.getLocation()).andReturn(bundleLocation).anyTimes();

        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel);

        props.put(DirectoryWatcher.DIR, watchedDirectoryPath);

        dw = new DirectoryWatcher(new FileInstall(), props, mockBundleContext);
        dw.noInitialDelay = true;
        dw.scanner = scanner;
        try {
            dw.start();
        }
        catch(RuntimeException e)
        {
            assertEquals(e, expectedException);
        }
        assertEquals(1, dw.currentManagedArtifacts.size());
        assertTrue(dw.currentManagedArtifacts.containsKey(bundleFile));

        EasyMock.verify(mockBundleContext, mockBundle);
    }

    /**
     * Test the {@link DirectoryWatcher#initializeCurrentManagedBundles()} in conjunction with a opaque Bundle Location.
     * Assert that a new created {@link Artifact} will be added into the {@link DirectoryWatcher#currentManagedArtifacts}.
     * 
     * The {@link DirectoryWatcher#process(java.util.Set)} execution will not be called.
     * This test breaks the execution in {@link Scanner#initialize(java.util.Map)}.
     * @throws URISyntaxException 
     */
    public void testInitializeCurrentManagedBundlesOpaqueURIOnBundleLocation() throws URISyntaxException
    {
        final RuntimeException expectedException = new RuntimeException("expected exception to break execution on defined point.");
        final File watchedDirectoryFile = new File("src/test/resources/watched");
        final String watchedDirectoryPath = watchedDirectoryFile.getAbsolutePath();

        final String bundleFileName = "firstjar.jar";
        final File bundleFile = new File(watchedDirectoryPath,bundleFileName);
        final String bundleLocation = "blueprint:file:"+watchedDirectoryPath+'/'+bundleFileName+"$Bundle-SymbolicName=foo&Bundle-Version=1.0";

        // break execution
        Scanner scanner = new Scanner(watchedDirectoryFile)
        {
            public void initialize(Map checksums)
            {
                throw expectedException;
            }
        };

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.expect(mockBundleContext.getBundles()).andReturn(new Bundle[]{mockBundle});
        EasyMock.expect(mockBundleContext.getDataFile((String) EasyMock.anyObject())).andReturn(null).anyTimes();
        EasyMock.expect(mockBundle.getLocation()).andReturn(bundleLocation).anyTimes();

        EasyMock.replay(mockBundleContext, mockBundle, mockSysBundle, mockStartLevel);

        props.put(DirectoryWatcher.DIR, watchedDirectoryPath);

        dw = new DirectoryWatcher(new FileInstall(), props, mockBundleContext);
        dw.noInitialDelay = true;
        dw.scanner = scanner;
        try {
        dw.start();
        }
        catch(RuntimeException e)
        {
            assertEquals(e, expectedException);
        }
        assertEquals(1, dw.currentManagedArtifacts.size());
        assertTrue(dw.currentManagedArtifacts.containsKey(bundleFile));

        EasyMock.verify(mockBundleContext, mockBundle);
    }

    /**
     * Test the {@link DirectoryWatcher#process(java.util.Set) } in conjunction with a opaque Bundle Location.
     * Assert that no bundle refresh will be called.
     * @throws URISyntaxException 
     */
    public void testProcessOpaqueURIOnBundleLocation() throws URISyntaxException
    {
        final RuntimeException expectedException = new RuntimeException("expected exception to break execution on defined point.");
        final File watchedDirectoryFile = new File("src/test/resources/watched");
        final String watchedDirectoryPath = watchedDirectoryFile.getAbsolutePath();

        final String bundleFileName = "firstjar.jar";
        final File bundleFile = new File(watchedDirectoryPath,bundleFileName);
        final String bundleLocation = "blueprint:file:"+watchedDirectoryPath+'/'+bundleFileName;

        final Scanner scanner = new Scanner(watchedDirectoryFile)
        {
            // bypass filesystem scan and return expected bundle file
            public Set<File> scan(boolean reportImmediately)
            {
                Set<File> fileSet = new HashSet<File>(1);
                fileSet.add(bundleFile);
                return fileSet;
            }
        };

        final ArtifactListener mockArtifactListener = EasyMock.createNiceMock(ArtifactListener.class);
        EasyMock.expect(mockArtifactListener.canHandle(bundleFile)).andReturn(Boolean.TRUE).anyTimes();
        final ServiceReference mockServiceReference = EasyMock.createNiceMock(ServiceReference.class);

        // simulate known/installed bundles
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        EasyMock.expect(mockBundleContext.getBundles()).andReturn(new Bundle[]{mockBundle});
        EasyMock.expect(mockBundleContext.getDataFile((String) EasyMock.anyObject())).andReturn(null).anyTimes();
        EasyMock.expect(mockBundle.getLocation()).andReturn(bundleLocation).anyTimes();

        EasyMock.replay(mockBundleContext, mockBundle,mockServiceReference, mockArtifactListener, mockSysBundle, mockStartLevel);

        final Artifact artifact = new Artifact();
        artifact.setBundleId(42);
        artifact.setChecksum(0);
        artifact.setListener(mockArtifactListener);
        artifact.setPath(bundleFile);

        FileInstall fileInstall = new FileInstall();
        fileInstall.listeners.put(mockServiceReference, mockArtifactListener);

        props.put(DirectoryWatcher.DIR, watchedDirectoryPath);

        dw = new DirectoryWatcher(fileInstall, props, mockBundleContext) {

            void refresh(Collection<Bundle> bundles) throws InterruptedException {
                Assert.fail("bundle refresh called");
            }
            
        };
        dw.noInitialDelay = true;
        // add expected bundle and artifact to the current managed artifacts
        dw.currentManagedArtifacts.put(bundleFile, artifact);
        dw.scanner = scanner;
        try {
            dw.start();
        }
        catch(RuntimeException e)
        {
            assertEquals(e, expectedException);
        }

        EasyMock.verify(mockBundleContext, mockBundle,mockServiceReference, mockArtifactListener);
    }

}
