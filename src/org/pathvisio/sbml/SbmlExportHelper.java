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



public class SbmlExportHelper
{
	private final Pathway pathway;
	private final File file;
	
	private Map<String, String> portmatrix = new HashMap<String,String>();
	private Map<String, String> reactionmatrix = new HashMap<String,String>();
	ListOf<SpeciesReference> listOfSpeciesReferences = new ListOf<SpeciesReference>();

	ListOf<Species> listOfSpecies = new ListOf<Species>();
	private ListOf<org.sbml.jsbml.Reaction> listOfReactions = new ListOf<org.sbml.jsbml.Reaction>();

	SbmlExportHelper (Pathway pathway, File file)
	{
		this.pathway = pathway;
		this.file = file;		
	}
	


	public void doExport() throws ConverterException
	{
			
		makePorts();
		for (PathwayElement elt : pathway.getDataObjects())
		{
			addSpeciesReferences(elt);
		
		}
		for (PathwayElement elt : pathway.getDataObjects())
		{
			addElement(elt);
		
		}
	SBMLDocument doc= new SBMLDocument();
	doc.setModel(doModel());
	doc.setLevelAndVersion(3, 1);
	
	try {
		 SBMLWriter w=new SBMLWriter();
		 
		w.writeSBMLToFile(doc, file.getAbsolutePath());
	} catch (SBMLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (XMLStreamException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	
	public void addSpeciesReferences(PathwayElement elt)
	{
		for (Map.Entry<String,String> entry : portmatrix.entrySet()) {
		   if(entry.getKey().equals(elt.getStartGraphRef()))
			  reactionmatrix.put( entry.getValue()+"-"+elt.getEndGraphRef()+"-1",elt.getEndGraphRef());//reaction,species
		   if(entry.getKey().equals(elt.getEndGraphRef()))
				  reactionmatrix.put(entry.getValue()+"-"+elt.getStartGraphRef()+"-2",elt.getStartGraphRef());//reaction,species
			   
				   
		}
	}
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
					addSpecies (elt, gc);
					
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
		int i = 0;
	
		for (PathwayElement elt : pathway.getDataObjects())
		{
			switch (elt.getObjectType())
			{
				case LINE:
				{
					// ports have been dealt with already, skip.
					if ("true".equals (elt.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_IS_PORT)))
					portmatrix.put(elt.getMAnchors().get(0).getGraphId(),elt.getStartGraphRef());
		
				
					}
			}
		}

	}
	

	private void addSpecies(PathwayElement elt, GlyphClazz gc)
	{ 
		Species sp= new Species();		
		sp.setId(elt.getGraphId());
		listOfSpecies.add(sp);
		
	}
	private void addReaction(PathwayElement elt)
	{
		org.sbml.jsbml.Reaction r= new org.sbml.jsbml.Reaction();
		r.setId(elt.getGraphId());
		for (Map.Entry<String,String> entry : reactionmatrix.entrySet()) {
			
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
		
		listOfReactions.add(r);
	}
	


	/** checks if the given SBML document uses the SBML-layout extension */
	private Model doModel()
	{
		Model model =new Model();

		model.setListOfSpecies(listOfSpecies);	
		model.setListOfReactions(listOfReactions);
	
	
		return model;
	}

}