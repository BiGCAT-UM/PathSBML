package org.pathvisio.sbml.peer;

import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElementEvent;
import org.pathvisio.core.model.PathwayElementListener;
import org.pathvisio.sbgn.SbgnTemplates;
import org.sbgn.GlyphClazz;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.Dimensions;
import org.sbml.jsbml.ext.layout.Point;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;

public class PeerSpecies implements PathwayElementListener
{
	private PathwayElement elt;
	private Species sp;
	private SpeciesGlyph sg;

	public PeerSpecies(PathwayElement elt, Species sp)
	{
		this.elt = elt;
		this.sp = sp;
		elt.addListener(this);
	}
	
	public static PeerSpecies createFromSpecies(PeerModel parent, Species sp, GlyphClazz gc)
	{
		PathwayElement elt = SbgnTemplates.createGlyph(gc, parent.getPathway(), 0, 0);
		elt.setGraphId(sp.getId());		
		PeerSpecies bs = new PeerSpecies(elt, sp);
		bs.updateElt();
		return bs;
	}

	public static PeerSpecies createFromElt (PeerModel parent, PathwayElement elt, GlyphClazz gc)
	{
		Species sp = parent.getModel().createSpecies(elt.getGraphId());
		PeerSpecies bs = new PeerSpecies(elt, sp);
		bs.updateSpecies();
		return bs;
	}

	public PathwayElement getSpeciesElement()
	{
		return elt;
	}

	@Override
	public void gmmlObjectModified(PathwayElementEvent e)
	{
		updateSpecies();
	}

	private boolean updatingSpecies = false;
	private boolean updatingElt = false;
	
	private void updateElt()
	{
		if (updatingSpecies) return;
		
		try
		{
			updatingElt = true;
			
			elt.setTextLabel(sp.getName());
			if (sg != null)
			{
				BoundingBox bb = sg.getBoundingBox();
				Point p = bb.getPosition();
				if (p != null)
				{
					elt.setMCenterX(p.getX());
					elt.setMCenterY(p.getY());
				}
				Dimensions d = bb.getDimensions();
				if (d != null)
				{
					elt.setMWidth(d.getWidth());
					elt.setMHeight(d.getHeight());
				}
			}
		}
		finally
		{
			updatingElt = false;
		}
	}
	
	private void updateSpecies()
	{
		if (updatingElt) return;
		
		try
		{
			updatingSpecies = true;
			sp.setName(elt.getTextLabel());
			if (sg != null)
			{
				BoundingBox bb = sg.getBoundingBox();
				if (bb == null) bb = sg.createBoundingBox();
				
				Point p = bb.getPosition();
				if (p == null) bb.createPosition();
					
				p.setX(elt.getMCenterX());
				p.setY(elt.getMCenterY());

				Dimensions d = bb.getDimensions();
				if (d == null) bb.createDimensions();
				
				d.setWidth(elt.getMWidth());
				d.setHeight(elt.getMHeight());
			}
		}
		finally
		{
			updatingSpecies = false;
		}
	}

	public void setSpeciesGlyph(SpeciesGlyph g)
	{
		this.sg = g;
		updateElt();
	}
	
}
