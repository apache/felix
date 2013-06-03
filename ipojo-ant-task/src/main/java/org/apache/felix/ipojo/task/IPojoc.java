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

import static java.lang.String.format;

import org.apache.commons.cli.*;
import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.util.Constants;

import java.io.File;
import java.util.List;

/**
 * A command line tools to manipulate iPOJO bundles.
 * It can be used as follow:
 * <code>java -jar this-jar.jar --input the-jar-to-manipulate</code><br/>
 * <code>java -jar this-jar.jar --input the-jar-to-manipulate  --output the-output-jar</code><br/>
 * <code>java -jar this-jar.jar --input the-jar-to-manipulate  --metadata the-xml-metadata</code><br/>
 */
public class IPojoc {

    /**
     * Input file to be manipulated.
     */
    private File m_input;

    /**
     * Output file (temporary or not).
     */
    private File m_output;

    /**
     * Metadata file (may be null).
     */
    private File m_metadata;

    /**
     * Manipulator.
     */
    private Pojoization m_pojoization;

    /**
     * The main entry point
     * @param args the arguments
     */
    public static void main(String[] args) {
        Options options = buildOptions();
        CommandLine cmd = null;
        try {
            cmd = buildCommandLine(args, options);
            if (cmd.hasOption('h')) {
                printHelp(options);
            } else {
                IPojoc compiler = new IPojoc();
                compiler.execute(cmd);
            }
        } catch (MissingOptionException e) {
            for (String opt : (List<String>) e.getMissingOptions()) {
                System.err.println("The '" + opt + "' option is missing");
            }
            printHelp(options);
        } catch (MissingArgumentException e) {
            System.err.println("The option '" + e.getOption() + "' requires an argument");
            printHelp(options);
        } catch (Exception e) {
            System.out.printf("Manipulation failed: %s%n", e.getMessage());
            if ((cmd != null) && cmd.hasOption('X')) {
                e.printStackTrace(System.out);
            } else {
                System.out.printf("Use -X option to print the full stack trace%n");
            }
        }
    }

    private static CommandLine buildCommandLine(final String[] args, final Options options) throws ParseException {
        CommandLineParser parser = new GnuParser();
        return parser.parse(options, args);
    }

    /**
     * The command line is valid, to processing the command line.
     * @param cmd the command line
     * @throws ParseException
     */
    private void execute(CommandLine cmd) throws Exception {
        System.out.printf("iPOJO Manipulation (%s)%n", Constants.getVersion());
        System.out.println("-----------------------------------------------");
        readInputOption(cmd);
        readOutputOption(cmd);
        readMetadataOption(cmd);

        manipulate();
        printStatus();
    }

    private void readMetadataOption(final CommandLine cmd) throws Exception {
        // Retrieve the metadata file
        if (cmd.hasOption("m")) {
            m_metadata = (File) cmd.getParsedOptionValue("m");
            if (m_metadata != null && !m_metadata.isFile()) {
                throw new Exception(
                        format("The metadata option must be an existing file, '%s' does not exist", cmd.getOptionValue('m'))
                );
            }
            System.out.println("metadata file  => " + m_metadata.getAbsolutePath());
        } else {
            System.out.println("metadata file  => no metadata file");
        }
    }

    private void readOutputOption(final CommandLine cmd) throws Exception {
        // Retrieve output file
        if (cmd.hasOption("o")) {
            try {
                m_output = (File) cmd.getParsedOptionValue("o");
            } catch (ParseException pe) {
                throw new Exception(
                        format("The output option must be an existing file, '%s' does not exist", cmd.getOptionValue('o'))
                );
            }
            System.out.println("output file    => " + m_output.getAbsolutePath());
        } else {
            // Inline replacement
            // We create a temporary file marked by a __ prefix
            // It will be substituted upon success.
            m_output = new File("__" + m_input.getName());
            System.out.println("output file    => " + m_input.getAbsolutePath());
        }
    }

    private void readInputOption(final CommandLine cmd) throws Exception {
        // Check that the input file exist
        m_input = (File) cmd.getParsedOptionValue("i");
        if (m_input == null || !m_input.isFile()) {
            throw new Exception(
                    format("The input option must be an existing file, '%s' does not exist", cmd.getOptionValue('i'))
            );
        }
        System.out.println("input file     => " + m_input.getAbsolutePath());
    }

    private void manipulate() {
        m_pojoization = new Pojoization();
        m_pojoization.pojoization(m_input, m_output, m_metadata);
    }

    private void printStatus() {
        if (m_pojoization.getErrors().size() != 0) {
            System.err.println("iPOJO Manipulation failed :");
            for (String error : m_pojoization.getErrors()) {
                System.err.println("\t" + error);
            }
            System.exit(-1);
        } else {
            System.err.println("iPOJO Manipulation successfully completed.");
            for (String warning : m_pojoization.getWarnings()) {
                System.err.println(warning);
            }
            cleanup();
            System.exit(0);
        }
    }

    /**
     * Upon success, cleanup temporary files.
     */
    private void cleanup() {
        if (m_output.getName().startsWith("__")) {
            m_input.delete();
            m_output.renameTo(m_input);
        }
    }

    /**
     * Print help.
     * @param options
     */
    private static void printHelp(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar org.apache.felix.ipojo.ant-" + Constants.getVersion() + ".jar", options);
    }

    /**
     * Builds the option list
     * @return the options
     */
    public static Options buildOptions() {
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

        Option verbose = new Option("X", "exception", false, "print exception stack trace in case of error");

        Option help =
                OptionBuilder.withLongOpt("help").withDescription("print this message").create('h');

        return new Options()
                .addOption(input)
                .addOption(output)
                .addOption(metadata)
                .addOption(verbose)
                .addOption(help);
    }
}
