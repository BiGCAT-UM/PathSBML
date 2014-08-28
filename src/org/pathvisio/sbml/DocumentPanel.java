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

import java.awt.BorderLayout;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.pathvisio.core.ApplicationEvent;
import org.pathvisio.core.Engine;
import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.sbml.jsbml.Species;

/**
 * This class adds action to the SBML side pane.
 * 
 * When there is an active pathway, the side pane displays the components of the
 * SBML file.
 * 
 * @author applecool
 * @author anwesha
 * @version 1.0.0
 * 
 */
public class DocumentPanel extends JPanel implements ApplicationEventListener {
	// private SBMLDocument lastImported = null;
	// Desktop desktop;
	JScrollPane treePane = new JScrollPane();
	private final ExecutorService executor;
	private Engine engine;
	private String selectednode;
	Pathway pathway;

	// SBMLDocument document;

	public DocumentPanel(Engine engine) {
		setLayout(new BorderLayout());
		treePane = new JScrollPane(new JTree(SBMLFormat.modelDoc));
		add(treePane);
		engine.addApplicationEventListener(this);
		this.engine = engine;
		executor = Executors.newSingleThreadExecutor();

	}

	/**
	 * Clears the backpage if a new pathway is opened & Shows information about
	 * the currently opened model
	 * 
	 */
	@Override
	public void applicationEvent(ApplicationEvent e) {
		switch (e.getType()) {
		case PATHWAY_NEW:
			clean();
			break;
		case PATHWAY_OPENED:
			doQuery();
			break;
		}

	}

	// /**
	// * This method is invoked in the applicationEvent if a new model is opened
	// *
	// * @param modelDoc
	// */
	// private void setInput() {
	// // lastImported = doc;
	// doQuery();
	// }

	/**
	 * This method is invoked by the setInput function.
	 * 
	 * This method adds and removes the tree component from the side pane.
	 * 
	 */
	private void doQuery() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				if (SBMLFormat.modelDoc == null)
					return;

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						remove(treePane);
						final JTree elementTree = new JTree();
						elementTree.setEditable(true);
						
				        MyTreeCellEditor editor = new MyTreeCellEditor(elementTree,(DefaultTreeCellRenderer) elementTree.getCellRenderer());
				        elementTree.setCellEditor(editor);
						elementTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
						elementTree.addTreeSelectionListener(new TreeSelectionListener() {
									public void valueChanged(TreeSelectionEvent e) {
										DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getLastSelectedPathComponent();
										if (node == null)
											return;
										Object nodeInfo = node.getUserObject();
										if (node.isLeaf()) {
											if (nodeInfo instanceof Species) {
												Species sp = (Species) nodeInfo;
												selectednode = sp.getId();

											} else {
												selectednode = nodeInfo.toString();
											}

										}
									}
								});

						TreeModel elementModel = new NavigationTree(SBMLFormat.modelDoc).getTreeModel();
						elementModel.addTreeModelListener(new MyTreeModelListener());
						elementTree.setModel(elementModel);
						treePane = new JScrollPane(elementTree);
						add(treePane);
						revalidate();
						repaint();
					}
				});
			}
		});
	}

	private void clean() {

		executor.execute(new Runnable() {
			@Override
			public void run() {

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						remove(treePane);
						revalidate();
						repaint();
					}
				});
			}
		});
	}

	private static class MyTreeCellEditor extends DefaultTreeCellEditor {

        public MyTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
        }

       

        @Override
        public boolean isCellEditable(EventObject e) {
            return super.isCellEditable(e)
                && ((TreeNode) lastPath.getLastPathComponent()).isLeaf();
        }
    }
	class MyTreeModelListener implements TreeModelListener {
		public void treeNodesChanged(TreeModelEvent e) {
			DefaultMutableTreeNode node;
			node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());

			try {
				int index = e.getChildIndices()[0];
				node = (DefaultMutableTreeNode) (node.getChildAt(index));
			} catch (NullPointerException exc) {
			}

			// update the related objects
			pathway = engine.getActivePathway();
			PathwayElement pe = pathway.getElementById(selectednode);

			List<PathwayElement> pelemets = pathway.getDataObjects();
			
			for (Iterator iterator = pelemets.iterator(); iterator.hasNext();) {
				PathwayElement pathwayElement = (PathwayElement) iterator
						.next();
				if (pathwayElement.getObjectType() == ObjectType.LINE) {
					if (pathwayElement.getStartGraphRef().equalsIgnoreCase(selectednode))
						pathwayElement.setStartGraphRef((String) node.getUserObject());
					if (pathwayElement.getEndGraphRef().equalsIgnoreCase(selectednode))
						pathwayElement.setEndGraphRef((String) node.getUserObject());

				}

			}

			if (pe.getObjectType() == ObjectType.DATANODE) {
				pe.setTextLabel((String) node.getUserObject());
				pe.setGraphId((String) node.getUserObject());
			}
			if (pe.getObjectType() == ObjectType.LABEL) {
				if (pe.getGraphId().equalsIgnoreCase(selectednode))
					pe.setGraphId((String) node.getUserObject());

			}

		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {
			
			
		}

		@Override
		public void treeNodesRemoved(TreeModelEvent e) {
			
			
		}

		@Override
		public void treeStructureChanged(TreeModelEvent e) {
		
			
		}

		
	}
}
