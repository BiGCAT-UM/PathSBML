// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2014 BiGCaT Bioinformatics
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

import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import org.sbml.jsbml.SBMLDocument;

/**
 * Displays the content of an SBML file in a {@link JTree}
 * 
 * Derived from org.sbml.jsbml.test.gui.JSBMLvisualizer, with significant
 * changes.
 * 
 * @author applecool
 * @author anwesha
 * @version 1.0.0
 */

@SuppressWarnings("serial")
public class JSBMLvisualizer extends JFrame
{

	/** @param document The sbml root node of an SBML file */
	public JSBMLvisualizer(SBMLDocument document) {
		super("JSBML viz");
		getContentPane().add(new JScrollPane(new JTree(document)));
		pack();
	}

	/**
	 * @param parent
	 */
	public void createAndShow (Component parent)
	{
		setLocationRelativeTo(parent);
		setVisible(true);
	}

}