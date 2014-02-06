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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.sbgn.SbgnFormat;
import org.sbgn.ArcClazz;
import org.sbgn.GlyphClazz;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;



/**
 * @author applecool
 * @author anwesha
 * 
 */
public class SbmlExportHelper
{
	private final Pathway pathway;
	private final File file;

	private final Map<String, String> portmatrix = new HashMap<String,String>();
	private final Map<String, String> reactionmatrix = new HashMap<String,String>();
	ListOf<SpeciesReference> listOfSpeciesReferences = new ListOf<SpeciesReference>();

	private ListOf<Species> listOfSpecies = new ListOf<Species>();
	private final ListOf<org.sbml.jsbml.Reaction> listOfReactions = new ListOf<org.sbml.jsbml.Reaction>();

	SbmlExportHelper (Pathway pathway, File file)
	{
		this.pathway = pathway;
		this.file = file;
	}



	/**
	 * @throws ConverterException
	 */
	public void doExport() throws ConverterException
	{

		makePorts();
		for (PathwayElement elt : getPathway().getDataObjects())
		{
			addSpeciesReferences(elt);

		}
		for (PathwayElement elt : getPathway().getDataObjects())
		{
			addElement(elt);

		}
		SBMLDocument doc= new SBMLDocument();
		doc.setModel(doModel());
		doc.setLevelAndVersion(3, 1);

		try {
			SBMLWriter w=new SBMLWriter();

			w.writeSBMLToFile(doc, getFile().getAbsolutePath());
		} catch (SBMLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param elt
	 */
	public void addSpeciesReferences(PathwayElement elt)
	{
		for (Map.Entry<String,String> entry : getPortmatrix().entrySet()) {
			if(entry.getKey().equals(elt.getStartGraphRef()))
			{
				getReactionmatrix().put( entry.getValue()+"-"+elt.getEndGraphRef()+"-1",elt.getEndGraphRef());//reaction,species
			}
			if(entry.getKey().equals(elt.getEndGraphRef()))
			{
				getReactionmatrix().put(entry.getValue()+"-"+elt.getStartGraphRef()+"-2",elt.getStartGraphRef());//reaction,species
			}


		}
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
					//	addSpeciesReference (elt);
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
				{
					addSpecies(elt);

				}
				break;
				}
			}
		}
	}



	/**
	 * Creates the Map for each ProcessNode (i.e) Map of each ProcessNode with
	 * its COnnected line-GraphId, starting Graph Reference,
	 */
	protected void makePorts() {
		for (PathwayElement elt : getPathway().getDataObjects())
		{
			switch (elt.getObjectType())
			{
			case LINE:
			{
				// ports have been dealt with already, skip.
				if ("true".equals (elt.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_IS_PORT))) {
					getPortmatrix().put(elt.getMAnchors().get(0).getGraphId(),elt.getStartGraphRef());
				}


			}
			}
		}

	}


	private void addSpecies(PathwayElement elt)
	{
		Species sp= new Species();
		sp.setId(elt.getGraphId());
		getListOfSpecies().add(sp);

	}
	private void addReaction(PathwayElement elt)
	{
		org.sbml.jsbml.Reaction r= new org.sbml.jsbml.Reaction();
		r.setId(elt.getGraphId());
		for (Map.Entry<String,String> entry : getReactionmatrix().entrySet()) {

			String[] s = entry.getKey().split("-",3);
			if(s[2].equalsIgnoreCase("2"))
			{
				if(elt.getGraphId().equals(s[0]))
				{

					SpeciesReference sr= r.createReactant();
					sr.setSpecies(new Species(s[1]));


				}
			}
			if(s[2].equalsIgnoreCase("1"))
			{
				if(elt.getGraphId().equals(s[0]))
				{

					SpeciesReference sr= r.createProduct();
					sr.setSpecies(new Species(s[1]));


				}
			}
		}

		getListOfReactions().add(r);
	}



	/** checks if the given SBML document uses the SBML-layout extension */
	private Model doModel()
	{
		Model model =new Model();

		model.setListOfSpecies(getListOfSpecies());
		model.setListOfReactions(getListOfReactions());


		return model;
	}



	/**
	 * @return the pathway
	 */
	public Pathway getPathway() {
		return this.pathway;
	}



	/**
	 * @return the file
	 */
	public File getFile() {
		return this.file;
	}



	/**
	 * @return the portmatrix
	 */
	public Map<String, String> getPortmatrix() {
		return this.portmatrix;
	}



	/**
	 * @return the reactionmatrix
	 */
	public Map<String, String> getReactionmatrix() {
		return this.reactionmatrix;
	}



	/**
	 * @return the listOfSpecies
	 */
	public ListOf<Species> getListOfSpecies() {
		return this.listOfSpecies;
	}



	/**
	 * @param listOfSpecies the listOfSpecies to set
	 */
	public void setListOfSpecies(ListOf<Species> listOfSpecies) {
		this.listOfSpecies = listOfSpecies;
	}



	/**
	 * @return the listOfReactions
	 */
	public ListOf<org.sbml.jsbml.Reaction> getListOfReactions() {
		return this.listOfReactions;
	}

}