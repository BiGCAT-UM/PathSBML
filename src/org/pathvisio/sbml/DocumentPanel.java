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
import java.awt.Desktop;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import org.pathvisio.core.ApplicationEvent;
import org.pathvisio.core.ApplicationEvent.Type;
import org.pathvisio.core.Engine;
import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.gui.SwingEngine;
import org.sbml.jsbml.SBMLDocument;

/**
 * This class adds action to the SBML side pane.
 * 
 * When there is an active pathway, the side pane displays the components of
 * the SBML file.
 * 
 * @author applecool
 *
 */
public class DocumentPanel extends JPanel implements ApplicationEventListener {
	private SwingEngine eng;
	private JPanel drawPanel;
	private SBMLDocument lastImported = null;
	Engine engine;
	Desktop desktop;
	private JScrollPane treePane = new JScrollPane();
	private ExecutorService executor;

	public DocumentPanel(SwingEngine eng) {
		// TODO Auto-generated constructor stub
		this.eng = eng;
		setLayout(new BorderLayout());
		treePane = new JScrollPane(new JTree(SBMLFormat.doc));
		add(treePane);
		eng.getEngine().addApplicationEventListener(this);
		executor = Executors.newSingleThreadExecutor();
	}
	
	/**
	 * This method sets the input as the SBML Document if a new pathway is opened.
	 * 
	 */
	@Override
	public void applicationEvent(ApplicationEvent e) {
		// TODO Auto-generated method stub

		if (e.getType() == Type.PATHWAY_NEW
				|| e.getType() == Type.PATHWAY_OPENED)

		{
			setInput(SBMLFormat.doc);

		}

	}
	
	/**
	 * This method is invoked in the applicationEvent if the pathway is new
	 * or if a new pathway is opened.
	 *  
	 * @param doc
	 */
	private void setInput(SBMLDocument doc) {
		// TODO Auto-generated method stub
		lastImported = doc;
		doQuery();
	}
	
	/**
	 * This method is invoked by the setInput function.
	 * 
	 * This method adds and removes the tree component from the side pane.
	 * 
	 */
	private void doQuery() {
		// TODO Auto-generated method stub

		executor.execute(new Runnable() {
			public void run() {
				if (lastImported == null)
					return;

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						remove(treePane);
						JTree elementTree = new JTree();
						TreeModel elementModel = new NavigationTree(
								SBMLFormat.doc).getTreeModel();
						elementTree.setModel(elementModel);
						treePane = new JScrollPane(elementTree);
						add(treePane);
					}
				});
			}
		});
	}
}