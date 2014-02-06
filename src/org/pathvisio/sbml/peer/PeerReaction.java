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
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.sbgn.SbgnTemplates;
import org.sbgn.GlyphClazz;
import org.sbml.jsbml.Reaction;

/**
 * @author anwesha
 * 
 */
public class PeerReaction
{
	private PathwayElement pn;
	private MAnchor pid1;
	private MAnchor pid2;

	/**
	 * @param parent
	 * @param re
	 * @param x
	 * @param y
	 * @return
	 */
	public static PeerReaction createFromSbml(final PeerModel parent,
			final Reaction re, final double x, final double y)
	{
		PeerReaction pr = new PeerReaction();

		// create a process node for the reaction.
		Pathway pwy = parent.getPathway();
		PathwayElement[] process = SbgnTemplates.createProcessNode(pwy, GlyphClazz.PROCESS, x, y, PeerModel.M_PN, re.getId());
		for (PathwayElement elt : process) {
			pwy.add(elt);
		}
		pr.setPn(process[2]);
		pr.setPid1(process[0].getMAnchors().get(0));
		pr.setPid2(process[1].getMAnchors().get(0));

		return pr;
	}


	/**
	 * @param i
	 * @return
	 */
	public GraphIdContainer getPortId(final int i)
	{
		switch (i)
		{
		case 0:
			return getPid1();
		case 1:
			return getPid2();
		default:
			throw new IndexOutOfBoundsException();
		}
	}


	/**
	 * @return
	 */
	public GraphIdContainer getProcessNodeElt()
	{
		return getPn();
	}

	/**
	 * @return the pid1
	 */
	public MAnchor getPid1() {
		return this.pid1;
	}

	/**
	 * @param pid1
	 *            the pid1 to set
	 */
	public void setPid1(MAnchor pid1) {
		this.pid1 = pid1;
	}

	/**
	 * @return the pid2
	 */
	public MAnchor getPid2() {
		return this.pid2;
	}

	/**
	 * @param pid2
	 *            the pid2 to set
	 */
	public void setPid2(MAnchor pid2) {
		this.pid2 = pid2;
	}

	/**
	 * @return the pn
	 */
	public PathwayElement getPn() {
		return this.pn;
	}

	/**
	 * @param pn
	 *            the pn to set
	 */
	public void setPn(PathwayElement pn) {
		this.pn = pn;
	}
}
