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
package spell.gui;

import org.apache.felix.ipojo.annotations.*;
import spell.services.SpellChecker;

import javax.swing.*;

/**
 * A very simple Gui interacting with the CheckSpeller service
 */
@Component // It's a component
@Instantiate // We declarare an instance
public class SpellCheckerGui extends JFrame {

    private static final long serialVersionUID = 1L;

    /**
     * Swing component where the user write the passage to check.
     */
    private JTextField passage = null;

    /**
     * Area where the result is displayed.
     */
    private JLabel result = null;

    /**
     * Service dependency on the SpellChecker.
     */
    @Requires // It's a service dependency
    private SpellChecker checker;

    /**
     * Constructor.
     * Initialize the GUI.
     */
    public SpellCheckerGui() {
        super();
        initComponents();
        this.setTitle("Spellchecker Gui");
    }

    /**
     * Initialize the Swing Gui.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        // The check button
        JButton checkButton = new JButton();
        result = new JLabel();
        passage = new JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE); // Stop Felix...
        getContentPane().setLayout(new java.awt.GridBagLayout());

        checkButton.setText("Check");
        checkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                check();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(checkButton, gridBagConstraints);

        result.setPreferredSize(new java.awt.Dimension(175, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(result, gridBagConstraints);

        passage.setPreferredSize(new java.awt.Dimension(175, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(passage, gridBagConstraints);

        pack();
    }

    /**
     * Check Button action.
     * Collects the user input and checks it.
     */
    private void check() {
        // TODO
    }

    /**
     * Start callback.
     * This method will be called when the instance becomes valid.
     * It set the Gui visibility to true.
     */
    @Validate
    public void start() {
        // TODO
    }

    /**
     * Stop callback.
     * This method will be called when the instance becomes invalid or stops.
     * It deletes the Gui.
     */
    @Invalidate
    public void stop() {
        // TODO
    }
}
