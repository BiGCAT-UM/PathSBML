// PathSBML Plugin
// SBML Plugin for PathVisio.
// Copyright 2013 developed for Google Summer of Code
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.pathvisio.sbml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.xml.stream.XMLStreamException;

import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.gui.ProgressDialog;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLReader;

/**
 * This class adds the validation functionality.It lets us validate the selected
 * SBML file.
 * 
 * @author applecool
 * @author anwesha
 * @version 1.0.0
 * 
 */
@SuppressWarnings("serial")
public class ValidatePanel extends JPanel {

	SBMLPlugin plugin;
	SBMLReader reader;
	SBMLDocument document;
	static Border etch = BorderFactory.createEtchedBorder();

	private final JButton openButton;
	private final JButton validateButton;
	private final JFileChooser modelChooser;
	private final JTextPane validationResultPane;

	private StyledDocument validationResultStyledDoc;
	private final JLabel validationStatusBar = new JLabel(
			"Name of model file selected to be validated will appear here",
			SwingConstants.RIGHT);
	private static String modelFileName;

	/**
	 * 
	 */
	@SuppressWarnings("serial")
	public ValidatePanel(final SBMLPlugin plugin) {
		System.out.println("validate");

		this.plugin = plugin;
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		/*
		 * This action shows a file chooser dialog box to select a model file
		 * and opens it
		 */
		Action openModelAction = new AbstractAction("openModels") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					validationResultPane.setText("");
					int returnVal = modelChooser
							.showOpenDialog(ValidatePanel.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = modelChooser.getSelectedFile();
						modelFileName = file.getPath();
						validationStatusBar
						.setText("You chose"
								+ " "
								+ file.getName());
						if (file.canRead()) {
							validateButton.setEnabled(true);
						}
					} else {

						validationStatusBar.setText("You cancelled.");
					}
				} catch (Exception ex) {
					JOptionPane
					.showMessageDialog(ValidatePanel.this,
							ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					Logger.log.error("Error while opening selected model", ex);
				}
			}

		};

		/*
		 * This action validates the selected model
		 */
		Action validateModelAction = new AbstractAction("validateModels") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					validate();
					validateButton.setEnabled(false);
				} catch (Exception ex) {
					JOptionPane
					.showMessageDialog(ValidatePanel.this,
							ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					Logger.log.error("Error while validating selected model", ex);
				}
			}

		};

		/*
		 * Buttons for button panel Panel appears on clicking Validate in
		 * Plugins -> SBML Plugin -> Validate
		 */
		openButton = new JButton("Open");
		validateButton = new JButton("Validate");
		// Add Tooltips
		openButton.setToolTipText("Choose model file for validation");
		validateButton.setToolTipText("Validate chosen model file");
		// Add actions
		openButton.addActionListener(openModelAction);
		validateButton.addActionListener(validateModelAction);
		validateButton.setEnabled(false);

		/*
		 * FileChooser dialog appears on clicking the Open Button
		 */
		// modelChooser = new JFileChooser(System.getProperty("user.home"));

		modelChooser = new JFileChooser(
				GlobalPreference.DIR_LAST_USED_IMPORT.toString());

		modelChooser.setVisible(true);
		// filtering the files based on their extensions.
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"SBML(Systems Biology Markup Language) (.sbml,.xml)", "sbml",
				"xml");
		modelChooser.setFileFilter(filter);

		/*
		 * Creates a text pane to show validation results
		 */
		validationResultPane = new JTextPane();
		validationResultPane.setEnabled(true);
		// adds scroll bars to the text pane.
		JScrollPane scrollPane = new JScrollPane(validationResultPane);
		scrollPane.setPreferredSize(new Dimension(500, 400));
		scrollPane
		.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane
		.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		/*
		 * Button panel containing open and validate buttons appears on clicking
		 * Validate menu item. (Plugins - > SBML Plugin -> Validate)
		 */
		JPanel buttonPanel = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints cc = new GridBagConstraints();
		buttonPanel.setLayout(gridbag);

		buttonPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Button Pane"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		cc.fill = GridBagConstraints.BOTH;
		cc.gridx = 0;
		cc.gridy = 0;
		cc.weightx = 0.5;
		cc.insets = new Insets(10, 10, 10, 10);
		buttonPanel.add(openButton, cc);

		cc.fill = GridBagConstraints.BOTH;
		cc.gridx = 1;
		cc.gridy = 0;
		cc.weightx = 0.5;
		cc.insets = new Insets(10, 10, 10, 10);
		buttonPanel.add(validateButton, cc);

		cc.fill = GridBagConstraints.BOTH;
		cc.gridx = 0;
		cc.gridy = 1;
		cc.gridwidth = 2;
		buttonPanel.add(validationStatusBar, cc);

		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		add(buttonPanel, c);

		/*
		 * Output Panel to show validation Results
		 */
		JPanel outputPanel = new JPanel(new GridBagLayout());
		GridBagLayout gridbag1 = new GridBagLayout();
		GridBagConstraints c1 = new GridBagConstraints();
		outputPanel.setLayout(gridbag1);
		c1.gridwidth = GridBagConstraints.REMAINDER; // last
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 1.0;
		outputPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Output Pane"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		outputPanel.add(scrollPane);
		// adding the output panel to the south of the dialog box.

		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;

		add(outputPanel, c);
	}

	/**
	 * This method will validate the selected file.
	 * 
	 */
	@Override
	public void validate() {
		reader = new SBMLReader();
		document = null;

		final String selectFile = ValidatePanel.modelFileName;

		System.out.println("the file is " + selectFile);

		System.currentTimeMillis();
		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(
				JOptionPane.getFrameForComponent(this), "", pk,
				true, true);
		SwingWorker<String, Void> sw = new SwingWorker<String, Void>() {

			private long start;
			private long stop;

			@Override
			protected String doInBackground() throws Exception {
				pk.setTaskName("Validating selected model");
				start = System.currentTimeMillis();
				try {
					document = reader.readSBML(selectFile);

					stop = System.currentTimeMillis();

					validationResultPane.setText("");
					System.out.println("errors" + document.getErrorCount());

					if (document.getErrorCount() > 0) {
						validationResultPane.setText("Encountered the following errors while reading the SBML file:\n");
						document.printErrors(System.out);
						validationResultPane.setText("\nFurther consistency checking and validation aborted.\n");
					} else {
						long errors = document.checkConsistency();
						long size = new File(selectFile).length();

						System.out.println("File Information: \n");
						System.out.println("modelFileName: " + selectFile + "\n");
						System.out.println("file size: " + size + "\n");
						System.out.println("read time (ms): " + (stop - start) + "\n");

						append("validation error(s): " + errors + "\n", Color.RED);

						if (errors > 0) {
							append("\nFollowing errors were encountered while reading the SBML File:\n\n",
									Color.BLACK);
							for (int i = 0; i < errors; i++) {
								String mainError = document.getError(i).toString();
								SBMLError validationError = document.getError(i);
								append("" + validationError.getCategory(), Color.BLACK);
								append(" (" + validationError.getSeverity() + ")" + "\n\n",
										Color.BLACK);
								append("" + validationError.getShortMessage() + "\n\n",
										Color.BLACK);
								append("Line:" + validationError.getLine(), Color.RED);
								append("" + validationError.getMessage() + "\n\n",
										Color.BLUE);
								System.out.println("main error is :" + mainError);
							}
						} else {
							append("There are no errors in the file\n", Color.BLACK);
						}

					}
				} catch (XMLStreamException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}finally {
					pk.finished();
				}
				return "works";
			}

			@Override
			protected void done() {
				if (pk.isCancelled()) {
					JOptionPane.showMessageDialog(null, "Validation cancelled");
					pk.finished();
				}
			}
		};

		sw.execute();
		d.setVisible(true);

	}



	/**
	 * This method appends the error messages generated by the SBMLError in the validate
	 * function to the text pane.
	 * 
	 * This adds color to the text which is being added to the text pane.
	 * 
	 * @param s
	 * @param c
	 */
	public void append(String s, Color c) {
		validationResultStyledDoc = validationResultPane.getStyledDocument();
		SimpleAttributeSet keyword = new SimpleAttributeSet();
		StyleConstants.setForeground(keyword, c);

		StyleConstants.setBold(keyword, true);
		try {

			validationResultStyledDoc.insertString(validationResultStyledDoc.getLength(), s, keyword);

		} catch (Exception e) {

		}
	}

}