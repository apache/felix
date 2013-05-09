/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.task;

import org.apache.commons.cli.*;
import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.util.Constants;

import java.io.File;
import java.util.List;

/**
 * A command line tools to manipulate iPOJO bundles.
 * It case be used as follow:
 * <code>java -jar this-jar.jar --input the-jar-to-manipulate</code><br/>
 * <code>java -jar this-jar.jar --input the-jar-to-manipulate  --output the-output-jar</code><br/>
 * <code>java -jar this-jar.jar --input the-jar-to-manipulate  --metadata the-xml-metadata</code><br/>
 */
public class IPojoc {

    /**
     * CLI options.
     */
    private static Options options;

    /**
     * The main entry point
     * @param args the arguments
     * @throws ParseException
     */
    public static void main(String[] args) throws ParseException {
        options = getOptions();
        CommandLineParser parser = new GnuParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                printHelp();
            } else {
                process(cmd);
            }
        } catch (MissingOptionException e) {
            for (String opt : (List<String>) e.getMissingOptions()) {
                System.err.println("The '" + opt + "' option is missing");
            }
            printHelp();
        } catch (MissingArgumentException e) {
            System.err.println("The option '" + e.getOption() + "' requires an argument");
            printHelp();
        }
    }

    /**
     * The command line is valid, to processing the command line.
     * @param cmd the command line
     * @throws ParseException
     */
    private static void process(CommandLine cmd) throws ParseException {
        // All arguments are set
        File input, output, metadata;

        // Check that the input file exist
        try {
            input = (File) cmd.getParsedOptionValue("i");
            if (input == null || !input.isFile()) {
                System.err.println("The input option must be an existing file, '" + cmd.getOptionValue('i') + "' does " +
                        "not exist");
                return;
            }
        } catch (ParseException pe) {
            System.err.println("The input option must be an existing file");
            return;
        }

        // Retrieve output file
        if (cmd.hasOption("o")) {
            try {
                output = (File) cmd.getParsedOptionValue("o");
            } catch (ParseException pe) {
                System.err.println("The output option must be a valid file");
                return;
            }
        } else {
            // Inline replacement
            // We create a temporary file marked by a __ prefix
            // It will be substituted upon success.
            output = new File("__" + input.getName());
        }

        // Retrieve the metadata file
        if (cmd.hasOption("m")) {
            try {
                metadata = (File) cmd.getParsedOptionValue("m");
                if (metadata != null && !metadata.isFile()) {
                    System.err.println("The metadata option must be an existing file , " +
                            "'" + cmd.getOptionValue('m') + "' does " +
                            "not exist");
                    return;
                }
            } catch (ParseException pe) {
                System.err.println("The metadata option must be a valid file");
                return;
            }
        } else {
            metadata = null;
        }

        System.out.println("iPOJO Manipulation:");
        System.out.println("input file     => " + input.getAbsolutePath());
        if (output.getName().startsWith("__")) {
            System.out.println("output file    => " + input.getAbsolutePath());
        } else {
            System.out.println("output file    => " + output.getAbsolutePath());
        }
        if (metadata != null) {
            System.out.println("metadata file  => " + metadata.getAbsolutePath());
        } else {
            System.out.println("metadata file  => no metadata file");
        }

        Pojoization pojoization = new Pojoization();
        pojoization.pojoization(input, output, metadata);

        if (pojoization.getErrors().size() != 0) {
            System.err.println("iPOJO Manipulation failed :");
            for (String error : pojoization.getErrors()) {
                System.err.println("\t" +error);
            }
            System.exit(-1);
        } else {
            System.err.println("iPOJO Manipulation successfully completed.");
            for (String warning : pojoization.getWarnings()) {
                System.err.println(warning);
            }
            cleanup(input, output);
            System.exit(0);
        }
    }

    /**
     * Upon success, cleanup temporary files.
     * @param input the input jar
     * @param output the output jar
     */
    private static void cleanup(File input, File output) {
        if (output.getName().startsWith("__")) {
            input.delete();
            output.renameTo(input);
        }
    }

    /**
     * Print help.
     */
    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar org.apache.felix.ipojo.ant-" + Constants.getVersion() + ".jar", options);
    }

    /**
     * Builds the option list
     * @return the options
     */
    public static Options getOptions() {
        Option input =
                OptionBuilder.withArgName("input file")
                        .withLongOpt("input")
                        .hasArg()
                        .withDescription("the input jar file")
                        .isRequired(true)
                        .withType(File.class)
                        .create('i');

        Option output =
                OptionBuilder
                        .withLongOpt("output")
                        .withArgName("output file")
                        .hasArg()
                        .withDescription("the output jar file, if not set the manipulation replaces the input file")
                        .isRequired(false)
                        .withType(File.class)
                        .create('o');

        Option metadata =
                OptionBuilder
                        .withLongOpt("metadata")
                        .withArgName("metadata file")
                        .hasArg()
                        .withDescription("the XML metadata file")
                        .isRequired(false)
                        .withType(File.class)
                        .create('m');

        Option help =
                OptionBuilder.withLongOpt("help").withDescription("print this message").create('h');

        return new Options().addOption(input).addOption(output).addOption(metadata).addOption(help);
    }
}
