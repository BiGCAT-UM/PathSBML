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

	public static PeerReaction createFromSbml(PeerModel parent, Reaction re,
			double x, double y)
	{
		PeerReaction pr = new PeerReaction();

		// create a process node for the reaction.
		Pathway pwy = parent.getPathway();
		PathwayElement[] process = SbgnTemplates.createProcessNode(pwy,
				GlyphClazz.PROCESS, x, y, PeerModel.M_PN, re.getId());
		for (PathwayElement elt : process) {
			pwy.add(elt);
		}
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

	public PathwayElement getProcessNodeElt() {
		return pn;
	}

}