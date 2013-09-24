
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
 * Prefuse Class<p>
 * Implements the Force-Directed layout algorithm from the Prefuse package.
 * @author applecool
 *
 */
public class Prefuse extends LayoutAbstract{
	
	public int numIterations = 100;
	public float defaultSpringCoefficient = 1e-4f;
	public float defaultSpringLength = 100.0f;
	public double defaultNodeMass = 3.0;
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
		for (PathwayElement pe: pwyNodes){
			ForceItem item = new ForceItem();
			item.location[0] = (float) pe.getMCenterX();
			item.location[1] = (float) pe.getMCenterY();
			nodes.put(pe.getGraphId(),item);
			f.addItem(item);
			
		}
		

	
		for (PathwayElement pe: pwyLines){
			String start="",end="";
			if( pwy.getElementById(pe.getStartGraphRef())==null )
			{
			 start =(getReaction(pe.getStartGraphRef()));
			}else{
			start=pe.getStartGraphRef();
			}
			if( pwy.getElementById(pe.getEndGraphRef())==null )
			{
				 end =(getReaction(pe.getEndGraphRef()));
			}
			else{
				end=pe.getEndGraphRef();
			}
			
			float springLength = pythagoras(pwy.getElementById(start).getMWidth()/2, pwy.getElementById(start).getMHeight()/2) + pythagoras(pwy.getElementById(end).getMWidth()/2,pwy.getElementById(end).getMHeight()/2);
			f.addSpring(nodes.get(start), nodes.get(end), defaultSpringCoefficient, springLength);
		}
		
		
		
		l.setForceSimulator(f);
		long timestep = 1000L;
		for (int i=0;i<numIterations; i++){
			timestep *= (1.0 - i/(double)numIterations);
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

}