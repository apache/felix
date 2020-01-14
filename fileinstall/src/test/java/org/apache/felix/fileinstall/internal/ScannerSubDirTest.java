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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.apache.felix.fileinstall.internal.Scanner.SUBDIR_MODE_RECURSE;
import static org.apache.felix.fileinstall.internal.Scanner.SUBDIR_MODE_SKIP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ScannerSubDirTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final static String FILTER = ".*\\.(cfg|config)";
    private final static Set<File> expectedRecurseFiles = new HashSet<>();
    private final static Set<File> excludedRecurseFiles = new HashSet<>();
    private final static Set<File> expectedSkipFiles = new HashSet<>();
    private final static Set<File> excludedSkipFiles = new HashSet<>();


    @Test
    public void testRecurse() throws Exception {
        prepareFiles();
        File watchedDirectory = folder.getRoot();
        Scanner recurseScanner = new Scanner(watchedDirectory, FILTER, SUBDIR_MODE_RECURSE);
        Set<File> filteredRecurseFiles = canon(recurseScanner.scan(true));
        assertEquals(filteredRecurseFiles.size(), expectedRecurseFiles.size());
        assertTrue(filteredRecurseFiles.containsAll(expectedRecurseFiles));
        for (File file : excludedRecurseFiles) {
            Assert.assertFalse(filteredRecurseFiles.contains(file));
        }

        Scanner skipScanner = new Scanner(watchedDirectory, FILTER, SUBDIR_MODE_SKIP);
        Set<File> filteredSkipFiles = canon(skipScanner.scan(true));

        //Assertions

        assertEquals(filteredSkipFiles.size(), expectedSkipFiles.size());
        assertTrue(filteredSkipFiles.containsAll(expectedSkipFiles));
        for (File file : excludedSkipFiles) {
            Assert.assertFalse(filteredSkipFiles.contains(file));
        }
    }

    private Set<File> canon(Set<File> set) throws IOException {
        Set<File> ns = new HashSet<>();
        for (File f : set) {
            ns.add(f.getCanonicalFile());
        }
        return ns;
    }


    private void prepareFiles() throws IOException {
        File cfgFirst = folder.newFile("first.cfg").getCanonicalFile();
        File mdFirst = folder.newFile("first.md").getCanonicalFile();

        excludedRecurseFiles.add(mdFirst);
        expectedRecurseFiles.add(cfgFirst);
        excludedSkipFiles.add(mdFirst);
        expectedSkipFiles.add(cfgFirst);

        // first level subfolder and files
        //expected
        File firstLevelSubfolder = folder.newFolder("firstSubfolder").getCanonicalFile();
        Path firstLevelSubfolderCfgFilePath = Paths.get(firstLevelSubfolder.getPath() + File.separator + "second.cfg");
        File firstLevelSubfolderCfgFile = new File(firstLevelSubfolderCfgFilePath.toString());
        firstLevelSubfolderCfgFile.createNewFile();
        expectedRecurseFiles.add(firstLevelSubfolderCfgFile);
        excludedSkipFiles.add(firstLevelSubfolderCfgFile);
        //md
        Path firstLevelSubfolderMdFilePath = Paths.get(firstLevelSubfolder.getPath() + File.separator + "second.md");
        File firstLevelSubfolderMdFile = new File(firstLevelSubfolderMdFilePath.toString());
        firstLevelSubfolderMdFile.createNewFile();
        excludedRecurseFiles.add(firstLevelSubfolderMdFile);
        excludedSkipFiles.add(firstLevelSubfolderMdFile);

        // second level subfolder and files
        //cfg
        Path secondLevelSubfolderCfgFilePath = Paths.get(firstLevelSubfolder.getPath() + File.separator + "secondSubfolder" + File.separator + "third.config");
        File secondLevelSubfolderCfgFile = new File(secondLevelSubfolderCfgFilePath.toString());
        secondLevelSubfolderCfgFile.getParentFile().mkdirs();
        secondLevelSubfolderCfgFile.createNewFile();
        expectedRecurseFiles.add(secondLevelSubfolderCfgFile);
        excludedSkipFiles.add(secondLevelSubfolderCfgFile);
        //Txt
        Path secondLevelSubfolderTxtFilePath = Paths.get(firstLevelSubfolder.getPath() + File.separator + "secondSubfolder" + File.separator + "third.txt");
        File secondLevelSubfolderTxtFile = new File(secondLevelSubfolderTxtFilePath.toString());
        secondLevelSubfolderTxtFile.createNewFile();
        excludedRecurseFiles.add(secondLevelSubfolderTxtFile);
        excludedSkipFiles.add(secondLevelSubfolderTxtFile);
    }

}
