package org.pathvisio.sbml.peer;

import org.pathvisio.core.model.GraphLink.GraphIdContainer;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.model.PathwayElementEvent;
import org.pathvisio.core.model.PathwayElementListener;
import org.pathvisio.sbgn.SbgnTemplates;
import org.sbgn.ArcClazz;
import org.sbml.jsbml.SimpleSpeciesReference;

public class PeerSpeciesReference implements PathwayElementListener
{
	private SimpleSpeciesReference sref;
	private PathwayElement elt;
	private PeerModel parent;
	
	public PeerSpeciesReference(PeerModel parent, PathwayElement elt, SimpleSpeciesReference sref)
	{
		this.parent = parent;
		this.elt = elt;
		this.sref = sref;
		elt.addListener(this);
	}
	
	public static PeerSpeciesReference createFromSpeciesReference(PeerModel parent, SimpleSpeciesReference sref, ArcClazz arcClazz, 
			double sx, double sy, GraphIdContainer sid, double ex, double ey, GraphIdContainer eid)
	{
		PathwayElement elt = SbgnTemplates.createArc(parent.getPathway(), arcClazz, sx, sy, sid, ex, ey, eid);
		PeerSpeciesReference result = new PeerSpeciesReference(parent, elt, sref);
		result.updatePv();
		return result;
	}
	
	public void updateSbml()
	{
	}
	
	public void updatePv()
	{
		
	}

	@Override
	public void gmmlObjectModified(PathwayElementEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	public PathwayElement getElement()
	{
		return elt;
	}

}
