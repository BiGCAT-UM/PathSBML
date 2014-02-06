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
package org.pathvisio.sbml.peer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

public class PeerModel implements PathwayListener
{
	private final SBMLDocument doc;
	private final Pathway pwy;

	private boolean updatingPathway = false;
	private boolean updatingSbml = false;
	
	public PeerModel (SBMLDocument doc, Pathway pwy)
	{
		this.doc = doc;
		this.pwy = pwy;
		pwy.addListener(this);
	}
	
	public Pathway getPathway() { return pwy; }
	public SBMLDocument getDoc() { return doc; }
	
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
		speciesPeers.put(elt.getGraphId(), bs);
	}

	private void addReaction(PathwayElement elt)
	{

	}

	private Map<String, PeerSpecies> speciesPeers = new HashMap<String, PeerSpecies>();

	public void putSpeciesPeer(String sId, PeerSpecies sbr)
	{
		speciesPeers.put(sId, sbr);
	}

	public PeerSpecies getSpeciesPeer(String sid)
	{
		return speciesPeers.get(sid);
	}

	public Model getModel()
	{
		return doc.getModel();
	}

	@Override
	public void pathwayModified(PathwayEvent e)
	{
		if (updatingSbml) return;
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

	public static PeerModel createFromDoc(SBMLDocument doc, File file)
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
		updatingSbml = true;
		doReactions();
		doSpecies();
		
		doQual();
		doLayout();
		updatingSbml = false;
	}

	private void doSpecies()
	{
		// do remaining species
		
		for (Species s : doc.getModel().getListOfSpecies())
		{
			// check it it was already added before
			String sid = s.getId();
			if (pwy.getElementById(sid) == null)
			{
				nextLocation();
				createOrGetSpecies(sid, xco, yco, GlyphClazz.BIOLOGICAL_ACTIVITY);
			}
		}
	}
	
	/** checks if the given SBML document uses the SBML-layout extension */
	private void doLayout()
	{
		Model model = doc.getModel();
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
					if (sbr != null) sbr.setSpeciesGlyph(g);
				}
			}
		}		
	}
	
	/** checks if the given SBML document uses the SBML-qual extension */
	private void doQual()
	{
		Model model = doc.getModel();
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
			PathwayElement pelt = SbgnTemplates.createGlyph(GlyphClazz.BIOLOGICAL_ACTIVITY, pwy, xco, yco);
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
					pelt.setGeneID(ref.getId());
					pelt.setDataSource(ref.getDataSource());
				}
			}
			
			pwy.add(pelt);
			
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
				
				PathwayElement iElt = pwy.getElementById(i.getQualitativeSpecies());
				PathwayElement oElt = pwy.getElementById(o.getQualitativeSpecies());
				
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
					PathwayElement arc = SbgnTemplates.createArc(pwy, ac, iElt.getMCenterX(), iElt.getMCenterY(), iElt, oElt.getMCenterX(), oElt.getMCenterY(), oElt);
					pwy.add(arc);
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
		for (Reaction re : doc.getModel().getListOfReactions())
		{
			double x = xco;
			double y = yco;
			PeerReaction pr = PeerReaction.createFromSbml(this, re, x, y);
		
			boolean next = true;
			if (re.getListOfReactants().size() > 0)
			{
				String sid = re.getProduct(0).getSpecies();
				PathwayElement pelt = pwy.getElementById(sid);
				if (pelt != null)
				{
					xco = pelt.getMCenterX() + 100;
					yco = pelt.getMCenterY();
					next = false;
				}
			}
			if (next) nextLocation();
			
			
			double yy = y;
			
			for (SpeciesReference j : re.getListOfProducts())
			{
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x + 80, yy, GlyphClazz.SIMPLE_CHEMICAL);
				PeerSpeciesReference bsref = PeerSpeciesReference.createFromSpeciesReference (this, j, ArcClazz.PRODUCTION, x + M_PN, y, pr.getPortId(1), pelt.getMLeft(), pelt.getMCenterY(), pelt);
				pwy.add(bsref.getElement());
				yy += 20;
			}
			
			yy = y;
			
			for (SpeciesReference j : re.getListOfReactants())
			{
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x - 80, yy, GlyphClazz.SIMPLE_CHEMICAL);
				PeerSpeciesReference bsref = PeerSpeciesReference.createFromSpeciesReference (this, j, ArcClazz.CONSUMPTION, pelt.getMLeft() + pelt.getMWidth(), pelt.getMCenterY(), pelt, x - M_PN, y, pr.getPortId(0));
				pwy.add(bsref.getElement());
				yy += 20;
			}
			
			for (ModifierSpeciesReference j : re.getListOfModifiers())
			{
				String sid = j.getSpecies();
				PathwayElement pelt = createOrGetSpecies(sid, x, y - 80, GlyphClazz.MACROMOLECULE);
				PeerSpeciesReference bsref = PeerSpeciesReference.createFromSpeciesReference (this, j, ArcClazz.CATALYSIS, pelt.getMCenterX(), pelt.getMTop() + pelt.getMHeight(), pelt, x, y, pr.getProcessNodeElt());
				pwy.add(bsref.getElement());
			}
		}
	}
	
	private void nextLocation()
	{
		yco += 150;
		if (yco > 1000)
		{
			yco = 30;
			xco += 300;
		}
	}

	private PathwayElement createOrGetSpecies (String sId, double prefX, double prefY, GlyphClazz gc)
	{
		PathwayElement pelt = pwy.getElementById(sId);
		if (pelt == null)
		{
			Species sp = doc.getModel().getSpecies(sId);
			
			PeerSpecies sbr = PeerSpecies.createFromSpecies(this, sp, gc);
			putSpeciesPeer (sId, sbr);
			pelt = sbr.getSpeciesElement();
			pelt.setMCenterX(prefX);
			pelt.setMCenterY(prefY);
			pelt.setTextLabel(sId);
			Annotation annotation = doc.getModel().getSpecies(sId).getAnnotation();
			for (int i = 0; i < annotation.getCVTermCount(); i++) {
				
				List<String> li = annotation.getCVTerm(i).getResources();
				for (String string : li) {
					String[] de = string.split("org/",2 );
					String[] xe = de[1].split("/",2);
					 DataSource ds = DataSource.getByFullName(xe[0]);
				
					
					pelt.setDataSource(ds);
					pelt.setGeneID(xe[1]);
					
				}
			}
			
			pwy.add(pelt);
		}
		return pelt;
	}

	private double xco = 500;
	private double yco = 500;
	final static double M_WIDTH = 80;
	final static double M_HEIGHT = 30;
	final static double M_PN = 20;

}
