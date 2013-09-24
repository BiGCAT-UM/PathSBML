package org.pathvisio.sbml.peer;

import org.pathvisio.core.model.GraphLink.GraphIdContainer;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.sbgn.SbgnTemplates;
import org.sbgn.GlyphClazz;
import org.sbml.jsbml.Reaction;

public class PeerReaction
{
	private PathwayElement pn;
	private PathwayElement port1;
	private PathwayElement port2;

	private Reaction r;
	private MAnchor pid1;
	private MAnchor pid2;

	public static PeerReaction createFromSbml(PeerModel parent, Reaction re, double x, double y)
	{
		PeerReaction pr = new PeerReaction();

		// create a process node for the reaction.
		Pathway pwy = parent.getPathway();
		PathwayElement[] process = SbgnTemplates.createProcessNode(pwy, GlyphClazz.PROCESS, x, y, PeerModel.M_PN, re.getId());
		for (PathwayElement elt : process) pwy.add(elt);
		pr.pn = process[2];
		pr.port1 = process[0];
		pr.port2 = process[1];
		pr.pid1 = process[0].getMAnchors().get(0);
		pr.pid2 = process[1].getMAnchors().get(0);

		return pr;
	}


	public GraphIdContainer getPortId(int i)
	{
		switch (i)
		{
		case 0:
			return pid1;
		case 1:
			return pid2;
		default:
			throw new IndexOutOfBoundsException();
		}
	}


	public GraphIdContainer getProcessNodeElt()
	{
		return pn;
	}
}
