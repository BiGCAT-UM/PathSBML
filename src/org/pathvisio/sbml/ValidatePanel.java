// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLReader;

/**
 * This class adds the validation functionality.It lets us validate the 
 * selected SBML file.
 * 
 * @author applecool
 *
 */
public class ValidatePanel extends JPanel implements ActionListener {

	JFileChooser fc;
	JButton openButton;
	JButton validateButton;
	JTextPane textPane;

	StyledDocument doc;
	final JLabel statusbar = new JLabel(
			"Output of your selection will appear here", SwingConstants.RIGHT);
	static String filename;

	public ValidatePanel() {

		super(new BorderLayout());
		// create a file chooser
		fc = new JFileChooser();
		// filtering the files based on their extensions.
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"SBML(Systems Biology Markup Language) (.sbml,.xml)", "sbml",
				"xml");
		fc.setFileFilter(filter);
		//create a new text pane. 
		textPane = new JTextPane();
		textPane.setEnabled(true);
		//added scroll bars to the text pane.
		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setPreferredSize(new Dimension(500, 400));
		scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		// buttons displayed in the validation dialog box.
		openButton = new JButton("Open");
		validateButton = new JButton("Validate the file");
		openButton.addActionListener(this);
		validateButton.addActionListener(this);
		
		//JPanel for the buttons with the borders.
		JPanel buttonPanel = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		buttonPanel.setLayout(gridbag);
		c.gridwidth = GridBagConstraints.REMAINDER; // last
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		buttonPanel.add(statusbar, c);
		buttonPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Button Pane"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		buttonPanel.add(openButton);
		buttonPanel.add(validateButton);
		buttonPanel.add(statusbar);
		// adding the buttonpanel in the center.
		add(buttonPanel, BorderLayout.CENTER);

		statusbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		
		//new panel for displaying the output with borders.
		JPanel outputPanel = new JPanel();
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
		add(outputPanel, BorderLayout.SOUTH);
	}
	
	/**
	 * This method adds the action to the "open" and "validate the file" buttons 
	 * in the button panel.
	 * 
	 * When clicked on the open button, a file chooser window gets opened
	 * which enables user to choose the sbml files.
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == openButton) {

			int returnVal = fc.showOpenDialog(ValidatePanel.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				filename = file.getPath();

				statusbar.setText("You chose" + " " + file.getName());
			} else {

				statusbar.setText("You cancelled.");
			}
		}

		else if (e.getSource() == validateButton) {

			validate();

		}

	}
	
	/**
	 * This method will validate the selected file.
	 * 
	 */
	public void validate() {

		String selectFile = ValidatePanel.filename;

		System.out.println("the file is " + selectFile);
		SBMLReader reader = new SBMLReader();
		SBMLDocument document = null;

		long start, stop;
		start = System.currentTimeMillis();
		try {
			document = reader.readSBML(selectFile);
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stop = System.currentTimeMillis();

		textPane.setText("");

		if (document.getErrorCount() > 0) {
			textPane.setText("Encountered the following errors while reading the SBML file:\n");
			document.printErrors(System.out);
			textPane.setText("\nFurther consistency checking and validation aborted.\n");
		} else {
			long errors = document.checkConsistency();
			long size = new File(selectFile).length();
			System.out.println("File Information: \n");
			System.out.println("            filename: " + selectFile + "\n");
			System.out.println("           file size: " + size + "\n");
			System.out
					.println("      read time (ms): " + (stop - start) + "\n");

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
		doc = textPane.getStyledDocument();
		SimpleAttributeSet keyword = new SimpleAttributeSet();
		StyleConstants.setForeground(keyword, c);

		StyleConstants.setBold(keyword, true);
		try {

			doc.insertString(doc.getLength(), s, keyword);

		} catch (Exception e) {

		}
	}

}