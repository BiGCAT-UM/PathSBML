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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.view.Graphics;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.gui.SwingEngine;
import org.pathvisio.sbgn.SbgnFormat;

/**
 * LayoutAbstract Class
 * 
 * Abstract class with functions used by all layout algorithms
 * 
 * @author applecool
 * @author anwesha
 * @version 1.0.0
 * 
 */
public abstract class LayoutAbstract {
	/**
	 * 
	 */
	public static String NAME;
	/**
	 * 
	 */
	public static String DESCRIPTION;
	private Pathway pwy;
	private VPathway vpwy;
	private SwingEngine swingEngine;
	boolean selection;
	List<PathwayElement> pwyNodes;
	List<PathwayElement> pwyLines;
	List<PathwayElement> pwyStates;

	LayoutAbstract(SwingEngine se){
		this(se,false);
	}
	LayoutAbstract(SwingEngine se,boolean selection){
		setPwy(se.getEngine().getActivePathway());
		setVpwy(se.getEngine().getActiveVPathway());
		setSwingEngine(se);
		setSelection(selection);
		setPwyNodes(new ArrayList<PathwayElement>());
		setPwyLines(new ArrayList<PathwayElement>());
		setPwyStates(new ArrayList<PathwayElement>());

		if (selection){
			List<Graphics> graphics = this.getVpwy().getSelectedGraphics();
			for (Graphics g : graphics){
				PathwayElement pe = g.getPathwayElement();
				if (pe.getObjectType().equals(ObjectType.DATANODE)|| pe.getObjectType().equals(ObjectType.LABEL)){
					this.getPwyNodes().add(pe);
				}
				else if (pe.getObjectType().equals(ObjectType.LINE)){
					if ("true".equals (pe.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_IS_PORT))) {
						this.getPwyStates().add(pe);
					} else {
						this.getPwyLines().add(pe);
					}
				}
			}
		}
		else {
			for (PathwayElement pe : getPwy().getDataObjects()) {
				if (pe.getObjectType().equals(ObjectType.DATANODE)||pe.getObjectType().equals(ObjectType.LABEL)){
					getPwyNodes().add(pe);
				}
				else if (pe.getObjectType().equals(ObjectType.LINE)){
					if ("true".equals (pe.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_IS_PORT))) {
						getPwyStates().add(pe);
					} else {
						getPwyLines().add(pe);
					}
				}
			}
		}
		for (PathwayElement pe : getPwyNodes()) {
			//Make sure each element has a unique graphId
			try {
				pe.getGraphId().isEmpty();
			}
			catch (NullPointerException e){
				pe.setGraphId(getPwy().getUniqueGraphId());
			}
		}
	}

	protected void setLocations(Map<String,Point2D> points){
		double plusx = 0;
		if (isSelection()) {
			// if it's the layout of a selection, first put the selection to 0, then put them on the right side of the other Pathway Elements
			for (PathwayElement pe : getPwyNodes()) {
				pe.setMCenterX(0);
			}
			PathwayElement lastx = getPwy().getDataObjects().get(0);
			for (PathwayElement pe : getPwy().getDataObjects()) {
				if (lastx.getMCenterX()<pe.getMCenterX()){
					lastx = pe;
				}
			}
			plusx = lastx.getMCenterX()+lastx.getMWidth()/2;
		}
		double minx = 0;
		double miny = 0;
		boolean first = true;
		for (Entry<String,Point2D> e : points.entrySet()){
			double x = e.getValue().getX();
			double y = e.getValue().getY();
			if (first){
				minx = x;
				miny = y;
				first = false;
			}
			else {
				if (x<minx){
					minx = x;
				}
				if (y<miny){
					miny = y;
				}
			}
		}
		for (Entry<String,Point2D> e : points.entrySet()){
			PathwayElement pe = getPwy().getElementById(e.getKey());
			if (minx<0){
				pe.setMCenterX(e.getValue().getX()+Math.abs(minx)+pe.getMWidth()/2+plusx);
			}
			else {
				pe.setMCenterX(e.getValue().getX()-minx+pe.getMWidth()/2+plusx);
			}
			if (miny<0){
				pe.setMCenterY(e.getValue().getY()+Math.abs(miny)+pe.getMHeight()/2);
			}
			else {
				pe.setMCenterY(e.getValue().getY()-Math.abs(miny)+pe.getMHeight()/2);
			}
		}
	}

	protected void drawLines(){
		for (PathwayElement line : getPwy().getDataObjects())
			if (line.getObjectType().equals(ObjectType.LINE)){
				PathwayElement startNode = getPwy().getElementById(
						line.getStartGraphRef());
				PathwayElement endNode = getPwy().getElementById(
						line.getEndGraphRef());

				line.getMStart().unlink();
				line.getMEnd().unlink();
				double differenceX;
				double differenceY;
				boolean startBiggerX = true;
				boolean startBiggerY = true;

				if (startNode.getMCenterX() < endNode.getMCenterX()){
					double endSide = endNode.getMCenterX() - endNode.getMWidth()/2;
					double startSide = startNode.getMCenterX() + startNode.getMWidth()/2;
					differenceX = endSide - startSide;
					startBiggerX = false;
				}
				else {
					double startSide = startNode.getMCenterX() - startNode.getMWidth()/2;
					double endSide = endNode.getMCenterX() + endNode.getMWidth()/2;
					differenceX = startSide - endSide;
				}
				if (startNode.getMCenterY() < endNode.getMCenterY()){
					double startSide = startNode.getMCenterY() + startNode.getMHeight()/2;
					double endSide = endNode.getMCenterY() - endNode.getMHeight()/2;
					differenceY = endSide - startSide;
					startBiggerY = false;
				}
				else {
					double startSide = startNode.getMCenterY() - startNode.getMHeight()/2;
					double endSide = endNode.getMCenterY() + endNode.getMHeight()/2;
					differenceY = startSide - endSide;
				}

				if (differenceX>differenceY && startBiggerX) {
					line.setMStartY(startNode.getMCenterY());
					line.setMStartX(startNode.getMCenterX() - startNode.getMWidth() / 2);
					line.setMEndY(endNode.getMCenterY());
					line.setMEndX(endNode.getMCenterX() + endNode.getMWidth() / 2);
				} else if (differenceX>differenceY && !startBiggerX) {
					line.setMStartY(startNode.getMCenterY());
					line.setMStartX(startNode.getMCenterX() + startNode.getMWidth() / 2);
					line.setMEndY(endNode.getMCenterY());
					line.setMEndX(endNode.getMCenterX() - endNode.getMWidth() / 2);
				} else if (differenceX<differenceY && startBiggerY){
					line.setMStartX(startNode.getMCenterX());
					line.setMStartY((startNode.getMCenterY() - startNode.getMHeight() /2));
					line.setMEndX(endNode.getMCenterX());
					line.setMEndY(endNode.getMCenterY() + startNode.getMHeight() /2);
				} else {
					line.setMStartX(startNode.getMCenterX());
					line.setMStartY(startNode.getMCenterY() + startNode.getMHeight() / 2);
					line.setMEndX(endNode.getMCenterX());
					line.setMEndY(endNode.getMCenterY() - endNode.getMHeight() / 2);
				}
				line.getMStart().linkTo(startNode);
				line.getMEnd().linkTo(endNode);
			}
	}
	private String getSpecies(String graphId) {
		for (PathwayElement pe : getPwy().getDataObjects()) {
			if (pe.getObjectType().equals(ObjectType.LINE) ){
				if (!("true".equals (pe.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_IS_PORT))))
				{
					if(pe.getEndGraphRef().equalsIgnoreCase(graphId))
						return pe.getStartGraphRef();

					if(pe.getStartGraphRef().equalsIgnoreCase(graphId))

						return pe.getEndGraphRef();
				}
			}
		}
		return "";
	}

	protected String getReaction(String graphId) {
		for (PathwayElement pe : getPwy().getDataObjects()) {
			if (pe.getObjectType().equals(ObjectType.LINE) ){
				if (("true".equals (pe.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_IS_PORT))))
				{
					if(pe.getMAnchors().get(0).getGraphId().equalsIgnoreCase(graphId))
						return pe.getStartGraphRef();


				}
			}
		}
		return "";
	}

	protected void drawStates(){
		for (PathwayElement line : getPwy().getDataObjects())
			if (line.getObjectType().equals(ObjectType.LINE)){
				if ("true".equals (line.getDynamicProperty(SbgnFormat.PROPERTY_SBGN_IS_PORT)))
				{
					PathwayElement startNode = getPwy().getElementById(
							line.getStartGraphRef());
					PathwayElement endNode = getPwy().getElementById(
							getSpecies(line.getMAnchors().get(0).getGraphId()));

					line.getMStart().unlink();
					line.getMEnd().unlink();
					double differenceX;
					double differenceY;
					boolean startBiggerX = true;
					boolean startBiggerY = true;

					if (startNode.getMCenterX() < endNode.getMCenterX()){
						double endSide = endNode.getMCenterX() - endNode.getMWidth()/2;
						double startSide = startNode.getMCenterX() + startNode.getMWidth()/2;
						differenceX = endSide - startSide;
						startBiggerX = false;
					}
					else {
						double startSide = startNode.getMCenterX() - startNode.getMWidth()/2;
						double endSide = endNode.getMCenterX() + endNode.getMWidth()/2;
						differenceX = startSide - endSide;
					}
					if (startNode.getMCenterY() < endNode.getMCenterY()){
						double startSide = startNode.getMCenterY() + startNode.getMHeight()/2;
						double endSide = endNode.getMCenterY() - endNode.getMHeight()/2;
						differenceY = endSide - startSide;
						startBiggerY = false;
					}
					else {
						double startSide = startNode.getMCenterY() - startNode.getMHeight()/2;
						double endSide = endNode.getMCenterY() + endNode.getMHeight()/2;
						differenceY = startSide - endSide;
					}

					if (differenceX>differenceY && startBiggerX) {
						line.setMStartY(startNode.getMCenterY());
						line.setMStartX(startNode.getMCenterX() - startNode.getMWidth() / 2);
						line.setMEndY(endNode.getMCenterY());
						line.setMEndX(endNode.getMCenterX() + endNode.getMWidth() / 2);
					} else if (differenceX>differenceY && !startBiggerX) {
						line.setMStartY(startNode.getMCenterY());
						line.setMStartX(startNode.getMCenterX() + startNode.getMWidth() / 2);
						line.setMEndY(endNode.getMCenterY());
						line.setMEndX(endNode.getMCenterX() - endNode.getMWidth() / 2);
					} else if (differenceX<differenceY && startBiggerY){
						line.setMStartX(startNode.getMCenterX());
						line.setMStartY((startNode.getMCenterY() - startNode.getMHeight() /2));
						line.setMEndX(endNode.getMCenterX());
						line.setMEndY(endNode.getMCenterY() + startNode.getMHeight() /2);
					} else {
						line.setMStartX(startNode.getMCenterX());
						line.setMStartY(startNode.getMCenterY() + startNode.getMHeight() / 2);
						line.setMEndX(endNode.getMCenterX());
						line.setMEndY(endNode.getMCenterY() - endNode.getMHeight() / 2);
					}
					line.getMStart().linkTo(startNode);
					line.getMEnd().linkTo(endNode);
				}
			}
	}

	/**
	 * @return the pwy
	 */
	public Pathway getPwy() {
		return this.pwy;
	}

	/**
	 * @param pwy
	 *            the pwy to set
	 */
	public void setPwy(Pathway pwy) {
		this.pwy = pwy;
	}

	/**
	 * @return the pwyNodes
	 */
	public List<PathwayElement> getPwyNodes() {
		return this.pwyNodes;
	}

	/**
	 * @param pwyNodes
	 *            the pwyNodes to set
	 */
	public void setPwyNodes(List<PathwayElement> pwyNodes) {
		this.pwyNodes = pwyNodes;
	}

	/**
	 * @return the pwyStates
	 */
	public List<PathwayElement> getPwyStates() {
		return this.pwyStates;
	}

	/**
	 * @param pwyStates
	 *            the pwyStates to set
	 */
	public void setPwyStates(List<PathwayElement> pwyStates) {
		this.pwyStates = pwyStates;
	}

	/**
	 * @return the pwyLines
	 */
	public List<PathwayElement> getPwyLines() {
		return this.pwyLines;
	}

	/**
	 * @param pwyLines
	 *            the pwyLines to set
	 */
	public void setPwyLines(List<PathwayElement> pwyLines) {
		this.pwyLines = pwyLines;
	}

	/**
	 * @return the selection
	 */
	public boolean isSelection() {
		return this.selection;
	}

	/**
	 * @param selection
	 *            the selection to set
	 */
	public void setSelection(boolean selection) {
		this.selection = selection;
	}
	/**
	 * @return the vpwy
	 */
	public VPathway getVpwy() {
		return this.vpwy;
	}
	/**
	 * @param vpwy the vpwy to set
	 */
	public void setVpwy(VPathway vpwy) {
		this.vpwy = vpwy;
	}
	/**
	 * @return the swingEngine
	 */
	public SwingEngine getSwingEngine() {
		return this.swingEngine;
	}
	/**
	 * @param swingEngine the swingEngine to set
	 */
	public void setSwingEngine(SwingEngine swingEngine) {
		this.swingEngine = swingEngine;
	}
}