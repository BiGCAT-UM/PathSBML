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

package org.pathvisio.sbml.peer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayEvent;
import org.pathvisio.core.model.PathwayListener;
import org.pathvisio.sbgn.SbgnFormat;
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

/**
 * @author anwesha
 * 
 */
public class PeerModel implements PathwayListener
{
	private final Pathway pwy;

	private boolean updatingSbml = false;

	/**
	 * @param doc
	 * @param pwy
	 */
	public PeerModel(final SBMLDocument doc, final Pathway pwy)
	{
		this.pwy = pwy;
		pwy.addListener(this);
	}

	/**
	 * @return
	 */
	public Pathway getPathway() { return getPwy(); }

	/**
	 * @return
	 */
	public SBMLDocument getDoc() {
		return getDoc();
	}

	/**
	 * @param elt
	 */
	public void addElement(PathwayElement elt)
	{
		String sbgnClass = elt.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_CLASS);
		if (sbgnClass != null)
		{
			if (elt.getObjectType() == ObjectType.LINE)
			{
				ArcClazz ac = ArcClazz.fromClazz(sbgnClass);
				switch (ac)
				{
				case CONSUMPTION:
				case PRODUCTION:
				case CATALYSIS:
				case STIMULATION:
					addSpeciesReference (elt);
				}
			}
			else
			{
				GlyphClazz gc = GlyphClazz.fromClazz(sbgnClass);
				switch (gc)
				{
				case PROCESS:
				case UNCERTAIN_PROCESS:
				case OMITTED_PROCESS:
				case ASSOCIATION:
				case DISSOCIATION:
					addReaction (elt);
					break;
				case SIMPLE_CHEMICAL:
				case SIMPLE_CHEMICAL_MULTIMER:
				case MACROMOLECULE:
				case MACROMOLECULE_MULTIMER:
					addSpecies (elt, gc);
					break;
				}
			}
		}
		else
		{
			// we only handle SBGN elements for now.
		}

	}

	private void addSpeciesReference(PathwayElement elt)
	{

	}

	private void addSpecies(PathwayElement elt, GlyphClazz gc)
	{
		PeerSpecies bs = PeerSpecies.createFromElt(this, elt, gc);
		getSpeciesPeers().put(elt.getGraphId(), bs);
	}

	private void addReaction(PathwayElement elt)
	{

	}

	private final Map<String, PeerSpecies> speciesPeers = new HashMap<String, PeerSpecies>();

	/**
	 * @param sId
	 * @param sbr
	 */
	public void putSpeciesPeer(final String sId, final PeerSpecies sbr)
	{
		getSpeciesPeers().put(sId, sbr);
	}

	/**
	 * @param sid
	 * @return
	 */
	public PeerSpecies getSpeciesPeer(final String sid)
	{
		return getSpeciesPeers().get(sid);
	}

	/**
	 * @return
	 */
	public Model getModel()
	{
		return getDoc().getModel();
	}

	@Override
	public void pathwayModified(PathwayEvent e)
	{
		if (isUpdatingSbml()) return;
		switch (e.getType())
		{
		case PathwayEvent.ADDED:
			addElement(e.getAffectedData());
			break;
		case PathwayEvent.DELETED:
			removeElement(e.getAffectedData());
			break;
		}
	}

	private void removeElement(PathwayElement affectedData)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @param doc
	 * @param file
	 * @return
	 */
	public static PeerModel createFromDoc(final SBMLDocument doc,
			final File file)
	{
		Pathway pathway = new Pathway();
		pathway.getMappInfo().setMapInfoName(file.getName());
		pathway.getMappInfo().setMapInfoDataSource("Converted from SBML");
		PeerModel bm = new PeerModel (doc, pathway);
		bm.updateModel();
		return bm;
	}

	private void updateModel()
	{
		setUpdatingSbml(true);
		doReactions();
		doSpecies();

		doQual();
		doLayout();
		/*
		 * why is it first set to true then false ?
		 */
		// setUpdatingSbml(false);
	}

	private void doSpecies()
	{
		// do remaining species

		for (Species s : getDoc().getModel().getListOfSpecies())
		{
			// check it it was already added before
			String sid = s.getId();
			if (getPwy().getElementById(sid) == null)
			{
				nextLocation();
				createOrGetSpecies(sid, getXco(), getYco(), GlyphClazz.BIOLOGICAL_ACTIVITY);
			}
		}
	}

	/** checks if the given SBML document uses the SBML-layout extension */
	private void doLayout()
	{
		Model model = getDoc().getModel();
		ExtendedLayoutModel sbase = (ExtendedLayoutModel)model.getExtension(LayoutConstants.namespaceURI);
		if (sbase != null)
		{
			for (Layout l : sbase.getListOfLayouts())
			{
				// TODO: list of compartment glyphs, text glyphs, etc...
				for (SpeciesGlyph g : l.getListOfSpeciesGlyphs())
				{
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
	private void doQual()
	{
		Model model = getDoc().getModel();
		QualitativeModel qualModel = (QualitativeModel)model.getExtension(QualConstant.namespaceURI);
		if (qualModel != null)
		{
			doQualitativeSpecies(qualModel);
			doTransitions(qualModel);
		}
	}


	/** used only in the SBML-qual extension */
	private void doQualitativeSpecies(QualitativeModel qualModel)
	{
		for (QualitativeSpecies qs : qualModel.getListOfQualitativeSpecies())
		{
			//			PathwayElement pelt = createOrGetSpecies(qs.getId(), xco, yco, GlyphClazz.BIOLOGICAL_ACTIVITY);
			PathwayElement pelt = SbgnTemplates.createGlyph(GlyphClazz.BIOLOGICAL_ACTIVITY, getPwy(), getXco(), getYco());
			pelt.setGraphId(qs.getId());
			pelt.setTextLabel(qs.getName());

			List<String> t = qs.filterCVTerms(CVTerm.Qualifier.BQB_IS, "miriam");
			if (t.size() > 0)
			{
				Xref ref = Xref.fromUrn(t.get(0));
				if (ref == null)
				{
					System.out.println ("WARNING: couldn't convert " + t.get(0) + " to Xref");
				}
				else
				{
					pelt.setElementID(ref.getId());
					pelt.setDataSource(ref.getDataSource());
				}
			}

			getPwy().add(pelt);

			nextLocation();
		}
	}

	/** used only in the SBML-qual extension */
	private void doTransitions(QualitativeModel qualModel)
	{
		for (Transition t : qualModel.getListOfTransitions())
		{
			if (t.getListOfInputs().size() == 1 &&
					t.getListOfOutputs().size() == 1)
			{
				Input i = t.getListOfInputs().get(0);
				Output o = t.getListOfOutputs().get(0);

				PathwayElement iElt = getPwy().getElementById(i.getQualitativeSpecies());
				PathwayElement oElt = getPwy().getElementById(o.getQualitativeSpecies());

				if (iElt == null || oElt == null)
				{
					System.out.println ("WARNING: missing input or output qualitative species");
				}
				else
				{
					ArcClazz ac = null;
					switch (i.getSign())
					{
					case dual: ac = ArcClazz.UNKNOWN_INFLUENCE; break;
					case positive: ac = ArcClazz.POSITIVE_INFLUENCE; break;
					case negative: ac = ArcClazz.NEGATIVE_INFLUENCE; break;
					case unknown: ac = ArcClazz.UNKNOWN_INFLUENCE; break;
					}
					PathwayElement arc = SbgnTemplates.createArc(getPwy(), ac, iElt.getMCenterX(), iElt.getMCenterY(), iElt, oElt.getMCenterX(), oElt.getMCenterY(), oElt);
					getPwy().add(arc);
				}
			}
			else
			{
				//TODO more complex transition functions.
			}
		}
	}

	private void doReactions()
	{
		for (Reaction re : getDoc().getModel().getListOfReactions())
		{
			double x = getXco();
			double y = getYco();
			PeerReaction pr = PeerReaction.createFromSbml(this, re, x, y);

			boolean next = true;
			if (re.getListOfReactants().size() > 0)
			{
				String sid = re.getProduct(0).getSpecies();
				PathwayElement pelt = getPwy().getElementById(sid);
				if (pelt != null)
				{
					setXco(pelt.getMCenterX() + 100);
					setYco(pelt.getMCenterY());
					next = false;
				}
			}
			if (next) {
				nextLocation();
			}


			double yy = y;

			for (SpeciesReference j : re.getListOfProducts())
			{
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x + 80, yy, GlyphClazz.SIMPLE_CHEMICAL);
				PeerSpeciesReference bsref = PeerSpeciesReference.createFromSpeciesReference (this, j, ArcClazz.PRODUCTION, x + M_PN, y, pr.getPortId(1), pelt.getMLeft(), pelt.getMCenterY(), pelt);
				getPwy().add(bsref.getElement());
				yy += 20;
			}

			yy = y;

			for (SpeciesReference j : re.getListOfReactants())
			{
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x - 80, yy, GlyphClazz.SIMPLE_CHEMICAL);
				PeerSpeciesReference bsref = PeerSpeciesReference.createFromSpeciesReference (this, j, ArcClazz.CONSUMPTION, pelt.getMLeft() + pelt.getMWidth(), pelt.getMCenterY(), pelt, x - M_PN, y, pr.getPortId(0));
				getPwy().add(bsref.getElement());
				yy += 20;
			}

			for (ModifierSpeciesReference j : re.getListOfModifiers())
			{
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x, y - 80, GlyphClazz.MACROMOLECULE);
				PeerSpeciesReference bsref = PeerSpeciesReference.createFromSpeciesReference (this, j, ArcClazz.CATALYSIS, pelt.getMCenterX(), pelt.getMTop() + pelt.getMHeight(), pelt, x, y, pr.getProcessNodeElt());
				getPwy().add(bsref.getElement());
			}
		}
	}

	private void nextLocation()
	{
		setYco(getYco() + 150);
		if (getYco() > 1000)
		{
			setYco(30);
			setXco(getXco() + 300);
		}
	}

	private PathwayElement createOrGetSpecies (String sId, double prefX, double prefY, GlyphClazz gc)
	{
		PathwayElement pelt = getPwy().getElementById(sId);
		if (pelt == null)
		{
			Species sp = getDoc().getModel().getSpecies(sId);

			PeerSpecies sbr = PeerSpecies.createFromSpecies(this, sp, gc);
			putSpeciesPeer (sId, sbr);
			pelt = sbr.getSpeciesElement();
			pelt.setMCenterX(prefX);
			pelt.setMCenterY(prefY);
			pelt.setTextLabel(sId);
			Annotation annotation = getDoc().getModel().getSpecies(sId)
					.getAnnotation();
			for (int i = 0; i < annotation.getCVTermCount(); i++) {

				List<String> li = annotation.getCVTerm(i).getResources();
				for (String string : li) {
					String[] de = string.split("org/",2 );
					String[] xe = de[1].split("/",2);
					DataSource ds = DataSource.getByFullName(xe[0]);


					pelt.setDataSource(ds);
					pelt.setElementID(xe[1]);

				}
			}

			getPwy().add(pelt);
		}
		return pelt;
	}

	/**
	 * @return the pwy
	 */
	public Pathway getPwy() {
		return this.pwy;
	}

	/**
	 * @return the updatingSbml
	 */
	public boolean isUpdatingSbml() {
		return this.updatingSbml;
	}

	/**
	 * @param updatingSbml the updatingSbml to set
	 */
	public void setUpdatingSbml(boolean updatingSbml) {
		this.updatingSbml = updatingSbml;
	}

	/**
	 * @return the xco
	 */
	public double getXco() {
		return this.xco;
	}

	/**
	 * @param xco the xco to set
	 */
	public void setXco(double xco) {
		this.xco = xco;
	}

	/**
	 * @return the yco
	 */
	public double getYco() {
		return this.yco;
	}

	/**
	 * @param yco the yco to set
	 */
	public void setYco(double yco) {
		this.yco = yco;
	}

	/**
	 * @return the speciesPeers
	 */
	public Map<String, PeerSpecies> getSpeciesPeers() {
		return speciesPeers;
	}

	double xco = 500;
	double yco = 500;
	final static double M_WIDTH = 80;
	final static double M_HEIGHT = 30;
	final static double M_PN = 20;

}
