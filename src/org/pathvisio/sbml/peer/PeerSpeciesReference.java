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

import org.pathvisio.core.model.GraphLink.GraphIdContainer;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElementEvent;
import org.pathvisio.core.model.PathwayElementListener;
import org.pathvisio.sbgn.SbgnTemplates;
import org.sbgn.ArcClazz;
import org.sbml.jsbml.SimpleSpeciesReference;

public class PeerSpeciesReference implements PathwayElementListener
{
	private final PathwayElement pathwayELement;
	public PeerSpeciesReference(PeerModel parent, PathwayElement elt, SimpleSpeciesReference sref)
	{
		this.pathwayELement = elt;
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
		return pathwayELement;
	}

}
