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

package org.pathvisio.sbml.peer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.sbgn.SbgnTemplates;
import org.sbgn.ArcClazz;
import org.sbgn.GlyphClazz;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.layout.ExtendedLayoutModel;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualConstant;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

public class PeerModel {

	private final SBMLDocument doc;
	private final Pathway pwy;

	private boolean updatingSbml = false;

	/**
	 * @param doc
	 * @param pwy
	 */
	public PeerModel(SBMLDocument doc, Pathway pwy) {
		this.doc = doc;
		this.pwy = pwy;

	}

	public Pathway getPathway() {
		return pwy;
	}

	public SBMLDocument getDoc() {
		return doc;
	}

	public Model getModel() {
		return doc.getModel();
	}

	private void addSpecies(PathwayElement elt, GlyphClazz gc) {
		PeerSpecies bs = PeerSpecies.createFromElt(this, elt, gc);
		speciesPeers.put(elt.getGraphId(), bs);
	}

	private final Map<String, PeerSpecies> speciesPeers = new HashMap<String, PeerSpecies>();

	public void putSpeciesPeer(String sId, PeerSpecies sbr) {
		speciesPeers.put(sId, sbr);
	}

	public PeerSpecies getSpeciesPeer(String sid) {
		return speciesPeers.get(sid);
	}


	/**
	 * Converts SBML doc to SBGN-PD pathway
	 * 
	 * @param doc
	 *            SBML document
	 * @return
	 */
	public static PeerModel createFromDoc(SBMLDocument doc) {
		Pathway pathway = new Pathway();
		pathway.getMappInfo().setMapInfoName(doc.getModel().getId());
		pathway.getMappInfo().setMapInfoDataSource("Converted from SBML");
		PeerModel bm = new PeerModel(doc, pathway);
		/*
		 * Save notes as comments
		 */
		if (doc.isSetNotes()) {
			try {
				pathway.getMappInfo().addComment(doc.getNotesString(), "SBML");
			} catch (XMLStreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		/*
		 * Save Annotation
		 */
		if (doc.isSetAnnotation()) {
			try {
				pathway.getMappInfo().addComment(doc.getAnnotationString(),
						"Annotation");
			} catch (XMLStreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (doc.isSetLevel()) {
			pathway.getMappInfo().setDynamicProperty("SBML_Level",
					String.valueOf(doc.getLevel()));
		} else {
			pathway.getMappInfo().setDynamicProperty("SBML_Level",
					String.valueOf(doc.getDefaultLevel()));
		}
		if (doc.isSetVersion()) {
			pathway.getMappInfo().setDynamicProperty("SBML_Version",
					String.valueOf(doc.getVersion()));
		} else {
			pathway.getMappInfo().setDynamicProperty("SBML_Version",
					String.valueOf(doc.getDefaultVersion()));
		}
		bm.sbml2sbgn();
		return bm;
	}

	private void sbml2sbgn() {
		updatingSbml = true;
		doReactions();
		doSpecies();
		doQual();
		doLayout();
		updatingSbml = false;
	}

	private void doSpecies() {
		// do remaining species

		for (Species s : doc.getModel().getListOfSpecies()) {
			// check it it was already added before
			String sid = s.getId();
			if (pwy.getElementById(sid) == null) {
				nextLocation();
				createOrGetSpecies(sid, xco, yco,
						GlyphClazz.BIOLOGICAL_ACTIVITY);
			}
		}
	}

	/** checks if the given SBML document uses the SBML-layout extension */
	private void doLayout() {
		Model model = doc.getModel();
		ExtendedLayoutModel sbase = (ExtendedLayoutModel) model
				.getExtension(LayoutConstants.namespaceURI);
		if (sbase != null) {
			for (Layout l : sbase.getListOfLayouts()) {
				// TODO: list of compartment glyphs, text glyphs, etc...
				for (SpeciesGlyph g : l.getListOfSpeciesGlyphs()) {
					String sid = g.getSpecies();
					PeerSpecies sbr = getSpeciesPeer(sid);
					if (sbr != null) {
						sbr.setSpeciesGlyph(g);

					}
				}
			}
		}
	}

	/** checks if the given SBML document uses the SBML-qual extension */
	private void doQual() {
		Model model = doc.getModel();
		QualitativeModel qualModel = (QualitativeModel) model
				.getExtension(QualConstant.namespaceURI);
		if (qualModel != null) {
			doQualitativeSpecies(qualModel);
			doTransitions(qualModel);
		}
	}


	/** used only in the SBML-qual extension */
	private void doQualitativeSpecies(QualitativeModel qualModel) {
		for (QualitativeSpecies qs : qualModel.getListOfQualitativeSpecies()) {
			// PathwayElement pelt = createOrGetSpecies(qs.getId(), xco, yco,
			// GlyphClazz.BIOLOGICAL_ACTIVITY);
			PathwayElement pelt = SbgnTemplates.createGlyph(
					GlyphClazz.BIOLOGICAL_ACTIVITY, pwy, xco, yco);
			pelt.setGraphId(qs.getId());
			pelt.setTextLabel(qs.getName());

			List<String> t = qs
					.filterCVTerms(CVTerm.Qualifier.BQB_IS, "miriam");
			if (t.size() > 0) {
				Xref ref = Xref.fromUrn(t.get(0));
				if (ref == null) {
					System.out.println("WARNING: couldn't convert " + t.get(0)
							+ " to Xref");
				} else {
					pelt.setElementID(ref.getId());
					pelt.setDataSource(ref.getDataSource());
				}
			}

			pwy.add(pelt);

			nextLocation();
		}
	}

	/** used only in the SBML-qual extension */
	private void doTransitions(QualitativeModel qualModel) {
		for (Transition t : qualModel.getListOfTransitions()) {
			if (t.getListOfInputs().size() == 1
					&& t.getListOfOutputs().size() == 1) {
				Input i = t.getListOfInputs().get(0);
				Output o = t.getListOfOutputs().get(0);

				PathwayElement iElt = pwy.getElementById(i
						.getQualitativeSpecies());
				PathwayElement oElt = pwy.getElementById(o
						.getQualitativeSpecies());

				if (iElt == null || oElt == null) {
					System.out
					.println("WARNING: missing input or output qualitative species");
				} else {
					ArcClazz ac = null;
					switch (i.getSign()) {
					case dual:
						ac = ArcClazz.UNKNOWN_INFLUENCE;
						break;
					case positive:
						ac = ArcClazz.POSITIVE_INFLUENCE;
						break;
					case negative:
						ac = ArcClazz.NEGATIVE_INFLUENCE;
						break;
					case unknown:
						ac = ArcClazz.UNKNOWN_INFLUENCE;
						break;
					}
					PathwayElement arc = SbgnTemplates.createArc(pwy, ac,
							iElt.getMCenterX(), iElt.getMCenterY(), iElt,
							oElt.getMCenterX(), oElt.getMCenterY(), oElt);
					pwy.add(sbgnAnnotate(arc, i.getId()));
				}
			} else {
				// TODO more complex transition functions.
			}
		}
	}

	private void doReactions() {
		for (Reaction re : doc.getModel().getListOfReactions()) {
			double x = xco;
			double y = yco;
			PeerReaction pr = PeerReaction.createFromSbml(this, re, x, y);
			boolean next = true;
			if (re.getListOfReactants().size() > 0) {
				String sid = re.getProduct(0).getSpecies();
				PathwayElement pelt = pwy.getElementById(sid);
				if (pelt != null) {
					pelt.setGraphId(pwy.getUniqueGraphId());
					xco = pelt.getMCenterX() + 100;
					yco = pelt.getMCenterY();
					next = false;
				}
			}
			if (next) {
				nextLocation();
			}

			double yy = y;

			for (SpeciesReference j : re.getListOfProducts()) {
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x + 80, yy,
						GlyphClazz.SIMPLE_CHEMICAL);
				PeerSpeciesReference bsref = PeerSpeciesReference
						.createFromSpeciesReference(this, j,
								ArcClazz.PRODUCTION, x + M_PN, y,
								pr.getPortId(1), pelt.getMLeft(),
								pelt.getMCenterY(), pelt);
				pwy.add(sbgnAnnotate(bsref.getElement(), sid));
				yy += 20;
			}

			yy = y;

			for (SpeciesReference j : re.getListOfReactants()) {
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x - 80, yy,
						GlyphClazz.SIMPLE_CHEMICAL);
				PeerSpeciesReference bsref = PeerSpeciesReference
						.createFromSpeciesReference(this, j,
								ArcClazz.CONSUMPTION,
								pelt.getMLeft() + pelt.getMWidth(),
								pelt.getMCenterY(), pelt, x - M_PN, y,
								pr.getPortId(0));
				pwy.add(sbgnAnnotate(bsref.getElement(), sid));
				yy += 20;
			}

			for (ModifierSpeciesReference j : re.getListOfModifiers()) {
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x, y - 80,
						GlyphClazz.MACROMOLECULE);
				PeerSpeciesReference bsref = PeerSpeciesReference
						.createFromSpeciesReference(this, j,
								ArcClazz.CATALYSIS, pelt.getMCenterX(),
								pelt.getMTop() + pelt.getMHeight(), pelt, x, y,
								pr.getProcessNodeElt());
				pwy.add(sbgnAnnotate(bsref.getElement(), sid));
			}
		}
	}

	private void nextLocation() {
		yco += 150;
		if (yco > 1000) {
			yco = 30;
			xco += 300;
		}
	}


	private PathwayElement createOrGetSpecies(String sId, double prefX,
			double prefY, GlyphClazz gc) {
		PathwayElement pelt = pwy.getElementById(sId);
		if (pelt == null) {
			Species sp = doc.getModel().getSpecies(sId);
			PeerSpecies sbr = PeerSpecies.createFromSpecies(this, sp, gc);
			putSpeciesPeer(sId, sbr);
			pelt = sbr.getSpeciesElement();
			pelt.setMCenterX(prefX);
			pelt.setMCenterY(prefY);
			pelt.setTextLabel(sId);
			pelt = sbgnAnnotate(pelt, sId);
			pwy.add(pelt);
		}
		return pelt;
	}



	PathwayElement sbgnAnnotate(PathwayElement pelt, String sId) {
		Boolean datasourceset = false;
		DataSource ds = DataSource.getExistingBySystemCode("L");
		if (doc.getModel().getSpecies(sId).isSetAnnotation()) {
			Annotation annotation = doc.getModel().getSpecies(sId)
					.getAnnotation();
			List<String> annotationList = annotation.getCVTerm(0)
					.getResources();
			String xrefString = annotationList.get(0);

			String[] de = xrefString.split("org/", 2);
			String[] xe = de[1].split("/", 2);

			String database = xe[0];
			String identifier = xe[1];
			/*
			 * Ontology databases
			 */
			if (database.contains("chebi") || database.contains("CHEBI")) {
				datasourceset = true;
				ds = DataSource.getExistingBySystemCode("Ce");
			}else if (database.contains("pubchem.substance") || database.contains("PUBCHEM")) {
				datasourceset = true;
				ds = DataSource.getExistingBySystemCode("Cps");
			}else if (database.contains("kegg") || database.contains("KEGG")) {
				datasourceset = true;
				ds = DataSource.getExistingBySystemCode("Ck");
			}else if (database.contains("cas") || database.contains("CAS")) {
				datasourceset = true;
				ds = DataSource.getExistingBySystemCode("Ca");
			}else {
				if (database.equalsIgnoreCase("obo.go") || database.equalsIgnoreCase("go")) {
					datasourceset = true;
					ds = DataSource.getExistingBySystemCode("T");
				}else {
					if (database.contains("uniprot") || database.contains("UNIPROT")) {
						datasourceset = true;
						ds = DataSource.getExistingBySystemCode("S");
					}else {
						try {
							ds = DataSource.getExistingByFullName(database.toUpperCase());
							if(ds == null){
							ds = DataSource.getExistingByFullName(database.toLowerCase());
							}
							datasourceset = true;
						} catch (Exception e) {
							System.out
							.println("WARNING : Could not find a match for "
									+ database);
						}
					}

				}
			}
			if (datasourceset) {
				pelt.setDataSource(ds);
				pelt.setElementID(identifier);
				if (ds.getType().equalsIgnoreCase("Protein")) {
					pelt.setDataNodeType(DataNodeType.PROTEIN);
				} else if (ds.getType().equalsIgnoreCase("Metabolite")) {
					pelt.setDataNodeType(DataNodeType.METABOLITE);
				} else {
					pelt.setDataNodeType(DataNodeType.GENEPRODUCT);
				}
				datasourceset = false;
			}
		}
		return pelt;
	}

	private double xco = 500;
	private double yco = 500;
	final static double M_WIDTH = 80;
	final static double M_HEIGHT = 30;
	final static double M_PN = 20;

}