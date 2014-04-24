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
package org.apache.felix.deploymentadmin;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Utils {
    private static final String MANIFEST_NAME = JarFile.MANIFEST_NAME;

    public static Manifest readManifest(File manifestFile) throws IOException {
        InputStream is = null;
        Manifest mf = null;
        try {
            is = new GZIPInputStream(new FileInputStream(manifestFile));
            mf = new Manifest(is);
        }
        finally {
            closeSilently(is);
        }
        return mf;
    }

    public static boolean replace(File target, File source) {
        return delete(target, true /* deleteRoot */) && rename(source, target);
    }

    public static boolean copy(File from, File to) {
        boolean result = true;
        if (from.isDirectory()) {
            if (!to.isDirectory()) {
                if (!to.mkdirs()) {
                    return false;
                }
                File[] files = from.listFiles();
                if (files == null) {
                    return false;
                }
                for (int i = 0; i < files.length; i++) {
                    result &= copy(files[i], new File(to, files[i].getName()));
                }
            }
        }
        else {
            InputStream input = null;
            OutputStream output = null;
            try {
                input = new FileInputStream(from);
                output = new FileOutputStream(to);
                byte[] buffer = new byte[4096];
                for (int i = input.read(buffer); i > -1; i = input.read(buffer)) {
                    output.write(buffer, 0, i);
                }
            }
            catch (IOException e) {
                return false;
            }
            finally {
                if (!closeSilently(output)) {
                    result = false;
                }
                if (!closeSilently(input)) {
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean rename(File from, File to) {
        if (!from.renameTo(to)) {
            if (copy(from, to)) {
                if (!delete(from, true /* deleteRoot */)) {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        return true;
    }

    public static boolean delete(File root, boolean deleteRoot) {
        boolean result = true;
        if (root.isDirectory()) {
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    result &= delete(files[i], true);
                }
                else {
                    result &= files[i].delete();
                }
            }
        }
        if (deleteRoot) {
            if (root.exists()) {
                result &= root.delete();
            }
        }
        return result;
    }

    public static void merge(File targetIndex, File target, File sourceIndex, File source) throws IOException {
        List targetFiles = readIndex(targetIndex);
        List sourceFiles = readIndex(sourceIndex);
        List result = new ArrayList(targetFiles);

        File manifestFile = new File(source, (String) sourceFiles.remove(0));
        Manifest resultManifest = Utils.readManifest(manifestFile);

        resultManifest.getMainAttributes().remove(new Name(Constants.DEPLOYMENTPACKAGE_FIXPACK));

        for (Iterator i = result.iterator(); i.hasNext();) {
            String targetFile = (String) i.next();
            if (!MANIFEST_NAME.equals(targetFile) && !resultManifest.getEntries().containsKey(targetFile)) {
                i.remove();
            }
        }

        for (Iterator iter = sourceFiles.iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            File from = new File(source, path);
            File to = new File(target, path);
            if (targetFiles.contains(path)) {
                if (!to.delete()) {
                    throw new IOException("Could not delete " + to);
                }
            }
            else {
                result.add(path);
            }
            if (!rename(from, to)) {
                throw new IOException("Could not rename " + from + " to " + to);
            }
        }

        targetFiles.removeAll(sourceFiles);

        for (Iterator iter = resultManifest.getEntries().keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            Attributes sourceAttribute = (Attributes) resultManifest.getEntries().get(path);
            if ("true".equals(sourceAttribute.remove(new Name(Constants.DEPLOYMENTPACKAGE_MISSING)))) {
                targetFiles.remove(path);
            }
        }

        for (Iterator iter = targetFiles.iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            File targetFile = new File(target, path);
            if (!targetFile.delete()) {
                throw new IOException("Could not delete " + targetFile);
            }
        }

        GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(new File(target, MANIFEST_NAME)));
        try {
            resultManifest.write(outputStream);
        }
        finally {
            outputStream.close();
        }
        writeIndex(targetIndex, result);
    }

    public static List readIndex(File index) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(index));
            List result = new ArrayList();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                result.add(line);
            }
            return result;
        }
        finally {
            closeSilently(reader);
        }
    }

    static boolean closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException exception) {
                // Ignore; nothing we can do about this...
                return false;
            }
        }
        return true;
    }

    private static void writeIndex(File index, List input) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(index));
            for (Iterator iterator = input.iterator(); iterator.hasNext();) {
                writer.println(iterator.next());
            }
        }
        finally {
            closeSilently(writer);
        }
    }
}
