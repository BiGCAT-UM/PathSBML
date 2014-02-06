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

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
/**
 * This class helps to visualize the components of the SBML document in the 
 * tree model format.
 * @author applecool
 *
 */
public class NavigationTree {
	public static final String COMPARTMENTS = "Compartments";
	public static final String SPECIES = "Species";
	public static final String REACTIONS = "Reactions";
	private Map<String, NamedSBase> objectMap;
	private Map<String, TreePath> objectPathMap;
	private DefaultTreeModel treeModel;

	public Map<String, NamedSBase> getObjectMap() {
		return this.objectMap;
	}

	public Map<String, TreePath> getObjectPathMap() {
		return this.objectPathMap;
	}

	public DefaultTreeModel getTreeModel() {
		return this.treeModel;
	}

	public NavigationTree() {
		this.objectMap = new HashMap();
		this.objectPathMap = new HashMap();

		this.treeModel = new DefaultTreeModel(
				new DefaultMutableTreeNode("sbml"));
	}
	
	/**
	 * 
	 * @param document
	 */
	public NavigationTree(SBMLDocument document) {
		this();

		Model model = document.getModel();
		String modelName = getModelNameFromModel(model);
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(modelName);
		this.treeModel = new DefaultTreeModel(top);
		addListOfCompartmentsToTreeModel(top, model.getListOfCompartments());
		addListOfSpeciesToTreeModel(top, model.getListOfSpecies());
		addListOfReactionsToTreeModel(top, model.getListOfReactions());

	}

	public NamedSBase getNamedSBaseById(String id) {
		NamedSBase nsb = null;
		if (this.objectMap.containsKey(id)) {
			return (NamedSBase) this.objectMap.get(id);
		}
		return nsb;
	}
	
	/**
	 * This method adds the list of components to the tree model.
	 * 
	 * @param top
	 * @param compartmentList
	 */
	private void addListOfCompartmentsToTreeModel(DefaultMutableTreeNode top,
			ListOf<Compartment> compartmentList) {
		addListOfNamedSBaseToTreeModel(top,
				createTreeNodeForName("Compartments"), compartmentList);
	}
	
	/**
	 * This method adds the list of species to the tree model.
	 * @param top
	 * @param speciesList
	 */
	private void addListOfSpeciesToTreeModel(DefaultMutableTreeNode top,
			ListOf<Species> speciesList) {
		addListOfNamedSBaseToTreeModel(top, createTreeNodeForName("Species"),
				speciesList);
	}
	
	/**
	 * This methods adds the list of reactions to the tree model.
	 * @param top
	 * @param reactionList
	 */
	private void addListOfReactionsToTreeModel(DefaultMutableTreeNode top,
			ListOf<Reaction> reactionList) {
		addListOfNamedSBaseToTreeModel(top, createTreeNodeForName("Reactions"),
				reactionList);
	}
	
	/**
	 * This method adds the list of namedSbase to the tree model.
	 * @param top
	 * @param category
	 * @param namedSBaseList
	 */
	private void addListOfNamedSBaseToTreeModel(DefaultMutableTreeNode top,
			DefaultMutableTreeNode category,
			ListOf<? extends NamedSBase> namedSBaseList) {
		if (namedSBaseList.size() > 0) {
			top.add(category);
			for (NamedSBase namedSBase : namedSBaseList) {
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(
						namedSBase, false);
				category.add(node);
				String id = namedSBase.getId();
				TreePath path = new TreePath(node.getPath());
				this.objectMap.put(id, namedSBase);
				this.objectPathMap.put(id, path);
			}
		}
	}
	
	/**
	 * This method creates a tree node with the name which will be SBML model name in general.
	 * 
	 * @param name
	 * @return
	 */
	private DefaultMutableTreeNode createTreeNodeForName(String name) {
		return new DefaultMutableTreeNode(name, true);
	}
	
	/**
	 * This method gets the name of the model which is imported.
	 * @param model
	 * @return name
	 */
	private String getModelNameFromModel(Model model) {
		String name = model.getId();
		if (name.equals("")) {
			name = model.getName();
		}
		return name;
	}
}