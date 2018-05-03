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

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.WeakZipFileFactory;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private static final ThreadLocal m_defaultBuffer = new ThreadLocal();
    private static volatile int DEFAULT_BUFFER = 1024 * 64;

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
        if (locking.equals(Boolean.TRUE.toString()))
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

    // Parse the main attributes of the manifest of the given jarfile.
    // The idea is to not open the jar file as a java.util.jarfile but
    // read the mainfest from the zipfile directly and parse it manually
    // to use less memory and be faster.
    //
    // @return the given map for convenience
    public static Map<String, Object> getMainAttributes(Map<String, Object> headers, InputStream inputStream, long size) throws Exception
    {
        if (size > 0)
        {
            return getMainAttributes(headers, inputStream, (int) (size < Integer.MAX_VALUE ? size : Integer.MAX_VALUE));
        }
        else
        {
            return headers;
        }
    }

    static byte[] read(InputStream input, long size) throws Exception
    {
        return read(input, size <= Integer.MAX_VALUE ? (int) size : Integer.MAX_VALUE);
    }

    static byte[] read(InputStream input, int size) throws Exception
    {
        if (size <= 0)
        {
            return new byte[0];
        }

        byte[] result = new byte[size];

        Exception exception = null;
        try
        {
            for (int i = input.read(result, 0, size); i != -1 && i < size; i += input.read(result, i, size - i))
            {

            }
        }
        catch (Exception ex)
        {
            exception = ex;
        }
        finally
        {
            try
            {
                input.close();
            }
            catch (Exception ex)
            {
                throw exception != null ? exception : ex;
            }
        }

        return result;
    }

    public static Map<String, Object> getMainAttributes(Map<String, Object> headers, InputStream inputStream, int size) throws Exception
    {
        if (size <= 0)
        {
            inputStream.close();
            return headers;
        }

        // Get the buffer for this thread if there is one already otherwise,
        // create one of size DEFAULT_BUFFER (64K) if the manifest is less
        // than 64k or of the size of the manifest.
        SoftReference ref = (SoftReference) m_defaultBuffer.get();
        byte[] bytes = null;
        if (ref != null)
        {
            bytes = (byte[]) ref.get();
        }

        if (bytes == null)
        {
            bytes = new byte[size + 1 > DEFAULT_BUFFER ? size + 1 : DEFAULT_BUFFER];
            m_defaultBuffer.set(new SoftReference(bytes));
        }
        else if (size + 1 > bytes.length)
        {
            bytes = new byte[size + 1];
            m_defaultBuffer.set(new SoftReference(bytes));
        }

        // Now read in the manifest in one go into the bytes array.
        // The InputStream should be already buffered and can handle up to 64K buffers in one go.
        try
        {
            int i = inputStream.read(bytes);
            while (i < size)
            {
                i += inputStream.read(bytes, i, bytes.length - i);
            }
        }
        finally
        {
            inputStream.close();
        }

        // Force a new line at the end of the manifest to deal with broken manifest without any line-ending
        bytes[size++] = '\n';

        // Now parse the main attributes. The idea is to do that
        // without creating new byte arrays. Therefore, we read through
        // the manifest bytes inside the bytes array and write them back into
        // the same array unless we don't need them (e.g., \r\n and \n are skipped).
        // That allows us to create the strings from the bytes array without the skipped
        // chars. We stop as soon as we see a blank line as that denotes that the main
        // attributes part is finished.
        String key = null;
        int last = 0;
        int current = 0;
        for (int i = 0; i < size; i++)
        {
            // skip \r and \n if it is followed by another \n
            // (we catch the blank line case in the next iteration)
            if (bytes[i] == '\r')
            {
                if ((i + 1 < size) && (bytes[i + 1] == '\n'))
                {
                    continue;
                }
            }
            if (bytes[i] == '\n')
            {
                if ((i + 1 < size) && (bytes[i + 1] == ' '))
                {
                    i++;
                    continue;
                }
            }
            // If we don't have a key yet and see the first : we parse it as the key
            // and skip the :<blank> that follows it.
            if ((key == null) && (bytes[i] == ':'))
            {
                key = new String(bytes, last, (current - last), "UTF-8");
                if ((i + 1 < size) && (bytes[i + 1] == ' '))
                {
                    last = current + 1;
                    continue;
                }
                else
                {
                    throw new Exception("Manifest error: Missing space separator - " + key);
                }
            }
            // if we are at the end of a line
            if (bytes[i] == '\n')
            {
                // and it is a blank line stop parsing (main attributes are done)
                if ((last == current) && (key == null))
                {
                    break;
                }
                // Otherwise, parse the value and add it to the map (we throw an
                // exception if we don't have a key or the key already exist.
                String value = new String(bytes, last, (current - last), "UTF-8");
                if (key == null)
                {
                    throw new Exception("Manifest error: Missing attribute name - " + value);
                }
                else if (headers.put(key.intern(), value) != null)
                {
                    throw new Exception("Manifest error: Duplicate attribute name - " + key);
                }
                last = current;
                key = null;
            }
            else
            {
                // write back the byte if it needs to be included in the key or the value.
                bytes[current++] = bytes[i];
            }
        }
        return headers;
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
                DEFAULT_BUFFER = Integer.parseInt(sBufSize);
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
        if (!getSecureAction().fileExists(sbDir) && !getSecureAction().mkdirs(sbDir) && !getSecureAction().fileExists(sbDir))
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "Unable to create system bundle directory.");
            throw new IOException("Unable to create system bundle directory.");
        }

        File dataFile = new File(sbDir, fileName);

        String dataFilePath = BundleCache.getSecureAction().getCanonicalPath(dataFile);
        String dataDirPath = BundleCache.getSecureAction().getCanonicalPath(sbDir);
        if (!dataFilePath.equals(dataDirPath) && !dataFilePath.startsWith(dataDirPath + File.separatorChar))
        {
            throw new IllegalArgumentException("The data file must be inside the data dir.");
        }

        return dataFile;
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
        // Get the buffer for this thread if there is one already otherwise,
        // create one of size DEFAULT_BUFFER
        SoftReference ref = (SoftReference) m_defaultBuffer.get();
        byte[] bytes = null;
        if (ref != null)
        {
            bytes = (byte[]) ref.get();
        }

        if (bytes == null)
        {
            bytes = new byte[DEFAULT_BUFFER];
            m_defaultBuffer.set(new SoftReference(bytes));
        }

        OutputStream os = null;

        try
        {
            os = getSecureAction().getFileOutputStream(outputFile);
            for (int i = is.read(bytes);i != -1; i = is.read(bytes))
            {
                os.write(bytes, 0, i);
            }
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            finally
            {
                if (os != null) os.close();
            }
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