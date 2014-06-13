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

import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElementEvent;
import org.pathvisio.core.model.PathwayElementListener;
import org.pathvisio.sbgn.SbgnTemplates;
import org.sbgn.GlyphClazz;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.Dimensions;
import org.sbml.jsbml.ext.layout.Point;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;

public class PeerSpecies implements PathwayElementListener
{
	private final PathwayElement pathwayElement;
	private final Species nodes;
	private SpeciesGlyph nodeGlyphs;

	public PeerSpecies(PathwayElement elt, Species sp)
	{
		this.pathwayElement = elt;
		this.nodes = sp;
		elt.addListener(this);
	}

	public static PeerSpecies createFromSpecies(PeerModel parent, Species sp,
			GlyphClazz gc)
	{
		PathwayElement elt = SbgnTemplates.createGlyph(gc, parent.getPathway(),
				0, 0);
		elt.setGraphId(sp.getId());

		PeerSpecies bs = new PeerSpecies(elt, sp);
		bs.updateElt();
		return bs;
	}

	/*
	 * Save all infiormation available in the model in the pathway
	 */
	private void addInfo(PathwayElement elt, Species sp) {
		if (sp.isSetName()) {
			elt.setTextLabel(sp.getName());
		}
		if (sp.isSetBoundaryCondition()) {
			elt.setDynamicProperty("BoundaryCondition",
					String.valueOf(sp.getBoundaryCondition()));
		}
		if (sp.isHasOnlySubstanceUnits()) {
			elt.setDynamicProperty("HasOnlySubstanceUnits",
					sp.getSubstanceUnits());
		}

	}
	public static PeerSpecies createFromElt(PeerModel parent,
			PathwayElement elt, GlyphClazz gc)
	{
		Species sp = parent.getModel().createSpecies(elt.getGraphId());
		PeerSpecies bs = new PeerSpecies(elt, sp);
		bs.updateSpecies();
		return bs;
	}

	public PathwayElement getSpeciesElement()
	{
		return pathwayElement;
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

			pathwayElement.setTextLabel(nodes.getName());
			if (nodeGlyphs != null)
			{
				BoundingBox bb = nodeGlyphs.getBoundingBox();
				Point p = bb.getPosition();
				if (p != null)
				{
					pathwayElement.setMCenterX(p.getX());
					pathwayElement.setMCenterY(p.getY());
				}
				Dimensions d = bb.getDimensions();
				if (d != null)
				{
					pathwayElement.setMWidth(d.getWidth());
					pathwayElement.setMHeight(d.getHeight());
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
			nodes.setName(pathwayElement.getTextLabel());
			if (nodeGlyphs != null)
			{
				BoundingBox bb = nodeGlyphs.getBoundingBox();
				if (bb == null) {
					bb = nodeGlyphs.createBoundingBox();
				}

				Point p = bb.getPosition();
				if (p == null) {
					bb.createPosition();
				}

				p.setX(pathwayElement.getMCenterX());
				p.setY(pathwayElement.getMCenterY());

				Dimensions d = bb.getDimensions();
				if (d == null) {
					bb.createDimensions();
				}

				d.setWidth(pathwayElement.getMWidth());
				d.setHeight(pathwayElement.getMHeight());
			}
		}
		finally
		{
			updatingSpecies = false;
		}
	}

	public void setSpeciesGlyph(SpeciesGlyph g)
	{
		this.nodeGlyphs = g;
		updateElt();
	}

}