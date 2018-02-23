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
package org.apache.felix.framework.cache;

import java.io.*;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.WeakZipFileFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * <p>
 * This class is a logical abstraction for a bundle archive. This class,
 * combined with <tt>BundleCache</tt> and concrete <tt>BundleRevision</tt>
 * subclasses, implement the bundle cache for Felix. The bundle archive
 * abstracts the actual bundle content into revisions and the revisions
 * provide access to the actual bundle content. When a bundle is
 * installed it has one revision associated with its content. Updating a
 * bundle adds another revision for the updated content. Any number of
 * revisions can be associated with a bundle archive. When the bundle
 * (or framework) is refreshed, then all old revisions are purged and only
 * the most recent revision is maintained.
 * </p>
 * <p>
 * The content associated with a revision can come in many forms, such as
 * a standard JAR file or an exploded bundle directory. The bundle archive
 * is responsible for creating all revision instances during invocations
 * of the <tt>revise()</tt> method call. Internally, it determines the
 * concrete type of revision type by examining the location string as an
 * URL. Currently, it supports standard JAR files, referenced JAR files,
 * and referenced directories. Examples of each type of URL are, respectively:
 * </p>
 * <ul>
 *   <li><tt>http://www.foo.com/bundle.jar</tt></li>
 *   <li><tt>reference:file:/foo/bundle.jar</tt></li>
 *   <li><tt>reference:file:/foo/bundle/</tt></li>
 * </ul>
 * <p>
 * The "<tt>reference:</tt>" notation signifies that the resource should be
 * used "in place", meaning that they will not be copied. For referenced JAR
 * files, some resources may still be copied, such as embedded JAR files or
 * native libraries, but for referenced exploded bundle directories, nothing
 * will be copied. Currently, reference URLs can only refer to "file:" targets.
 * </p>
 * @see org.apache.felix.framework.cache.BundleCache
 * @see org.apache.felix.framework.cache.BundleRevision
**/
public class BundleArchive
{
    public static final transient String FILE_PROTOCOL = "file:";
    public static final transient String REFERENCE_PROTOCOL = "reference:";
    public static final transient String INPUTSTREAM_PROTOCOL = "inputstream:";

    private static final transient String BUNDLE_INFO_FILE = "bundle.info";
    private static final transient String REVISION_LOCATION_FILE = "revision.location";
    private static final transient String REVISION_DIRECTORY = "version";
    private static final transient String DATA_DIRECTORY = "data";

    private final Logger m_logger;
    private final Map m_configMap;
    private final WeakZipFileFactory m_zipFactory;
    private final File m_archiveRootDir;
    private final boolean m_isSingleBundleFile;

    private long m_id = -1;
    private String m_originalLocation = null;
    private int m_persistentState = -1;
    private int m_startLevel = -1;
    private long m_lastModified = -1;

    /**
     * The refresh count field is used when generating the bundle revision
     * directory name where native libraries are extracted. This is necessary
     * because Sun's JVM requires a one-to-one mapping between native libraries
     * and class loaders where the native library is uniquely identified by its
     * absolute path in the file system. This constraint creates a problem when
     * a bundle is refreshed, because it gets a new class loader. Using the
     * refresh counter to generate the name of the bundle revision directory
     * resolves this problem because each time bundle is refresh, the native
     * library will have a unique name. As a result of the unique name, the JVM
     * will then reload the native library without a problem.
    **/
    private long m_refreshCount = -1;

    // Maps a Long revision number to a BundleRevision.
    private final SortedMap<Long, BundleArchiveRevision> m_revisions
        = new TreeMap<Long, BundleArchiveRevision>();

    /**
     * <p>
     * This constructor is used for creating new archives when a bundle is
     * installed into the framework. Each archive receives a logger, a root
     * directory, its associated bundle identifier, the associated bundle
     * location string, and an input stream from which to read the bundle
     * content. The root directory is where any required state can be
     * stored. The input stream may be null, in which case the location is
     * used as an URL to the bundle content.
     * </p>
     * @param logger the logger to be used by the archive.
     * @param archiveRootDir the archive root directory for storing state.
     * @param id the bundle identifier associated with the archive.
     * @param location the bundle location string associated with the archive.
     * @param is input stream from which to read the bundle content.
     * @throws Exception if any error occurs.
    **/
    public BundleArchive(Logger logger, Map configMap, WeakZipFileFactory zipFactory,
        File archiveRootDir, long id, int startLevel, String location, InputStream is)
        throws Exception
    {
        m_logger = logger;
        m_configMap = configMap;
        m_zipFactory = zipFactory;
        m_archiveRootDir = archiveRootDir;
        m_id = id;
        if (m_id <= 0)
        {
            throw new IllegalArgumentException(
                "Bundle ID cannot be less than or equal to zero.");
        }
        m_originalLocation = location;
        m_persistentState = Bundle.INSTALLED;
        m_startLevel = startLevel;
        m_lastModified = System.currentTimeMillis();
        m_refreshCount = 0;

        String s = (String) m_configMap.get(BundleCache.CACHE_SINGLEBUNDLEFILE_PROP);
        m_isSingleBundleFile = ((s == null) || s.equalsIgnoreCase("true")) ? true : false;

        // Save state.
        initialize();

        // Add a revision for the content.
        reviseInternal(false, new Long(0), m_originalLocation, is);
    }

    /**
     * <p>
     * This constructor is called when an archive for a bundle is being
     * reconstructed when the framework is restarted. Each archive receives
     * a logger, a root directory, and its associated bundle identifier.
     * The root directory is where any required state can be stored.
     * </p>
     * @param logger the logger to be used by the archive.
     * @param archiveRootDir the archive root directory for storing state.
     * @param configMap configMap for BundleArchive
     * @throws Exception if any error occurs.
    **/
    public BundleArchive(Logger logger, Map configMap, WeakZipFileFactory zipFactory,
        File archiveRootDir)
        throws Exception
    {
        m_logger = logger;
        m_configMap = configMap;
        m_zipFactory = zipFactory;
        m_archiveRootDir = archiveRootDir;

        String s = (String) m_configMap.get(BundleCache.CACHE_SINGLEBUNDLEFILE_PROP);
        m_isSingleBundleFile = ((s == null) || s.equalsIgnoreCase("true")) ? true : false;

        if (m_isSingleBundleFile)
        {
            readBundleInfo();
        }

        // Add a revision number for each revision that exists in the file
        // system. The file system might contain more than one revision if
        // the bundle was updated in a previous session, but the framework
        // was not refreshed; this might happen if the framework did not
        // exit cleanly. We must add the existing revisions so that
        // they can be properly purged.

        // Find the existing revision directories, which will be named like:
        //     "${REVISION_DIRECTORY)${refresh-count}.${revision-number}"
        File[] children = m_archiveRootDir.listFiles();
        for (File child : children)
        {
            if (child.getName().startsWith(REVISION_DIRECTORY)
                && child.isDirectory())
            {
                // Determine the revision number and add it to the revision map.
                int idx = child.getName().lastIndexOf('.');
                if (idx > 0)
                {
                    Long revNum = Long.decode(child.getName().substring(idx + 1));
                    m_revisions.put(revNum, null);
                }
            }
        }

        if (m_revisions.isEmpty())
        {
            throw new Exception(
                "No valid revisions in bundle archive directory: "
                + archiveRootDir);
        }

        // Remove the last revision number since the call to reviseInternal()
        // will properly add the most recent bundle revision.
        // NOTE: We do not actually need to add a real revision object for the
        // older revisions since they will be purged immediately on framework
        // startup.
        Long currentRevNum = m_revisions.lastKey();
        m_revisions.remove(currentRevNum);

        // Add the revision object for the most recent revision.
        reviseInternal(true, currentRevNum, getRevisionLocation(currentRevNum), null);
    }

    /**
     * <p>
     * Returns the bundle identifier associated with this archive.
     * </p>
     * @return the bundle identifier associated with this archive.
     * @throws Exception if any error occurs.
    **/
    public synchronized long getId() throws Exception
    {
        if (m_id <= 0)
        {
            m_id = readId();
        }
        return m_id;
    }

    /**
     * <p>
     * Returns the location string associated with this archive.
     * </p>
     * @return the location string associated with this archive.
     * @throws Exception if any error occurs.
    **/
    public synchronized String getLocation() throws Exception
    {
        if (m_originalLocation == null)
        {
            m_originalLocation = readLocation();
        }
        return m_originalLocation;
    }

    /**
     * <p>
     * Returns the persistent state of this archive. The value returned is
     * one of the following: <tt>Bundle.INSTALLED</tt>, <tt>Bundle.ACTIVE</tt>,
     * or <tt>Bundle.UNINSTALLED</tt>.
     * </p>
     * @return the persistent state of this archive.
     * @throws Exception if any error occurs.
    **/
    public synchronized int getPersistentState() throws Exception
    {
        if (m_persistentState < 0)
        {
            m_persistentState = readPersistentState();
        }
        return m_persistentState;
    }

    /**
     * <p>
     * Sets the persistent state of this archive. The value is
     * one of the following: <tt>Bundle.INSTALLED</tt>, <tt>Bundle.ACTIVE</tt>,
     * or <tt>Bundle.UNINSTALLED</tt>.
     * </p>
     * @param state the persistent state value to set for this archive.
     * @throws Exception if any error occurs.
    **/
    public synchronized void setPersistentState(int state) throws Exception
    {
        if (m_persistentState != state)
        {
            m_persistentState = state;
            if (m_isSingleBundleFile)
            {
                writeBundleInfo();
            }
            else
            {
                writePersistentState();
            }
        }
    }

    /**
     * <p>
     * Returns the start level of this archive.
     * </p>
     * @return the start level of this archive.
     * @throws Exception if any error occurs.
    **/
    public synchronized int getStartLevel() throws Exception
    {
        if (m_startLevel < 0)
        {
            m_startLevel = readStartLevel();
        }
        return m_startLevel;
    }

    /**
     * <p>
     * Sets the the start level of this archive this archive.
     * </p>
     * @param level the start level to set for this archive.
     * @throws Exception if any error occurs.
    **/
    public synchronized void setStartLevel(int level) throws Exception
    {
        if (m_startLevel != level)
        {
            m_startLevel = level;
            if (m_isSingleBundleFile)
            {
                writeBundleInfo();
            }
            else
            {
                writeStartLevel();
            }
        }
    }

    /**
     * <p>
     * Returns the last modification time of this archive.
     * </p>
     * @return the last modification time of this archive.
     * @throws Exception if any error occurs.
    **/
    public synchronized long getLastModified() throws Exception
    {
        if (m_lastModified < 0)
        {
            m_lastModified = readLastModified();
        }
        return m_lastModified;
    }

    /**
     * <p>
     * Sets the the last modification time of this archive.
     * </p>
     * @param lastModified The time of the last modification to set for
     *      this archive. According to the OSGi specification this time is
     *      set each time a bundle is installed, updated or uninstalled.
     *
     * @throws Exception if any error occurs.
    **/
    public synchronized void setLastModified(long lastModified) throws Exception
    {
        if (m_lastModified != lastModified)
        {
            m_lastModified = lastModified;
            if (m_isSingleBundleFile)
            {
                writeBundleInfo();
            }
            else
            {
                writeLastModified();
            }
        }
    }

    /**
     * This utility method is used to retrieve the current refresh
     * counter value for the bundle. This value is used when generating
     * the bundle revision directory name where native libraries are extracted.
     * This is necessary because Sun's JVM requires a one-to-one mapping
     * between native libraries and class loaders where the native library
     * is uniquely identified by its absolute path in the file system. This
     * constraint creates a problem when a bundle is refreshed, because it
     * gets a new class loader. Using the refresh counter to generate the name
     * of the bundle revision directory resolves this problem because each time
     * bundle is refresh, the native library will have a unique name.
     * As a result of the unique name, the JVM will then reload the
     * native library without a problem.
    **/
    private long getRefreshCount() throws Exception
    {
        // If the refresh counter is not yet initialized, do so now.
        if (m_refreshCount < 0)
        {
            m_refreshCount = readRefreshCount();
        }

        return m_refreshCount;
    }

    /**
     * This utility method is used to retrieve the current refresh
     * counter value for the bundle. This value is used when generating
     * the bundle revision directory name where native libraries are extracted.
     * This is necessary because Sun's JVM requires a one-to-one mapping
     * between native libraries and class loaders where the native library
     * is uniquely identified by its absolute path in the file system. This
     * constraint creates a problem when a bundle is refreshed, because it
     * gets a new class loader. Using the refresh counter to generate the name
     * of the bundle revision directory resolves this problem because each time
     * bundle is refresh, the native library will have a unique name.
     * As a result of the unique name, the JVM will then reload the
     * native library without a problem.
    **/
    private void setRefreshCount(long count)
        throws Exception
    {
        if (m_refreshCount != count)
        {
            m_refreshCount = count;
            if (m_isSingleBundleFile)
            {
                writeBundleInfo();
            }
            else
            {
                writeRefreshCount();
            }
        }
    }

    /**
     * <p>
     * Returns a <tt>File</tt> object corresponding to the data file
     * of the relative path of the specified string.
     * </p>
     * @return a <tt>File</tt> object corresponding to the specified file name.
     * @throws Exception if any error occurs.
    **/
    public synchronized File getDataFile(String fileName) throws Exception
    {
        // Do some sanity checking.
        if ((fileName.length() > 0) && (fileName.charAt(0) == File.separatorChar))
        {
            throw new IllegalArgumentException(
                "The data file path must be relative, not absolute.");
        }
        else if (fileName.indexOf("..") >= 0)
        {
            throw new IllegalArgumentException(
                "The data file path cannot contain a reference to the \"..\" directory.");
        }

        // Get bundle data directory.
        File dataDir = new File(m_archiveRootDir, DATA_DIRECTORY);
        // Create the data directory if necessary.
        if (!BundleCache.getSecureAction().fileExists(dataDir))
        {
            if (!BundleCache.getSecureAction().mkdir(dataDir))
            {
                throw new IOException("Unable to create bundle data directory.");
            }
        }

        // Return the data file.
        return new File(dataDir, fileName);
    }

    /**
     * <p>
     * Returns the current revision object for the archive.
     * </p>
     * @return the current revision object for the archive.
    **/
    public synchronized Long getCurrentRevisionNumber()
    {
        return (m_revisions.isEmpty()) ? null : m_revisions.lastKey();
    }

    /**
     * <p>
     * Returns the current revision object for the archive.
     * </p>
     * @return the current revision object for the archive.
    **/
    public synchronized BundleArchiveRevision getCurrentRevision()
    {
        return (m_revisions.isEmpty()) ? null : m_revisions.get(m_revisions.lastKey());
    }

    public synchronized boolean isRemovalPending()
    {
        return (m_revisions.size() > 1);
    }

    /**
     * <p>
     * This method adds a revision to the archive using the associated
     * location and input stream. If the input stream is null, then the
     * location is used a URL to obtain an input stream.
     * </p>
     * @param location the location string associated with the revision.
     * @param is the input stream from which to read the revision.
     * @throws Exception if any error occurs.
    **/
    public synchronized void revise(String location, InputStream is)
        throws Exception
    {
        Long revNum = (m_revisions.isEmpty())
            ? new Long(0)
            : new Long(m_revisions.lastKey().longValue() + 1);

        reviseInternal(false, revNum, location, is);
    }

    /**
     * Actually adds a revision to the bundle archive. This method is also
     * used to reload cached bundles too. The revision is given the specified
     * revision number and is read from the input stream if supplied or from
     * the location URL if not.
     * @param isReload if the bundle is being reloaded or not.
     * @param revNum the revision number of the revision.
     * @param location the location associated with the revision.
     * @param is the input stream from which to read the revision.
     * @throws Exception if any error occurs.
     */
    private void reviseInternal(
        boolean isReload, Long revNum, String location, InputStream is)
        throws Exception
    {
        // If we have an input stream, then we have to use it
        // no matter what the update location is, so just ignore
        // the update location and set the location to be input
        // stream.
        if (is != null)
        {
            location = "inputstream:";
        }

        // Create a bundle revision for revision number.
        BundleArchiveRevision revision = createRevisionFromLocation(location, is, revNum);
        if (revision == null)
        {
            throw new Exception("Unable to revise archive.");
        }

        if (!isReload)
        {
            setRevisionLocation(location, revNum);
        }

        // Add new revision to revision map.
        m_revisions.put(revNum, revision);
    }

    /**
     * <p>
     * This method undoes the previous revision to the archive; this method will
     * remove the latest revision from the archive. This method is only called
     * when there are problems during an update after the revision has been
     * created, such as errors in the update bundle's manifest. This method
     * can only be called if there is more than one revision, otherwise there
     * is nothing to undo.
     * </p>
     * @return true if the undo was a success false if there is no previous revision
     * @throws Exception if any error occurs.
     */
    public synchronized boolean rollbackRevise() throws Exception
    {
        // Can only undo the revision if there is more than one.
        if (m_revisions.size() <= 1)
        {
            return false;
        }

        Long revNum = m_revisions.lastKey();
        BundleArchiveRevision revision = m_revisions.remove(revNum);

        try
        {
            revision.close();
        }
        catch(Exception ex)
        {
           m_logger.log(Logger.LOG_ERROR, getClass().getName() +
               ": Unable to dispose latest revision", ex);
        }

        File revisionDir = new File(m_archiveRootDir, REVISION_DIRECTORY +
            getRefreshCount() + "." + revNum.toString());

        if (BundleCache.getSecureAction().fileExists(revisionDir))
        {
            BundleCache.deleteDirectoryTree(revisionDir);
        }

        return true;
    }

    private synchronized String getRevisionLocation(Long revNum) throws Exception
    {
        InputStream is = null;
        BufferedReader br = null;
        try
        {
            is = BundleCache.getSecureAction().getFileInputStream(new File(
                new File(m_archiveRootDir, REVISION_DIRECTORY +
                getRefreshCount() + "." + revNum.toString()), REVISION_LOCATION_FILE));

            br = new BufferedReader(new InputStreamReader(is));
            return br.readLine();
        }
        finally
        {
            if (br != null) br.close();
            if (is != null) is.close();
        }
    }

    private synchronized void setRevisionLocation(String location, Long revNum)
        throws Exception
    {
        // Save current revision location.
        OutputStream os = null;
        BufferedWriter bw = null;
        try
        {
            os = BundleCache.getSecureAction()
                .getFileOutputStream(new File(
                    new File(m_archiveRootDir, REVISION_DIRECTORY +
                    getRefreshCount() + "." + revNum.toString()), REVISION_LOCATION_FILE));
            bw = new BufferedWriter(new OutputStreamWriter(os));
            bw.write(location, 0, location.length());
        }
        finally
        {
            if (bw != null) bw.close();
            if (os != null) os.close();
        }
    }

    public synchronized void close()
    {
        // Get the current revision count.
        for (BundleArchiveRevision revision : m_revisions.values())
        {
            // Dispose of the revision, but this might be null in certain
            // circumstances, such as if this bundle archive was created
            // for an existing bundle that was updated, but not refreshed
            // due to a system crash; see the constructor code for details.
            if (revision != null)
            {
                try
                {
                    revision.close();
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                            "Unable to close revision - "
                            + revision.getRevisionRootDir(), ex);
                }
            }
        }
    }

    /**
     * <p>
     * This method closes any revisions and deletes the bundle archive directory.
     * </p>
     * @throws Exception if any error occurs.
    **/
    public synchronized void closeAndDelete()
    {
        // Close the revisions and delete the archive directory.
        close();
        if (!BundleCache.deleteDirectoryTree(m_archiveRootDir))
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "Unable to delete archive directory - " + m_archiveRootDir);
        }
    }

    /**
     * <p>
     * This method removes all old revisions associated with the archive
     * and keeps only the current revision.
     * </p>
     * @throws Exception if any error occurs.
    **/
    public synchronized void purge() throws Exception
    {
        // Remember current revision number.
        Long currentRevNum = getCurrentRevisionNumber();

        // Record whether the current revision has native libraries, which
        // we'll use later to determine if we need to rename its directory.
        boolean hasNativeLibs = getCurrentRevision().getManifestHeader()
            .containsKey(Constants.BUNDLE_NATIVECODE);

        // Close all revisions and then delete all but the current revision.
        // We don't delete it the current revision, because we want to rename it
        // to the new refresh level.
        close();

        // Delete all old revisions.
        long refreshCount = getRefreshCount();
        for (Long revNum : m_revisions.keySet())
        {
            if (!revNum.equals(currentRevNum))
            {
                File revisionDir = new File(
                    m_archiveRootDir,
                    REVISION_DIRECTORY + refreshCount + "." + revNum.toString());
                if (BundleCache.getSecureAction().fileExists(revisionDir))
                {
                    BundleCache.deleteDirectoryTree(revisionDir);
                }
            }
        }

        // If the revision has native libraries, then rename its directory
        // to avoid the issue of being unable to load the same native library
        // into two different class loaders.
        if (hasNativeLibs)
        {
            // Increment the refresh count.
            setRefreshCount(refreshCount + 1);

            // Rename the current revision directory to the new refresh level.
            File currentDir = new File(m_archiveRootDir,
                REVISION_DIRECTORY + (refreshCount + 1) + "." + currentRevNum.toString());
            File revisionDir = new File(m_archiveRootDir,
                REVISION_DIRECTORY + refreshCount + "." + currentRevNum.toString());
            BundleCache.getSecureAction().renameFile(revisionDir, currentDir);
        }

        // Clear the revision map since they are all invalid now.
        m_revisions.clear();

        // Recreate the revision for the current location.
        BundleArchiveRevision revision = createRevisionFromLocation(
            getRevisionLocation(currentRevNum), null, currentRevNum);
        // Add new revision to the revision map.
        m_revisions.put(currentRevNum, revision);
    }

    /**
     * <p>
     * Initializes the bundle archive object by creating the archive
     * root directory and saving the initial state.
     * </p>
     * @throws Exception if any error occurs.
    **/
    private void initialize() throws Exception
    {
        OutputStream os = null;
        BufferedWriter bw = null;

        try
        {
            // If the archive directory exists, then we don't
            // need to initialize since it has already been done.
            if (BundleCache.getSecureAction().fileExists(m_archiveRootDir))
            {
                return;
            }

            // Create archive directory, if it does not exist.
            if (!BundleCache.getSecureAction().mkdir(m_archiveRootDir))
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    getClass().getName() + ": Unable to create archive directory.");
                throw new IOException("Unable to create archive directory.");
            }

            if (m_isSingleBundleFile)
            {
                writeBundleInfo();
            }
            else
            {
                writeId();
                writeLocation();
                writePersistentState();
                writeStartLevel();
                writeLastModified();
            }
        }
        finally
        {
            if (bw != null) bw.close();
            if (os != null) os.close();
        }
    }

    /**
     * <p>
     * Creates a revision based on the location string and/or input stream.
     * </p>
     * @return the location string associated with this archive.
    **/
    private BundleArchiveRevision createRevisionFromLocation(
        String location, InputStream is, Long revNum)
        throws Exception
    {
        // The revision directory is named using the refresh count and
        // the revision number. The revision number is an increasing
        // counter of the number of times the bundle was revised.
        // The refresh count is necessary due to how native libraries
        // are handled in Java; needless to say, every time a bundle is
        // refreshed we must change the name of its native libraries so
        // that we can reload them. Thus, we use the refresh counter as
        // a way to change the name of the revision directory to give
        // native libraries new absolute names.
        File revisionRootDir = new File(m_archiveRootDir,
            REVISION_DIRECTORY + getRefreshCount() + "." + revNum.toString());

        BundleArchiveRevision result = null;

        try
        {
            // Check if the location string represents a reference URL.
            if ((location != null) && location.startsWith(REFERENCE_PROTOCOL))
            {
                // Reference URLs only support the file protocol.
                location = location.substring(REFERENCE_PROTOCOL.length());
                if (!location.startsWith(FILE_PROTOCOL))
                {
                    throw new IOException("Reference URLs can only be files: " + location);
                }

                // Decode any URL escaped sequences.
                location = decode(location);

                // Make sure the referenced file exists.
                File file = new File(location.substring(FILE_PROTOCOL.length()));
                if (!BundleCache.getSecureAction().fileExists(file))
                {
                    throw new IOException("Referenced file does not exist: " + file);
                }

                // If the referenced file is a directory, then create a directory
                // revision; otherwise, create a JAR revision with the reference
                // flag set to true.
                if (BundleCache.getSecureAction().isFileDirectory(file))
                {
                    result = new DirectoryRevision(m_logger, m_configMap,
                        m_zipFactory, revisionRootDir, location);
                }
                else
                {
                    result = new JarRevision(m_logger, m_configMap,
                        m_zipFactory, revisionRootDir, location, true, null);
                }
            }
            else if (location.startsWith(INPUTSTREAM_PROTOCOL))
            {
                // Assume all input streams point to JAR files.
                result = new JarRevision(m_logger, m_configMap,
                    m_zipFactory, revisionRootDir, location, false, is);
            }
            else
            {
                // Anything else is assumed to be a URL to a JAR file.
                result = new JarRevision(m_logger, m_configMap,
                    m_zipFactory, revisionRootDir, location, false, null);
            }
        }
        catch (Exception ex)
        {
            if (BundleCache.getSecureAction().fileExists(revisionRootDir))
            {
                if (!BundleCache.deleteDirectoryTree(revisionRootDir))
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        getClass().getName()
                            + ": Unable to delete revision directory - "
                            + revisionRootDir);
                }
            }
            throw ex;
        }

        return result;
    }

    // Method from Harmony java.net.URIEncoderDecoder (luni subproject)
    // used by URI to decode uri components.
    private static String decode(String s) throws UnsupportedEncodingException
    {
        StringBuffer result = new StringBuffer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < s.length();)
        {
            char c = s.charAt(i);
            if (c == '%')
            {
                out.reset();
                do
                {
                    if ((i + 2) >= s.length())
                    {
                        throw new IllegalArgumentException(
                            "Incomplete % sequence at: " + i);
                    }
                    int d1 = Character.digit(s.charAt(i + 1), 16);
                    int d2 = Character.digit(s.charAt(i + 2), 16);
                    if ((d1 == -1) || (d2 == -1))
                    {
                        throw new IllegalArgumentException("Invalid % sequence ("
                            + s.substring(i, i + 3)
                            + ") at: " + String.valueOf(i));
                    }
                    out.write((byte) ((d1 << 4) + d2));
                    i += 3;
                }
                while ((i < s.length()) && (s.charAt(i) == '%'));
                result.append(out.toString("UTF-8"));
                continue;
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

    private void readBundleInfo() throws Exception
    {
        File infoFile = new File(m_archiveRootDir, BUNDLE_INFO_FILE);

        // Read the bundle start level.
        InputStream is = null;
        BufferedReader br= null;
        try
        {
            is = BundleCache.getSecureAction()
                .getFileInputStream(infoFile);
            br = new BufferedReader(new InputStreamReader(is));

            // Read id.
            m_id = Long.parseLong(br.readLine());
            // Read location.
            m_originalLocation = br.readLine();
            // Read state.
            m_persistentState = Integer.parseInt(br.readLine());
            // Read start level.
            m_startLevel = Integer.parseInt(br.readLine());
            // Read last modified.
            m_lastModified = Long.parseLong(br.readLine());
            // Read refresh count.
            m_refreshCount = Long.parseLong(br.readLine());
        }
        catch (FileNotFoundException ex)
        {
            // If there wasn't an info file, then maybe this is an old-style
            // bundle cache, so try to read the files individually. We can
            // delete this eventually.
            m_id = readId();
            m_originalLocation = readLocation();
            m_persistentState = readPersistentState();
            m_startLevel = readStartLevel();
            m_lastModified = readLastModified();
            m_refreshCount = readRefreshCount();
        }
        finally
        {
            if (br != null) br.close();
            if (is != null) is.close();
        }
    }

    private void writeBundleInfo() throws Exception
    {
        // Write the bundle start level.
        OutputStream os = null;
        BufferedWriter bw = null;
        try
        {
            os = BundleCache.getSecureAction()
                .getFileOutputStream(new File(m_archiveRootDir, BUNDLE_INFO_FILE));
            bw = new BufferedWriter(new OutputStreamWriter(os));

            // Write id.
            String s = Long.toString(m_id);
            bw.write(s, 0, s.length());
            bw.newLine();
            // Write location.
            s = (m_originalLocation == null) ? "" : m_originalLocation;
            bw.write(s, 0, s.length());
            bw.newLine();
            // Write state.
            s = Integer.toString(m_persistentState);
            bw.write(s, 0, s.length());
            bw.newLine();
            // Write start level.
            s = Integer.toString(m_startLevel);
            bw.write(s, 0, s.length());
            bw.newLine();
            // Write last modified.
            s = Long.toString(m_lastModified);
            bw.write(s, 0, s.length());
            bw.newLine();
            // Write refresh count.
            s = Long.toString(m_refreshCount);
            bw.write(s, 0, s.length());
            bw.newLine();
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                getClass().getName() + ": Unable to cache bundle info - " + ex);
            throw ex;
        }
        finally
        {
            if (bw != null) bw.close();
            if (os != null) os.close();
        }
    }

    //
    // Deprecated bundle cache format to be deleted eventually.
    //

    private static final transient String BUNDLE_ID_FILE = "bundle.id";
    private static final transient String BUNDLE_LOCATION_FILE = "bundle.location";
    private static final transient String BUNDLE_STATE_FILE = "bundle.state";
    private static final transient String BUNDLE_START_LEVEL_FILE = "bundle.startlevel";
    private static final transient String BUNDLE_LASTMODIFIED_FILE = "bundle.lastmodified";
    private static final transient String REFRESH_COUNTER_FILE = "refresh.counter";

    private void writeId() throws Exception
    {
        OutputStream os = BundleCache.getSecureAction()
            .getFileOutputStream(new File(m_archiveRootDir, BUNDLE_ID_FILE));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write(Long.toString(m_id), 0, Long.toString(m_id).length());
        bw.close();
        os.close();
    }

    private long readId() throws Exception
    {
        long id;

        InputStream is = null;
        BufferedReader br = null;
        try
        {
            is = BundleCache.getSecureAction()
                .getFileInputStream(new File(m_archiveRootDir, BUNDLE_ID_FILE));
            br = new BufferedReader(new InputStreamReader(is));
            id = Long.parseLong(br.readLine());
        }
        catch (FileNotFoundException ex)
        {
            // HACK: Get the bundle identifier from the archive root directory
            // name, which is of the form "bundle<id>" where <id> is the bundle
            // identifier numbers. This is a hack to deal with old archives that
            // did not save their bundle identifier, but instead had it passed
            // into them. Eventually, this can be removed.
            id = Long.parseLong(
                m_archiveRootDir.getName().substring(
                    BundleCache.BUNDLE_DIR_PREFIX.length()));
        }
        finally
        {
            if (br != null) br.close();
            if (is != null) is.close();
        }

        return id;
    }

    private void writeLocation() throws Exception
    {
        OutputStream os = BundleCache.getSecureAction()
            .getFileOutputStream(new File(m_archiveRootDir, BUNDLE_LOCATION_FILE));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write(m_originalLocation, 0, m_originalLocation.length());
        bw.close();
        os.close();
    }

    private String readLocation() throws Exception
    {
        InputStream is = null;
        BufferedReader br = null;
        try
        {
            is = BundleCache.getSecureAction()
                .getFileInputStream(new File(m_archiveRootDir, BUNDLE_LOCATION_FILE));
            br = new BufferedReader(new InputStreamReader(is));
            return br.readLine();
        }
        finally
        {
            if (br != null) br.close();
            if (is != null) is.close();
        }
    }

    private static final transient String ACTIVE_STATE = "active";
    private static final transient String STARTING_STATE = "starting";
    private static final transient String INSTALLED_STATE = "installed";
    private static final transient String UNINSTALLED_STATE = "uninstalled";

    private void writePersistentState() throws Exception
    {
        OutputStream os = null;
        BufferedWriter bw = null;
        try
        {
            os = BundleCache.getSecureAction()
                .getFileOutputStream(new File(m_archiveRootDir, BUNDLE_STATE_FILE));
            bw = new BufferedWriter(new OutputStreamWriter(os));
            String s = null;
            switch (m_persistentState)
            {
                case Bundle.ACTIVE:
                    s = ACTIVE_STATE;
                    break;
                case Bundle.STARTING:
                    s = STARTING_STATE;
                    break;
                case Bundle.UNINSTALLED:
                    s = UNINSTALLED_STATE;
                    break;
                default:
                    s = INSTALLED_STATE;
                    break;
            }
            bw.write(s, 0, s.length());
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                getClass().getName() + ": Unable to record state - " + ex);
            throw ex;
        }
        finally
        {
            if (bw != null) bw.close();
            if (os != null) os.close();
        }
    }

    private int readPersistentState() throws Exception
    {
        int state = Bundle.INSTALLED;

        // Get bundle state file.
        File stateFile = new File(m_archiveRootDir, BUNDLE_STATE_FILE);

        // If the state file doesn't exist, then
        // assume the bundle was installed.
        if (BundleCache.getSecureAction().fileExists(stateFile))
        {
            // Read the bundle state.
            InputStream is = null;
            BufferedReader br = null;
            try
            {
                is = BundleCache.getSecureAction()
                    .getFileInputStream(stateFile);
                br = new BufferedReader(new InputStreamReader(is));
                String s = br.readLine();
                if ((s != null) && s.equals(ACTIVE_STATE))
                {
                    state = Bundle.ACTIVE;
                }
                else if ((s != null) && s.equals(STARTING_STATE))
                {
                    state = Bundle.STARTING;
                }
                else if ((s != null) && s.equals(UNINSTALLED_STATE))
                {
                    state = Bundle.UNINSTALLED;
                }
                else
                {
                    state = Bundle.INSTALLED;
                }
            }
            catch (Exception ex)
            {
                state = Bundle.INSTALLED;
            }
            finally
            {
                if (br != null) br.close();
                if (is != null) is.close();
            }
        }

        return state;
    }

    private void writeStartLevel() throws Exception
    {
        OutputStream os = null;
        BufferedWriter bw = null;
        try
        {
            os = BundleCache.getSecureAction()
                .getFileOutputStream(new File(m_archiveRootDir, BUNDLE_START_LEVEL_FILE));
            bw = new BufferedWriter(new OutputStreamWriter(os));
            String s = Integer.toString(m_startLevel);
            bw.write(s, 0, s.length());
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                getClass().getName() + ": Unable to record start level - " + ex);
            throw ex;
        }
        finally
        {
            if (bw != null) bw.close();
            if (os != null) os.close();
        }
    }

    private int readStartLevel() throws Exception
    {
        int level = -1;

        // Get bundle start level file.
        File levelFile = new File(m_archiveRootDir, BUNDLE_START_LEVEL_FILE);

        // If the start level file doesn't exist, then
        // return an error.
        if (!BundleCache.getSecureAction().fileExists(levelFile))
        {
            level = -1;
        }
        else
        {
            // Read the bundle start level.
            InputStream is = null;
            BufferedReader br= null;
            try
            {
                is = BundleCache.getSecureAction()
                    .getFileInputStream(levelFile);
                br = new BufferedReader(new InputStreamReader(is));
                level = Integer.parseInt(br.readLine());
            }
            finally
            {
                if (br != null) br.close();
                if (is != null) is.close();
            }
        }
        return level;
    }

    private void writeLastModified() throws Exception
    {
        OutputStream os = null;
        BufferedWriter bw = null;
        try
        {
            os = BundleCache.getSecureAction()
                .getFileOutputStream(new File(m_archiveRootDir, BUNDLE_LASTMODIFIED_FILE));
            bw = new BufferedWriter(new OutputStreamWriter(os));
            String s = Long.toString(m_lastModified);
            bw.write(s, 0, s.length());
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                getClass().getName() + ": Unable to record start level - " + ex);
            throw ex;
        }
        finally
        {
            if (bw != null) bw.close();
            if (os != null) os.close();
        }
    }

    private long readLastModified() throws Exception
    {
        long last = 0;

        InputStream is = null;
        BufferedReader br = null;
        try
        {
            is = BundleCache.getSecureAction()
                .getFileInputStream(new File(m_archiveRootDir, BUNDLE_LASTMODIFIED_FILE));
            br = new BufferedReader(new InputStreamReader(is));
            last = Long.parseLong(br.readLine());
        }
        catch (Exception ex)
        {
            last = 0;
        }
        finally
        {
            if (br != null) br.close();
            if (is != null) is.close();
        }

        return last;
    }

    private void writeRefreshCount() throws Exception
    {
        OutputStream os = null;
        BufferedWriter bw = null;
        try
        {
            os = BundleCache.getSecureAction()
                .getFileOutputStream(new File(m_archiveRootDir, REFRESH_COUNTER_FILE));
            bw = new BufferedWriter(new OutputStreamWriter(os));
            String s = Long.toString(m_refreshCount);
            bw.write(s, 0, s.length());
        }
        catch (IOException ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                getClass().getName() + ": Unable to write refresh count: " + ex);
            throw ex;
        }
        finally
        {
            if (bw != null) bw.close();
            if (os != null) os.close();
        }
    }

    private long readRefreshCount() throws Exception
    {
        long count = 0;

        InputStream is = null;
        BufferedReader br = null;
        try
        {
            is = BundleCache.getSecureAction()
                .getFileInputStream(new File(m_archiveRootDir, REFRESH_COUNTER_FILE));
            br = new BufferedReader(new InputStreamReader(is));
            count = Long.parseLong(br.readLine());
        }
        catch (Exception ex)
        {
            count = 0;
        }
        finally
        {
            if (br != null) br.close();
            if (is != null) is.close();
        }

        return count;
    }
}
