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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.xml.stream.XMLStreamException;

import org.bridgedb.Xref;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.sbgn.SbgnFormat;
import org.pathvisio.sbml.peer.PeerModel;
import org.sbgn.ArcClazz;
import org.sbgn.GlyphClazz;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;



public class SbmlExportHelper {
		
	/**
	 * @throws ConverterException
	 */
	public void doExport() {
		System.out.println(pathway.getMappInfo().getMapInfoName());
		/*
		 * Only export if all interactions are connected
		 */
		if (testInteractionConnectivity()) {
			System.out.println("All interactions connected!");

			makePorts();
			
			for (PathwayElement elt : pathway.getDataObjects()) {
				addSpeciesReferences(elt);

			}
			for (PathwayElement elt : pathway.getDataObjects()) {
				sbgn2sbml(elt);

			}

//			 createfromPathway();

			SBMLDocument doc = new SBMLDocument();

			doc.setLevel(Integer.parseInt(pathway.getMappInfo()
					.getDynamicProperty("SBML_Level")));
			doc.setVersion(Integer.parseInt(pathway.getMappInfo()
					.getDynamicProperty("SBML_Version")));
			doc.setModel(doModel());

			try {
				SBMLWriter w = new SBMLWriter();

				w.writeSBMLToFile(doc, file.getAbsolutePath());
			} catch (SBMLException e) {
				JOptionPane.showMessageDialog(null,
						"SBML");
				e.printStackTrace();
			} catch (XMLStreamException e) {
				JOptionPane.showMessageDialog(null,
						"XML");
				e.printStackTrace();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						"IO");
				e.printStackTrace();
			}
		} else {
			System.out.println("WARNING :" + pathway.getMappInfo().getMapInfoName()
					+ "could not be converted");
			JOptionPane.showMessageDialog(null,
					"Unconnected interactions found! Model cannot be exported");
		}
	}



	private final Pathway pathway;
	private final File file;
	private boolean UpdatingSbml = false;

	private final Map<String, String> portmatrix = new HashMap<String, String>();
	private final Map<String, String> reactionmatrix = new HashMap<String, String>();
	ListOf<SpeciesReference> listOfSpeciesReferences = new ListOf<SpeciesReference>();

	ListOf<Species> listOfSpecies = new ListOf<Species>();
	private final ListOf<Reaction> listOfReactions = new ListOf<Reaction>();

	SbmlExportHelper(File file, Pathway pathway) {
		this.pathway = pathway;
		this.file = file;
	}


	private void addReactions(PathwayElement elt) {
		Reaction r = new Reaction();
		r.setId(elt.getGraphId());
		for (Map.Entry<String, String> entry : reactionmatrix.entrySet()) {
			System.out.println("Reaction : " + entry);
			String[] s = entry.getKey().split("-", 3);
			if (s[2].equalsIgnoreCase("2")) {
				System.out.println("s2 = " + s[2]);
				if (elt.getGraphId().equals(s[0])) {
					System.out.println("graphid = " + s[0]);
					SpeciesReference sr = r.createReactant();
					try {
						sr.setSpecies(new Species(s[1]));
					} catch (NullPointerException ne) {
						System.err
						.println("Reactant not connected properly... Skipping reaction");
					}
					System.out.println("reactant = " + s[1]);
				}
			}
			if (s[2].equalsIgnoreCase("1"))
			{
				if (elt.getGraphId().equals(s[0])) {
					System.out.println("graphid = " + s[0]);
					SpeciesReference sr = r.createProduct();
					try {
						sr.setSpecies(new Species(s[1]));
					} catch (NullPointerException ne) {
						System.err
						.println("Product not connected properly... Skipping reaction");
					}

					System.out.println("product = " + s[1]);

				}
			}
		}

		listOfReactions.add(r);
	}


	private void addSpecies(PathwayElement elt, GlyphClazz gc) {
		try {
			Species sp = new Species();
			sp.setId(elt.getGraphId());
			if (!elt.getElementID().isEmpty()
					|| elt.getDataSource().getFullName().isEmpty()) {
				if (sbmlAnnotate(elt).isSetAnnotation()) {
					sp.setAnnotation(sbmlAnnotate(elt));
				} else {
					System.out.println("Annotation element not properly set");
				}
			}
			listOfSpecies.add(sp);
		} catch (NullPointerException e) {
			System.err.println("Null species... Skipping");
		}

	}

	public void addSpeciesReferences(PathwayElement elt) {
		for (Map.Entry<String, String> entry : portmatrix.entrySet()) {
			if (entry.getKey().equals(elt.getStartGraphRef())) {
				reactionmatrix.put(
						entry.getValue() + "-" + elt.getEndGraphRef() + "-1",
						elt.getEndGraphRef());// reaction,species
			}
			if (entry.getKey().equals(elt.getEndGraphRef())) {
				reactionmatrix.put(
						entry.getValue() + "-" + elt.getStartGraphRef() + "-2",
						elt.getStartGraphRef());// reaction,species
			}

		}
	}

	// private void createfromPathway() {
	// SBMLDocument doc = new SBMLDocument();
	//
	// doc.setLevel(Integer.parseInt(pathway.getMappInfo().getDynamicProperty(
	// "SBML_Level")));
	// doc.setVersion(Integer.parseInt(pathway.getMappInfo()
	// .getDynamicProperty("SBML_Version")));
	// doc.setModel(doModel());
	//
	// try {
	// SBMLWriter w = new SBMLWriter();
	//
	// w.writeSBMLToFile(doc, file.getAbsolutePath());
	// } catch (SBMLException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (XMLStreamException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	private boolean testInteractionConnectivity() {
		boolean answer = true;
		for (PathwayElement pe : pathway.getDataObjects()) {
			if (pe.getObjectType() == ObjectType.LINE) {
				String grs = pe.getStartGraphRef();
				String gre = pe.getEndGraphRef();
				if (grs == null || "".equals(grs) || gre == null
						|| "".equals(gre)) {
					answer = false;
				}
			}
		}
		return answer;

	}

	private Model doModel() {
		Model model = new Model();
		model.setId(pathway.getMappInfo().getMapInfoName());
		model.setListOfSpecies(listOfSpecies);
		model.setListOfReactions(listOfReactions);
		return model;
	}

	/**
	 * Creates the Map for each ProcessNode (i.e) Map of each ProcessNode with
	 * its COnnected line-GraphId, starting Graph Reference,
	 * 
	 * @param pwy
	 */
	protected void makePorts() {
		System.out.println("Making ports");
		for (PathwayElement elt : pathway.getDataObjects()) {
			switch (elt.getObjectType()) {
			case LINE: {
				// ports have been dealt with already, skip.
				if ("true".equals(elt
						.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_IS_PORT))) {
					portmatrix.put(elt.getMAnchors().get(0).getGraphId(),
							elt.getStartGraphRef());
				}

			}
			default:
				break;
			}
		}

	}

	// @Override
	// public void pathwayModified(PathwayEvent e) {
	// if (updatingSbml)
	// return;
	// switch (e.getType()) {
	// case PathwayEvent.ADDED:
	// addElement(e.getAffectedData());
	// break;
	// case PathwayEvent.DELETED:
	// removeElement(e.getAffectedData());
	// break;
	// }
	// }
	/**
	 * @param e
	 *            Pathwayevents
	 */
	// @Override
	// public void pathwayModified(PathwayEvent e) {
	// setUpdatingSbml(true);
	// switch (e.getType()) {
	// case PathwayEvent.ADDED:
	// sbgn2sbml(e.getAffectedData());
	// // br.getDoc().fireNodeAddedEvent();
	// break;
	// case PathwayEvent.DELETED:
	// sbgn2sbml(e.getAffectedData());
	// // br.getDoc().fireNodeRemovedEvent();
	// break;
	//
	//
	// }
	// }

	/**
	 * Converts a SBGN pathway diagram to SBML format
	 * 
	 * @param elt
	 *            pathway elements
	 */
	public void sbgn2sbml(PathwayElement elt) {
		String sbgnClass = elt
				.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_CLASS);

		if (sbgnClass != null) {
			if (elt.getObjectType() == ObjectType.LINE) {
				ArcClazz ac = ArcClazz.fromClazz(sbgnClass);
				switch (ac) {
				case CONSUMPTION:
				case PRODUCTION:
				case CATALYSIS:
				case STIMULATION:
					addReactions(elt);
				}
			} else {
				GlyphClazz gc = GlyphClazz.fromClazz(sbgnClass);
				switch (gc) {
				case PROCESS:
				case UNCERTAIN_PROCESS:
				case OMITTED_PROCESS:
				case ASSOCIATION:
				case DISSOCIATION:
					addReactions(elt);
					break;
				case SIMPLE_CHEMICAL:
				case SIMPLE_CHEMICAL_MULTIMER:
				case MACROMOLECULE:
				case MACROMOLECULE_MULTIMER: {
					addSpecies(elt, gc);

				}
				break;
				}
			}
		}
	}

	private void setUpdatingSbml(boolean b) {
		UpdatingSbml = b;
	}



	private Annotation sbmlAnnotate(PathwayElement elt) {
		Annotation annotation = new Annotation();
		CVTerm term = new CVTerm();
		Xref xref = elt.getXref();
		// term.setBiologicalQualifierType(Qualifier.BQB_IS);
		// term.fireNodeRemovedEvent();
		// CVTerm.Qualifier.BQB_IS, "miriam";
		// String xrefString = "http://identifiers.org/";
		// String identifier = "";
		// String database = "";

		// identifier = elt.getElementID();
		// database = elt.getDataSource().getFullName();
		//
		// if (database.contains("chebi")) {
		// database = database.replace("chebi", "obo.chebi");
		// }
		// if (database.contains("pubchem")) {
		// database = database.replace("pubchem", "pubchem.compound");
		// }
		// System.out.println("Xref - URN" + xrefString + database + "/"
		// + identifier);
		term.addResource(xref.getURN());
		annotation.addCVTerm(term);
		annotation.setAbout("#metaid_" + elt.getTextLabel());
		System.out.println("urn - about = " + annotation.getAbout());
		return annotation;
	}

}