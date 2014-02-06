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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.gui.SwingEngine;

import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.util.force.DragForce;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.force.SpringForce;

/**
 * Prefuse Class Implements the Force-Directed layout algorithm from the Prefuse
 * package.
 * 
 * @author applecool
 * @author anwesha
 * @version 1.0.0
 * 
 */
public class Prefuse extends LayoutAbstract{

	/**
	 * 
	 */
	private int numIterations = 100;
	/**
	 * 
	 */
	private float defaultSpringCoefficient = 1e-4f;
	/**
	 * 
	 */
	public float defaultSpringLength = 100.0f;
	/**
	 * 
	 */
	public double defaultNodeMass = 3.0;
	/**
	 * 
	 */
	public boolean isDeterministic;

	/**
	 * create a new prefuse Force-Directed Layout.
	 * @param swingEngine The PathVisio swing engine
	 * @param selection Boolean whether to use currently selected nodes or complete pathway
	 */
	public Prefuse(SwingEngine swingEngine, boolean selection){
		super(swingEngine,selection);
		ForceDirectedLayout l = new ForceDirectedLayout("Layout");
		ForceSimulator f = new ForceSimulator();
		f.addForce(new NBodyForce());
		f.addForce(new SpringForce());
		f.addForce(new DragForce());
		Map<String,ForceItem> nodes = new HashMap<String,ForceItem>();
		for (PathwayElement pe: getPwyNodes()){
			ForceItem item = new ForceItem();
			item.location[0] = (float) pe.getMCenterX();
			item.location[1] = (float) pe.getMCenterY();
			nodes.put(pe.getGraphId(),item);
			f.addItem(item);

		}



		for (PathwayElement pe: getPwyLines()){
			String start="",end="";
			if( getPwy().getElementById(pe.getStartGraphRef())==null )
			{
				start =(getReaction(pe.getStartGraphRef()));
			}else{
				start=pe.getStartGraphRef();
			}
			if( getPwy().getElementById(pe.getEndGraphRef())==null )
			{
				end =(getReaction(pe.getEndGraphRef()));
			}
			else{
				end=pe.getEndGraphRef();
			}

			float springLength = pythagoras(getPwy().getElementById(start).getMWidth()/2, getPwy().getElementById(start).getMHeight()/2) + pythagoras(getPwy().getElementById(end).getMWidth()/2,getPwy().getElementById(end).getMHeight()/2);
			f.addSpring(nodes.get(start), nodes.get(end), getDefaultSpringCoefficient(), springLength);
		}



		l.setForceSimulator(f);
		long timestep = 1000L;
		for (int i=0;i<getNumIterations(); i++){
			timestep *= (1.0 - i/(double)getNumIterations());
			long step = timestep+50;
			f.runSimulator(step);
		}
		Map<String,Point2D> points = new HashMap<String,Point2D>();
		for (Entry<String,ForceItem> e : nodes.entrySet()){
			points.put(e.getKey(), new Point2D.Float(e.getValue().location[0], e.getValue().location[1]));
		}
		setLocations(points);
		//	drawStates();
		//drawLines();

	}

	/**
	 * calculate the length of the hypotenuse
	 * @param a length of side a
	 * @param b length of side b
	 * @return length of the hypotenuse
	 */
	public static float pythagoras(double a, double b){

		return (float)Math.sqrt(Math.pow(a,2) + Math.pow(b, 2));
	}

	/**
	 * @return the defaultSpringCoefficient
	 */
	public float getDefaultSpringCoefficient() {
		return this.defaultSpringCoefficient;
	}

	/**
	 * @param defaultSpringCoefficient the defaultSpringCoefficient to set
	 */
	public void setDefaultSpringCoefficient(float defaultSpringCoefficient) {
		this.defaultSpringCoefficient = defaultSpringCoefficient;
	}

	/**
	 * @return the numIterations
	 */
	public int getNumIterations() {
		return this.numIterations;
	}

	/**
	 * @param numIterations the numIterations to set
	 */
	public void setNumIterations(int numIterations) {
		this.numIterations = numIterations;
	}

}