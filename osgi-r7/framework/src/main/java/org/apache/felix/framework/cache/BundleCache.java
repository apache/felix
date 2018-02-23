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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.WeakZipFileFactory;
import org.osgi.framework.Constants;

/**
 * <p>
 * This class, combined with <tt>BundleArchive</tt>, and concrete
 * <tt>BundleRevision</tt> subclasses, implement the Felix bundle cache.
 * It is possible to configure the default behavior of this class by
 * passing properties into Felix' constructor. The configuration properties
 * for this class are (properties starting with "<tt>felix</tt>" are specific
 * to Felix, while those starting with "<tt>org.osgi</tt>" are standard OSGi
 * properties):
 * </p>
 * <ul>
 *   <li><tt>felix.cache.filelimit</tt> - The integer value of this string
 *       sets an upper limit on how many files the cache will open. The default
 *       value is zero, which means there is no limit.
 *   </li>
 *   <li><tt>org.osgi.framework.storage</tt> - Sets the directory to use as
 *       the bundle cache; by default bundle cache directory is
 *       <tt>felix-cache</tt> in the current working directory. The value
 *       should be a valid directory name. The directory name can be either absolute
 *       or relative. Relative directory names are relative to the current working
 *       directory. The specified directory will be created if it does
 *       not exist.
 *   </li>
 *   <li><tt>felix.cache.rootdir</tt> - Sets the root directory to use to
 *       calculate the bundle cache directory for relative directory names. If
 *       <tt>org.osgi.framework.storage</tt> is set to a relative name, by
 *       default it is relative to the current working directory. If this
 *       property is set, then it will be calculated as being relative to
 *       the specified root directory.
 *   </li>
 *   <li><tt>felix.cache.locking</tt> - Enables or disables bundle cache locking,
 *       which is used to prevent concurrent access to the bundle cache. This is
 *       enabled by default, but on older/smaller JVMs file channel locking is
 *       not available; set this property to <tt>false</tt> to disable it.
 *   </li>
 *   <li><tt>felix.cache.bufsize</tt> - Sets the buffer size to be used by
 *       the cache; the default value is 4096. The integer value of this
 *       string provides control over the size of the internal buffer of the
 *       disk cache for performance reasons.
 *   </li>
 * <p>
 * For specific information on how to configure the Felix framework, refer
 * to the Felix framework usage documentation.
 * </p>
 * @see org.apache.felix.framework.cache.BundleArchive
**/
public class BundleCache
{
    public static final String CACHE_BUFSIZE_PROP = "felix.cache.bufsize";
    public static final String CACHE_ROOTDIR_PROP = "felix.cache.rootdir";
    public static final String CACHE_LOCKING_PROP = "felix.cache.locking";
    public static final String CACHE_FILELIMIT_PROP = "felix.cache.filelimit";
    // TODO: CACHE - This should eventually be removed along with the code
    //       supporting the old multi-file bundle cache format.
    public static final String CACHE_SINGLEBUNDLEFILE_PROP = "felix.cache.singlebundlefile";

    protected static transient int BUFSIZE = 4096;

    private static transient final String CACHE_DIR_NAME = "felix-cache";
    private static transient final String CACHE_ROOTDIR_DEFAULT = ".";
    private static transient final String CACHE_LOCK_NAME = "cache.lock";
    static transient final String BUNDLE_DIR_PREFIX = "bundle";

    private static final SecureAction m_secureAction = new SecureAction();

    private final Logger m_logger;
    private final Map m_configMap;
    private final WeakZipFileFactory m_zipFactory;
    private final Object m_lock;

    public BundleCache(Logger logger, Map configMap)
        throws Exception
    {
        m_logger = logger;
        m_configMap = configMap;

        int limit = 0;
        String limitStr = (String) m_configMap.get(CACHE_FILELIMIT_PROP);
        if (limitStr != null)
        {
            try
            {
                limit = Integer.parseInt(limitStr);
            }
            catch (NumberFormatException ex)
            {
                limit = 0;
            }
        }
        m_zipFactory = new WeakZipFileFactory(limit);

        // Create the cache directory, if it does not exist.
        File cacheDir = determineCacheDir(m_configMap);
        if (!getSecureAction().fileExists(cacheDir))
        {
            if (!getSecureAction().mkdirs(cacheDir))
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Unable to create cache directory: " + cacheDir);
                throw new RuntimeException("Unable to create cache directory.");
            }
        }

        Object locking = m_configMap.get(CACHE_LOCKING_PROP);
        locking = (locking == null)
            ? Boolean.TRUE.toString()
            : locking.toString().toLowerCase();
        if (((String) locking).equals(Boolean.TRUE.toString()))
        {
            File lockFile = new File(cacheDir, CACHE_LOCK_NAME);
            FileChannel fc = null;
            FileOutputStream fos = null;
            try
            {
                if (!getSecureAction().fileExists(lockFile))
                {
                    fos = getSecureAction().getFileOutputStream(lockFile);
                    fc = fos.getChannel();
                }
                else
                {
                    fos = getSecureAction().getFileOutputStream(lockFile);
                    fc = fos.getChannel();
                }
            }
            catch (Exception ex)
            {
                try
                {
                    if (fos != null) fos.close();
                    if (fc != null) fc.close();
                }
                catch (Exception ex2)
                {
                    // Ignore.
                }
                throw new Exception("Unable to create bundle cache lock file: " + ex);
            }
            try
            {
                m_lock = fc.tryLock();
            }
            catch (Exception ex)
            {
                throw new Exception("Unable to lock bundle cache: " + ex);
            }
        }
        else
        {
            m_lock = null;
        }
    }

    public synchronized void release()
    {
        if (m_lock != null)
        {
            try
            {
                ((FileLock) m_lock).release();
                ((FileLock) m_lock).channel().close();
            }
            catch (Exception ex)
            {
                // Not much we can do here, just log it.
                m_logger.log(
                    Logger.LOG_WARNING,
                    "Exception releasing bundle cache.", ex);
            }
        }
    }

    /* package */ static SecureAction getSecureAction()
    {
        return m_secureAction;
    }

    public synchronized void delete() throws Exception
    {
        // Delete the cache directory.
        File cacheDir = determineCacheDir(m_configMap);
        deleteDirectoryTree(cacheDir);
    }

    public BundleArchive[] getArchives()
        throws Exception
    {
        // Get buffer size value.
        try
        {
            String sBufSize = (String) m_configMap.get(CACHE_BUFSIZE_PROP);
            if (sBufSize != null)
            {
                BUFSIZE = Integer.parseInt(sBufSize);
            }
        }
        catch (NumberFormatException ne)
        {
            // Use the default value.
        }

        // Create the existing bundle archives in the directory, if any exist.
        File cacheDir = determineCacheDir(m_configMap);
        List archiveList = new ArrayList();
        File[] children = getSecureAction().listDirectory(cacheDir);
        for (int i = 0; (children != null) && (i < children.length); i++)
        {
            // Ignore directories that aren't bundle directories or
            // is the system bundle directory.
            if (children[i].getName().startsWith(BUNDLE_DIR_PREFIX) &&
                !children[i].getName().equals(BUNDLE_DIR_PREFIX + Long.toString(0)))
            {
                // Recreate the bundle archive.
                try
                {
                    archiveList.add(
                        new BundleArchive(
                            m_logger, m_configMap, m_zipFactory, children[i]));
                }
                catch (Exception ex)
                {
                    // Log exception and remove bundle archive directory.
                    m_logger.log(Logger.LOG_ERROR,
                        "Error reloading cached bundle, removing it: " + children[i], ex);
                    deleteDirectoryTree(children[i]);
                }
            }
        }

        return (BundleArchive[])
            archiveList.toArray(new BundleArchive[archiveList.size()]);
    }

    public BundleArchive create(long id, int startLevel, String location, InputStream is)
        throws Exception
    {
        File cacheDir = determineCacheDir(m_configMap);

        // Construct archive root directory.
        File archiveRootDir =
            new File(cacheDir, BUNDLE_DIR_PREFIX + Long.toString(id));

        try
        {
            // Create the archive and add it to the list of archives.
            BundleArchive ba =
                new BundleArchive(
                    m_logger, m_configMap, m_zipFactory, archiveRootDir,
                    id, startLevel, location, is);
            return ba;
        }
        catch (Exception ex)
        {
            if (m_secureAction.fileExists(archiveRootDir))
            {
                if (!BundleCache.deleteDirectoryTree(archiveRootDir))
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Unable to delete the archive directory: "
                            + archiveRootDir);
                }
            }
            throw ex;
        }
    }

    /**
     * Provides the system bundle access to its private storage area; this
     * special case is necessary since the system bundle is not really a
     * bundle and therefore must be treated in a special way.
     * @param fileName the name of the file in the system bundle's private area.
     * @return a <tt>File</tt> object corresponding to the specified file name.
     * @throws Exception if any error occurs.
    **/
    public File getSystemBundleDataFile(String fileName)
        throws Exception
    {
        // Make sure system bundle directory exists.
        File sbDir = new File(determineCacheDir(m_configMap), BUNDLE_DIR_PREFIX + Long.toString(0));

        // If the system bundle directory exists, then we don't
        // need to initialize since it has already been done.
        if (!getSecureAction().fileExists(sbDir))
        {
            // Create system bundle directory, if it does not exist.
            if (!getSecureAction().mkdirs(sbDir))
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Unable to create system bundle directory.");
                throw new IOException("Unable to create system bundle directory.");
            }
        }

        // Do some sanity checking.
        if ((fileName.length() > 0) && (fileName.charAt(0) == File.separatorChar))
            throw new IllegalArgumentException("The data file path must be relative, not absolute.");
        else if (fileName.indexOf("..") >= 0)
            throw new IllegalArgumentException("The data file path cannot contain a reference to the \"..\" directory.");

        // Return the data file.
        return new File(sbDir, fileName);
    }

    //
    // Static file-related utility methods.
    //

    /**
     * This method copies an input stream to the specified file.
     * @param is the input stream to copy.
     * @param outputFile the file to which the input stream should be copied.
    **/
    static void copyStreamToFile(InputStream is, File outputFile)
        throws IOException
    {
        OutputStream os = null;

        try
        {
            os = getSecureAction().getFileOutputStream(outputFile);
            os = new BufferedOutputStream(os, BUFSIZE);
            byte[] b = new byte[BUFSIZE];
            int len = 0;
            while ((len = is.read(b)) != -1)
            {
                os.write(b, 0, len);
            }
        }
        finally
        {
            if (is != null) is.close();
            if (os != null) os.close();
        }
    }

    static boolean deleteDirectoryTree(File target)
    {
        if (!deleteDirectoryTreeRecursive(target))
        {
            // We might be talking windows and native libs -- hence,
            // try to trigger a gc and try again. The hope is that
            // this releases the classloader that loaded the native
            // lib and allows us to delete it because it then
            // would not be used anymore.
            System.gc();
            System.gc();
            return deleteDirectoryTreeRecursive(target);
        }
        return true;
    }

    //
    // Private methods.
    //

    private static File determineCacheDir(Map configMap)
    {
        File cacheDir;

        // Check to see if the cache directory is specified in the storage
        // configuration property.
        String cacheDirStr = (String) configMap.get(Constants.FRAMEWORK_STORAGE);
        // Get the cache root directory for relative paths; the default is ".".
        String rootDirStr = (String) configMap.get(CACHE_ROOTDIR_PROP);
        rootDirStr = (rootDirStr == null) ? CACHE_ROOTDIR_DEFAULT : rootDirStr;
        if (cacheDirStr != null)
        {
            // If the specified cache directory is relative, then use the
            // root directory to calculate the absolute path.
            cacheDir = new File(cacheDirStr);
            if (!cacheDir.isAbsolute())
            {
                cacheDir = new File(rootDirStr, cacheDirStr);
            }
        }
        else
        {
            // If no cache directory was specified, then use the default name
            // in the root directory.
            cacheDir = new File(rootDirStr, CACHE_DIR_NAME);
        }

        return cacheDir;
    }

    private static boolean deleteDirectoryTreeRecursive(File target)
    {
    	if (!getSecureAction().fileExists(target))
        {
            return true;
        }

        if (getSecureAction().isFileDirectory(target))
        {
            File[] files = getSecureAction().listDirectory(target);
            if (files != null)
            {
                for (int i = 0; i < files.length; i++)
                {
                    deleteDirectoryTreeRecursive(files[i]);
                }
            }
        }

        return getSecureAction().deleteFile(target);
    }
}